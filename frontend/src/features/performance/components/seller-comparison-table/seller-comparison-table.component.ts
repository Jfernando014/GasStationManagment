import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { AvatarComponent } from '../../../../atomic-design/atoms/avatar/avatar.component';
import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { lineScale } from '../../helpers/performance-view.helpers';
import { ScenarioLine, SellerView } from '../../models/performance-view.model';
import { ComparisonLineComponent } from '../comparison-line/comparison-line.component';

/**
 * Table "¿y por qué?": one row per seller with the turns compared, the average of the seller and of the station and the
 * index. Each row opens to show the same comparison day by day of the week, as holder and as support. A row without a
 * comparable scenario says it has no sufficient data instead of showing a made-up number.
 */
@Component({
  selector: 'app-seller-comparison-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AvatarComponent, InfoTooltipComponent, SkeletonComponent, ComparisonLineComponent],
  templateUrl: './seller-comparison-table.component.html',
  styleUrl: './seller-comparison-table.component.css'
})
export class SellerComparisonTableComponent {
  readonly sellers = input.required<SellerView[]>();
  readonly loading = input(false);
  /** Unit written next to the averages: "gal" or "COP". */
  readonly unit = input('gal');
  /** Real range of the data, for the legend: "nov 2025 a ago 2026". */
  readonly range = input<string | null>(null);
  /** Changes with every new answer, so the rows come in again. */
  readonly version = input(0);

  protected readonly skeletonRows = [0, 1, 2, 3];
  protected readonly maximum = computed(() => lineScale(this.sellers()));
  private readonly openRows = signal<ReadonlySet<string>>(new Set());
  private readonly openSupport = signal<ReadonlySet<string>>(new Set());

  protected isOpen(workerId: string): boolean {
    return this.openRows().has(workerId);
  }

  protected supportOpen(workerId: string): boolean {
    return this.openSupport().has(workerId);
  }

  protected toggle(workerId: string): void {
    this.openRows.update(current => this.flip(current, workerId));
  }

  protected toggleSupport(workerId: string): void {
    this.openSupport.update(current => this.flip(current, workerId));
  }

  protected tone(percent: number | null): 'up' | 'down' | 'none' {
    if (percent === null) return 'none';
    return percent >= 100 ? 'up' : 'down';
  }

  protected sign(percent: number | null): string {
    if (percent === null) return '';
    return percent >= 100 ? '▲' : '▼';
  }

  protected sentence(view: SellerView): string {
    return view.hasData
      ? `${view.name}: ${view.turns} turnos comparados, vende ${view.sellerAverage} ${this.unit()} en promedio frente a ${view.stationAverage} de la estación.`
      : `${view.name}: sin datos suficientes para comparar.`;
  }

  protected lineSentence(line: ScenarioLine): string {
    return line.summary;
  }

  protected readonly formula = 'Lo que vendió dividido por lo que habría vendido con el promedio de la estación en esos mismos turnos';

  private flip(current: ReadonlySet<string>, id: string): ReadonlySet<string> {
    const next = new Set(current);
    if (!next.delete(id)) next.add(id);
    return next;
  }
}
