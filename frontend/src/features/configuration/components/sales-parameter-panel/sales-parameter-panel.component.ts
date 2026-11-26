import {
  ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, effect, inject, input, output, signal, untracked
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { map } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SpinnerComponent } from '../../../../atomic-design/atoms/spinner/spinner.component';
import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { SidePanelComponent } from '../../../../atomic-design/organisms/side-panel/side-panel.component';
import { SalesParametersService } from '../../../../core/services/sales-parameters.service';
import { newestFirst, valueOn } from '../../../../shared/helpers/dated-value.helpers';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { CALENDAR_DAY, startDateValidator } from '../../../../shared/helpers/form-control.helpers';
import { isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { SALES_PARAMETERS } from '../../../../shared/helpers/sales-parameter.helpers';
import { SalesParameterDto, SalesParameterKey } from '../../../../shared/models/dtos/sales-parameter.dto';

const SAVE_ERROR = 'No se pudo guardar. Revisa tu conexión e inténtalo de nuevo.';
const REJECTED_ERROR = 'No se pudo guardar. Revisa los datos e inténtalo de nuevo.';
const FORBIDDEN_ERROR = 'Solo el administrador puede cambiar este valor.';

/** Side panel to set a new value of a sales parameter from a date. */
@Component({
  selector: 'app-sales-parameter-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, ActionButtonDirective, SpinnerComponent, FormFieldComponent, SidePanelComponent],
  templateUrl: './sales-parameter-panel.component.html',
  // The same form and footer as the panel of the fuel prices.
  styleUrl: '../fuel-value-panel/fuel-value-panel.component.css'
})
export class SalesParameterPanelComponent implements OnInit {
  private readonly service = inject(SalesParametersService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly open = input(false);
  readonly parameterKey = input.required<SalesParameterKey>();
  /** Saved values of the parameter. */
  readonly values = input.required<SalesParameterDto[]>();
  readonly today = input.required<string>();
  readonly closed = output<void>();
  readonly saved = output<SalesParameterDto>();

  protected readonly meta = computed(() => SALES_PARAMETERS[this.parameterKey()]);

  protected readonly form = new FormGroup({
    value: new FormControl('', { nonNullable: true }),
    validFrom: new FormControl('', { nonNullable: true, validators: startDateValidator })
  });

  protected readonly saving = signal(false);
  protected readonly generalError = signal('');

  private readonly typed = toSignal(this.form.valueChanges.pipe(map(() => this.form.getRawValue())),
    { initialValue: this.form.getRawValue() });

  /** Which value stops ruling on the chosen date, or that the one of that very day is replaced. */
  protected readonly replacement = computed(() => {
    const date = this.typed().validFrom;
    if (!CALENDAR_DAY.test(date)) return '';
    const { format } = this.meta();
    const same = this.values().find(value => value.validFrom === date);
    if (same) return `Ya hay un valor desde ese día (${format(same.value)}). Al guardar, el nuevo lo reemplaza.`;
    const before = valueOn(this.values(), date);
    const after = newestFirst(this.values()).filter(value => value.validFrom > date).at(-1);
    const parts = [before
      ? `Desde ese día reemplaza a ${format(before.value)} (rige desde ${formatShortDate(before.validFrom)}).`
      : 'Antes de esa fecha no hay ningún valor cargado.'];
    if (after) parts.push(`Rige hasta que empiece el del ${formatShortDate(after.validFrom)}.`);
    return parts.join(' ');
  });

  constructor() {
    // The form starts over each time the panel opens.
    effect(() => {
      if (this.open()) {
        untracked(() => {
          this.generalError.set('');
          this.form.reset({ value: '', validFrom: this.today() });
        });
      }
    });
  }

  ngOnInit(): void {
    // The rules of the value depend on the parameter, which is only known once the inputs are set.
    const control = this.form.controls.value;
    control.addValidators((typed: AbstractControl<string>): ValidationErrors | null => {
      const { error } = this.meta().parse(typed.value);
      return error ? { parameterValue: error } : null;
    });
    control.updateValueAndValidity();
  }

  /** Writes the figure the way it will be read, so a misplaced comma is noticed before saving. */
  protected tidyValue(): void {
    const control = this.form.controls.value;
    const { value } = this.meta().parse(control.value);
    if (value !== null) control.setValue(this.meta().toInput(value));
  }

  protected close(): void {
    if (!this.saving()) this.closed.emit();
  }

  protected save(): void {
    if (this.saving()) return;
    this.form.controls.value.markAsDirty();
    this.form.controls.validFrom.markAsDirty();
    this.form.markAllAsTouched();
    const { value: text, validFrom } = this.form.getRawValue();
    const { value } = this.meta().parse(text);
    if (this.form.invalid || value === null) {
      const field = this.form.controls.value.invalid ? 'value' : 'date';
      this.host.nativeElement.querySelector<HTMLElement>(`#parameter-${field}`)?.focus();
      return;
    }
    this.saving.set(true);
    this.generalError.set('');
    this.service.register(this.parameterKey(), { value, validFrom }).subscribe({
      next: saved => {
        this.saving.set(false);
        this.saved.emit(saved);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const status = isApiError(error) ? error.status : 0;
        this.generalError.set(status === 403 ? FORBIDDEN_ERROR : status === 400 ? REJECTED_ERROR : SAVE_ERROR);
      }
    });
  }
}
