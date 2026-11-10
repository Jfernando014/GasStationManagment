import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ShiftTone, shiftTone, shortCode } from '../../../shared/helpers/shift-display.helpers';

/**
 * Código de turno con el color de su familia (día, día 6 h, 9-6, noche, descanso, apoyo).
 * `chip`: ficha con el código completo. `cell`: bloque que llena una celda de la malla, con el código corto.
 */
@Component({
  selector: 'app-shift-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `{{ text() }}<ng-content />`,
  styleUrl: './shift-chip.component.css',
  host: {
    '[class]': "'shift-chip shift-chip--' + variant() + ' shift-chip--' + tone()",
    '[class.shift-chip--changed]': 'changed()',
    '[attr.title]': 'title() || null'
  }
})
export class ShiftChipComponent {
  readonly code = input<string | null>(null);
  readonly variant = input<'chip' | 'cell'>('chip');
  /** Marca un código distinto al de la malla (por una novedad). */
  readonly changed = input(false);
  readonly title = input('');
  /** Forces the color family when the caller knows it better than the code name does (for example, from the catalog). */
  readonly toneOverride = input<ShiftTone | null>(null);

  readonly tone = computed(() => this.toneOverride() ?? shiftTone(this.code()));
  readonly text = computed(() => (this.variant() === 'cell' ? shortCode(this.code()) : (this.code() ?? '—')));
}
