import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Gray bar that pulses while content loads. Screen readers skip it: the container says it is busy. */
@Component({
  selector: 'app-skeleton',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '',
  styles: `
    :host {
      display: block;
      border-radius: 6px;
      background: var(--border-main);
      animation: skeleton-pulse 1.2s ease-in-out infinite;
    }

    @keyframes skeleton-pulse {
      0%, 100% { opacity: 0.5; }
      50% { opacity: 1; }
    }

    @media (prefers-reduced-motion: reduce) {
      :host { animation: none; }
    }
  `,
  host: {
    'aria-hidden': 'true',
    '[style.width]': 'width()',
    '[style.height]': 'height()'
  }
})
export class SkeletonComponent {
  /** Any CSS length: "120px", "60%". */
  readonly width = input('100%');
  readonly height = input('14px');
}
