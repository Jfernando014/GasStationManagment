import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Small pill that labels a state: "Vigente" (green), "Programado" (blue), "Anterior" (gray). */
@Component({
  selector: 'app-tag',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<ng-content />`,
  styleUrl: './tag.component.css',
  host: { '[class]': "'tag tag--' + variant()" }
})
export class TagComponent {
  readonly variant = input<'green' | 'blue' | 'gray'>('gray');
}
