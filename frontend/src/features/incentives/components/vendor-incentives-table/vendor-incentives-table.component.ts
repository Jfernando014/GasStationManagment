import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { SearchFieldComponent } from '../../../../atomic-design/atoms/search-field/search-field.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { formatCop, formatShortDate } from '../../../../shared/helpers/format.helpers';
import { monthLabel } from '../../../../shared/helpers/shift-display.helpers';
import {
  IncentiveLine, INCENTIVES_START_MONTH, VendorIncentive
} from '../../models/incentive-calculation.model';

type SortKey = 'name' | 'total';

interface LineView {
  key: string;
  sku: string;
  name: string;
  /** "Todo el mes", "1 al 15 ago" or "Desde el 16 ago". */
  when: string;
  /** The second (or later) value of an additive whose rule changed during the month. */
  changed: boolean;
  /** Same additive as the line above: its SKU and name are shown dimmed. */
  repeated: boolean;
  units: number;
  unitValue: string;
  subtotal: string;
}

interface VendorRowView {
  document: string;
  name: string;
  /** False when the seller sold nothing in the month: the total is replaced by "Sin ventas este mes". */
  hasSales: boolean;
  monthTotal: string;
  accumulated: string;
  /** "faltan 3 unidades sin valorar"; null when the accumulated total is complete. */
  missing: string | null;
  lines: LineView[];
  unitsTotal: number;
}

const MISSING_ONE = 'falta 1 unidad sin valorar';

/** Removes accents and case so "Mónica" is found by "monica". */
function plain(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLocaleLowerCase('es');
}

/** What the "Valor aplicado" column says: the days of the month the value applied. */
function whenText(line: IncentiveLine, split: boolean): string {
  const { periodStart: start, periodEnd: end } = line;
  if (end) {
    return start === end ? `El ${formatShortDate(end, false)}` : `${Number(start.slice(8, 10))} al ${formatShortDate(end, false)}`;
  }
  // A value that held from the 1st to the end of the month needs no dates when it is the only one.
  return split || !start.endsWith('-01') ? `Desde el ${formatShortDate(start, false)}` : 'Todo el mes';
}

/** Lines of the same additive stay together, in the order of their days. */
function groupBySku(lines: IncentiveLine[]): IncentiveLine[][] {
  const groups = new Map<string, IncentiveLine[]>();
  for (const line of lines) {
    groups.set(line.sku, [...(groups.get(line.sku) ?? []), line]);
  }
  return [...groups.values()].map(group => [...group].sort((a, b) => a.periodStart.localeCompare(b.periodStart)));
}

function toLineViews(lines: IncentiveLine[]): LineView[] {
  return groupBySku(lines).flatMap(group => group.map<LineView>((line, index) => ({
    key: `${line.sku}-${line.valuePerUnit}-${line.periodStart}`,
    sku: line.sku,
    name: line.productName,
    when: whenText(line, group.length > 1),
    changed: index > 0,
    repeated: index > 0,
    units: line.units,
    unitValue: formatCop(line.valuePerUnit),
    subtotal: formatCop(line.subtotal)
  })));
}

function toRowView(vendor: VendorIncentive): VendorRowView {
  const missingUnits = vendor.accumulatedUnresolvedUnits;
  return {
    document: vendor.document,
    name: vendor.name,
    hasSales: vendor.lines.length > 0 || vendor.unresolved.length > 0,
    monthTotal: formatCop(vendor.monthTotal),
    accumulated: formatCop(vendor.accumulatedTotal),
    missing: missingUnits <= 0 ? null : missingUnits === 1 ? MISSING_ONE : `faltan ${missingUnits} unidades sin valorar`,
    lines: toLineViews(vendor.lines),
    unitsTotal: vendor.lines.reduce((sum, line) => sum + line.units, 0)
  };
}

/** Sellers of the month: search by name or document, order by name or total, and the detail of each one. */
@Component({
  selector: 'app-vendor-incentives-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InfoTooltipComponent, SearchFieldComponent, SkeletonComponent],
  templateUrl: './vendor-incentives-table.component.html',
  styleUrl: './vendor-incentives-table.component.css'
})
export class VendorIncentivesTableComponent {
  readonly vendors = input<VendorIncentive[]>([]);
  readonly loading = input(false);
  /** Name of the month in lower case: "agosto". */
  readonly monthName = input.required<string>();
  /** First month of the accumulated total, "yyyy-MM"; null until the backend answers. */
  readonly accumulatedFrom = input<string | null>(null);

  protected readonly query = signal('');
  protected readonly sort = signal<SortKey>('name');
  /** Document of the seller whose detail is open; only one at a time. */
  protected readonly openDocument = signal<string | null>(null);
  protected readonly skeletonRows = [0, 1, 2, 3, 4];

  protected readonly accumulatedHelp = computed(() =>
    `Suma de todos sus incentivos por aditivos desde ${monthLabel(this.accumulatedFrom() ?? INCENTIVES_START_MONTH)}. No se reinicia cada mes.`);

  protected readonly rows = computed<VendorRowView[]>(() => {
    const query = plain(this.query().trim());
    const vendors = this.vendors().filter(vendor =>
      !query || plain(vendor.name).includes(query) || plain(vendor.document).includes(query));
    const byName = (a: VendorIncentive, b: VendorIncentive) => a.name.localeCompare(b.name, 'es');
    const sorted = [...vendors].sort(this.sort() === 'total'
      ? (a, b) => b.monthTotal - a.monthTotal || byName(a, b)
      : byName);
    return sorted.map(toRowView);
  });

  protected toggle(document: string): void {
    this.openDocument.update(open => (open === document ? null : document));
  }
}
