import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * One indicator of a grid: its name, the figure and a line that explains it. The grid draws the borders between
 * tiles. Projected content replaces the figure, e.g. for a value that is not defined yet.
 */
@Component({
  selector: 'app-metric-tile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="label">{{ label() }}</span>
    @if (value()) {
      <b class="value">{{ value() }}</b>
    } @else {
      <span class="custom"><ng-content /></span>
    }
    @if (note()) {
      <small class="note">{{ note() }}</small>
    }
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: 2px; min-width: 0; padding: 12px 16px; }

    .label { font-size: 12.5px; color: var(--text-muted); }

    .value {
      font-family: 'Plus Jakarta Sans', sans-serif;
      font-size: 19px;
      font-weight: 800;
      font-variant-numeric: tabular-nums;
      color: var(--text-heading);
    }

    :host([data-tone='short']) .value { color: var(--shortage); }
    :host([data-tone='over']) .value { color: var(--surplus); }

    .custom { display: inline-flex; align-items: center; gap: 6px; margin-top: 3px; font-size: 14px; font-weight: 600; color: var(--text-muted); }

    .note { font-size: 12px; line-height: 1.35; color: var(--text-muted); }

    @media (max-width: 640px) {
      :host { gap: 1px; padding: 10px 12px; }
      .label { font-size: 11.5px; }
      .value { font-size: 16px; }
      .custom { font-size: 13.5px; }
      .note { font-size: 11px; }
    }
  `,
  host: { '[attr.data-tone]': 'tone()' }
})
export class MetricTileComponent {
  readonly label = input.required<string>();
  readonly value = input('');
  readonly note = input('');
  /** Colors a difference: a shortage or a surplus. */
  readonly tone = input<'short' | 'over' | 'even'>('even');
}
