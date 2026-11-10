import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Amber panel that warns about incomplete data. Content slots:
 * an `svg` (icon), the default content (the message), `[panelAction]` (a button at the end of the message row)
 * and `[panelDetails]` (what goes under the message row). It is a live region, so a screen reader announces
 * it when it appears or its text changes.
 */
@Component({
  selector: 'app-warning-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="head">
      <span class="icon" aria-hidden="true"><ng-content select="svg" /></span>
      <div class="message"><ng-content /></div>
      <ng-content select="[panelAction]" />
    </div>
    <ng-content select="[panelDetails]" />
  `,
  styleUrl: './warning-panel.component.css',
  host: { role: 'status' }
})
export class WarningPanelComponent {}
