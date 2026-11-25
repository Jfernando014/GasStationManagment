import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import {
  StatusStripComponent, StripItem, StripTone
} from '../../../../atomic-design/molecules/status-strip/status-strip.component';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { dayLabel } from '../../helpers/overtime-display.helpers';
import { JournalDay, JournalState } from '../../helpers/overtime-view.helpers';

const TONES: Record<JournalState, StripTone> = {
  closed: 'done', 'no-hours': 'warning', open: 'pending', today: 'current', future: 'upcoming', unknown: 'upcoming'
};

function stateText({ state, reason }: JournalDay): string {
  switch (state) {
    case 'closed':
      return reason === 'NOVELTIES_NOT_APPLIED' ? 'Cerrada · sin novedades confirmadas' : 'Jornada cerrada';
    case 'no-hours':
      return reason === 'PENDING' ? 'Cerrada · horas sin calcular' : 'Cerrada · sin registro de horas';
    case 'open':
      return reason === 'MISSING_RECORD' ? 'Jornada sin cerrar' : 'Sin cerrar · sus horas ya cuentan';
    case 'today':
      return 'Hoy';
    case 'future':
      return 'Por venir';
    default:
      return 'Estado no disponible';
  }
}

function toItem(day: JournalDay): StripItem {
  const date = formatShortDate(day.date, false);
  const item: StripItem = {
    id: day.date, tone: TONES[day.state], name: date, tooltip: `${dayLabel(day.date)} · ${stateText(day)}`
  };
  // A closed day without hours leads to its detail, where it is reprocessed.
  return day.state === 'no-hours'
    ? { ...item, name: `Ver el día ${date}`, link: ['/personal/horas-extra/dia', day.date] } : item;
}

/** How far a fortnight has gone: how many of its journals are closed, which are not, and the way to close them. */
@Component({
  selector: 'app-fortnight-progress',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, SkeletonComponent, StatusStripComponent],
  templateUrl: './fortnight-progress.component.html',
  styleUrl: './fortnight-progress.component.css'
})
export class FortnightProgressComponent {
  readonly days = input<JournalDay[]>([]);
  readonly loading = input(false);
  /** Month and fortnight on screen: the detail of a day brings the user back to them. */
  readonly queryParams = input<Record<string, string | number>>({});

  protected readonly closed = computed(() =>
    this.days().filter(day => day.state === 'closed' || day.state === 'no-hours').length);
  protected readonly withoutHours = computed(() => this.days().filter(day => day.state === 'no-hours').length);
  protected readonly open = computed(() => this.days().filter(day => day.state === 'open').length);
  /** The first journal still open, where closing them starts; null when there is none. */
  protected readonly oldestOpen = computed(() => this.days().find(day => day.state === 'open')?.date ?? null);
  protected readonly items = computed<StripItem[]>(() => this.days().map(toItem));
}
