import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type DotTone = 'blue' | 'orange';

/**
 * A figure with its name behind a colored dot: "● 16 h nocturno". The color only groups; the text always says what
 * the figure is.
 */
@Component({
  selector: 'app-dot-value',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="dot" aria-hidden="true"></span><b>{{ value() }}</b> <span>{{ label() }}</span>`,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      white-space: nowrap;
      color: var(--text-body);
      font-variant-numeric: tabular-nums;
    }

    .dot { flex-shrink: 0; width: 7px; height: 7px; margin-right: 7px; border-radius: 50%; }
    :host(.dot-value--blue) .dot { background: var(--semantic-sky); }
    :host(.dot-value--orange) .dot { background: var(--brand-primary); }
    b { margin-right: 4px; font-weight: 700; color: var(--text-heading); }
    :host(.dot-value--negative) b { color: var(--semantic-red-strong); }
  `,
  host: {
    '[class]': "'dot-value dot-value--' + tone()",
    '[class.dot-value--negative]': 'negative()'
  }
})
export class DotValueComponent {
  readonly tone = input.required<DotTone>();
  readonly value = input.required<string>();
  readonly label = input.required<string>();
  /** A figure below zero, written in red; the minus sign is part of `value`. */
  readonly negative = input(false);
}
