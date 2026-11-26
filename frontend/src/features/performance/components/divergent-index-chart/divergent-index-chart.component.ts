import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { ColorThemeService } from '../../../../core/services/color-theme.service';
import { IndexChartModel } from '../../models/performance-view.model';

/**
 * Divergent chart of the index against the station: one bar per seller that leaves the center (100 %) to the right in
 * green when the seller sells more than the station and to the left in amber when less. The scale is the same on both
 * sides. Colors are never the only signal: every bar has its percentage with an arrow and an accessible sentence.
 */
@Component({
  selector: 'app-divergent-index-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InfoTooltipComponent, SkeletonComponent],
  templateUrl: './divergent-index-chart.component.html',
  styleUrl: './divergent-index-chart.component.css'
})
export class DivergentIndexChartComponent {
  private readonly colorTheme = inject(ColorThemeService, { optional: true });

  readonly model = input.required<IndexChartModel>();
  /** There is no data yet: the chart is drawn with skeletons. */
  readonly loading = input(false);
  /** Changes with every new answer, so the bars grow again. */
  readonly version = input(0);

  protected readonly skeletonRows = [0, 1, 2, 3];
  protected readonly formula = 'Lo que vendió dividido por lo que habría vendido con el promedio de la estación en esos mismos turnos';

  protected sellerDotColor(workerId: string, name: string): string | null {
    return this.colorTheme?.resolveWorkerColor({ workerId, name })?.background ?? null;
  }
}
