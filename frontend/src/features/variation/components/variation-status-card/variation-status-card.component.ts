import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { AlertChipComponent } from '../../../../atomic-design/atoms/alert-chip/alert-chip.component';
import { ALERT_LEVEL_LABELS, AlertLevel } from '../../../../shared/models/dtos/alert-level.dto';
import { StatusView } from '../../helpers/variation-detail.helpers';

/** Where the month of a fuel stands, in plain words: what happens, what to do and how far it has escalated. */
@Component({
  selector: 'app-variation-status-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AlertChipComponent],
  template: `
    <div class="head">
      <app-alert-chip size="large" [level]="status().level" />
      @if (status().asOf) {
        <span class="as-of">Estado del mes · {{ status().asOf }}</span>
      }
    </div>
    <h2>{{ status().title }}</h2>
    <p class="text">{{ status().text }}</p>
    @if (status().advice) {
      <p class="advice"><b>Qué hacer:</b> {{ status().advice }}</p>
    }
    <div class="track">
      <ol aria-label="Niveles de alerta">
        @for (level of levels; track level) {
          <li [class.is-current]="level === status().level" [attr.data-level]="level"
            [attr.aria-current]="level === status().level ? 'step' : null">{{ labels[level] }}</li>
        }
      </ol>
      @if (status().streak) {
        <span class="streak">{{ status().streak }}</span>
      }
    </div>
  `,
  styleUrl: './variation-status-card.component.css',
  host: { '[attr.data-level]': 'status().level' }
})
export class VariationStatusCardComponent {
  readonly status = input.required<StatusView>();

  protected readonly levels: AlertLevel[] = ['OK', 'REVISAR', 'AYUDA', 'ALARMA'];
  protected readonly labels = ALERT_LEVEL_LABELS;
}
