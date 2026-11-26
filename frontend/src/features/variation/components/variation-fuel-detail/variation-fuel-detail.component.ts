import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { MetricTileComponent } from '../../../../atomic-design/molecules/metric-tile/metric-tile.component';
import { FuelDetailView } from '../../helpers/variation-detail.helpers';
import { VariationDayListComponent } from '../variation-day-list/variation-day-list.component';
import { VariationDayTableComponent } from '../variation-day-table/variation-day-table.component';
import { VariationMonthChartComponent } from '../variation-month-chart/variation-month-chart.component';
import { VariationShortageRangesComponent } from '../variation-shortage-ranges/variation-shortage-ranges.component';
import { VariationStatusCardComponent } from '../variation-status-card/variation-status-card.component';

/** Everything about the month of one fuel: where it stands, its indicators, how it went and each day. */
@Component({
  selector: 'app-variation-fuel-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MetricTileComponent, VariationStatusCardComponent, VariationMonthChartComponent, VariationShortageRangesComponent,
    VariationDayTableComponent, VariationDayListComponent
  ],
  template: `
    <div class="top">
      <app-variation-status-card class="enter" [status]="view().status" />
      <div class="metrics enter" [style.--i]="1">
        @for (metric of phone() ? view().compactMetrics : view().metrics; track metric.label) {
          <app-metric-tile [style.--i]="$index" [label]="metric.label" [value]="metric.value" [note]="metric.note"
            [tone]="metric.tone">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="10" /><circle cx="12" cy="12" r="5" /></svg>
            {{ metric.unset }}
          </app-metric-tile>
        }
      </div>
    </div>

    <div class="middle">
      <app-variation-month-chart class="enter" [style.--i]="2" [chart]="view().chart" />
      <app-variation-shortage-ranges class="enter" [style.--i]="3" [ranges]="view().ranges" [compact]="phone()" />
    </div>

    @if (phone()) {
      <app-variation-day-list class="enter" [style.--i]="4" [rows]="view().rows" />
    } @else {
      <app-variation-day-table class="enter" [style.--i]="4" [rows]="view().rows" [totals]="view().totals"
        [fuel]="view().name" />
    }
  `,
  styleUrl: './variation-fuel-detail.component.css'
})
export class VariationFuelDetailComponent {
  readonly view = input.required<FuelDetailView>();
  readonly phone = input(false);
}
