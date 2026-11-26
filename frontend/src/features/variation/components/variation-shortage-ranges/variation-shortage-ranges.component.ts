import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { AlertChipComponent } from '../../../../atomic-design/atoms/alert-chip/alert-chip.component';
import { PercentBarComponent } from '../../../../atomic-design/atoms/percent-bar/percent-bar.component';
import { RangesView } from '../../helpers/variation-detail.helpers';

/** How many days fuel was missing by each size, against the days the rules allow. */
@Component({
  selector: 'app-variation-shortage-ranges',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AlertChipComponent, PercentBarComponent],
  template: `
    <h2>¿Cuántos días faltó combustible?</h2>
    <p class="intro">
      @if (compact()) {
        La raya negra es el máximo de días permitido.
      } @else {
        Cada tamaño de faltante tiene un número de días permitidos en el mes. La raya negra es el límite.
      }
    </p>
    <ul>
      @for (row of ranges().rows; track row.range) {
        <li [style.--i]="$index">
          <span class="name">{{ row.name }}</span>
          <app-percent-bar [value]="row.count" [reference]="row.limit" [scaleMax]="row.scaleMax" [tone]="row.tone" />
          @if (compact()) {
            <span class="state">
              <app-alert-chip [level]="row.level" [text]="row.shortText" [title]="row.text" />
            </span>
          } @else {
            <span class="count">{{ row.text }}</span>
            <span class="state"><app-alert-chip [level]="row.level" /></span>
          }
        </li>
      }
    </ul>
    <p class="surplus">{{ compact() ? ranges().shortSurplus : ranges().surplus }}</p>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
      padding: 14px 18px;
      border: 1px solid var(--border-main);
      border-radius: 14px;
      background: var(--surface-card);
      box-shadow: var(--shadow-card);
    }

    h2 { margin: 0; font-size: 15px; font-weight: 700; color: var(--text-heading); }
    p { margin: 0; }
    .intro { margin-bottom: 4px; font-size: 12.5px; color: var(--text-muted); }
    ul { margin: 0; padding: 0; list-style: none; }

    li {
      display: grid;
      grid-template-columns: 72px minmax(0, 1fr) 78px 92px;
      align-items: center;
      column-gap: 12px;
      min-height: 38px;
      font-size: 13.5px;
      font-variant-numeric: tabular-nums;
    }

    .name { font-weight: 600; color: var(--text-heading); }
    app-percent-bar { width: 100%; height: 10px; --percent-bar-delay: calc(var(--i, 0) * 70ms + 300ms); }
    .count { text-align: right; color: var(--text-body); white-space: nowrap; }
    .state { text-align: right; }

    .surplus {
      margin-top: 6px;
      padding-top: 8px;
      border-top: 1px dashed var(--border-main);
      font-size: 12.5px;
      color: var(--text-muted);
    }

    @media (max-width: 640px) {
      :host { gap: 2px; padding: 12px 14px; }
      h2 { font-size: 14px; }
      .intro,
      .surplus { font-size: 11.5px; }
      li { grid-template-columns: 66px minmax(0, 1fr) 82px; column-gap: 10px; min-height: 32px; font-size: 12.5px; }
      app-percent-bar { height: 8px; }
    }
  `
})
export class VariationShortageRangesComponent {
  readonly ranges = input.required<RangesView>();
  /** Shorter texts for a phone. */
  readonly compact = input(false);
}
