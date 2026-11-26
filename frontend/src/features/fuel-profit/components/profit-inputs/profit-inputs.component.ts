import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { WarningPanelComponent } from '../../../../atomic-design/molecules/warning-panel/warning-panel.component';
import { SalesParametersService } from '../../../../core/services/sales-parameters.service';
import { valueOn } from '../../../../shared/helpers/dated-value.helpers';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { formatGallonMoney, gallonMargin } from '../../../../shared/helpers/fuel-price.helpers';
import { monthName } from '../../../../shared/helpers/month-options.helpers';
import { formatPercentage } from '../../../../shared/helpers/percentage.helpers';
import { SalesParameterDto } from '../../../../shared/models/dtos/sales-parameter.dto';
import { SalesParameterCardComponent } from '../../../configuration/components/sales-parameter-card/sales-parameter-card.component';
import { fuelIdentity } from '../../helpers/fuel-profit.helpers';
import { FuelProfitDto, FuelProfitReportDto, MissingDatum } from '../../models/fuel-profit.dto';
import { CardPercentageComponent } from '../card-percentage/card-percentage.component';

const PRICE_DATA: Partial<Record<MissingDatum, string>> = {
  SALE_PRICE: 'precio de venta',
  PURCHASE_COST: 'costo de compra'
};

/** The line under a fuel that had no price or no cost at some point of the month. */
function priceNote(fuel: FuelProfitDto): string {
  return fuel.missingData
    .filter(item => PRICE_DATA[item.datum])
    .map(item => `Faltó el ${PRICE_DATA[item.datum]} desde el ${formatShortDate(item.since, false)}`)
    .join(' · ');
}

/** "Datos del cálculo": everything the formula uses, and what the administrator may change here. */
@Component({
  selector: 'app-profit-inputs',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, WarningPanelComponent, SalesParameterCardComponent, CardPercentageComponent],
  templateUrl: './profit-inputs.component.html',
  styleUrl: './profit-inputs.component.css'
})
export class ProfitInputsComponent implements OnInit {
  private readonly parameters = inject(SalesParametersService);

  readonly report = input.required<FuelProfitReportDto>();
  readonly canEdit = input(false);
  readonly refreshing = input(false);
  /** The freight or the financial cost changed: the report has to be read again. */
  readonly parameterChanged = output<void>();
  readonly cardSettingSaved = output<void>();

  /** History of the commission, to say since when the one of the month rules. */
  private readonly commissions = signal<SalesParameterDto[]>([]);

  protected readonly month = computed(() => monthName(this.report().month));

  protected readonly prices = computed(() => this.report().fuels.map(fuel => {
    const sale = fuel.referenceValues?.salePrice ?? null;
    const purchase = fuel.referenceValues?.purchaseCost ?? null;
    return {
      ...fuelIdentity(fuel.fuelType),
      sale: sale === null ? 'Sin precio' : formatGallonMoney(sale),
      saleMissing: sale === null,
      purchase: purchase === null ? 'Sin costo' : formatGallonMoney(purchase, true),
      purchaseMissing: purchase === null,
      difference: sale === null || purchase === null ? '—' : formatGallonMoney(gallonMargin(sale, purchase), true),
      note: priceNote(fuel)
    };
  }));

  protected readonly commission = computed(() => {
    const { commissionRate, commissionRateDate } = this.report().card;
    const since = valueOn(this.commissions(), commissionRateDate)?.validFrom;
    return {
      rate: formatPercentage(commissionRate * 100, 4),
      since: since ? `vigente desde el ${formatShortDate(since)}` : ''
    };
  });

  ngOnInit(): void {
    // Without the history the rate still shows, only without its date.
    this.parameters.history('credibanco-commission-rate').subscribe({
      next: values => this.commissions.set(values),
      error: () => this.commissions.set([])
    });
  }
}
