import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { AlertChipComponent } from '../../../../atomic-design/atoms/alert-chip/alert-chip.component';
import { AnimatedNumberComponent } from '../../../../atomic-design/atoms/animated-number/animated-number.component';
import { TrendBandChartComponent } from '../../../../atomic-design/atoms/trend-band-chart/trend-band-chart.component';
import { signedPercent } from '../../helpers/variation-format.helpers';
import { SummaryView } from '../../helpers/variation-summary.helpers';

/** The month at a glance: how many fuels are in each level, one card per fuel and what each level means. */
@Component({
  selector: 'app-variation-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AlertChipComponent, AnimatedNumberComponent, TrendBandChartComponent],
  templateUrl: './variation-summary.component.html',
  styleUrl: './variation-summary.component.css'
})
export class VariationSummaryComponent {
  readonly view = input.required<SummaryView>();
  readonly phone = input(false);
  /** Slug of the fuel whose tab should open. */
  readonly fuelChosen = output<string>();

  protected readonly percent = signedPercent;
}
