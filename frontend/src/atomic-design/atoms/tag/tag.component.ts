import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Small pill that labels a state: "Vigente" (green), "Programado" (blue), "Anterior" (gray); amber, red and orange for warnings, subtractions and holidays. */
@Component({
  selector: 'app-tag',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<ng-content />`,
  styleUrl: './tag.component.css',
  host: { '[class]': "'tag tag--' + variant()" }
})
export class TagComponent {
  readonly variant = input<'green' | 'blue' | 'gray' | 'amber' | 'red' | 'orange'>('gray');
}
