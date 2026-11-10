import { ChangeDetectionStrategy, Component, computed, effect, input, signal, untracked } from '@angular/core';

import { prefersReducedMotion } from '../../../shared/helpers/shift-display.helpers';

const EASE_OUT_CUBIC = (progress: number) => 1 - Math.pow(1 - progress, 3);

/**
 * Number that counts up (or down) to its value whenever the value changes. With reduced motion it shows the
 * value at once. The counting is hidden from screen readers, which only read the final text.
 */
@Component({
  selector: 'app-animated-number',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span aria-hidden="true">{{ shown() }}</span>
    <span class="jr-sr">{{ final() }}</span>
  `,
  styles: `:host { font-variant-numeric: tabular-nums; }`
})
export class AnimatedNumberComponent {
  readonly value = input.required<number>();
  /** How the number is written, e.g. `formatCop`. It also receives the in-between values. */
  readonly format = input<(value: number) => string>(value => String(Math.round(value)));
  readonly durationMs = input(700);

  private readonly current = signal(0);
  protected readonly shown = computed(() => this.format()(this.current()));
  protected readonly final = computed(() => this.format()(this.value()));

  constructor() {
    effect(onCleanup => {
      const target = this.value();
      const from = untracked(this.current);
      if (prefersReducedMotion() || from === target) {
        this.current.set(target);
        return;
      }
      const duration = untracked(this.durationMs);
      const start = performance.now();
      let frame = 0;
      const step = (now: number) => {
        const progress = Math.min((now - start) / duration, 1);
        this.current.set(progress === 1 ? target : from + (target - from) * EASE_OUT_CUBIC(progress));
        if (progress < 1) frame = requestAnimationFrame(step);
      };
      frame = requestAnimationFrame(step);
      onCleanup(() => cancelAnimationFrame(frame));
    });
  }
}
