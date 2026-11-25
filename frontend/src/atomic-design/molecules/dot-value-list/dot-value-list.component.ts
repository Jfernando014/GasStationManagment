import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotTone, DotValueComponent } from '../../atoms/dot-value/dot-value.component';
import { InfoTooltipComponent } from '../../atoms/info-tooltip/info-tooltip.component';

export interface DotValueItem {
  key: string;
  tone: DotTone;
  value: string;
  label: string;
  tooltip: string;
  negative: boolean;
}

/** Figures with their dot, each one with a tooltip (hover, focus or tap); `emptyText` when there is none. */
@Component({
  selector: 'app-dot-value-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DotValueComponent, InfoTooltipComponent],
  template: `
    @for (item of items(); track item.key) {
      <app-info-tooltip [inline]="true" [help]="true" [text]="item.tooltip">
        <app-dot-value [tone]="item.tone" [value]="item.value" [label]="item.label" [negative]="item.negative" />
      </app-info-tooltip>
    } @empty {
      <span class="none">{{ emptyText() }}</span>
    }
  `,
  styles: `
    :host { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 16px; min-width: 0; }

    /*
     * Positioned, so it stays above the link that covers a clickable row. Without z-index on purpose: it would trap
     * the bubble under the figures that follow.
     */
    app-info-tooltip { position: relative; }

    .none { color: var(--text-muted); }

    @media (max-width: 640px) {
      :host { gap: 0 14px; }
      app-info-tooltip ::ng-deep .trigger { min-height: 44px; }
    }
  `
})
export class DotValueListComponent {
  readonly items = input.required<DotValueItem[]>();
  readonly emptyText = input.required<string>();
}
