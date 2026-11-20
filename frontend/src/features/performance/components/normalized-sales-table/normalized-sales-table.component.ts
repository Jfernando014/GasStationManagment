import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { AvatarComponent } from '../../../../atomic-design/atoms/avatar/avatar.component';
import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { formatStatistics } from '../../../../shared/helpers/sales-statistics.helpers';
import { NormalizedSale, SellerCompliance } from '../../../../shared/models/dtos/seller-performance.dto';
import { plainReason, plainScenario } from '../../helpers/performance-view.helpers';

/**
 * "Ajustado por turno": what each seller really sold, the factor that makes a shift comparable with the others and the
 * adjusted sale, one line per scenario. It keeps the vendor chips to include or leave out a seller from the comparison.
 */
@Component({
  selector: 'app-normalized-sales-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AvatarComponent, InfoTooltipComponent, ShiftChipComponent, SkeletonComponent, EmptyStateComponent],
  templateUrl: './normalized-sales-table.component.html',
  styleUrl: './normalized-sales-table.component.css'
})
export class NormalizedSalesTableComponent {
  readonly rows = input.required<NormalizedSale[]>();
  /** Every seller of the period, for the chips. */
  readonly people = input<SellerCompliance[]>([]);
  readonly excluded = input<string[]>([]);
  readonly unit = input('gal');
  readonly loading = input(false);
  /** The period has results but the search left none. */
  readonly noMatches = input(false);
  /** Changes with every new answer, so the rows come in again. */
  readonly version = input(0);
  readonly toggleWorker = output<string>();

  protected readonly skeletonRows = [0, 1, 2, 3];
  protected readonly format = formatStatistics;
  protected readonly reasonText = plainReason;
  protected readonly scenarioText = plainScenario;
  protected readonly explanation = 'Un turno de noche y uno de día no se venden igual. El factor de ajuste deja las ventas de todos los turnos en una misma escala para poder compararlos. Los factores solo se leen aquí: no se pueden cambiar desde esta pantalla.';

  protected included(workerId: string): boolean {
    return !this.excluded().includes(workerId);
  }

  protected rowKey(row: NormalizedSale, index: number): string {
    return `${row.workerId}|${row.scenario.weekday}|${row.scenario.dayType}|${row.scenario.period}|${row.scenario.workedCode}|${row.scenario.effectiveRole}|${index}`;
  }
}
