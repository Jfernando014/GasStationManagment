import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { formatDays, formatGallons, FUEL_COLORS, FUEL_LABELS } from '../../../../shared/helpers/inventory-display.helpers';
import { FuelAutonomyResponse } from '../../../../shared/models/dtos/fuel-autonomy.dto';

/** Step 5a: how many days each fuel lasts with the expected daily sales. */
@Component({
  selector: 'app-fuel-autonomy-table',
  standalone: true,
  templateUrl: './fuel-autonomy-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FuelAutonomyTableComponent {
  readonly autonomy = input.required<FuelAutonomyResponse[]>();

  readonly fuelLabels = FUEL_LABELS;
  readonly fuelColors = FUEL_COLORS;
  readonly formatGallons = formatGallons;
  readonly formatDays = formatDays;

  autonomyLabel(autonomy: FuelAutonomyResponse): string {
    if (autonomy.status === 'INCOMPLETE_MEASUREMENT') return 'Falta medición';
    if (autonomy.status === 'NO_SALES_HISTORY') return 'Sin historial de ventas';
    if (autonomy.level === 'CRITICAL') return 'Crítico';
    if (autonomy.level === 'WARNING') return 'Advertencia';
    return 'Normal';
  }

  autonomyTone(autonomy: FuelAutonomyResponse): string {
    if (autonomy.level === 'CRITICAL') return 'jr-chip--falta';
    if (autonomy.level === 'WARNING') return 'jr-chip--aviso';
    if (autonomy.level === 'OK') return 'jr-chip--cerrada';
    return 'jr-chip--pendiente';
  }
}
