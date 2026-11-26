import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { AnimatedNumberComponent } from '../../../../atomic-design/atoms/animated-number/animated-number.component';
import { SparklineComponent } from '../../../../atomic-design/atoms/sparkline/sparkline.component';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { FuelSummary, formatGallonMoney } from '../../../../shared/helpers/fuel-price.helpers';
import { FuelValueKind } from '../../../../shared/models/dtos/fuel-price.dto';

const formatWhole = (value: number) => formatGallonMoney(Math.round(value));
const formatCents = (value: number) => formatGallonMoney(value, true);

/**
 * Summary of one fuel: sale price and purchase cost that rule today, how they changed and the difference between
 * them. The whole card picks the fuel; the "Agregar" buttons of a missing value sit above that area.
 */
@Component({
  selector: 'app-fuel-price-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AnimatedNumberComponent, SparklineComponent],
  templateUrl: './fuel-price-card.component.html',
  styleUrl: './fuel-price-card.component.css',
  host: { '[style.--fuel]': 'fuel().color' }
})
export class FuelPriceCardComponent {
  readonly fuel = input.required<FuelSummary>();
  readonly selected = input(false);
  readonly chosen = output<void>();
  readonly add = output<FuelValueKind>();

  protected readonly sides = computed(() => {
    const fuel = this.fuel();
    return [
      {
        kind: 'sale' as const,
        label: 'Venta',
        missing: 'Sin precio de venta',
        addLabel: `Agregar el precio de venta de ${fuel.name}`,
        ...fuel.sale,
        // Decided with the final figure, so the cents do not flicker while it counts up.
        format: Number.isInteger(fuel.sale.value) ? formatWhole : formatCents
      },
      {
        kind: 'purchase' as const,
        label: 'Compra',
        missing: 'Sin costo de compra',
        addLabel: `Agregar el costo de compra de ${fuel.name}`,
        ...fuel.purchase,
        format: formatCents
      }
    ];
  });

  protected readonly margin = computed(() => {
    const margin = this.fuel().margin;
    return margin === null ? null : formatGallonMoney(margin, true);
  });

  protected scheduledText(day: string): string {
    return `Hay uno programado desde el ${formatShortDate(day)}`;
  }
}
