import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { WorkerPerformance } from '../../../../shared/models/dtos/worker-profile.dto';
import { formatGallons, formatWeekdayDate, toneForCode } from '../../helpers/worker-profile.helpers';

/** Side card of the performance tab: the best sale of the worker in a shift and how many closed shifts back it. */
@Component({
  selector: 'app-worker-profile-max-sale',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ShiftChipComponent],
  templateUrl: './worker-profile-max-sale.component.html',
  styleUrl: './worker-profile-max-sale.component.css'
})
export class WorkerProfileMaxSaleComponent {
  readonly performance = input.required<WorkerPerformance>();
  readonly catalog = input<Record<string, ShiftCode>>({});

  protected readonly sale = computed(() => {
    const maxSale = this.performance().maxSale;
    return maxSale
      ? {
          gallons: formatGallons(maxSale.gallons),
          date: formatWeekdayDate(maxSale.date),
          code: maxSale.shiftCode,
          tone: toneForCode(maxSale.shiftCode, this.catalog()[maxSale.shiftCode])
        }
      : null;
  });
}
