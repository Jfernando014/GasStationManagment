import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Tones of a tag; the tabs reuse them for their status dot. */
export type TagVariant = 'green' | 'blue' | 'gray' | 'amber' | 'red' | 'orange';

/**
 * Small pill that labels a state: "Vigente" (green), "Programado" (blue), "Anterior" (gray); amber, red and orange for warnings,
 * subtractions and holidays. `dot` adds a small colored dot before the text.
 */
@Component({
  selector: 'app-tag',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (dot()) { <span class="dot" aria-hidden="true"></span> }<ng-content />`,
  styleUrl: './tag.component.css',
  host: { '[class]': "'tag tag--' + variant()" }
})
export class TagComponent {
  readonly variant = input<TagVariant>('gray');
  readonly dot = input(false);
}
