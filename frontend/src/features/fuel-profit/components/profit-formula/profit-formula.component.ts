import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';

import { SegmentOption, SegmentedControlComponent } from '../../../../atomic-design/molecules/segmented-control/segmented-control.component';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { formatGallonMoney } from '../../../../shared/helpers/fuel-price.helpers';
import { formatPercentage } from '../../../../shared/helpers/percentage.helpers';
import { FuelType } from '../../../../shared/models/dtos/tank.dto';
import { fuelIdentity } from '../../helpers/fuel-profit.helpers';
import { FuelProfitDto, FuelProfitReportDto } from '../../models/fuel-profit.dto';

const DEFAULT_FUEL: FuelType = 'MOTOR';

/** The parts of the sale of one gallon, when every one of them is known. */
function gallonParts(fuel: FuelProfitDto) {
  const values = fuel.referenceValues;
  const margin = fuel.referenceMarginPerGallon;
  if (!fuel.complete || margin === null || !values || values.salePrice === null || values.purchaseCost === null
    || values.freightPerGallon === null || values.financialCostPerGallon === null) {
    return null;
  }
  const sale = formatGallonMoney(values.salePrice);
  const purchase = formatGallonMoney(values.purchaseCost, true);
  const freight = formatGallonMoney(values.freightPerGallon);
  const financial = formatGallonMoney(values.financialCostPerGallon);
  const left = formatGallonMoney(Math.abs(margin), true);
  const loses = margin < 0;
  return {
    sale, purchase, freight, financial, left, loses,
    summary: `${sale} de venta: compra ${purchase}, flete ${freight}, costo financiero ${financial} y `
      + `${loses ? 'se pierden' : 'quedan'} ${left}`
  };
}

/** "Cómo se calcula": where the money of one gallon goes, for one fuel. */
@Component({
  selector: 'app-profit-formula',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SegmentedControlComponent],
  templateUrl: './profit-formula.component.html',
  styleUrl: './profit-formula.component.css'
})
export class ProfitFormulaComponent {
  readonly report = input.required<FuelProfitReportDto>();

  /** Starts on Corriente again with each report. */
  protected readonly selected = linkedSignal<FuelProfitReportDto, FuelType>({
    source: this.report,
    computation: () => DEFAULT_FUEL
  });

  /** The fuels whose example can be drawn, besides the one shown. */
  protected readonly options = computed<SegmentOption<FuelType>[]>(() =>
    this.report().fuels
      .filter(fuel => fuel.fuelType === this.selected() || gallonParts(fuel) !== null)
      .map(fuel => ({ value: fuel.fuelType, label: fuelIdentity(fuel.fuelType).name })));

  protected readonly fuel = computed(() => fuelIdentity(this.selected()));

  protected readonly parts = computed(() => {
    const fuel = this.report().fuels.find(item => item.fuelType === this.selected());
    return fuel ? gallonParts(fuel) : null;
  });

  protected readonly referenceDate = computed(() => formatShortDate(this.report().referenceDate, false));

  protected readonly commissionRule = computed(() => {
    const { appliedPercentage, commissionRate } = this.report().card;
    const percentage = appliedPercentage === null ? 'el %' : formatPercentage(appliedPercentage);
    return `ventas × ${percentage} con tarjeta × ${formatPercentage(commissionRate * 100, 4)}`;
  });
}
