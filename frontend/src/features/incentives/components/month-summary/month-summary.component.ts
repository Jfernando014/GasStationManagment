import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { AnimatedNumberComponent } from '../../../../atomic-design/atoms/animated-number/animated-number.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { formatCop } from '../../../../shared/helpers/format.helpers';

/** Total of the month and number of sellers with sales; both count up when they arrive or change. */
@Component({
  selector: 'app-month-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AnimatedNumberComponent, SkeletonComponent],
  templateUrl: './month-summary.component.html',
  styleUrl: './month-summary.component.css',
  host: { role: 'group', 'aria-label': 'Resumen del mes', '[attr.aria-busy]': 'loading()' }
})
export class MonthSummaryComponent {
  readonly total = input(0);
  readonly vendorCount = input(0);
  /** Name of the month in lower case: "agosto". */
  readonly monthName = input.required<string>();
  readonly loading = input(false);

  protected readonly formatCop = formatCop;
}
