import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { linePosition } from '../../helpers/performance-view.helpers';

/**
 * One line that compares a seller with the station: a dot for the seller, a mark for the station and a stretch between
 * them, green when the seller is above and amber when below. The scale is the same for every line on screen. It is only
 * a drawing, so it carries the sentence that says the same in words.
 */
@Component({
  selector: 'app-comparison-line',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="rail"></span>
    <span class="stretch" [style.left.%]="start()" [style.width.%]="length()"></span>
    <span class="station" [style.left.%]="stationPosition()"></span>
    <span class="seller" [style.left.%]="sellerPosition()"></span>
  `,
  styleUrl: './comparison-line.component.css',
  host: {
    role: 'img',
    '[attr.aria-label]': 'description()',
    '[attr.title]': 'description()',
    '[class.is-up]': 'seller() >= station()',
    '[class.is-muted]': 'muted()'
  }
})
export class ComparisonLineComponent {
  readonly seller = input.required<number>();
  readonly station = input.required<number>();
  /** Value at the end of the line, shared by every line. */
  readonly maximum = input.required<number>();
  /** Sentence that says the same as the drawing. */
  readonly description = input('');
  /** Too few shifts to count: the line is drawn in gray. */
  readonly muted = input(false);

  protected readonly sellerPosition = computed(() => linePosition(this.seller(), this.maximum()));
  protected readonly stationPosition = computed(() => linePosition(this.station(), this.maximum()));
  protected readonly start = computed(() => Math.min(this.sellerPosition(), this.stationPosition()));
  protected readonly length = computed(() => Math.abs(this.sellerPosition() - this.stationPosition()));
}
