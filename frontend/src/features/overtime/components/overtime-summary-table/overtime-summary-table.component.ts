import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { CATEGORY_HEADS, formatHours } from '../../helpers/overtime-display.helpers';
import { CategoryHours, OVERTIME_CATEGORIES, OvertimeSummaryRow } from '../../models/overtime.model';

interface Cell {
  text: string;
  zero: boolean;
  negative: boolean;
}

interface RowView {
  id: string;
  name: string;
  role: string | null;
  hours: number;
  cells: Cell[];
  status: { label: string; variant: 'green' | 'amber' };
}

function toCells(hours: CategoryHours): Cell[] {
  return OVERTIME_CATEGORIES.map(category => ({
    text: formatHours(hours[category]), zero: hours[category] === 0, negative: hours[category] < 0
  }));
}

function statusOf(row: OvertimeSummaryRow): RowView['status'] {
  if (row.incomplete) return { label: 'Incompleta', variant: 'amber' };
  if (row.noveltiesUnconfirmed) return { label: 'Sin novedades confirmadas', variant: 'amber' };
  return { label: 'Completa', variant: 'green' };
}

/** One row per worker with the hours to pay of the fortnight; a row opens the sheet of that worker. */
@Component({
  selector: 'app-overtime-summary-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, InfoTooltipComponent, SkeletonComponent, TagComponent],
  templateUrl: './overtime-summary-table.component.html',
  styleUrl: './overtime-summary-table.component.css'
})
export class OvertimeSummaryTableComponent {
  readonly rows = input<OvertimeSummaryRow[]>([]);
  readonly loading = input(false);
  /** Month and fortnight on screen: the sheet brings the user back to them. */
  readonly month = input.required<string>();
  readonly fortnight = input.required<number>();

  private readonly router = inject(Router);

  protected readonly heads = CATEGORY_HEADS;
  protected readonly skeletonRows = [0, 1, 2, 3];
  protected readonly queryParams = computed(() => ({ mes: this.month(), quincena: this.fortnight() }));

  /**
   * The link of the name covers the row on wide screens. With the worker column fixed (tablet and phone) the link
   * only covers its own cell, so a click on any other cell opens the sheet from here.
   */
  protected openRow(event: Event, id: string): void {
    if ((event.target as HTMLElement).closest('a')) return;
    void this.router.navigate(['/personal/horas-extra/trabajador', id], { queryParams: this.queryParams() });
  }

  protected readonly views = computed<RowView[]>(() => this.rows().map(row => ({
    id: row.worker.id,
    name: row.worker.name,
    role: row.worker.role,
    hours: row.grossHours,
    cells: toCells(row.totalToPay),
    status: statusOf(row)
  })));

  protected readonly totalHours = computed(() => this.rows().reduce((sum, row) => sum + row.grossHours, 0));

  protected readonly totalCells = computed<Cell[]>(() => {
    const totals = Object.fromEntries(OVERTIME_CATEGORIES.map(category =>
      [category, this.rows().reduce((sum, row) => sum + row.totalToPay[category], 0)])) as CategoryHours;
    return toCells(totals);
  });
}
