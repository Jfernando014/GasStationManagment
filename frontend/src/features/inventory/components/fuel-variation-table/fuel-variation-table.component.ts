import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { formatGallons, FUEL_COLORS, FUEL_LABELS } from '../../../../shared/helpers/inventory-display.helpers';
import { FuelVariationResponse } from '../../../../shared/models/dtos/fuel-variation.dto';

/** Step 5a: difference between the theoretical and the measured fuel of each type. */
@Component({
  selector: 'app-fuel-variation-table',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './fuel-variation-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FuelVariationTableComponent {
  readonly variations = input.required<FuelVariationResponse[]>();

  readonly fuelLabels = FUEL_LABELS;
  readonly fuelColors = FUEL_COLORS;
  readonly formatGallons = formatGallons;

  variationLabel(variation: FuelVariationResponse): string {
    if (variation.status === 'NO_PREVIOUS_MEASUREMENT') return 'Sin medición anterior';
    if (variation.status === 'INCOMPLETE_MEASUREMENT') return 'Medición incompleta';
    if (variation.status === 'NO_SALES_DATA') return 'Sin ventas del paso 2';
    return variation.outOfTolerance ? 'Fuera de tolerancia' : 'Cuadra';
  }

  variationTone(variation: FuelVariationResponse): string {
    if (variation.status !== 'OK') return 'jr-chip--aviso';
    return variation.outOfTolerance ? 'jr-chip--falta' : 'jr-chip--cerrada';
  }
}
