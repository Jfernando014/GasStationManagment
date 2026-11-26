import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const WIDTH = 70;
const HEIGHT = 26;
const PAD = 4;

/**
 * Tiny line with the shape of a series, oldest value first, drawn once when it appears. It takes the text color
 * of its host. Equal values (or a single one) give a flat line. The figures it summarizes must be readable elsewhere.
 */
@Component({
  selector: 'app-sparkline',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg [attr.viewBox]="viewBox" width="70" height="26" role="img" [attr.aria-label]="label()">
      <polyline [attr.points]="points()" pathLength="100" fill="none" stroke="currentColor" stroke-width="2.2"
        stroke-linecap="round" stroke-linejoin="round" />
    </svg>
  `,
  styles: `
    :host { display: inline-flex; flex-shrink: 0; }

    polyline {
      stroke-dasharray: 100;
      animation: sparkline-draw 1100ms var(--spring) var(--sparkline-delay, 0ms) backwards;
    }

    @keyframes sparkline-draw {
      from { stroke-dashoffset: 100; }
      to { stroke-dashoffset: 0; }
    }

    @media (prefers-reduced-motion: reduce) {
      polyline { animation: none; }
    }
  `
})
export class SparklineComponent {
  readonly values = input.required<number[]>();
  /** Accessible name, e.g. "Historial del precio de venta". */
  readonly label = input.required<string>();

  protected readonly viewBox = `0 0 ${WIDTH} ${HEIGHT}`;

  protected readonly points = computed(() => {
    const raw = this.values();
    const values = raw.length === 1 ? [raw[0], raw[0]] : raw;
    if (values.length === 0) return '';
    const low = Math.min(...values);
    const range = Math.max(...values) - low;
    const step = (WIDTH - PAD * 2) / (values.length - 1);
    return values.map((value, index) => {
      const y = range === 0 ? HEIGHT / 2 : HEIGHT - PAD - ((value - low) / range) * (HEIGHT - PAD * 2);
      return `${(PAD + index * step).toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  });
}
