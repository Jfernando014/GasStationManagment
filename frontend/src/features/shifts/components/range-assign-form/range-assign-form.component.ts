import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Output, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { SegmentOption, SegmentedControlComponent } from '../../../../atomic-design/molecules/segmented-control/segmented-control.component';
import { SelectFieldComponent, SelectOption } from '../../../../atomic-design/molecules/select-field/select-field.component';
import { ShiftHoursService } from '../../../../core/services/shift-hours.service';
import { applyFieldErrors } from '../../../../shared/helpers/problem-details.helpers';
import {
  addDays,
  dateRange,
  dayOfMonth,
  daysBetween,
  isSunday,
  segmentsLabel,
  shortDateLabel,
  weekdayShort
} from '../../../../shared/helpers/shift-display.helpers';
import { codeForHours } from '../../../../shared/helpers/support-hours.helpers';
import { ShiftCode, ShiftScheduleSegment } from '../../../../shared/models/dtos/shift-catalog.dto';
import { AssignRangeRequest } from '../../../../shared/models/dtos/shift-schedule.dto';
import { Worker } from '../../../../shared/models/dtos/worker.dto';
import { SupportHoursDraft } from '../support-hours-picker/support-hours-draft';
import { SupportHoursPickerComponent } from '../support-hours-picker/support-hours-picker.component';

/** Tope del backend para asignar un turno a un rango. */
export const MAX_RANGE_DAYS = 62;

/** Lo que se le asigna a un apoyo: un horario o el descanso. */
type SupportMode = 'hours' | 'rest';

interface PreviewDay {
  date: string;
  day: number;
  weekday: string;
  code: string;
  segments: ShiftScheduleSegment[] | null;
  /** Lo que se lee del turno: el código, o las horas si es un horario personalizado. */
  text: string;
  rest: boolean;
}

/**
 * Asigna un mismo turno a un vendedor en un rango de días, con vista previa antes de guardar: un código para un
 * titular; las horas o el descanso para un apoyo.
 */
@Component({
  selector: 'app-range-assign-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    ActionButtonDirective,
    FormFieldComponent,
    SegmentedControlComponent,
    SelectFieldComponent,
    ShiftChipComponent,
    SupportHoursPickerComponent
  ],
  templateUrl: './range-assign-form.component.html',
  styleUrl: './range-assign-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RangeAssignFormComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly shiftHours = inject(ShiftHoursService);

  readonly workers = input<Worker[]>([]);
  readonly catalog = input<ShiftCode[]>([]);
  /** Primer día que se puede escribir (hoy en la zona de la estación). */
  readonly editableFrom = input.required<string>();
  /** Rango sugerido al abrir: el mes que se está viendo. */
  readonly defaultFrom = input.required<string>();
  readonly defaultTo = input.required<string>();
  readonly initialWorkerId = input<string | null>(null);
  readonly saving = input(false);
  /** El horario quedó guardado: el botón lo confirma antes de que se cierre el panel. */
  readonly saved = input(false);
  readonly submitError = input<unknown>(null);

  @Output() readonly submitted = new EventEmitter<AssignRangeRequest>();
  @Output() readonly cancelled = new EventEmitter<void>();

  readonly form = new FormGroup({
    workerId: new FormControl<string | null>(null, Validators.required),
    shiftCode: new FormControl<string | null>(null, Validators.required),
    from: new FormControl('', { nonNullable: true, validators: Validators.required }),
    to: new FormControl('', { nonNullable: true, validators: Validators.required }),
    leaveSundaysAsRest: new FormControl(true, { nonNullable: true }),
    note: new FormControl('', { nonNullable: true, validators: Validators.maxLength(255) })
  });

  /** Valores del formulario como señal, para que la vista previa se recalcule al escribir. */
  readonly value = signal(this.form.getRawValue());
  readonly maxDays = MAX_RANGE_DAYS;
  readonly triedSubmit = signal(false);

  readonly modeOptions: SegmentOption<SupportMode>[] = [
    { value: 'hours', label: 'Horario' },
    { value: 'rest', label: 'Descanso' }
  ];
  readonly mode = signal<SupportMode>('hours');
  readonly hours = new SupportHoursDraft();

  readonly workerOptions = computed<SelectOption<string>[]>(() =>
    this.workers().map(w => ({ value: w.id, label: `${w.fullName} · ${w.roleName === 'APOYO' ? 'Apoyo' : 'Titular'}` }))
  );

  readonly selectedWorker = computed(() => this.workers().find(w => w.id === this.value().workerId) ?? null);
  readonly isSupport = computed(() => this.selectedWorker()?.roleName === 'APOYO');
  readonly usesHours = computed(() => this.isSupport() && this.mode() === 'hours');

  readonly codeOptions = computed<SelectOption<string>[]>(() => {
    const role = this.selectedWorker()?.roleName;
    return this.catalog()
      .filter(c => c.active && (c.role === null || !role || c.role.name === role))
      .map(c => ({ value: c.code, label: `${c.code} · ${segmentsLabel(c.schedule?.segments ?? [])}` }));
  });

  readonly rangeError = computed<string | null>(() => {
    const { from, to } = this.value();
    if (!from || !to) return null;
    if (from < this.editableFrom()) return `Solo se puede asignar desde hoy (${shortDateLabel(this.editableFrom())}). Los días pasados no se tocan.`;
    if (to < from) return 'La fecha final no puede ser anterior a la inicial.';
    const days = daysBetween(from, to) + 1;
    if (days > MAX_RANGE_DAYS) return `Máximo ${MAX_RANGE_DAYS} días por vez; elegiste ${days}. Acorta el rango.`;
    return null;
  });

  /** El turno elegido, venga del selector de horas o de la lista de códigos; null mientras no esté completo. */
  private readonly chosen = computed<{ code: string; segments: ShiftScheduleSegment[] | null } | null>(() => {
    if (this.usesHours()) {
      const segments = this.hours.segments();
      return segments ? { code: codeForHours(segments, this.catalog()), segments } : null;
    }
    const code = this.value().shiftCode;
    return code ? { code, segments: null } : null;
  });

  readonly preview = computed<PreviewDay[]>(() => {
    const { from, to, leaveSundaysAsRest } = this.value();
    const chosen = this.chosen();
    if (!from || !to || !chosen || this.rangeError()) return [];
    const work = { ...chosen, text: this.shiftHours.text(chosen.code, chosen.segments) };
    const sundayRest = { code: 'DESCANSO', segments: null, text: 'DESCANSO' };
    return dateRange(from, to).map(date => {
      const rest = leaveSundaysAsRest && isSunday(date) && chosen.code !== 'DESCANSO';
      return { date, day: dayOfMonth(date), weekday: weekdayShort(date), rest, ...(rest ? sundayRest : work) };
    });
  });

  readonly summary = computed(() => {
    const days = this.preview();
    if (!days.length) return '';
    const rests = days.filter(d => d.rest).length;
    const work = days.length - rests;
    const name = this.selectedWorker()?.fullName ?? 'El vendedor';
    const shift = days.find(d => !d.rest)?.text ?? this.chosen()?.code;
    let text = `${name} queda con ${work} ${work === 1 ? 'día' : 'días'} de ${shift}`;
    if (rests) text += ` y ${rests} ${rests === 1 ? 'domingo' : 'domingos'} en descanso`;
    return `${text}. Lo que esos días tengan hoy se reemplaza.`;
  });

  readonly maxTo = computed(() => {
    const from = this.value().from;
    return from ? addDays(from, MAX_RANGE_DAYS - 1) : null;
  });

  constructor() {
    effect(() => {
      const from = this.defaultFrom() < this.editableFrom() ? this.editableFrom() : this.defaultFrom();
      const capped = addDays(from, MAX_RANGE_DAYS - 1);
      const to = this.defaultTo() > capped ? capped : this.defaultTo();
      const workerId = this.initialWorkerId();
      untracked(() => {
        this.mode.set('hours');
        this.hours.reset();
        this.form.reset({ workerId, shiftCode: null, from, to, leaveSundaysAsRest: true, note: '' });
        this.syncShiftControl(workerId);
        this.triedSubmit.set(false);
      });
    });

    effect(() => {
      const error = this.submitError();
      if (!error) return;
      untracked(() => {
        applyFieldErrors(this.form, error);
        if (this.usesHours()) this.hours.applyServerError(error, ['workerId', 'from', 'to', 'note']);
      });
    });

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.value.set(this.form.getRawValue()));

    this.form.controls.workerId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(workerId => this.syncShiftControl(workerId));
  }

  setMode(mode: SupportMode): void {
    this.mode.set(mode);
    this.syncShiftControl(this.form.controls.workerId.value);
  }

  submit(): void {
    if (this.saving() || this.saved()) return;
    this.triedSubmit.set(true);
    const usesHours = this.usesHours();
    const segments = usesHours ? this.hours.validate() : null;
    if (this.form.invalid || this.rangeError() || (usesHours && !segments)) {
      Object.values(this.form.controls).forEach(c => {
        c.markAsDirty();
        c.markAsTouched();
      });
      return;
    }
    const v = this.form.getRawValue();
    const range = {
      workerId: v.workerId ?? '',
      from: v.from,
      to: v.to,
      note: v.note.trim() || null,
      leaveSundaysAsRest: v.leaveSundaysAsRest
    };
    this.hours.clearServerErrors();
    this.submitted.emit(segments ? { ...range, segments } : { ...range, shiftCode: v.shiftCode ?? '' });
  }

  /**
   * Deja el código listo para el vendedor elegido, para no enviar algo que el backend rechazará: con horas no se
   * envía código, el descanso de un apoyo es DESCANSO y un titular no conserva un código de otro rol.
   */
  private syncShiftControl(workerId: string | null): void {
    const role = this.workers().find(w => w.id === workerId)?.roleName;
    const control = this.form.controls.shiftCode;
    if (role === 'APOYO') {
      if (this.mode() === 'hours') {
        control.setValue(null);
        control.disable();
      } else {
        control.enable();
        control.setValue('DESCANSO');
      }
      return;
    }
    control.enable();
    const code = control.value;
    const fits = this.catalog().some(c => c.code === code && c.active && (c.role === null || !role || c.role.name === role));
    if (code && !fits) control.setValue(null);
  }
}
