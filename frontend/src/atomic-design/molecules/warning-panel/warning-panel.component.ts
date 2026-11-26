import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

let nextId = 0;

/**
 * Compact bar that warns about incomplete data: an amber dot, the message and, below, a detail that can fold.
 * Content slots: the default content (the message; a `<b>` is the title) and `[panelDetails]` (what goes under
 * the bar). With `collapsible` the bar gets a "Ocultar detalle" button that folds the details; without it the
 * details are always shown. It is a live region, so a screen reader announces it when it appears or changes.
 */
@Component({
  selector: 'app-warning-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="bar">
      <span class="dot" aria-hidden="true"></span>
      <div class="message"><ng-content /></div>
      @if (collapsible()) {
        <button type="button" class="toggle" [attr.aria-expanded]="open()" [attr.aria-controls]="detailsId"
          (click)="open.set(!open())">
          {{ open() ? 'Ocultar detalle' : 'Ver detalle' }}
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.4"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
        </button>
      }
    </div>
    <div class="details" [id]="detailsId" [class.is-open]="!collapsible() || open()"
      [attr.inert]="collapsible() && !open() ? '' : null">
      <div class="details-inner"><div class="details-body"><ng-content select="[panelDetails]" /></div></div>
    </div>
  `,
  styleUrl: './warning-panel.component.css',
  host: {
    role: 'status',
    '[class.warning-panel--touch]': 'touchTarget()',
    '[class.warning-panel--neutral]': "tone() === 'neutral'",
    '[class.warning-panel--danger]': "tone() === 'danger'"
  }
})
export class WarningPanelComponent {
  /**
   * Gray dot for a notice that warns about nothing, e.g. that the screen is read only; red bar for an action that
   * was refused.
   */
  readonly tone = input<'warning' | 'neutral' | 'danger'>('warning');
  /** Adds the button that folds the details. */
  readonly collapsible = input(false);
  /** On a phone the button that folds the details is at least 44 px high. */
  readonly touchTarget = input(false);
  /** Whether the details are shown; only matters when `collapsible`. */
  readonly open = model(true);

  protected readonly detailsId = `warning-panel-details-${nextId++}`;
}
