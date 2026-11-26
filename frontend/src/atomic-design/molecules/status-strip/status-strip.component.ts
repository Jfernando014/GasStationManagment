import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { InfoTooltipComponent } from '../../atoms/info-tooltip/info-tooltip.component';

/** `warning` is a step that is done but missing something; it carries a mark besides its color. */
export type StripTone = 'done' | 'warning' | 'pending' | 'current' | 'upcoming';

export interface StripItem {
  id: string;
  tone: StripTone;
  /** Accessible name of the square, e.g. the date. */
  name: string;
  /** What the bubble says: the name and its state in words. */
  tooltip: string;
  /** Route the square opens, for a step that can be fixed. */
  link?: string[];
}

/**
 * One small square per step of a period, colored by its state; each one tells what it is on hover, focus or tap.
 * A square with a link opens it on click or tap. By default the squares share one line; on a phone they wrap in a
 * grid with touch-sized cells.
 */
@Component({
  selector: 'app-status-strip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InfoTooltipComponent],
  template: `
    <ul class="strip" [attr.aria-label]="label()" [style.--count]="items().length">
      @for (item of items(); track item.id) {
        <li [style.--i]="$index">
          <app-info-tooltip [inline]="true" [help]="true" [text]="item.tooltip" [link]="item.link ?? null"
            [linkParams]="linkParams()">
            <span class="cell" [class]="'cell cell--' + item.tone" aria-hidden="true"></span>
            <span class="jr-sr">{{ item.name }}</span>
          </app-info-tooltip>
        </li>
      }
    </ul>
  `,
  styles: `
    :host { display: block; min-width: 0; }

    .strip {
      display: grid;
      grid-template-columns: repeat(var(--count), minmax(0, 1fr));
      gap: 3px;
      list-style: none;
    }

    app-info-tooltip { display: flex; }
    app-info-tooltip ::ng-deep .trigger { width: 100%; height: 22px; }

    /* The square animates, not its item: a transformed ancestor would move the bubble, which is fixed. */
    .cell {
      display: block;
      width: 100%;
      height: 8px;
      border-radius: 3px;
      transition: transform var(--duration-fast) var(--spring);
      animation: cell-in 200ms var(--spring) backwards;
      animation-delay: min(calc(var(--i) * 14ms), 200ms);
    }

    app-info-tooltip:hover .cell { transform: scaleY(1.5); }
    .cell--done { background: var(--semantic-green); }
    .cell--warning { position: relative; background: var(--semantic-green); }
    .cell--warning::after {
      content: '!';
      position: absolute;
      bottom: calc(100% + 1px);
      left: 50%;
      width: 10px;
      height: 10px;
      margin-left: -5px;
      border-radius: 50%;
      background: var(--semantic-amber-strong);
      color: #FFFFFF;
      font-size: 8px;
      font-weight: 800;
      line-height: 10px;
      text-align: center;
    }
    .cell--pending { background: #FCD34D; }
    /* Today also stands out by its height, not only by its color. */
    .cell--current { height: 14px; background: var(--brand-primary); }
    .cell--upcoming { background: var(--border-main); }

    @keyframes cell-in {
      from { opacity: 0; transform: translateY(6px); }
    }

    @media (max-width: 640px) {
      .strip { grid-template-columns: repeat(auto-fill, minmax(44px, 1fr)); gap: 0; }
      app-info-tooltip ::ng-deep .trigger { height: 44px; padding: 0 3px; }
    }

    @media (prefers-reduced-motion: reduce) {
      .cell { animation: none; transition: none; }
    }
  `
})
export class StatusStripComponent {
  readonly items = input.required<StripItem[]>();
  /** Accessible name of the list. */
  readonly label = input.required<string>();
  /** Query parameters of the links of the squares. */
  readonly linkParams = input<Record<string, string | number>>({});
}
