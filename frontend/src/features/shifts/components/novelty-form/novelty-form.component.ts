import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Output, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { SelectFieldComponent, SelectOption } from '../../../../atomic-design/molecules/select-field/select-field.component';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { applyFieldErrors, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { segmentsLabel } from '../../../../shared/helpers/shift-display.helpers';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { DayStaffStepResponse, NoveltyType, RegisterNoveltyRequest } from '../../../../shared/models/dtos/day-staff.dto';

/** Desde dónde se abre el formulario: la fila de quien trabaja, o la de quien descansa y va a cubrir. */
export interface NoveltyContext {
  mode: 'member' | 'resting';
  workerId: string;
}

interface NoveltyTypeOption {
  value: NoveltyType;
  label: string;
  help: string;
}

const NOVELTY_TYPES: NoveltyTypeOption[] = [
  { value: 'REPLACEMENT', label: 'Reemplazo', help: 'no trabaja y otra persona cubre su turno.' },
  { value: 'ABSENCE', label: 'Ausencia', help: 'no trabaja y nadie lo cubre: sale del personal del día.' },
  { value: 'SHIFT_CHANGE', label: 'Cambio de turno', help: 'trabaja un turno distinto al de la malla.' },
  { value: 'SWAP', label: 'Intercambio', help: 'intercambia su turno con otra persona que también trabaja hoy.' }
];

const REASON_MIN = 3;
const REASON_MAX = 255;

/** Formulario de una novedad del día. No guarda nada: emite la solicitud y el panel padre la envía. */
@Component({
  selector: 'app-novelty-form',
  standalone: true,
  imports: [ReactiveFormsModule, ActionButtonDirective, FormFieldComponent, SelectFieldComponent, ShiftChipComponent],
  templateUrl: './novelty-form.component.html',
  styleUrl: './novelty-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NoveltyFormComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly step = input.required<DayStaffStepResponse>();
  readonly catalog = input<ShiftCode[]>([]);
  readonly context = input.required<NoveltyContext>();
  readonly saving = input(false);
  /** Error devuelto por el servidor al guardar; sus errores por campo se marcan en el formulario. */
  readonly submitError = input<unknown>(null);

  @Output() readonly submitted = new EventEmitter<RegisterNoveltyRequest>();
  @Output() readonly cancelled = new EventEmitter<void>();

  readonly form = new FormGroup({
    type: new FormControl<NoveltyType | null>('REPLACEMENT'),
    workerId: new FormControl<string | null>(null, Validators.required),
    counterpartWorkerId: new FormControl<string | null>(null),
    shiftCode: new FormControl<string | null>(null),
    exceptionalCoverage: new FormControl(false, { nonNullable: true }),
    reason: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(REASON_MIN), Validators.maxLength(REASON_MAX)]
    })
  });

  readonly selectedType = signal<NoveltyType>('REPLACEMENT');
  readonly selectedWorkerId = signal<string | null>(null);

  readonly typeOptions = computed(() =>
    this.context().mode === 'resting' ? NOVELTY_TYPES.filter(t => t.value === 'REPLACEMENT') : NOVELTY_TYPES
  );

  readonly mainWorkerName = computed(() => this.nameOf(this.selectedWorkerId()) ?? 'El vendedor');

  readonly typeHelp = computed(() => {
    const type = NOVELTY_TYPES.find(t => t.value === this.selectedType());
    return type ? `${this.mainWorkerName()} ${type.help}` : '';
  });

  /** Quién trabaja hoy: puede ser el ausente de un reemplazo o el otro de un intercambio. */
  readonly memberOptions = computed<SelectOption<string>[]>(() =>
    this.step().members
      .filter(m => m.noveltyType === 'NONE')
      .map(m => ({ value: m.workerId, label: `${m.fullName} · ${m.workedCode}` }))
  );

  readonly swapOptions = computed<SelectOption<string>[]>(() =>
    this.memberOptions().filter(o => o.value !== this.selectedWorkerId())
  );

  /** Quien cubre un reemplazo sale de los que descansan hoy. */
  readonly restingOptions = computed<SelectOption<string>[]>(() =>
    this.step().resting.map(r => ({ value: r.workerId, label: r.fullName }))
  );

  readonly codeOptions = computed<SelectOption<string>[]>(() =>
    this.catalog()
      .filter(c => c.active && c.code !== 'DESCANSO')
      .map(c => ({ value: c.code, label: `${c.code} · ${segmentsLabel(c.schedule?.segments ?? [])}` }))
  );

  /** Código de la malla de la persona principal: el reemplazo lo hereda si no se elige otro. */
  readonly gridCode = computed(() => this.step().members.find(m => m.workerId === this.selectedWorkerId())?.workedCode ?? null);

  /** El servidor pide marcar la cobertura excepcional: se resalta la casilla para que se vea qué falta. */
  readonly needsExceptional = computed(() => {
    const error = this.submitError();
    return isApiError(error) && error.code === 'EXCEPTIONAL_COVERAGE_REQUIRED' && !this.exceptionalChecked();
  });
  readonly exceptionalChecked = signal(false);

  readonly reasonLength = signal(0);
  readonly reasonMax = REASON_MAX;

  constructor() {
    effect(() => {
      const ctx = this.context();
      this.form.reset({
        type: 'REPLACEMENT',
        workerId: ctx.mode === 'member' ? ctx.workerId : null,
        counterpartWorkerId: ctx.mode === 'resting' ? ctx.workerId : null,
        shiftCode: null,
        exceptionalCoverage: false,
        reason: ''
      });
      this.selectedType.set('REPLACEMENT');
      this.selectedWorkerId.set(ctx.mode === 'member' ? ctx.workerId : null);
      this.applyTypeRules('REPLACEMENT');
    });

    effect(() => {
      const error = this.submitError();
      if (error) {
        applyFieldErrors(this.form, error);
      }
    });

    this.form.controls.workerId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(id => this.selectedWorkerId.set(id));
    this.form.controls.exceptionalCoverage.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(checked => this.exceptionalChecked.set(checked));
    this.form.controls.reason.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(text => this.reasonLength.set(text.length));
  }

  chooseType(type: NoveltyType): void {
    this.form.controls.type.setValue(type);
    this.selectedType.set(type);
    this.applyTypeRules(type);
  }

  /** Cada tipo pide campos distintos; se ajustan los obligatorios para validar en línea solo lo que se ve. */
  private applyTypeRules(type: NoveltyType): void {
    const { counterpartWorkerId, shiftCode } = this.form.controls;
    const needsCounterpart = type === 'REPLACEMENT' || type === 'SWAP';
    counterpartWorkerId.setValidators(needsCounterpart ? Validators.required : null);
    shiftCode.setValidators(type === 'SHIFT_CHANGE' ? Validators.required : null);
    if (!needsCounterpart && this.context().mode === 'member') {
      counterpartWorkerId.setValue(null);
    }
    if (type !== 'SHIFT_CHANGE' && type !== 'REPLACEMENT') {
      shiftCode.setValue(null);
    }
    counterpartWorkerId.updateValueAndValidity();
    shiftCode.updateValueAndValidity();
  }

  submit(): void {
    if (this.form.invalid) {
      Object.values(this.form.controls).forEach(control => {
        control.markAsDirty();
        control.markAsTouched();
      });
      return;
    }
    const value = this.form.getRawValue();
    const type = value.type ?? 'REPLACEMENT';
    this.submitted.emit({
      type,
      workerId: value.workerId ?? '',
      counterpartWorkerId: type === 'REPLACEMENT' || type === 'SWAP' ? value.counterpartWorkerId : null,
      shiftCode: type === 'SHIFT_CHANGE' || type === 'REPLACEMENT' ? value.shiftCode : null,
      exceptionalCoverage: value.exceptionalCoverage,
      reason: value.reason.trim()
    });
  }

  nameOf(workerId: string | null): string | null {
    if (!workerId) return null;
    const step = this.step();
    return step.members.find(m => m.workerId === workerId)?.fullName
      ?? step.resting.find(r => r.workerId === workerId)?.fullName
      ?? step.absentees.find(a => a.workerId === workerId)?.fullName
      ?? null;
  }
}
