import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface DivergingBar {
  /** Position on the horizontal axis, from 0 to `slots - 1`. */
  index: number;
  /** Null when there is no data: a dashed mark takes its place. */
  value: number | null;
  title: string;
}

const SLOT = 24;
const BAR = 16;
const HEIGHT = 110;
const MIDDLE = HEIGHT / 2;
const REACH = 50;
/** The smallest bar, so that a zero is still seen. */
const MIN_BAR = 2;

/**
 * One bar per position, up or down from zero: negative values in the shortage color and positive ones in the
 * surplus color. It fills the size of its host and each bar has a tooltip.
 */
@Component({
  selector: 'app-diverging-bar-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg [attr.viewBox]="viewBox()" preserveAspectRatio="none" role="img" [attr.aria-label]="label()">
      <line class="zero" x1="0" [attr.x2]="width()" [attr.y1]="middle" [attr.y2]="middle" />
      @for (bar of placed(); track bar.index) {
        <g>
          <title>{{ bar.title }}</title>
          @if (bar.gap) {
            <line class="gap" [attr.x1]="bar.x + half" [attr.x2]="bar.x + half" y1="20" y2="90" />
          } @else {
            <rect [attr.class]="'bar bar--' + bar.side" [attr.x]="bar.x" [attr.y]="bar.y" [attr.width]="barWidth"
              [attr.height]="bar.height" rx="3" [style.animation-delay.ms]="bar.index * 22 + 200" />
          }
          <!-- Wider than the mark, so the tooltip is easy to reach. -->
          <rect class="hit" [attr.x]="bar.x - 4" y="0" [attr.width]="slot" [attr.height]="height" />
        </g>
      }
    </svg>
  `,
  styleUrl: './diverging-bar-chart.component.css'
})
export class DivergingBarChartComponent {
  readonly bars = input.required<DivergingBar[]>();
  /** How many positions the horizontal axis has. */
  readonly slots = input.required<number>();
  readonly label = input.required<string>();

  protected readonly middle = MIDDLE;
  protected readonly height = HEIGHT;
  protected readonly slot = SLOT;
  protected readonly barWidth = BAR;
  protected readonly half = BAR / 2;

  protected readonly width = computed(() => Math.max(1, this.slots()) * SLOT);
  protected readonly viewBox = computed(() => `0 0 ${this.width()} ${HEIGHT}`);

  protected readonly placed = computed(() => {
    const top = Math.max(1, ...this.bars().map(bar => Math.abs(bar.value ?? 0)));
    return this.bars().map(bar => {
      const value = bar.value ?? 0;
      const height = Math.max(MIN_BAR, (Math.abs(value) / top) * REACH);
      return {
        index: bar.index,
        title: bar.title,
        gap: bar.value === null,
        side: value < 0 ? 'down' : value > 0 ? 'up' : 'zero',
        x: bar.index * SLOT + (SLOT - BAR) / 2,
        y: value < 0 ? MIDDLE : MIDDLE - height,
        height
      };
    });
  });
}
