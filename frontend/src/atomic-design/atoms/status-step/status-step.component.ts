import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Marker and name of a step in a vertical timeline.
 * upcoming: dashed ring that turns slowly · active: solid green dot with a halo that breathes · past: quiet gray dot.
 * The text is always shown, so the state never depends on color alone. The line that joins the steps is drawn
 * by the list that holds them; the marker is 24 px wide, so the line goes through x = 12 px.
 */
@Component({
  selector: 'app-status-step',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="marker" aria-hidden="true"><span class="dot"></span></span>
    <span class="label"><ng-content /></span>
  `,
  styleUrl: './status-step.component.css',
  host: { '[class]': "'step step--' + tone()" }
})
export class StatusStepComponent {
  readonly tone = input<'upcoming' | 'active' | 'past'>('past');
}
