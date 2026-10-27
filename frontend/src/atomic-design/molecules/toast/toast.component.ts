import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ToastService } from '../../../core/services/toast.service';

/** Success message at the top right. It is always in the page so screen readers announce each new text. */
@Component({
  selector: 'app-toast',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="check" aria-hidden="true">
      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3"
        stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
    </span>
    <b>{{ toast.message() }}</b>
  `,
  styleUrl: './toast.component.css',
  host: {
    role: 'status',
    'aria-live': 'polite',
    '[class.is-visible]': 'toast.visible()'
  }
})
export class ToastComponent {
  protected readonly toast = inject(ToastService);
}
