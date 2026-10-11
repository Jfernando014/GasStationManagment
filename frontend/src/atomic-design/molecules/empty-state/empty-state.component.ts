import { Component, input } from '@angular/core';

/**
 * Icon, heading and text for an empty or error state.
 * The icon is projected as an `svg` element and any action (e.g. a button) as the rest of the content.
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <span class="empty-state-icon" [class.empty-state-icon--error]="tone() === 'error'" aria-hidden="true">
      <ng-content select="svg" />
    </span>
    <b class="empty-state-title">{{ heading() }}</b>
    @if (description()) {
      <p class="empty-state-text">{{ description() }}</p>
    }
    <div class="empty-state-actions"><ng-content /></div>
  `,
  host: { class: 'empty-state' }
})
export class EmptyStateComponent {
  readonly heading = input.required<string>();
  readonly description = input('');
  readonly tone = input<'neutral' | 'error'>('neutral');
}
