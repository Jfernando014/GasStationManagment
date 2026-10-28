import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Output, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { RotationPreviewGridComponent, rotationCoverageTip } from '../rotation-preview-grid/rotation-preview-grid.component';
import { ShiftsScheduleService } from '../../../../core/services/shifts-schedule.service';
import { getErrorMessage, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import {
  PHASE_LABELS,
  ROTATION_CYCLE,
  addDays,
  addMonths,
  monthLabel,
  monthOf,
  shortDateLabel
} from '../../../../shared/helpers/shift-display.helpers';
import {
  RotationPreviewResponse,
  RotationResultResponse,
  RotationTitularPhase
} from '../../../../shared/models/dtos/shift-rotation.dto';
import { Worker } from '../../../../shared/models/dtos/worker.dto';

type WizardStep = 1 | 2 | 3;
type Screen = 'setup' | 'preview' | 'review' | 'result' | 'changed' | 'phase';

export interface RotationDone {
  month: string;
  from: string;
  to: string;
  workerIds: string[];
  result: RotationResultResponse;
}


/**
 * Asistente de la rotación automática 2x2 de los titulares de un mes: (1) mes y titulares, (2) vista previa calculada
 * por el backend sin guardar, (3) confirmación con la versión de la malla que devolvió la vista previa.
 */
@Component({
  selector: 'app-rotation-wizard',
  standalone: true,
  imports: [ActionButtonDirective, ShiftChipComponent, RotationPreviewGridComponent],
  templateUrl: './rotation-wizard.component.html',
  styleUrl: './rotation-wizard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RotationWizardComponent {
  private readonly scheduleService = inject(ShiftsScheduleService);
  private readonly destroyRef = inject(DestroyRef);

  /** Titulares activos: la rotación los genera a todos a la vez. */
  readonly titulares = input<Worker[]>([]);
  /** Hoy en la zona de la estación; la rotación del mes en curso escribe desde mañana. */
  readonly today = input.required<string>();
  /** Mes que se está viendo en la malla; se propone al abrir si todavía se puede generar. */
  readonly initialMonth = input.required<string>();

  @Output() readonly done = new EventEmitter<RotationDone>();
  @Output() readonly cancelled = new EventEmitter<void>();

  readonly phaseLabels = PHASE_LABELS;
  /** Los seis días del ciclo, para mostrarlo tal cual en la explicación. */
  readonly cycle = ROTATION_CYCLE;

  readonly screen = signal<Screen>('setup');
  readonly month = signal('');
  readonly replaceManual = signal(false);
  /** Fases elegidas a mano, por id de vendedor; las demás las deduce el backend. */
  readonly phases = signal<Record<string, number>>({});
  readonly preview = signal<RotationPreviewResponse | null>(null);
  readonly calculating = signal(false);
  readonly confirming = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly conflictMessage = signal<string | null>(null);
  readonly result = signal<RotationResultResponse | null>(null);

  readonly step = computed<WizardStep>(() => {
    switch (this.screen()) {
      case 'setup': return 1;
      case 'preview': return 2;
      default: return 3;
    }
  });

  readonly monthOptions = computed(() => {
    const current = monthOf(this.today());
    return [0, 1, 2].map(n => {
      const month = addMonths(current, n);
      const label = monthLabel(month);
      return {
        value: month,
        label: n === 0 ? `${label} · mes en curso, desde el ${shortDateLabel(addDays(this.today(), 1))}` : label
      };
    });
  });

  readonly phaseRows = computed(() => {
    const preview = this.preview();
    if (!preview) return [];
    const chosen = this.phases();
    return preview.phases.map(p => ({
      ...p,
      value: chosen[p.workerId] ?? p.phase,
      pending: p.needsChoice && chosen[p.workerId] === undefined
    }));
  });

  /** Titulares cuya fase no se pudo deducir y nadie ha confirmado todavía: bloquean guardar. */
  readonly pendingPhases = computed(() => this.phaseRows().filter(p => p.pending));

  readonly uncoveredDays = computed(() =>
    (this.preview()?.days ?? [])
      .filter(d => !d.coverage.covered)
      .map(d => ({ date: d.date, tip: rotationCoverageTip(d.date, d.coverage) }))
  );


  private request?: Subscription;

  constructor() {
    effect(() => {
      const options = this.monthOptions().map(o => o.value);
      const initial = this.initialMonth();
      this.month.set(options.includes(initial) ? initial : options[0]);
    });
    this.destroyRef.onDestroy(() => this.request?.unsubscribe());
  }

  phaseSourceLabel(row: RotationTitularPhase & { pending: boolean }): string {
    if (row.pending) return 'Sugerida · confírmala';
    if (this.phases()[row.workerId] !== undefined) return 'Elegida por ti';
    switch (row.source) {
      case 'DEDUCED': return 'Deducida de la malla';
      case 'PROVIDED': return 'Elegida por ti';
      default: return 'Sugerida';
    }
  }

  chooseMonth(value: string): void {
    this.month.set(value);
    this.phases.set({});
  }

  /** Calcula (o recalcula) la vista previa en el backend; no guarda nada. */
  calculate(): void {
    this.request?.unsubscribe();
    this.calculating.set(true);
    this.errorMessage.set(null);
    const phases = this.phases();
    this.request = this.scheduleService
      .previewRotation({
        month: this.month(),
        phases: Object.keys(phases).length ? phases : undefined,
        replaceManual: this.replaceManual()
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: preview => {
          this.calculating.set(false);
          this.preview.set(preview);
          this.screen.set('preview');
        },
        error: (error: unknown) => {
          this.calculating.set(false);
          this.errorMessage.set(getErrorMessage(error));
        }
      });
  }

  /** Corregir una fase recalcula la vista previa: la malla propuesta cambia y la versión también. */
  setPhase(workerId: string, value: string): void {
    this.phases.update(p => ({ ...p, [workerId]: Number(value) }));
    this.calculate();
  }

  acceptSuggested(row: RotationTitularPhase): void {
    this.setPhase(row.workerId, String(row.phase));
  }

  acceptAllSuggested(): void {
    const next = { ...this.phases() };
    this.pendingPhases().forEach(p => (next[p.workerId] = p.phase));
    this.phases.set(next);
    this.calculate();
  }

  toggleReplaceManual(checked: boolean): void {
    this.replaceManual.set(checked);
  }

  goToReview(): void {
    if (this.pendingPhases().length) return;
    this.screen.set('review');
  }

  back(): void {
    switch (this.screen()) {
      case 'preview': this.screen.set('setup'); break;
      case 'review':
      case 'changed':
      case 'phase': this.screen.set('preview'); break;
    }
  }

  /** Envía la versión de la malla de la vista previa; si algo cambió desde entonces, el backend no guarda nada. */
  confirm(): void {
    const preview = this.preview();
    if (!preview || this.confirming()) return;
    this.confirming.set(true);
    this.errorMessage.set(null);
    const phases = this.phases();
    this.scheduleService
      .confirmRotation({
        month: preview.month,
        phases: Object.keys(phases).length ? phases : undefined,
        replaceManual: this.replaceManual(),
        gridVersion: preview.gridVersion
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.confirming.set(false);
          this.result.set(result);
          this.screen.set('result');
        },
        error: (error: unknown) => {
          this.confirming.set(false);
          if (isApiError(error) && error.code === 'SCHEDULE_CHANGED') {
            this.conflictMessage.set(null);
            this.screen.set('changed');
          } else if (isApiError(error) && error.code === 'PHASE_REQUIRED') {
            this.conflictMessage.set(error.detail ?? null);
            this.screen.set('phase');
          } else {
            this.errorMessage.set(getErrorMessage(error));
          }
        }
      });
  }

  /** Tras "la malla cambió": misma elección, vista previa nueva. */
  recalculate(): void {
    this.calculate();
  }

  finish(): void {
    const preview = this.preview();
    const result = this.result();
    if (!preview || !result) return;
    this.done.emit({
      month: preview.month,
      from: preview.from ?? preview.days[0]?.date ?? '',
      to: preview.to ?? preview.days[preview.days.length - 1]?.date ?? '',
      workerIds: preview.phases.map(p => p.workerId),
      result
    });
  }

  monthText(month: string): string {
    return monthLabel(month);
  }

  dateText(date: string): string {
    return shortDateLabel(date);
  }

  datesText(dates: string[]): string {
    return dates.map(shortDateLabel).join(' · ');
  }


}
