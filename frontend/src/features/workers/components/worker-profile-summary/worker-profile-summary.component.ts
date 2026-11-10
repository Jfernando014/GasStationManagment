import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { ShiftTone } from '../../../../shared/helpers/shift-display.helpers';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { WorkerMonthSummary } from '../../../../shared/models/dtos/worker-profile.dto';
import { monthName, toneForCode } from '../../helpers/worker-profile.helpers';

/** Side card of the month: scheduled hours, days per code and the totals the server calculated. */
@Component({
  selector: 'app-worker-profile-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ShiftChipComponent, InfoTooltipComponent],
  templateUrl: './worker-profile-summary.component.html',
  styleUrl: './worker-profile-summary.component.css'
})
export class WorkerProfileSummaryComponent {
  /** yyyy-MM. */
  readonly month = input.required<string>();
  readonly summary = input<WorkerMonthSummary | null>(null);
  readonly loading = input(false);
  /** The month could not be loaded: say so instead of showing the skeleton forever. */
  readonly failed = input(false);
  readonly catalog = input<Record<string, ShiftCode>>({});

  protected readonly monthWord = computed(() => monthName(this.month()));

  protected readonly codes = computed(() =>
    (this.summary()?.shiftsByCode ?? []).map(entry => ({
      ...entry,
      tone: toneForCode(entry.code, this.catalog()[entry.code]) as ShiftTone
    }))
  );
}
