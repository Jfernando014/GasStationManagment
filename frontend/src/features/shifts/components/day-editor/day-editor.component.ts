import { ChangeDetectionStrategy, Component, EventEmitter, Output, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { ShiftHoursService } from '../../../../core/services/shift-hours.service';
import { SERVER_ERROR_KEY } from '../../../../shared/helpers/form-control.helpers';
import { getErrorMessage, getFieldErrors } from '../../../../shared/helpers/problem-details.helpers';
import { capitalize, longDateLabel, segmentsLabel } from '../../../../shared/helpers/shift-display.helpers';
import { codeForHours, completeSegments, isCustomShiftCode } from '../../../../shared/helpers/support-hours.helpers';
import { ShiftCode, ShiftScheduleSegment } from '../../../../shared/models/dtos/shift-catalog.dto';
import { GridCell } from '../schedule-grid/schedule-grid.component';
import { SupportHoursDraft } from '../support-hours-picker/support-hours-draft';
import { SupportHoursPickerComponent } from '../support-hours-picker/support-hours-picker.component';

export interface DayChoice {
  code: string;
  note: string | null;
  /** Horas elegidas para un apoyo; null cuando se eligió un código del catálogo. */
  segments: ShiftScheduleSegment[] | null;
  /** Ficha pulsada: desde aquí "vuela" hasta la celda. Null cuando se guardan horas. */
  origin: HTMLElement | null;
}

const NOTE_MAX = 255;

/**
 * Contenido del panel de un día y una nota opcional. Un titular elige entre las fichas del catálogo de su rol;
 * un apoyo elige las horas de inicio y fin, o el descanso.
 */
@Component({
  selector: 'app-day-editor',
  standalone: true,
  imports: [ActionButtonDirective, FormFieldComponent, ShiftChipComponent, SupportHoursPickerComponent],
  templateUrl: './day-editor.component.html',
  styleUrl: './day-editor.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DayEditorComponent {
  private readonly shiftHours = inject(ShiftHoursService);

  readonly workerName = input.required<string>();
  /** TITULAR o APOYO, según el registro del vendedor. */
  readonly roleName = input.required<string>();
  readonly date = input.required<string>();
  readonly cell = input.required<GridCell>();
  readonly catalog = input<ShiftCode[]>([]);
  readonly readOnly = input(false);
  readonly saving = input(false);
  readonly savingCode = input<string | null>(null);
  /** El horario quedó guardado: el botón lo confirma antes de que se cierre el panel. */
  readonly saved = input(false);
  /** Respuesta del servidor cuando rechaza el cambio. */
  readonly error = input<unknown>(null);

  @Output() readonly chosen = new EventEmitter<DayChoice>();

  readonly note = new FormControl('', { nonNullable: true, validators: [Validators.maxLength(NOTE_MAX)] });
  readonly noteMax = NOTE_MAX;
  readonly hours = new SupportHoursDraft();
  /** Con qué se envió el último cambio: su error se muestra junto a ese selector. */
  private readonly attempt = signal<'code' | 'hours'>('code');

  readonly dateLabel = computed(() => capitalize(longDateLabel(this.date())));
  readonly isSupport = computed(() => this.roleName() === 'APOYO');
  readonly roleLabel = computed(() => (this.isSupport() ? 'apoyo' : 'titular'));

  readonly errorMessage = computed(() =>
    this.error() && this.attempt() === 'code' ? this.shiftHours.inText(getErrorMessage(this.error())) : null
  );

  /**
   * Solo se ofrece lo que el backend aceptaría: códigos activos del rol del vendedor y DESCANSO. Los turnos de un
   * apoyo van en el selector de horas, así que aquí solo le queda el descanso.
   */
  readonly options = computed(() =>
    this.catalog()
      .filter(c => c.active && (c.role === null || (!this.isSupport() && c.role.name === this.roleName())))
      .sort((a, b) => Number(a.code === 'DESCANSO') - Number(b.code === 'DESCANSO'))
      .map(c => ({ code: c.code, name: c.name, hours: segmentsLabel(c.schedule?.segments ?? []) }))
  );

  readonly currentHours = computed(() => {
    const { code, segments } = this.cell();
    if (code && isCustomShiftCode(code)) return this.shiftHours.fullText(code, segments);
    const entry = this.catalog().find(c => c.code === code);
    return entry ? segmentsLabel(entry.schedule?.segments ?? []) : '';
  });

  constructor() {
    // Solo al cambiar de día: si el servidor rechaza un cambio, se conserva lo que se eligió y se escribió
    effect(() => {
      this.date();
      this.workerName();
      untracked(() => {
        const { note, segments } = this.cell();
        this.note.setValue(note ?? '');
        this.hours.reset(segments && completeSegments(segments) ? segments : []);
        this.attempt.set('code');
      });
    });

    effect(() => {
      if (this.readOnly()) this.note.disable(); else this.note.enable();
    });

    effect(() => {
      const error = this.error();
      if (!error) return;
      untracked(() => {
        const noteError = getFieldErrors(error).find(f => f.field === 'note');
        if (noteError) {
          this.note.setErrors({ [SERVER_ERROR_KEY]: noteError.message });
          this.note.markAsTouched();
        }
        if (this.attempt() === 'hours') this.hours.applyServerError(error, ['note']);
      });
    });
  }

  choose(code: string, event: Event): void {
    if (this.readOnly() || this.saving() || this.saved() || this.note.invalid) return;
    const origin = (event.currentTarget as HTMLElement).querySelector<HTMLElement>('app-shift-chip') ?? (event.currentTarget as HTMLElement);
    this.attempt.set('code');
    this.chosen.emit({ code, note: this.noteValue(), segments: null, origin });
  }

  saveHours(): void {
    if (this.readOnly() || this.saving() || this.saved()) return;
    const segments = this.hours.validate();
    if (!segments || this.note.invalid) return;
    this.attempt.set('hours');
    this.hours.clearServerErrors();
    this.chosen.emit({ code: codeForHours(segments, this.catalog()), note: this.noteValue(), segments, origin: null });
  }

  private noteValue(): string | null {
    return this.note.value.trim() || null;
  }
}
