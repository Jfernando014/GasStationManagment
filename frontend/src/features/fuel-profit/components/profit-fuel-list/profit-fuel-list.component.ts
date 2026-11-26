import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { formatGallons } from '../../../../shared/helpers/inventory-display.helpers';
import { formatPercentage } from '../../../../shared/helpers/percentage.helpers';
import { FuelType } from '../../../../shared/models/dtos/tank.dto';
import {
  UNDEFINED_FIGURE, commissionText, fuelIdentity, gallonsText, missingNotes, money
} from '../../helpers/fuel-profit.helpers';
import { FuelProfitDto, FuelProfitReportDto } from '../../models/fuel-profit.dto';
import { ProfitDayDetailComponent } from '../profit-day-detail/profit-day-detail.component';

const NO_TARGET = 'Meta no definida';

/** Compliance of the gallon target, as drawn under the gallons. */
interface TargetView {
  /** Width of the filled part, 0 to 100. */
  fill: number;
  reached: boolean;
  text: string;
}

function targetOf(gallons: number | null, compliance: number | null): TargetView | null {
  if (gallons === null) return null;
  const target = `${formatGallons(gallons, 1)} gal`;
  return {
    fill: Math.min(100, compliance ?? 0),
    reached: (compliance ?? 0) >= 100,
    text: compliance === null ? `Meta de ${target}` : `${formatPercentage(compliance, 1)} de ${target}`
  };
}

/**
 * Profit of each fuel and the total of the month. A table where there is room; one card per fuel on narrow screens.
 * Each fuel opens to show its days.
 */
@Component({
  selector: 'app-profit-fuel-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TagComponent, ProfitDayDetailComponent],
  templateUrl: './profit-fuel-list.component.html',
  styleUrl: './profit-fuel-list.component.css'
})
export class ProfitFuelListComponent {
  readonly report = input.required<FuelProfitReportDto>();
  /** A missing figure is fixed in "Datos del cálculo". */
  readonly openInputs = output<void>();

  protected readonly open = signal<ReadonlySet<FuelType>>(new Set());
  protected readonly noTarget = NO_TARGET;

  protected readonly rows = computed(() => this.report().fuels.map(fuel => this.toRow(fuel)));

  protected readonly total = computed(() => {
    const { totals, fuels } = this.report();
    const defined = totals.complete;
    const withTarget = fuels.length - totals.fuelsWithoutTarget.length;
    const figure = (value: number | null) => defined && value !== null ? money(value) : UNDEFINED_FIGURE;
    return {
      gallons: gallonsText(totals.gallons),
      target: totals.targetGallons === null
        ? null
        : targetOf(totals.targetGallons, totals.compliancePercentage),
      targetCount: `${withTarget} de ${fuels.length} combustibles tienen meta`,
      sales: money(totals.sales),
      before: figure(totals.realProfit),
      commission: defined && totals.commission !== null ? commissionText(totals.commission) : UNDEFINED_FIGURE,
      after: figure(totals.profitAfterCommission),
      targetProfit: defined && totals.targetProfit === null && withTarget < fuels.length
        ? NO_TARGET : figure(totals.targetProfit)
    };
  });

  protected toggle(type: FuelType): void {
    this.open.update(open => {
      const next = new Set(open);
      if (!next.delete(type)) next.add(type);
      return next;
    });
  }

  protected isUndefined(text: string): boolean {
    return text === UNDEFINED_FIGURE || text === NO_TARGET || text === '—';
  }

  private toRow(fuel: FuelProfitDto) {
    const profit = (value: number | null) => value === null ? UNDEFINED_FIGURE : money(value);
    return {
      ...fuelIdentity(fuel.fuelType),
      source: fuel,
      complete: fuel.complete,
      missing: missingNotes(fuel),
      gallons: gallonsText(fuel.gallons),
      target: targetOf(fuel.targetGallons, fuel.compliancePercentage),
      sales: money(fuel.sales),
      before: profit(fuel.realProfit),
      negative: (fuel.profitAfterCommission ?? 0) < 0,
      commission: fuel.commission === null ? UNDEFINED_FIGURE : commissionText(fuel.commission),
      after: profit(fuel.profitAfterCommission),
      targetProfit: fuel.targetGallons === null ? NO_TARGET : profit(fuel.targetProfit)
    };
  }
}
