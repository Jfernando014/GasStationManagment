import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { NumberStepperComponent } from '../../../../atomic-design/molecules/number-stepper/number-stepper.component';
import { getControlErrorMessage } from '../../../../shared/helpers/form-control.helpers';
import { formatInputDate } from '../../../../shared/helpers/format.helpers';
import {
  MAX_ESCALATION_DAYS, MIN_ESCALATION_DAYS, VariationSettingForm, VariationSettingFormValue,
  formatLimitPercentage, parseLimitPercentage
} from '../../helpers/variation-alert-settings.helpers';
import { ShortageLimitsFieldComponent } from '../shortage-limits-field/shortage-limits-field.component';

/** The fields of a variation alert setting. The form belongs to the screen that saves it. */
@Component({
  selector: 'app-variation-alert-setting-fields',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormFieldComponent, NumberStepperComponent, ShortageLimitsFieldComponent],
  templateUrl: './variation-alert-setting-fields.component.html',
  styleUrl: './variation-alert-setting-fields.component.css'
})
export class VariationAlertSettingFieldsComponent {
  readonly form = input.required<VariationSettingForm>();
  /** What the form holds; it changes with every change of the form, errors included. */
  readonly value = input.required<VariationSettingFormValue>();
  /** The base covers every earlier month: its date cannot change. */
  readonly base = input(false);
  /** The server refused the date because another setting starts on it. */
  readonly dateTaken = input(false);
  /** Explains which months the date covers. */
  readonly dateNote = input('');

  protected readonly minDays = MIN_ESCALATION_DAYS;
  protected readonly maxDays = MAX_ESCALATION_DAYS;
  protected readonly formatDate = formatInputDate;

  protected errorOf(field: 'validFrom' | 'escalationDays'): string | null {
    return getControlErrorMessage(this.form().controls[field]);
  }

  protected setDays(days: number): void {
    const control = this.form().controls.escalationDays;
    control.markAsDirty();
    control.setValue(days);
  }

  /** Writes the percentage with a decimal comma once it has been typed. */
  protected tidyPercentage(): void {
    const control = this.form().controls.cumulativeLimitPercentage;
    if (control.invalid) return;
    const value = parseLimitPercentage(control.value);
    if (value === null) return;
    const text = formatLimitPercentage(value);
    if (text !== control.value) control.setValue(text);
  }
}
