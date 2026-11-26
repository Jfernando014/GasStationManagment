import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { AlertChipComponent } from '../../../../atomic-design/atoms/alert-chip/alert-chip.component';
import { DayRow, TotalsRow } from '../../helpers/variation-days.helpers';

/**
 * Day by day on a wide screen: what each day had, and how the month had accumulated up to it. The header, the
 * totals and the column of the day stay in view while the table scrolls.
 */
@Component({
  selector: 'app-variation-day-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ActionButtonDirective, AlertChipComponent],
  templateUrl: './variation-day-table.component.html',
  styleUrl: './variation-day-table.component.css'
})
export class VariationDayTableComponent {
  readonly rows = input.required<DayRow[]>();
  readonly totals = input.required<TotalsRow>();
  /** Name of the fuel, for the accessible name of the table. */
  readonly fuel = input.required<string>();

  protected readonly accumulated = signal(true);
}
