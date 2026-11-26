import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Small turning ring for a button that is working, in the color of its text. It only decorates: the button says
 * what it is doing.
 */
@Component({
  selector: 'app-spinner',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '',
  styles: `
    :host {
      display: inline-block;
      box-sizing: border-box;
      flex-shrink: 0;
      width: 15px;
      height: 15px;
      border: 2px solid color-mix(in srgb, currentColor 40%, transparent);
      border-top-color: currentColor;
      border-radius: 50%;
      animation: spinner-turn 0.7s linear infinite;
    }

    @keyframes spinner-turn { to { transform: rotate(360deg); } }

    @media (prefers-reduced-motion: reduce) {
      :host { border-right-color: currentColor; animation: none; }
    }
  `,
  host: { 'aria-hidden': 'true' }
})
export class SpinnerComponent {}
