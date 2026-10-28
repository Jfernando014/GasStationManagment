import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, catchError, of, switchMap, tap } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { SidePanelComponent } from '../../../../atomic-design/organisms/side-panel/side-panel.component';
import { ShiftsScheduleService } from '../../../../core/services/shifts-schedule.service';
import { WorkersService } from '../../../../core/services/workers.service';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import {
  addMonths,
  capitalize,
  firstDayOfMonth,
  lastDayOfMonth,
  monthDays,
  monthLabel,
  monthOf,
  operationalToday,
  prefersReducedMotion,
  segmentsLabel,
  shortDateLabel,
  shiftTone,
  ShiftTone,
  stationToday
} from '../../../../shared/helpers/shift-display.helpers';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { AssignRangeRequest, DailyCoverage, ScheduleResponse } from '../../../../shared/models/dtos/shift-schedule.dto';
import { Worker } from '../../../../shared/models/dtos/worker.dto';
import { DayChoice, DayEditorComponent } from '../../components/day-editor/day-editor.component';
import { RangeAssignFormComponent } from '../../components/range-assign-form/range-assign-form.component';
import { RotationDone, RotationWizardComponent } from '../../components/rotation-wizard/rotation-wizard.component';
import {
  GridCell,
  GridCellRef,
  GridRow,
  GridSweep,
  ScheduleGridComponent
} from '../../components/schedule-grid/schedule-grid.component';

type Panel = 'day' | 'range' | 'rotation';

const TOAST_DURATION_MS = 3500;
/** Duración del vuelo de la ficha hasta la celda y del barrido de una fila guardada. */
const FLIGHT_MS = 320;
const SWEEP_MS = 1400;

/** Leyenda de turnos: también es el filtro de la malla. El código de muestra es el que da el color. */
const LEGEND_SHIFTS: { tone: ShiftTone; code: string; label: string; hint: string }[] = [
  { tone: 'dia', code: 'DIA', label: 'Día', hint: '06–18' },
  { tone: 'dia6', code: 'DIA6', label: 'Día corto', hint: '06–12' },
  { tone: 'nueve-seis', code: '9-6', label: 'Día tarde', hint: '09–18' },
  { tone: 'noche', code: 'NOCHE', label: 'Noche', hint: '18–06' },
  { tone: 'apoyo', code: '12-7', label: 'Apoyo', hint: 'el código es el horario' },
  { tone: 'descanso', code: 'DESCANSO', label: 'Descanso', hint: '' }
];

/** Personal › Malla de turnos: el mes por vendedor, la edición de un día, rangos y la rotación automática. */
@Component({
  selector: 'app-shifts-schedule-page',
  standalone: true,
  imports: [
    RouterLink,
    ActionButtonDirective,
    ShiftChipComponent,
    EmptyStateComponent,
    SidePanelComponent,
    ScheduleGridComponent,
    DayEditorComponent,
    RangeAssignFormComponent,
    RotationWizardComponent
  ],
  templateUrl: './shifts-schedule-page.component.html',
  styleUrl: './shifts-schedule-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'legendOpen.set(false)'
  }
})
export class ShiftsSchedulePageComponent {
  private readonly scheduleService = inject(ShiftsScheduleService);
  private readonly workersService = inject(WorkersService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly grid = viewChild(ScheduleGridComponent);

  readonly today = stationToday();
  readonly highlight = operationalToday();
  readonly month = signal(monthOf(this.highlight));
  readonly monthTitle = computed(() => capitalize(monthLabel(this.month())));

  readonly workers = signal<Worker[]>([]);
  readonly catalog = signal<ShiftCode[]>([]);
  readonly schedule = signal<ScheduleResponse | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  /** Cambios ya mostrados en la malla mientras el servidor responde (o hasta la siguiente recarga). */
  readonly localCells = signal<Record<string, GridCell>>({});

  readonly panel = signal<Panel | null>(null);
  readonly dayRef = signal<GridCellRef | null>(null);
  readonly daySavingCode = signal<string | null>(null);
  readonly dayError = signal<string | null>(null);
  readonly rangeSaving = signal(false);
  readonly rangeError = signal<unknown>(null);
  readonly rangeWorkerId = signal<string | null>(null);

  readonly sweep = signal<GridSweep | null>(null);
  readonly updatedKey = signal<string | null>(null);
  readonly toastText = signal('');
  readonly toastVisible = signal(false);

  /** Familia de turno elegida en la leyenda: la malla deja ver solo esos días. */
  readonly filter = signal<ShiftTone | null>(null);
  readonly legendShifts = LEGEND_SHIFTS;
  /** La leyenda vive en un desplegable; se abre desde "Cómo leer la malla". */
  readonly legendOpen = signal(false);

  readonly days = computed(() => monthDays(this.month()));

  readonly coverage = computed<Record<string, DailyCoverage>>(() => {
    const map: Record<string, DailyCoverage> = {};
    this.schedule()?.coverage.forEach(c => (map[c.date] = c));
    return map;
  });

  /** Cuántos días del mes tiene cada familia de turno, para mostrarlo en la leyenda. */
  readonly toneCounts = computed<Record<string, number>>(() => {
    const counts: Record<string, number> = {};
    this.rows().forEach(row =>
      Object.values(row.cells).forEach(cell => {
        if (!cell.code) return;
        const tone = shiftTone(cell.code);
        counts[tone] = (counts[tone] ?? 0) + 1;
      })
    );
    return counts;
  });

  /** Nombre y horario de cada código, para la tarjeta que sale al pasar por una celda. */
  readonly codeInfo = computed<Record<string, { name: string; hours: string }>>(() => {
    const info: Record<string, { name: string; hours: string }> = {};
    this.catalog().forEach(code => {
      info[code.code] = {
        name: LEGEND_SHIFTS.find(l => l.code === code.code)?.label ?? code.name,
        hours: segmentsLabel(code.schedule.segments).replace(/–/g, ' a ')
      };
    });
    return info;
  });

  readonly filterLabel = computed(() => LEGEND_SHIFTS.find(l => l.tone === this.filter())?.label ?? '');

  readonly uncoveredCount = computed(() => this.schedule()?.coverage.filter(c => !c.covered).length ?? 0);

  /** Titulares primero y luego apoyos; también los activos que todavía no tienen turnos en el mes. */
  readonly rows = computed<GridRow[]>(() => {
    const schedule = this.schedule();
    const local = this.localCells();
    const byWorker = new Map(schedule?.workers.map(w => [w.workerId, w]) ?? []);
    const people = this.workers().map(w => ({ id: w.id, name: w.fullName, role: w.roleName }));
    schedule?.workers.forEach(w => {
      if (!people.some(p => p.id === w.workerId)) people.push({ id: w.workerId, name: w.fullName, role: '' });
    });
    people.sort((a, b) => Number(a.role === 'APOYO') - Number(b.role === 'APOYO') || a.name.localeCompare(b.name, 'es'));
    return people.map(p => {
      const cells: Record<string, GridCell> = {};
      byWorker.get(p.id)?.days.forEach(d => (cells[d.date] = { code: d.shiftCode, note: d.note }));
      Object.entries(local).forEach(([key, cell]) => {
        const [workerId, date] = key.split('|');
        if (workerId === p.id) cells[date] = cell;
      });
      return { workerId: p.id, name: p.name, roleLabel: p.role === 'APOYO' ? 'Apoyo' : 'Titular', cells };
    });
  });

  toggleLegend(): void {
    this.legendOpen.update(open => !open);
  }

  /** Un clic fuera del desplegable lo cierra; dentro no, para poder usar los filtros. */
  onDocumentClick(event: MouseEvent): void {
    if (!this.legendOpen()) return;
    const target = event.target as Node | null;
    const wrap = this.host.nativeElement.querySelector('.sp-legend-wrap');
    if (target && wrap?.contains(target)) return;
    this.legendOpen.set(false);
  }

  /** La misma ficha enciende y apaga el filtro. */
  toggleFilter(tone: ShiftTone): void {
    this.filter.update(current => (current === tone ? null : tone));
  }

  countOf(tone: ShiftTone): number {
    return this.toneCounts()[tone] ?? 0;
  }

  readonly titulares = computed(() => this.workers().filter(w => w.roleName !== 'APOYO'));

  readonly dayWorker = computed(() => {
    const ref = this.dayRef();
    return ref ? this.rows().find(r => r.workerId === ref.workerId) ?? null : null;
  });

  readonly dayRole = computed(() => {
    const ref = this.dayRef();
    return this.workers().find(w => w.id === ref?.workerId)?.roleName ?? 'TITULAR';
  });

  readonly dayCell = computed<GridCell>(() => {
    const ref = this.dayRef();
    const row = this.dayWorker();
    return ref && row ? row.cells[ref.date] ?? { code: null, note: null } : { code: null, note: null };
  });

  readonly panelHeading = computed(() => {
    switch (this.panel()) {
      case 'day': {
        const ref = this.dayRef();
        return ref ? `${this.dayWorker()?.name ?? ''} · ${shortDateLabel(ref.date)}` : 'Día';
      }
      case 'range': return 'Asignar un código a un rango';
      case 'rotation': return 'Generar la rotación del mes';
      default: return '';
    }
  });

  readonly panelDescription = computed(() => {
    switch (this.panel()) {
      case 'day': {
        const ref = this.dayRef();
        const role = this.dayWorker()?.roleLabel ?? '';
        return ref && ref.date < this.today
          ? `${role}. Este día ya pasó: solo lectura.`
          : `${role}. Elige un turno y se guarda al instante.`;
      }
      case 'range': return 'Mismo turno para un vendedor en varios días seguidos.';
      case 'rotation': return 'Ciclo 2x2 de los titulares, con vista previa antes de guardar.';
      default: return '';
    }
  });

  readonly savingKey = computed(() => {
    const ref = this.dayRef();
    return this.daySavingCode() && ref ? `${ref.workerId}|${ref.date}` : null;
  });

  readonly panelWidth = computed(() => (this.panel() === 'rotation' ? 880 : this.panel() === 'range' ? 480 : 420));

  private readonly reload$ = new Subject<{ silent: boolean }>();
  private pendingSweep: GridSweep | null = null;
  private toastTimer?: ReturnType<typeof setTimeout>;
  private sweepTimer?: ReturnType<typeof setTimeout>;
  private popTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.reload$
      .pipe(
        tap(({ silent }) => {
          if (!silent) this.loading.set(true);
          this.loadError.set(null);
        }),
        switchMap(({ silent }) => {
          const month = this.month();
          return this.scheduleService.getSchedule(firstDayOfMonth(month), lastDayOfMonth(month)).pipe(
            catchError((error: unknown) => {
              if (!silent) this.loadError.set(getErrorMessage(error));
              return of(null);
            })
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe(schedule => {
        this.loading.set(false);
        if (!schedule) return;
        this.schedule.set(schedule);
        this.localCells.set({});
        if (this.pendingSweep) {
          this.runSweep(this.pendingSweep);
          this.pendingSweep = null;
        }
      });

    this.workersService
      .list({ active: true })
      .pipe(takeUntilDestroyed())
      .subscribe({ next: workers => this.workers.set(workers), error: (error: unknown) => this.loadError.set(getErrorMessage(error)) });

    this.scheduleService
      .getCatalog()
      .pipe(takeUntilDestroyed())
      .subscribe({ next: codes => this.catalog.set(codes), error: () => this.catalog.set([]) });

    this.destroyRef.onDestroy(() => {
      clearTimeout(this.toastTimer);
      clearTimeout(this.sweepTimer);
      clearTimeout(this.popTimer);
    });

    this.reload$.next({ silent: false });
  }

  changeMonth(step: number): void {
    this.month.update(m => addMonths(m, step));
    this.reload$.next({ silent: false });
  }

  /** "Hoy": vuelve al mes en curso o, si ya está en él, desplaza la malla hasta el día de hoy. */
  goToToday(): void {
    const current = monthOf(this.highlight);
    if (this.month() === current) {
      this.grid()?.scrollToDate(this.highlight);
      return;
    }
    this.month.set(current);
    this.reload$.next({ silent: false });
  }

  /** Lleva la malla al primer día sin cobertura completa que todavía se puede corregir (o al primero del mes). */
  goToFirstUncovered(): void {
    const uncovered = this.schedule()?.coverage.filter(c => !c.covered).map(c => c.date) ?? [];
    const target = uncovered.find(date => date >= this.today) ?? uncovered[0];
    if (target) this.grid()?.scrollToDate(target);
  }

  retry(): void {
    if (!this.workers().length) {
      this.workersService.list({ active: true }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(w => this.workers.set(w));
    }
    this.reload$.next({ silent: false });
  }

  openDay(ref: GridCellRef): void {
    this.dayError.set(null);
    this.dayRef.set(ref);
    this.panel.set('day');
  }

  openRange(workerId: string | null = null): void {
    this.rangeError.set(null);
    this.rangeWorkerId.set(workerId);
    this.panel.set('range');
  }

  openRotation(): void {
    this.panel.set('rotation');
  }

  closePanel(): void {
    this.panel.set(null);
    this.daySavingCode.set(null);
  }

  /** Guarda al elegir: la ficha vuela a la celda y la malla la muestra ya; si el servidor la rechaza, se revierte. */
  chooseCode(choice: DayChoice): void {
    const ref = this.dayRef();
    if (!ref || this.daySavingCode()) return;
    const previous = this.dayCell();
    if (previous.code === choice.code && (previous.note ?? null) === choice.note) {
      this.closePanel();
      return;
    }
    const key = `${ref.workerId}|${ref.date}`;
    this.dayError.set(null);
    this.daySavingCode.set(choice.code);
    this.flyChip(choice.origin, ref);
    this.setLocalCell(key, { code: choice.code, note: choice.note });

    this.scheduleService
      .assign({ workerId: ref.workerId, date: ref.date, shiftCode: choice.code, note: choice.note })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.daySavingCode.set(null);
          this.pop(key);
          this.closePanel();
          const name = this.dayWorker()?.name ?? '';
          const warning = result.uncoveredDates.length
            ? ` Ojo: el ${result.uncoveredDates.map(shortDateLabel).join(', ')} queda sin cobertura completa.`
            : '';
          this.showToast(`Guardado: ${name} · ${shortDateLabel(ref.date)} → ${choice.code}.${warning}`);
          this.reload$.next({ silent: true });
        },
        error: (error: unknown) => {
          this.daySavingCode.set(null);
          this.setLocalCell(key, previous);
          this.dayError.set(getErrorMessage(error));
        }
      });
  }

  submitRange(request: AssignRangeRequest): void {
    if (this.rangeSaving()) return;
    this.rangeSaving.set(true);
    this.rangeError.set(null);
    this.scheduleService
      .assignRange(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.rangeSaving.set(false);
          this.closePanel();
          const warning = result.uncoveredDates.length ? ` ${result.uncoveredDates.length} días quedan sin cobertura completa.` : '';
          this.showToast(`Se guardaron ${result.saved} días.${warning}`);
          this.pendingSweep = { workerIds: new Set([request.workerId]), from: request.from, to: request.to };
          const month = monthOf(request.from);
          if (month !== this.month()) this.month.set(month);
          this.reload$.next({ silent: true });
        },
        error: (error: unknown) => {
          this.rangeSaving.set(false);
          this.rangeError.set(error);
        }
      });
  }

  rotationDone(done: RotationDone): void {
    this.closePanel();
    this.showToast(`Rotación de ${monthLabel(done.month)} guardada: ${done.result.generated} días escritos.`);
    this.pendingSweep = { workerIds: new Set(done.workerIds), from: done.from, to: done.to };
    if (done.month !== this.month()) {
      this.month.set(done.month);
      this.reload$.next({ silent: false });
    } else {
      this.reload$.next({ silent: true });
    }
  }

  rangeErrorMessage(): string | null {
    const error = this.rangeError();
    return error ? getErrorMessage(error) : null;
  }

  rangeDefaultFrom(): string {
    return firstDayOfMonth(this.month());
  }

  rangeDefaultTo(): string {
    return lastDayOfMonth(this.month());
  }

  private setLocalCell(key: string, cell: GridCell): void {
    this.localCells.update(cells => ({ ...cells, [key]: cell }));
  }

  private pop(key: string): void {
    clearTimeout(this.popTimer);
    this.updatedKey.set(key);
    this.popTimer = setTimeout(() => this.updatedKey.set(null), 400);
  }

  private runSweep(sweep: GridSweep): void {
    clearTimeout(this.sweepTimer);
    this.sweep.set(sweep);
    this.sweepTimer = setTimeout(() => this.sweep.set(null), SWEEP_MS);
  }

  /** La ficha elegida viaja del panel a la celda: así se ve dónde cayó el cambio. */
  private flyChip(origin: HTMLElement, ref: GridCellRef): void {
    const target = this.grid()?.cellElement(ref.workerId, ref.date);
    if (!target || prefersReducedMotion() || typeof origin.animate !== 'function') return;
    const from = origin.getBoundingClientRect();
    const to = target.getBoundingClientRect();
    if (!to.width || !from.width) return;
    const clone = origin.cloneNode(true) as HTMLElement;
    Object.assign(clone.style, {
      position: 'fixed',
      left: `${from.left}px`,
      top: `${from.top}px`,
      width: `${from.width}px`,
      height: `${from.height}px`,
      margin: '0',
      zIndex: '70',
      pointerEvents: 'none',
      boxShadow: 'var(--shadow-popover)'
    });
    document.body.appendChild(clone);
    const dx = to.left + to.width / 2 - (from.left + from.width / 2);
    const dy = to.top + to.height / 2 - (from.top + from.height / 2);
    const scale = Math.min(1, to.width / from.width);
    const flight = clone.animate(
      [
        { transform: 'translate(0, 0) scale(1)', opacity: 1 },
        { transform: `translate(${dx}px, ${dy}px) scale(${scale})`, opacity: 0.4 }
      ],
      { duration: FLIGHT_MS, easing: 'cubic-bezier(0.16, 1, 0.3, 1)' }
    );
    flight.onfinish = () => clone.remove();
    flight.oncancel = () => clone.remove();
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
