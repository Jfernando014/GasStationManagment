import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ShownAlertLevel } from '../../../shared/models/dtos/alert-level.dto';

export interface TrendPoint {
  /** Position on the horizontal axis, from 0 to `slots - 1`. */
  index: number;
  value: number;
  /** Text of the tooltip of this point. */
  title: string;
  /** Draws a dot on it. */
  marked?: boolean;
}

const WIDTH = 1000;
const HEIGHT = 100;
const MIDDLE = HEIGHT / 2;
/** Room left above and below the farthest value. */
const REACH = 45;

/**
 * Line of a series around zero over the green band of what is allowed (zero ± `limit`). It fills the size of its
 * host, is drawn once when it appears and takes the color of its level. Positions without a point are skipped.
 */
@Component({
  selector: 'app-trend-band-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg [attr.viewBox]="viewBox" preserveAspectRatio="none" role="img" [attr.aria-label]="label()">
      <rect class="band" x="0" [attr.y]="band().top" [attr.width]="width" [attr.height]="band().height" />
      <line class="zero" x1="0" [attr.x2]="width" [attr.y1]="middle" [attr.y2]="middle" />
      <g class="series">
        <polyline [attr.points]="line()" />
      </g>
      @for (point of placed(); track point.index) {
        <rect class="hit" [attr.x]="point.left" y="0" [attr.width]="slotWidth()" [attr.height]="height">
          <title>{{ point.title }}</title>
        </rect>
      }
    </svg>
    <!-- The dots are drawn outside the SVG so that stretching it does not flatten them. -->
    @for (point of placed(); track point.index) {
      @if (point.marked) {
        <span class="dot" [style.left.%]="point.x / 10" [style.top.%]="point.y" [attr.data-level]="levelAt(point.value)"
          aria-hidden="true"></span>
      }
    }
  `,
  styleUrl: './trend-band-chart.component.css',
  host: { '[attr.data-level]': 'level()' }
})
export class TrendBandChartComponent {
  readonly points = input.required<TrendPoint[]>();
  /** How many positions the horizontal axis has. */
  readonly slots = input.required<number>();
  /** Half the height of the allowed band, in the unit of the values. */
  readonly limit = input.required<number>();
  /** Accessible name; it should say what the line shows. */
  readonly label = input.required<string>();
  readonly level = input<ShownAlertLevel>('SIN_DATOS');

  protected readonly viewBox = `0 0 ${WIDTH} ${HEIGHT}`;
  protected readonly width = WIDTH;
  protected readonly height = HEIGHT;
  protected readonly middle = MIDDLE;

  /** Value that reaches the edge: always wide enough to see the band. */
  private readonly reach = computed(() =>
    Math.max(this.limit() * 2, ...this.points().map(point => Math.abs(point.value) * 1.05)));

  protected readonly slotWidth = computed(() => WIDTH / Math.max(1, this.slots()));

  protected readonly placed = computed(() => this.points().map(point => {
    const left = point.index * this.slotWidth();
    return { ...point, left, x: left + this.slotWidth() / 2, y: this.toY(point.value) };
  }));

  protected readonly line = computed(() => {
    const points = this.placed().map(point => `${point.x.toFixed(1)},${point.y.toFixed(1)}`);
    // A single point would draw nothing: it becomes a short flat stroke.
    if (points.length === 1) {
      const [point] = this.placed();
      points.push(`${(point.x + this.slotWidth() / 4).toFixed(1)},${point.y.toFixed(1)}`);
    }
    return points.join(' ');
  });

  protected readonly band = computed(() => {
    const top = this.toY(this.limit());
    return { top, height: this.toY(-this.limit()) - top };
  });

  /** A dot is green inside the band and takes the color of the line outside it. */
  protected levelAt(value: number): ShownAlertLevel {
    if (Math.abs(value) <= this.limit()) return 'OK';
    return this.level() === 'OK' ? 'REVISAR' : this.level();
  }

  private toY(value: number): number {
    return MIDDLE - (value / this.reach()) * REACH;
  }
}
