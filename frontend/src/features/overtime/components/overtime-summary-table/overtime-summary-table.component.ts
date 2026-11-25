import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import {
  DotValueItem, DotValueListComponent
} from '../../../../atomic-design/molecules/dot-value-list/dot-value-list.component';
import { addCategoryHours, extraItems, hoursShort, roleLabel, surchargeItems } from '../../helpers/overtime-view.helpers';
import { OvertimeSummaryRow } from '../../models/overtime.model';

interface RowView {
  id: string;
  name: string;
  role: string;
  hours: string;
  surcharges: DotValueItem[];
  extras: DotValueItem[];
}

function fixedHours(row: OvertimeSummaryRow): number {
  return row.fixedHedo?.applies ? row.fixedHedo.hours : 0;
}

/** One row per worker with the hours of the fortnight; a row opens the detail of that worker. */
@Component({
  selector: 'app-overtime-summary-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, InfoTooltipComponent, SkeletonComponent, DotValueListComponent],
  templateUrl: './overtime-summary-table.component.html',
  styleUrl: './overtime-summary-table.component.css'
})
export class OvertimeSummaryTableComponent {
  readonly rows = input<OvertimeSummaryRow[]>([]);
  readonly loading = input(false);
  /** Month and fortnight on screen: the detail brings the user back to them. */
  readonly month = input.required<string>();
  readonly fortnight = input.required<number>();

  protected readonly skeletonRows = [0, 1, 2];
  protected readonly queryParams = computed(() => ({ mes: this.month(), quincena: this.fortnight() }));

  protected readonly views = computed<RowView[]>(() => this.rows().map(row => ({
    id: row.worker.id,
    name: row.worker.name,
    role: roleLabel(row.worker.role),
    hours: hoursShort(row.grossHours),
    surcharges: surchargeItems(row.totalToPay),
    extras: extraItems(row.totalToPay, fixedHours(row))
  })));

  protected readonly total = computed(() => {
    const rows = this.rows();
    const hours = addCategoryHours(rows.map(row => row.totalToPay));
    return {
      hours: hoursShort(rows.reduce((sum, row) => sum + row.grossHours, 0)),
      surcharges: surchargeItems(hours),
      extras: extraItems(hours, rows.reduce((sum, row) => sum + fixedHours(row), 0))
    };
  });
}
