import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { formatCop, formatShortDate } from '../../../../shared/helpers/format.helpers';
import { formatGallonMoney } from '../../../../shared/helpers/fuel-price.helpers';
import { formatGallons } from '../../../../shared/helpers/inventory-display.helpers';
import { FuelDayDto } from '../../models/fuel-profit.dto';

const MISSING = 'Falta dato';

/** What a fuel did day by day: gallons, the sale price of that day, what each gallon left and the profit. */
@Component({
  selector: 'app-profit-day-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (rows().length === 0) {
      <p class="none">No tuvo ventas en este mes.</p>
    } @else {
      <div class="scroll" tabindex="0" role="region" [attr.aria-label]="'Ventas por día de ' + fuelName()">
        <table>
          <thead>
            <tr>
              <th scope="col">Día</th>
              <th scope="col">Galones</th>
              <th scope="col">Precio de venta</th>
              <th scope="col">Queda por galón</th>
              <th scope="col">Ganancia del día</th>
            </tr>
          </thead>
          <tbody>
            @for (row of rows(); track row.date) {
              <tr>
                <th scope="row">{{ row.label }}</th>
                <td>{{ row.gallons }}</td>
                <td [class.is-missing]="row.salePrice === missing">{{ row.salePrice }}</td>
                <td [class.is-missing]="row.margin === missing">{{ row.margin }}</td>
                <td [class.is-missing]="row.profit === missing">{{ row.profit }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <p class="hint">Ganancia de cada día antes de comisión.</p>
    }
  `,
  styleUrl: './profit-day-detail.component.css'
})
export class ProfitDayDetailComponent {
  readonly days = input.required<FuelDayDto[]>();
  readonly fuelName = input.required<string>();

  protected readonly missing = MISSING;

  protected readonly rows = computed(() => this.days().map(day => ({
    date: day.date,
    label: formatShortDate(day.date, false),
    gallons: formatGallons(day.gallons, 1),
    salePrice: day.salePrice === null ? MISSING : formatGallonMoney(day.salePrice),
    margin: day.marginPerGallon === null ? MISSING : formatGallonMoney(day.marginPerGallon, true),
    profit: day.profit === null ? MISSING : formatCop(day.profit)
  })));
}
