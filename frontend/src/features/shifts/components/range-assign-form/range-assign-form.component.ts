import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Output, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { SelectFieldComponent, SelectOption } from '../../../../atomic-design/molecules/select-field/select-field.component';
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
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { AssignRangeRequest } from '../../../../shared/models/dtos/shift-schedule.dto';
import { Worker } from '../../../../shared/models/dtos/worker.dto';

/** Tope del backend para asignar un código a un rango. */
export const MAX_RANGE_DAYS = 62;

interface PreviewDay {
  date: string;
  day: number;
  weekday: string;
  code: string;
  rest: boolean;
}

/** Asigna un mismo código a un vendedor en un rango de días, con vista previa antes de guardar. */
@Component({
  selector: 'app-range-assign-form',
  standalone: true,
  imports: [ReactiveFormsModule, ActionButtonDirective, SelectFieldComponent, ShiftChipComponent],
  templateUrl: './range-assign-form.component.html',
  styleUrl: './range-assign-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RangeAssignFormComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly workers = input<Worker[]>([]);
  readonly catalog = input<ShiftCode[]>([]);
  /** Primer día que se puede escribir (hoy en la zona de la estación). */
  readonly editableFrom = input.required<string>();
  /** Rango sugerido al abrir: el mes que se está viendo. */
  readonly defaultFrom = input.required<string>();
  readonly defaultTo = input.required<string>();
  readonly initialWorkerId = input<string | null>(null);
  readonly saving = input(false);
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

  readonly workerOptions = computed<SelectOption<string>[]>(() =>
    this.workers().map(w => ({ value: w.id, label: `${w.fullName} · ${w.roleName === 'APOYO' ? 'Apoyo' : 'Titular'}` }))
  );

  readonly selectedWorker = computed(() => this.workers().find(w => w.id === this.value().workerId) ?? null);

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

  readonly preview = computed<PreviewDay[]>(() => {
    const { from, to, shiftCode, leaveSundaysAsRest } = this.value();
    if (!from || !to || !shiftCode || this.rangeError()) return [];
    return dateRange(from, to).map(date => {
      const rest = leaveSundaysAsRest && isSunday(date) && shiftCode !== 'DESCANSO';
      return { date, day: dayOfMonth(date), weekday: weekdayShort(date), code: rest ? 'DESCANSO' : shiftCode, rest };
    });
  });

  readonly summary = computed(() => {
    const days = this.preview();
    if (!days.length) return '';
    const rests = days.filter(d => d.rest).length;
    const work = days.length - rests;
    const name = this.selectedWorker()?.fullName ?? 'El vendedor';
    let text = `${name} queda con ${work} ${work === 1 ? 'día' : 'días'} de ${this.value().shiftCode}`;
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
      this.form.reset({
        workerId: this.initialWorkerId(),
        shiftCode: null,
        from,
        to: this.defaultTo() > capped ? capped : this.defaultTo(),
        leaveSundaysAsRest: true,
        note: ''
      });
      this.triedSubmit.set(false);
    });

    effect(() => {
      const error = this.submitError();
      if (error) applyFieldErrors(this.form, error);
    });

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.value.set(this.form.getRawValue()));

    // Si cambia el vendedor y el código ya no es de su rol, se limpia para no enviar algo que el backend rechazará
    this.form.controls.workerId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      const code = this.form.controls.shiftCode.value;
      if (code && !this.codeOptions().some(o => o.value === code)) this.form.controls.shiftCode.setValue(null);
    });
  }

  submit(): void {
    this.triedSubmit.set(true);
    if (this.form.invalid || this.rangeError()) {
      Object.values(this.form.controls).forEach(c => {
        c.markAsDirty();
        c.markAsTouched();
      });
      return;
    }
    const v = this.form.getRawValue();
    this.submitted.emit({
      workerId: v.workerId ?? '',
      from: v.from,
      to: v.to,
      shiftCode: v.shiftCode ?? '',
      note: v.note.trim() || null,
      leaveSundaysAsRest: v.leaveSundaysAsRest
    });
  }
}
