import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, output, signal, untracked
} from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { SidePanelComponent } from '../../../../atomic-design/organisms/side-panel/side-panel.component';
import { ToastService } from '../../../../core/services/toast.service';
import { WorkersService } from '../../../../core/services/workers.service';
import { SERVER_ERROR_KEY } from '../../../../shared/helpers/form-control.helpers';
import { addDays, capitalize, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { ParameterChange, parameterChanges } from '../../helpers/overtime-display.helpers';
import { MAX_REASON_LENGTH, MIN_REASON_LENGTH, ParameterVersion } from '../../models/overtime.model';
import { OvertimeParametersService } from '../../services/overtime-parameters.service';

const CALENDAR_DAY = /^\d{4}-\d{2}-\d{2}$/;

/** Ranges the backend accepts. */
const NIGHT_START = { min: 12, max: 23 };
const REST = { min: 0, max: 4 };
const REST_MIN_SHIFT = { min: 1, max: 24 };
const FIXED_HEDO = { min: 0, max: 80 };

function range(min: number, max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = String(control.value ?? '');
    return /^\d+$/.test(value) && Number(value) >= min && Number(value) <= max
      ? null : { range: `Debe estar entre ${min} y ${max}` };
  };
}

function validDay(control: AbstractControl): ValidationErrors | null {
  return CALENDAR_DAY.test(String(control.value ?? '')) ? null : { format: 'Elige una fecha' };
}

/** The reason is optional; if it is written it must have 3 to 255 characters. */
function optionalReason(control: AbstractControl): ValidationErrors | null {
  const length = String(control.value ?? '').trim().length;
  return length === 0 || (length >= MIN_REASON_LENGTH && length <= MAX_REASON_LENGTH)
    ? null : { length: `El motivo debe tener entre ${MIN_REASON_LENGTH} y ${MAX_REASON_LENGTH} caracteres` };
}

/** Side panel to create a new version of the overtime parameters; the version it replaces gives the first values. */
@Component({
  selector: 'app-parameter-version-drawer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ActionButtonDirective, FormFieldComponent, SidePanelComponent],
  templateUrl: './parameter-version-drawer.component.html',
  styleUrl: './parameter-version-drawer.component.css'
})
export class ParameterVersionDrawerComponent {
  private readonly service = inject(OvertimeParametersService);
  private readonly workers = inject(WorkersService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly open = input(false);
  /** Every version, the newest first: the newest one gives the first values and its number the next one. */
  readonly versions = input.required<ParameterVersion[]>();
  /** The version was created: the list has to be asked again. */
  readonly created = output<void>();
  readonly closed = output<void>();

  protected readonly submitting = signal(false);
  protected readonly generalError = signal('');
  /** Roles of the shifts catalog that can have the fixed HEDO line. */
  private readonly catalogRoles = signal<string[]>([]);
  protected readonly selectedRoles = signal<string[]>([]);

  protected readonly form = new FormGroup({
    validFrom: new FormControl('', { nonNullable: true, validators: [Validators.required, validDay] }),
    nightStartHour: new FormControl('', { nonNullable: true, validators: [Validators.required, range(NIGHT_START.min, NIGHT_START.max)] }),
    restHours: new FormControl('', { nonNullable: true, validators: [Validators.required, range(REST.min, REST.max)] }),
    restMinShiftHours: new FormControl('', { nonNullable: true, validators: [Validators.required, range(REST_MIN_SHIFT.min, REST_MIN_SHIFT.max)] }),
    fixedHedoHours: new FormControl('', { nonNullable: true, validators: [Validators.required, range(FIXED_HEDO.min, FIXED_HEDO.max)] }),
    reason: new FormControl('', { nonNullable: true, validators: [optionalReason] })
  });
  private readonly values = signal(this.form.getRawValue());

  protected readonly maxReasonLength = MAX_REASON_LENGTH;

  /** The version this one replaces: the newest one. */
  private readonly base = computed(() => this.versions()[0] ?? null);
  protected readonly nextNumber = computed(() => (this.base()?.number ?? 0) + 1);
  protected readonly heading = computed(() => `Nueva versión (v${this.nextNumber()})`);
  protected readonly roleOptions = computed(() =>
    [...new Set([...this.catalogRoles(), ...(this.base()?.fixedHedoRoles ?? [])])].sort());

  /** What the new version changes, to say it in one sentence before it is created. */
  protected readonly changes = computed<ParameterChange[]>(() => {
    const base = this.base();
    const value = this.values();
    const number = (text: string) => (text.trim() === '' ? Number.NaN : Number(text));
    return base ? parameterChanges(base, {
      nightStartHour: number(value.nightStartHour),
      restHours: number(value.restHours),
      restMinShiftHours: number(value.restMinShiftHours),
      fixedHedoHours: number(value.fixedHedoHours),
      fixedHedoRoles: this.selectedRoles()
    }) : [];
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.values.set(this.form.getRawValue()));
    // Each time the drawer opens it starts again from the newest version.
    effect(() => {
      if (this.open()) untracked(() => this.start());
    });
  }

  protected roleLabel(role: string): string {
    return capitalize(role.toLowerCase());
  }

  protected toggleRole(role: string, checked: boolean): void {
    this.selectedRoles.update(roles => checked ? [...new Set([...roles, role])] : roles.filter(item => item !== role));
  }

  protected submit(): void {
    this.generalError.set('');
    for (const control of Object.values(this.form.controls)) {
      control.markAsDirty();
      control.markAsTouched();
    }
    if (this.form.invalid || this.submitting()) return;
    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.service.create({
      validFrom: value.validFrom,
      nightStartHour: Number(value.nightStartHour),
      restHours: Number(value.restHours),
      restMinShiftHours: Number(value.restMinShiftHours),
      fixedHedoHours: Number(value.fixedHedoHours),
      fixedHedoRoles: this.selectedRoles(),
      reason: value.reason
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      this.submitting.set(false);
      if (result.outcome === 'OK') {
        this.toast.show(`Versión ${this.nextNumber()} creada`);
        this.created.emit();
        this.closed.emit();
        return;
      }
      const control = result.field ? this.form.get(result.field) : null;
      if (control) {
        control.setErrors({ [SERVER_ERROR_KEY]: result.message });
        control.markAsTouched();
      } else {
        this.generalError.set(result.message);
      }
    });
  }

  private start(): void {
    const base = this.base();
    this.generalError.set('');
    this.form.reset({
      validFrom: addDays(stationToday(), 1),
      nightStartHour: String(base?.nightStartHour ?? ''),
      restHours: String(base?.restHours ?? ''),
      restMinShiftHours: String(base?.restMinShiftHours ?? ''),
      fixedHedoHours: String(base?.fixedHedoHours ?? ''),
      reason: ''
    });
    this.selectedRoles.set([...(base?.fixedHedoRoles ?? [])]);
    this.values.set(this.form.getRawValue());
    if (this.catalogRoles().length === 0) {
      this.workers.listRoles().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: roles => this.catalogRoles.set(roles.map(role => role.name)),
        error: () => undefined
      });
    }
  }
}
