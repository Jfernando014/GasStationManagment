import { Component, input } from '@angular/core';

/** Active / inactive label with a colored dot. */
@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: "{{ active() ? activeLabel() : inactiveLabel() }}",
  host: {
    class: 'ps-estado',
    '[class.ps-estado--activo]': 'active()',
    '[class.ps-estado--inactivo]': '!active()'
  }
})
export class StatusBadgeComponent {
  readonly active = input.required<boolean>();
  readonly activeLabel = input('Activo');
  readonly inactiveLabel = input('Inactivo');
}
