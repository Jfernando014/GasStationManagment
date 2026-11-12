import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { WarningPanelComponent } from '../../../../atomic-design/molecules/warning-panel/warning-panel.component';
import { dayLabel } from '../../helpers/overtime-display.helpers';
import { AffectedDate, IncompleteReason } from '../../models/overtime.model';

/** Up to this many days the list starts open. */
const MAX_OPEN_ROWS = 5;

/** What the user reads for each reason, and what the link of the day says. */
const REASON_VIEW: Record<IncompleteReason, { message: string; link: string }> = {
  PENDING: { message: 'La jornada se cerró, pero las horas todavía no se calcularon.', link: 'Ver el día' },
  MISSING_RECORD: { message: 'No hay registro de horas de ese día.', link: 'Revisar en la malla' },
  NOVELTIES_NOT_APPLIED: { message: 'Se guardó con la malla, sin novedades confirmadas.', link: 'Ver el día' }
};

/** Days of the fortnight whose hours are missing, pending or saved without the novelties; each one links to its detail. */
@Component({
  selector: 'app-overtime-notice',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, WarningPanelComponent],
  templateUrl: './overtime-notice.component.html',
  styleUrl: './overtime-notice.component.css'
})
export class OvertimeNoticeComponent {
  readonly dates = input.required<AffectedDate[]>();
  /** Month and fortnight on screen: the detail of a day brings the user back to them. */
  readonly month = input.required<string>();
  readonly fortnight = input.required<number>();

  protected readonly rows = computed(() => this.dates().map(item => ({
    date: item.date,
    label: dayLabel(item.date),
    message: REASON_VIEW[item.reason].message,
    link: REASON_VIEW[item.reason].link
  })));

  protected readonly queryParams = computed(() => ({ mes: this.month(), quincena: this.fortnight() }));

  /** A short list opens by itself; a long one (a whole fortnight without records) starts folded. */
  protected readonly open = signal(true);

  constructor() {
    effect(() => this.open.set(this.rows().length <= MAX_OPEN_ROWS));
  }
}
