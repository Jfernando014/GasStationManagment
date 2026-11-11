import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { WarningPanelComponent } from '../../../../atomic-design/molecules/warning-panel/warning-panel.component';
import { UnresolvedReason, UnresolvedUnits } from '../../models/incentive-calculation.model';

interface UnresolvedRowView {
  key: string;
  sku: string;
  /** Null when Ventas sent no name for the product. */
  name: string | null;
  units: number;
  reason: string;
  linkLabel: string;
  linkTarget: string;
}

/** What the user reads for each reason, and where the problem is fixed. */
const REASON_VIEW: Record<UnresolvedReason, { text: string; linkLabel: string; linkTarget: string }> = {
  NO_RULE_IN_FORCE: {
    text: 'Ese día el aditivo no tenía valor de incentivo.',
    linkLabel: 'Ir a Reglas',
    linkTarget: '/resultados/incentivos/aditivos'
  },
  PRODUCT_NOT_FOUND: {
    text: 'El código no está en el catálogo de productos.',
    linkLabel: 'Ir a Productos',
    linkTarget: '/configuracion/productos'
  }
};

/** Units of the month that could not be turned into pesos, grouped by product and reason. */
@Component({
  selector: 'app-unresolved-units-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, WarningPanelComponent],
  templateUrl: './unresolved-units-panel.component.html',
  styleUrl: './unresolved-units-panel.component.css'
})
export class UnresolvedUnitsPanelComponent {
  /** The unresolved units of every seller of the month. */
  readonly items = input.required<UnresolvedUnits[]>();
  /** Name of the month in lower case: "agosto". */
  readonly monthName = input.required<string>();

  protected readonly open = signal(true);

  protected readonly rows = computed<UnresolvedRowView[]>(() => {
    const grouped = new Map<string, UnresolvedRowView>();
    for (const item of this.items()) {
      const key = `${item.sku}-${item.reason}`;
      const existing = grouped.get(key);
      if (existing) {
        existing.units += item.units;
      } else {
        grouped.set(key, {
          key,
          sku: item.sku,
          name: item.productName.trim() || null,
          units: item.units,
          reason: REASON_VIEW[item.reason].text,
          linkLabel: REASON_VIEW[item.reason].linkLabel,
          linkTarget: REASON_VIEW[item.reason].linkTarget
        });
      }
    }
    return [...grouped.values()].sort((a, b) => b.units - a.units || a.sku.localeCompare(b.sku));
  });

  protected readonly totalUnits = computed(() => this.rows().reduce((sum, row) => sum + row.units, 0));
}
