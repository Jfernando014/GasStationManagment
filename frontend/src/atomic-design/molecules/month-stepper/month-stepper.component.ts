import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { monthOptionLabel } from '../../../shared/helpers/month-options.helpers';
import { addMonths } from '../../../shared/helpers/shift-display.helpers';

/**
 * Month picker with a button for the previous and the next month: "‹ Junio 2026 ›". A button is disabled at its
 * limit.
 */
@Component({
  selector: 'app-month-stepper',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button type="button" aria-label="Mes anterior" [disabled]="atStart()" (click)="step(-1)">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
        stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m15 18-6-6 6-6" /></svg>
    </button>
    <span aria-live="polite">{{ label() }}</span>
    <button type="button" aria-label="Mes siguiente" [disabled]="atEnd()" (click)="step(1)">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
        stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
    </button>
  `,
  styleUrl: './month-stepper.component.css',
  host: { role: 'group', 'aria-label': 'Mes' }
})
export class MonthStepperComponent {
  /** The month shown, "yyyy-MM". */
  readonly month = input.required<string>();
  /** First and last month that can be reached; null for no limit. */
  readonly min = input<string | null>(null);
  readonly max = input<string | null>(null);
  readonly monthChange = output<string>();

  protected readonly label = computed(() => monthOptionLabel(this.month(), ''));

  protected readonly atStart = computed(() => {
    const min = this.min();
    return min !== null && this.month() <= min;
  });

  protected readonly atEnd = computed(() => {
    const max = this.max();
    return max !== null && this.month() >= max;
  });

  protected step(months: number): void {
    this.monthChange.emit(addMonths(this.month(), months));
  }
}
