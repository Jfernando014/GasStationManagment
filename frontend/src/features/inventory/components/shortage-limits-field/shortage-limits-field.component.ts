import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import {
  RECOMMENDED_VALUES, SHORTAGE_RANGES, VariationSettingForm, VariationSettingFormValue, checkRangeOrder
} from '../../helpers/variation-alert-settings.helpers';

/** The smallest bar of the staircase, so that an empty range is still drawn. */
const MIN_BAR_PERCENT = 6;

/**
 * Days with shortage allowed per month for each size of the shortage, with a staircase of bars that follows what is
 * typed. A range that allows more days than the one before it is marked.
 */
@Component({
  selector: 'app-shortage-limits-field',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormFieldComponent],
  templateUrl: './shortage-limits-field.component.html',
  styleUrl: './shortage-limits-field.component.css'
})
export class ShortageLimitsFieldComponent {
  readonly form = input.required<VariationSettingForm>();
  /** What the form holds; it changes with every change of the form, errors included. */
  readonly value = input.required<VariationSettingFormValue>();

  protected readonly ranges = SHORTAGE_RANGES;

  private readonly limits = computed(() => SHORTAGE_RANGES.map(range => this.value()[range.field]));
  protected readonly order = computed(() => checkRangeOrder(this.limits()));

  /** Heights in percent; the tallest recommended value fills the staircase unless a larger one is typed. */
  protected readonly bars = computed(() => {
    const numbers = this.limits().map(Number);
    const top = Math.max(RECOMMENDED_VALUES.shortageLimit1To49, ...numbers);
    return numbers.map(number => Math.max(MIN_BAR_PERCENT, (number / top) * 100));
  });
}
