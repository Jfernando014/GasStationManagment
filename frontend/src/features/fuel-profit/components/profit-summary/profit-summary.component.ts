import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { AnimatedNumberComponent } from '../../../../atomic-design/atoms/animated-number/animated-number.component';
import { WarningPanelComponent } from '../../../../atomic-design/molecules/warning-panel/warning-panel.component';
import { formatCop } from '../../../../shared/helpers/format.helpers';
import { formatPercentage } from '../../../../shared/helpers/percentage.helpers';
import { UNDEFINED_FIGURE, commissionText, fuelIdentity, incompleteSummary } from '../../helpers/fuel-profit.helpers';
import { FuelProfitReportDto } from '../../models/fuel-profit.dto';
import { ProfitFormulaComponent } from '../profit-formula/profit-formula.component';
import { ProfitFuelListComponent } from '../profit-fuel-list/profit-fuel-list.component';

interface MainFigure {
  label: string;
  /** Null shows "Sin definir". */
  value: number | null;
  format: (value: number) => string;
  note: string;
  tone: 'main' | 'plain' | 'commission';
}

/** "Resumen del mes": the notice of what is missing, the main figures, the table per fuel and the example. */
@Component({
  selector: 'app-profit-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, ActionButtonDirective, AnimatedNumberComponent, WarningPanelComponent,
    ProfitFuelListComponent, ProfitFormulaComponent
  ],
  templateUrl: './profit-summary.component.html',
  styleUrl: './profit-summary.component.css'
})
export class ProfitSummaryComponent {
  readonly report = input.required<FuelProfitReportDto>();
  readonly monthInProgress = input(false);
  /** A missing figure is fixed in "Datos del cálculo". */
  readonly openInputs = output<void>();

  protected readonly undefinedFigure = UNDEFINED_FIGURE;

  protected readonly incomplete = computed(() =>
    this.report().totals.complete ? null : incompleteSummary(this.report()));

  protected readonly figures = computed<MainFigure[]>(() => {
    const { totals, card, fuels } = this.report();
    const defined = totals.complete;
    const value = (figure: number | null) => defined ? figure : null;
    const percentage = card.appliedPercentage === null
      ? 'Sin % con tarjeta'
      : `${formatPercentage(card.appliedPercentage)} con tarjeta${card.mode === 'MANUAL' ? ' (manual)' : ''}`;
    return [
      {
        label: 'Ganancia después de comisión', tone: 'main', format: formatCop,
        value: value(totals.profitAfterCommission),
        note: defined ? 'Después de pagar la comisión' : this.missingNote()
      },
      {
        label: 'Ganancia antes de comisión', tone: 'plain', format: formatCop, value: value(totals.realProfit),
        note: 'Venta − compra − flete − costo financiero'
      },
      {
        label: 'Comisión de tarjeta', tone: 'commission', format: commissionText, value: value(totals.commission),
        note: `${percentage} × ${formatPercentage(card.commissionRate * 100, 4)}`
      },
      {
        label: this.monthInProgress() ? 'Si se cumple la meta' : 'Si se cumplía la meta', tone: 'plain',
        format: formatCop, value: value(totals.targetProfit),
        note: this.targetNote(totals.compliancePercentage, fuels.length - totals.fuelsWithoutTarget.length, fuels.length)
      }
    ];
  });

  protected readonly someWithoutTarget = computed(() => this.report().totals.fuelsWithoutTarget.length > 0);

  private missingNote(): string {
    const incomplete = this.report().fuels.filter(fuel => !fuel.complete);
    if (incomplete.length !== 1) return `Faltan datos de ${incomplete.length} combustibles`;
    const name = fuelIdentity(incomplete[0].fuelType).name;
    return incomplete[0].missingData.length === 1 ? `Falta un dato de ${name}` : `Faltan datos de ${name}`;
  }

  private targetNote(compliance: number | null, withTarget: number, fuels: number): string {
    const count = `${withTarget} de ${fuels} combustibles tienen meta`;
    if (compliance === null) return count;
    const done = formatPercentage(compliance, 1);
    const progress = this.monthInProgress() ? `Llevamos el ${done}` : `Se cumplió el ${done}`;
    return `${progress} de los galones de la meta · ${count}`;
  }
}
