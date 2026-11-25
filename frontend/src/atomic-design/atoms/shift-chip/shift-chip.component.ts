import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ShiftHoursService } from '../../../core/services/shift-hours.service';
import { ShiftTone, shiftTone, shortCode } from '../../../shared/helpers/shift-display.helpers';
import { isCustomShiftCode } from '../../../shared/helpers/support-hours.helpers';
import { ShiftScheduleSegment } from '../../../shared/models/dtos/shift-catalog.dto';

/**
 * Código de turno con el color de su familia (día, día 6 h, 9-6, noche, descanso, apoyo).
 * `chip`: ficha con el código completo. `cell`: bloque que llena una celda de la malla, con el código corto.
 * Un horario de apoyo personalizado no muestra su código sino sus horas; en una celda, un tramo por línea.
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
    '[class.shift-chip--hours]': 'custom()',
    '[class.shift-chip--split]': 'split()',
    '[attr.title]': 'title() || null'
  }
})
export class ShiftChipComponent {
  private readonly shiftHours = inject(ShiftHoursService);

  readonly code = input<string | null>(null);
  readonly variant = input<'chip' | 'cell'>('chip');
  /** Marca un código distinto al de la malla (por una novedad). */
  readonly changed = input(false);
  readonly title = input('');
  /** Forces the color family when the caller knows it better than the code name does (for example, from the catalog). */
  readonly toneOverride = input<ShiftTone | null>(null);
  /** Hours of the shift when the caller already has them, so they are not requested again. */
  readonly segments = input<ShiftScheduleSegment[] | null | undefined>(null);

  readonly tone = computed(() => this.toneOverride() ?? shiftTone(this.code()));
  readonly custom = computed(() => isCustomShiftCode(this.code()));
  readonly text = computed(() => {
    const code = this.code();
    if (code && this.custom()) {
      const hours = this.shiftHours.text(code, this.segments());
      return this.variant() === 'cell' ? hours.replace(' y ', '\n') : hours;
    }
    return this.variant() === 'cell' ? shortCode(code) : (code ?? '—');
  });
  /** Custom hours in two blocks, which need more room than a code. */
  readonly split = computed(() => this.custom() && /\n| y /.test(this.text()));
}
