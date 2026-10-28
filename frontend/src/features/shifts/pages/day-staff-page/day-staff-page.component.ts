import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, distinctUntilChanged, filter, of, switchMap, tap } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { AvatarComponent } from '../../../../atomic-design/atoms/avatar/avatar.component';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { SidePanelComponent } from '../../../../atomic-design/organisms/side-panel/side-panel.component';
import { DayStaffService } from '../../../../core/services/day-staff.service';
import { JournalService } from '../../../../core/services/journal.service';
import { ShiftsScheduleService } from '../../../../core/services/shifts-schedule.service';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import {
  capitalize,
  longDateLabel,
  prefersReducedMotion,
  segmentOnOperationalDay,
  segmentsLabel
} from '../../../../shared/helpers/shift-display.helpers';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import {
  DayStaffNovelty,
  DayStaffStepResponse,
  NoveltyType,
  RegisterNoveltyRequest,
  Step1Status
} from '../../../../shared/models/dtos/day-staff.dto';
import { StepStatus } from '../../../../shared/models/journal.model';
import { NoveltyContext, NoveltyFormComponent } from '../../components/novelty-form/novelty-form.component';
import { NoveltyItem, NoveltyListComponent } from '../../components/novelty-list/novelty-list.component';

const TOAST_DURATION_MS = 3000;
/** Igual que --duration-normal: la tarjeta anulada termina de plegarse antes de salir de la lista. */
const FOLD_DURATION_MS = 320;
const STEP_NUMBER = 1;

const NOVELTY_LABELS: Record<NoveltyType, string> = {
  REPLACEMENT: 'Reemplazo',
  ABSENCE: 'Ausencia',
  SHIFT_CHANGE: 'Cambio de turno',
  SWAP: 'Intercambio'
};

/** Estado del paso, resumido en una etiqueta junto al título. */
interface StepState {
  tone: 'pending' | 'ok' | 'warn' | 'locked';
  label: string;
  /** Solo cuando hace falta explicar algo que no se ve en pantalla. */
  note: string;
}

/** Una persona de la lista del día, con su franja horaria ya calculada. */
interface DayPerson {
  workerId: string;
  fullName: string;
  code: string;
  gridCode: string;
  changed: boolean;
  hours: string;
  totalHours: number;
  roleLabel: string;
  dispenser: number | null;
  support: boolean;
  bars: { left: number; width: number; label: string }[];
  novelty: string;
  noveltyReason: string | null;
  noveltyWith: string | null;
  /** 'works' trabaja · 'absent' no trabaja por una novedad · 'resting' descansa. */
  kind: 'works' | 'absent' | 'resting';
}


/** Paso 1 de la jornada: quién trabaja hoy según la malla, novedades de último momento y confirmación. */
@Component({
  selector: 'app-day-staff-page',
  standalone: true,
  imports: [
    RouterLink,
    ActionButtonDirective,
    AvatarComponent,
    ShiftChipComponent,
    EmptyStateComponent,
    SidePanelComponent,
    NoveltyFormComponent,
    NoveltyListComponent
  ],
  templateUrl: './day-staff-page.component.html',
  styleUrl: './day-staff-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DayStaffPageComponent {
  private readonly dayStaffService = inject(DayStaffService);
  private readonly journalService = inject(JournalService);
  private readonly scheduleService = inject(ShiftsScheduleService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** La fecha sale de la jornada que abrió el backend; nunca se recalcula aquí. */
  readonly journal = toSignal(this.journalService.currentJournal$, { initialValue: null });
  readonly date = computed(() => this.journal()?.date ?? null);

  readonly step = signal<DayStaffStepResponse | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly catalog = signal<ShiftCode[]>([]);

  readonly panelContext = signal<NoveltyContext | null>(null);
  readonly panelOpen = computed(() => this.panelContext() !== null);
  readonly saving = signal(false);
  readonly submitError = signal<unknown>(null);
  readonly submitMessage = signal<string | null>(null);

  readonly confirming = signal(false);
  readonly confirmError = signal<string | null>(null);
  /** Novedad que pide "¿Anular?" en línea antes de enviarse. */
  readonly askingCancelId = signal<string | null>(null);
  /** Novedades que se pliegan mientras el servidor responde; vuelven a abrirse si lo rechaza. */
  readonly foldingIds = signal<ReadonlySet<string>>(new Set());
  readonly cancelError = signal<string | null>(null);
  /** Filas que acaban de recibir un cambio: se resaltan un momento para que se vea dónde cayó. */
  readonly flashIds = signal<ReadonlySet<string>>(new Set());

  readonly toastText = signal('');
  readonly toastVisible = signal(false);

  readonly status = computed<Step1Status>(() => this.step()?.step1Status ?? 'UNKNOWN');
  readonly editable = computed(() => !!this.step()?.editable);
  readonly dateLabel = computed(() => {
    const date = this.date();
    return date ? longDateLabel(date) : '';
  });

  readonly members = computed(() => this.step()?.members ?? []);
  readonly resting = computed(() => this.step()?.resting ?? []);
  readonly absentees = computed(() => this.step()?.absentees ?? []);
  readonly novelties = computed(() => this.step()?.novelties ?? []);
  readonly warnings = computed(() => this.step()?.warnings ?? []);

  /** El estado va en una etiqueta corta; la explicación larga solo cuando aporta algo. */
  readonly state = computed<StepState | null>(() => {
    const step = this.step();
    if (!step) return null;
    if (!step.editable) {
      return step.step1Status === 'CONFIRMED'
        ? { tone: 'ok', label: 'Confirmado', note: 'La jornada está cerrada: solo lectura.' }
        : { tone: 'locked', label: 'Solo lectura', note: 'No hay una jornada abierta para este día.' };
    }
    switch (step.step1Status) {
      case 'CONFIRMED':
        return { tone: 'ok', label: 'Confirmado', note: '' };
      case 'PENDING':
        return { tone: 'pending', label: 'Por confirmar', note: '' };
      default:
        return { tone: 'warn', label: 'Estado desconocido', note: 'No se pudo saber el estado de la jornada; inténtalo de nuevo antes de confirmar.' };
    }
  });

  /** Quien trabaja hoy, con su franja horaria sobre el día operativo. */
  readonly working = computed<DayPerson[]>(() =>
    this.members().map(m => ({
      workerId: m.workerId,
      fullName: m.fullName,
      code: m.workedCode,
      gridCode: m.gridCode,
      changed: m.workedCode !== m.gridCode,
      hours: segmentsLabel(m.segments),
      totalHours: m.totalHours,
      roleLabel: m.dayRole === 'APOYO' ? 'Apoyo' : 'Titular',
      dispenser: m.dispenser,
      support: m.support,
      bars: m.segments.map((seg, i) => ({
        ...segmentOnOperationalDay(seg),
        label: i === 0 ? `${m.workedCode} · ${segmentsLabel([seg])}` : segmentsLabel([seg])
      })),
      novelty: m.noveltyType === 'NONE' ? '' : NOVELTY_LABELS[m.noveltyType],
      noveltyReason: m.noveltyReason,
      noveltyWith: m.otherWorkerName,
      kind: 'works'
    }))
  );

  /** Quien no trabaja: primero los sacados por una novedad, luego los que descansan. */
  readonly notWorking = computed<DayPerson[]>(() => [
    ...this.absentees().map(a => this.simplePerson(a.workerId, a.fullName, a.gridCode, 'absent', {
      novelty: a.type === 'NONE' ? 'Ausente' : NOVELTY_LABELS[a.type],
      reason: a.reason
    })),
    ...this.resting().map(r => this.simplePerson(r.workerId, r.fullName, 'DESCANSO', 'resting'))
  ]);

  readonly axisHours = ['06', '09', '12', '15', '18', '21', '00', '03', '06'];

  readonly noveltyItems = computed<NoveltyItem[]>(() =>
    this.novelties().map(novelty => ({
      novelty,
      label: NOVELTY_LABELS[novelty.type],
      summary: this.noveltySummary(novelty),
      folding: this.foldingIds().has(novelty.id),
      asking: this.askingCancelId() === novelty.id
    }))
  );

  /** Lo único que impide confirmar es no dejar a nadie trabajando; los avisos solo informan. */
  readonly confirmBlockedReason = computed<string | null>(() => {
    const step = this.step();
    if (!step) return 'Cargando el personal del día…';
    if (!step.editable) return 'La jornada no se puede editar.';
    if (step.members.length === 0) return 'Nadie queda trabajando: registra un reemplazo antes de confirmar.';
    return null;
  });

  readonly confirmLabel = computed(() => (this.status() === 'CONFIRMED' ? 'Confirmar de nuevo' : 'Confirmar personal'));

  readonly dateShort = computed(() => {
    const date = this.date();
    return date ? capitalize(longDateLabel(date)) : '';
  });

  readonly panelHeading = computed(() => {
    const ctx = this.panelContext();
    if (!ctx) return 'Registrar novedad';
    const name = this.nameOf(ctx.workerId) ?? '';
    return ctx.mode === 'member' ? `Registrar novedad · ${name}` : `${name} cubre un turno`;
  });

  private toastTimer?: ReturnType<typeof setTimeout>;
  private flashTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    toObservable(this.date)
      .pipe(
        filter((date): date is string => !!date),
        distinctUntilChanged(),
        tap(() => {
          this.loading.set(true);
          this.loadError.set(null);
        }),
        switchMap(date =>
          this.dayStaffService.getStep(date).pipe(
            catchError((error: unknown) => {
              this.loadError.set(getErrorMessage(error));
              return of(null);
            })
          )
        ),
        takeUntilDestroyed()
      )
      .subscribe(step => {
        this.loading.set(false);
        if (step) this.step.set(step);
      });

    this.scheduleService
      .getCatalog()
      .pipe(takeUntilDestroyed())
      .subscribe({ next: codes => this.catalog.set(codes), error: () => this.catalog.set([]) });

    this.destroyRef.onDestroy(() => {
      clearTimeout(this.toastTimer);
      clearTimeout(this.flashTimer);
    });
  }

  retry(): void {
    const date = this.date();
    if (!date) return;
    this.loading.set(true);
    this.loadError.set(null);
    this.dayStaffService
      .getStep(date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: step => {
          this.loading.set(false);
          this.step.set(step);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.loadError.set(getErrorMessage(error));
        }
      });
  }

  openPanel(mode: NoveltyContext['mode'], workerId: string): void {
    if (!this.editable()) return;
    this.submitError.set(null);
    this.submitMessage.set(null);
    this.panelContext.set({ mode, workerId });
  }

  closePanel(): void {
    this.panelContext.set(null);
  }

  saveNovelty(request: RegisterNoveltyRequest): void {
    const date = this.date();
    if (!date || this.saving()) return;
    this.saving.set(true);
    this.submitError.set(null);
    this.submitMessage.set(null);
    this.dayStaffService
      .registerNovelty(date, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: step => {
          this.saving.set(false);
          this.step.set(step);
          this.closePanel();
          this.flash([request.workerId, request.counterpartWorkerId ?? '']);
          this.showToast(`${NOVELTY_LABELS[request.type]} registrado`);
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.submitError.set(error);
          this.submitMessage.set(getErrorMessage(error));
        }
      });
  }

  askCancel(novelty: DayStaffNovelty): void {
    this.cancelError.set(null);
    this.askingCancelId.set(novelty.id);
    // El botón "Anular" desaparece: el foco pasa a la pregunta para no perderse
    requestAnimationFrame(() => this.host.nativeElement.querySelector<HTMLElement>('.ds-ask button')?.focus());
  }

  keepNovelty(): void {
    this.askingCancelId.set(null);
  }

  /** Se pliega en el acto (optimista); si el servidor la rechaza, la tarjeta vuelve a abrirse con el motivo. */
  cancelNovelty(novelty: DayStaffNovelty): void {
    const date = this.date();
    if (!date) return;
    this.askingCancelId.set(null);
    this.cancelError.set(null);
    this.setFolding(novelty.id, true);
    const startedAt = Date.now();
    this.dayStaffService
      .cancelNovelty(date, novelty.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: step => {
          const wait = prefersReducedMotion() ? 0 : Math.max(0, FOLD_DURATION_MS - (Date.now() - startedAt));
          setTimeout(() => {
            this.setFolding(novelty.id, false);
            this.step.set(step);
            this.flash([novelty.workerId, novelty.counterpartWorkerId ?? '']);
            this.showToast('Novedad anulada: el día vuelve al turno de la malla');
          }, wait);
        },
        error: (error: unknown) => {
          this.setFolding(novelty.id, false);
          this.cancelError.set(getErrorMessage(error));
        }
      });
  }

  confirm(): void {
    const date = this.date();
    if (!date || this.confirming() || this.confirmBlockedReason()) return;
    this.confirming.set(true);
    this.confirmError.set(null);
    this.dayStaffService
      .confirm(date)
      .pipe(
        tap(step => this.step.set(step)),
        switchMap(() => this.markJournalStep()),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: marked => {
          this.confirming.set(false);
          this.showToast(marked ? 'Personal del día confirmado' : 'Personal confirmado. El avance de la jornada se actualizará al recargar.');
        },
        error: (error: unknown) => {
          this.confirming.set(false);
          this.confirmError.set(getErrorMessage(error));
        }
      });
  }

  /** Marca el paso en la jornada para que el stepper lo muestre completo. Si falla, el personal ya quedó confirmado. */
  private markJournalStep() {
    const journal = this.journal();
    const current = journal?.steps.find(s => s.stepNumber === STEP_NUMBER);
    if (!journal || !current || current.status === StepStatus.COMPLETED) return of(true);
    return this.journalService.completeStep(journal.id, STEP_NUMBER).pipe(
      tap(step => this.journalService.updateCurrentStep(step)),
      switchMap(() => of(true)),
      catchError(() => of(false))
    );
  }

  noveltyLabel(type: NoveltyType | 'NONE'): string {
    return type === 'NONE' ? '' : NOVELTY_LABELS[type];
  }

  nameOf(workerId: string | null): string | null {
    if (!workerId) return null;
    const step = this.step();
    if (!step) return null;
    return step.members.find(m => m.workerId === workerId)?.fullName
      ?? step.resting.find(r => r.workerId === workerId)?.fullName
      ?? step.absentees.find(a => a.workerId === workerId)?.fullName
      ?? null;
  }

  noveltySummary(novelty: DayStaffNovelty): string {
    const who = this.nameOf(novelty.workerId) ?? 'Vendedor';
    const other = this.nameOf(novelty.counterpartWorkerId);
    switch (novelty.type) {
      case 'REPLACEMENT':
        return `${other ?? 'Otra persona'} cubre a ${who}`;
      case 'ABSENCE':
        return `${who} no trabaja`;
      case 'SHIFT_CHANGE':
        return `${who} cambia de turno`;
      case 'SWAP':
        return `${who} intercambia con ${other ?? 'otra persona'}`;
    }
  }

  private simplePerson(
    workerId: string,
    fullName: string,
    code: string,
    kind: DayPerson['kind'],
    novelty?: { novelty: string; reason: string }
  ): DayPerson {
    return {
      workerId,
      fullName,
      code,
      gridCode: code,
      changed: false,
      hours: '—',
      totalHours: 0,
      roleLabel: kind === 'resting' ? 'Descansa' : 'No trabaja',
      dispenser: null,
      support: false,
      bars: [],
      novelty: novelty?.novelty ?? '',
      noveltyReason: novelty?.reason ?? null,
      noveltyWith: null,
      kind
    };
  }

  isFolding(id: string): boolean {
    return this.foldingIds().has(id);
  }

  isFlashing(workerId: string): boolean {
    return this.flashIds().has(workerId);
  }

  private setFolding(id: string, on: boolean): void {
    this.foldingIds.update(ids => {
      const next = new Set(ids);
      if (on) next.add(id); else next.delete(id);
      return next;
    });
  }

  private flash(workerIds: string[]): void {
    clearTimeout(this.flashTimer);
    this.flashIds.set(new Set(workerIds.filter(Boolean)));
    this.flashTimer = setTimeout(() => this.flashIds.set(new Set()), 1200);
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
