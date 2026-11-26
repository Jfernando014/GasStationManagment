import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DivergingBarChartComponent } from '../../../../atomic-design/atoms/diverging-bar-chart/diverging-bar-chart.component';
import { TrendBandChartComponent } from '../../../../atomic-design/atoms/trend-band-chart/trend-band-chart.component';
import { ChartView } from '../../helpers/variation-detail.helpers';

/** The difference of every day and, below, how the month accumulated against what is allowed. */
@Component({
  selector: 'app-variation-month-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DivergingBarChartComponent, TrendBandChartComponent],
  template: `
    <div class="head">
      <h2>Cómo fue el mes</h2>
      <p class="key">
        <span><i class="mark mark--short" aria-hidden="true"></i>faltó</span>
        <span><i class="mark mark--over" aria-hidden="true"></i>sobró</span>
        <span><i class="mark mark--gap" aria-hidden="true"></i>sin datos</span>
      </p>
    </div>
    <p class="caption">Diferencia de cada día (galones)</p>
    <app-diverging-bar-chart [bars]="chart().bars" [slots]="chart().slots" [label]="chart().barsLabel" />
    <p class="caption caption--second">Acumulado del mes (%) · la franja verde es lo permitido</p>
    <app-trend-band-chart [points]="chart().line" [slots]="chart().slots" [limit]="chart().limit"
      [level]="chart().level" [label]="chart().lineLabel" />
    <div class="axis" aria-hidden="true">
      @for (label of chart().axis; track $index) {
        <span>{{ label }}</span>
      }
    </div>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 6px;
      min-width: 0;
      padding: 14px 18px;
      border: 1px solid var(--border-main);
      border-radius: 14px;
      background: var(--surface-card);
      box-shadow: var(--shadow-card);
    }

    .head { display: flex; align-items: baseline; justify-content: space-between; flex-wrap: wrap; gap: 4px 12px; }
    h2 { margin: 0; font-size: 15px; font-weight: 700; color: var(--text-heading); }
    p { margin: 0; }
    .key { display: flex; gap: 14px; font-size: 12px; color: var(--text-muted); }
    .key span { display: inline-flex; align-items: center; gap: 5px; }
    .mark { display: inline-block; width: 6px; height: 13px; border-radius: 2px; }
    .mark--short { background: var(--shortage-bar); }
    .mark--over { background: var(--surplus-bar); }
    .mark--gap { width: 0; border-radius: 0; border-left: 2px dashed #CBD5E1; }

    .caption { font-size: 12.5px; color: var(--text-muted); }
    .caption--second { margin-top: 4px; }

    app-trend-band-chart { height: 110px; --trend-delay: 300ms; }

    .axis {
      display: flex;
      justify-content: space-between;
      font-size: 11.5px;
      font-variant-numeric: tabular-nums;
      color: var(--text-muted);
    }

    @media (max-width: 640px) {
      :host { gap: 4px; padding: 12px 14px; }
      h2 { font-size: 14px; }
      .caption { font-size: 11.5px; }
      app-diverging-bar-chart,
      app-trend-band-chart { height: 72px; }
    }
  `
})
export class VariationMonthChartComponent {
  readonly chart = input.required<ChartView>();
}
