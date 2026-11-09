import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Thin bar that shows a percentage against a reference mark (100 % = the reference). It only decorates: the number
 * is always written next to it, so the bar is hidden from assistive technology. The fill grows when it enters.
 */
@Component({
  selector: 'app-percent-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="fill" [style.width.%]="fillWidth()"></span><span class="mark" [style.left.%]="markPosition()"></span>`,
  styleUrl: './percent-bar.component.css',
  host: {
    '[class.is-below]': 'value() < reference()',
    '[class.is-muted]': 'muted()',
    'aria-hidden': 'true'
  }
})
export class PercentBarComponent {
  /** Percentage to draw. */
  readonly value = input.required<number>();
  /** Where the mark goes: the value that means "same as the reference". */
  readonly reference = input(100);
  /** Value at the end of the track; anything above it fills the bar. */
  readonly scaleMax = input(150);
  /** Gray instead of colored, for values that are not reliable yet. */
  readonly muted = input(false);

  protected readonly fillWidth = computed(() => this.toPosition(this.value()));
  protected readonly markPosition = computed(() => this.toPosition(this.reference()));

  private toPosition(value: number): number {
    return Math.min(100, Math.max(0, (value / this.scaleMax()) * 100));
  }
}
