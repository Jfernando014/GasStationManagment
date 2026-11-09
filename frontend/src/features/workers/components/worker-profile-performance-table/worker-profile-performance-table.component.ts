import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { PercentBarComponent } from '../../../../atomic-design/atoms/percent-bar/percent-bar.component';
import { RolePerformance, WorkerPerformance } from '../../../../shared/models/dtos/worker-profile.dto';
import {
  MIN_RELIABLE_SHIFTS,
  PERFORMANCE_WEEKDAY_LABELS,
  formatGallons,
  formatMonthYearShort,
  formatPercent
} from '../../helpers/worker-profile.helpers';

interface GroupView {
  shifts: number;
  average: string;
  percent: string;
  percentValue: number | null;
  /** Fewer shifts than the minimum: the numbers are shown in gray. */
  dim: boolean;
}

interface RowView {
  weekday: string;
  holder: GroupView;
  support: GroupView;
}

const HELP_TEXT =
  'Galones que vendió en promedio por turno, según el día de la semana. El rendimiento compara ese promedio con el ' +
  'de la estación para el mismo tipo de turno y día: 100 % es igual a la estación, y la rayita de la barra marca ese punto.';

/** Average gallons per shift and performance against the station, by weekday, as holder and as support. */
@Component({
  selector: 'app-worker-profile-performance-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InfoTooltipComponent, PercentBarComponent],
  templateUrl: './worker-profile-performance-table.component.html',
  styleUrl: './worker-profile-performance-table.component.css'
})
export class WorkerProfilePerformanceTableComponent {
  readonly performance = input.required<WorkerPerformance>();

  protected readonly helpText = HELP_TEXT;
  protected readonly minShifts = MIN_RELIABLE_SHIFTS;

  protected readonly rows = computed<RowView[]>(() =>
    this.performance().byWeekday.map(row => ({
      weekday: PERFORMANCE_WEEKDAY_LABELS[row.weekday],
      holder: this.group(row.asHolder),
      support: this.group(row.asSupport)
    }))
  );

  protected readonly hasDimmed = computed(() =>
    this.rows().some(row => row.holder.dim || row.support.dim)
  );

  protected readonly range = computed(() => {
    const { periodFrom, periodTo } = this.performance();
    return periodFrom && periodTo
      ? `Jornadas cerradas de ${formatMonthYearShort(periodFrom)} a ${formatMonthYearShort(periodTo)}`
      : '';
  });

  private group(role: RolePerformance): GroupView {
    const hasData = role.shifts > 0 && role.averageGallons !== null;
    return {
      shifts: role.shifts,
      average: hasData ? `${formatGallons(role.averageGallons as number)} gal` : '—',
      percent: hasData && role.performancePercent !== null ? formatPercent(role.performancePercent) : '—',
      percentValue: hasData ? role.performancePercent : null,
      dim: role.shifts < MIN_RELIABLE_SHIFTS
    };
  }
}
