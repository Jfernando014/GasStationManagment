import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ALERT_LEVEL_LABELS, ShownAlertLevel } from '../../../shared/models/dtos/alert-level.dto';

/**
 * Pill of an alert level: an icon, a color and a text, so the color is never the only sign. "Sin datos" is
 * neutral: it is not an alert. `text` replaces the name of the level, e.g. "1 en alarma".
 */
@Component({
  selector: 'app-alert-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<span class="icon" aria-hidden="true"></span>{{ shown() }}',
  styleUrl: './alert-chip.component.css',
  host: {
    '[attr.data-level]': 'level()',
    '[class.is-large]': "size() === 'large'",
    '[class.is-muted]': 'muted()'
  }
})
export class AlertChipComponent {
  readonly level = input.required<ShownAlertLevel>();
  readonly text = input('');
  readonly size = input<'normal' | 'large'>('normal');
  /** Faded, for a level carried over from an earlier day. */
  readonly muted = input(false);

  protected readonly shown = computed(() => this.text() || ALERT_LEVEL_LABELS[this.level()]);
}
