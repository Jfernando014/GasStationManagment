import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { formatLongDate } from '../../../../shared/helpers/format.helpers';
import { dayOfMonth, ShiftTone } from '../../../../shared/helpers/shift-display.helpers';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import {
  WorkerMonth,
  WorkerMonthDay,
  WorkerMonthNovelty,
  WorkerNoveltyKind
} from '../../../../shared/models/dtos/worker-profile.dto';
import { leadingBlankDays, monthName, monthTitle, toneForCode } from '../../helpers/worker-profile.helpers';

/** Side the grid slides in from when the month changes. */
export type CalendarDirection = 'next' | 'previous' | 'none';

interface NoveltyView {
  kind: WorkerNoveltyKind;
  label: string;
  modifier: 'absent' | 'covering' | 'change' | 'swap';
  tip: string;
  /** Reason alone, for the spoken description of an absent day. */
  reason: string;
}

interface DayCell {
  date: string;
  day: number;
  today: boolean;
  /** Spoken description of the cell; the visible number and code are hidden from screen readers. */
  aria: string;
  chipCode: string | null;
  tone: ShiftTone;
  /** The code worked differs from the schedule because of a novelty. */
  changed: boolean;
  absent: boolean;
  rest: boolean;
  novelties: NoveltyView[];
}

const WEEKDAY_HEADERS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

const NOVELTY_STYLE: Record<WorkerNoveltyKind, Pick<NoveltyView, 'label' | 'modifier'>> = {
  ABSENT: { label: 'Ausente', modifier: 'absent' },
  COVERING: { label: 'Reemplazo', modifier: 'covering' },
  SHIFT_CHANGE: { label: 'Cambio', modifier: 'change' },
  SWAP: { label: 'Intercambio', modifier: 'swap' }
};

const SKELETON_CELLS = Array.from({ length: 35 }, (_, index) => index);

/** Month of the worker as a calendar: one cell per day with the code worked and the novelties of the day. */
@Component({
  selector: 'app-worker-profile-calendar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ShiftChipComponent, InfoTooltipComponent, EmptyStateComponent],
  templateUrl: './worker-profile-calendar.component.html',
  styleUrl: './worker-profile-calendar.component.css'
})
export class WorkerProfileCalendarComponent {
  /** yyyy-MM. */
  readonly month = input.required<string>();
  readonly data = input<WorkerMonth | null>(null);
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly catalog = input<Record<string, ShiftCode>>({});
  /** Operational day of today, yyyy-MM-dd. */
  readonly today = input.required<string>();
  readonly direction = input<CalendarDirection>('none');

  readonly previous = output<void>();
  readonly next = output<void>();
  readonly retry = output<void>();

  protected readonly weekdayHeaders = WEEKDAY_HEADERS;
  protected readonly skeletonCells = SKELETON_CELLS;

  protected readonly title = computed(() => monthTitle(this.month()));
  protected readonly monthWord = computed(() => monthName(this.month()));
  protected readonly todayInMonth = computed(() => this.today().startsWith(`${this.month()}-`));
  protected readonly blanks = computed(() => Array.from({ length: leadingBlankDays(this.month()) }, (_, index) => index));

  protected readonly cells = computed<DayCell[]>(() => {
    const data = this.data();
    return data ? data.days.map(day => this.buildCell(day)) : [];
  });

  protected readonly hasShifts = computed(() =>
    (this.data()?.days ?? []).some(day => day.gridCode || day.workedCode || day.novelties.length)
  );

  private buildCell(day: WorkerMonthDay): DayCell {
    const novelties = day.novelties.map(novelty => this.noveltyView(novelty));
    const absent = day.novelties.some(novelty => novelty.kind === 'ABSENT');
    const chipCode = absent ? (day.novelties[0].originalCode ?? day.gridCode) : (day.workedCode ?? day.gridCode);
    const today = day.date === this.today();
    return {
      date: day.date,
      day: dayOfMonth(day.date),
      today,
      aria: this.describe(day, chipCode, absent, today, novelties),
      chipCode,
      tone: toneForCode(chipCode, chipCode ? this.catalog()[chipCode] : undefined),
      changed: !absent && novelties.length > 0,
      absent,
      rest: chipCode === 'DESCANSO',
      novelties
    };
  }

  private describe(day: WorkerMonthDay, chipCode: string | null, absent: boolean, today: boolean, novelties: NoveltyView[]): string {
    const parts = [formatLongDate(day.date)];
    if (!chipCode) {
      parts.push('sin turno');
    } else if (absent) {
      parts.push(`ausente, tenía ${this.codeName(chipCode)}`);
    } else {
      parts.push(this.codeName(chipCode));
    }
    if (today) {
      parts.push('día operativo de hoy');
    }
    novelties.forEach(novelty => parts.push(novelty.kind === 'ABSENT' ? novelty.reason : novelty.tip));
    return parts.join(', ');
  }

  private noveltyView(novelty: WorkerMonthNovelty): NoveltyView {
    const original = this.codeName(novelty.originalCode);
    const final = novelty.finalCode ? this.codeName(novelty.finalCode) : '';
    const reason = novelty.reason ? ` Motivo: ${novelty.reason}.` : '';
    const exceptional = novelty.exceptionalCoverage ? ' Cobertura excepcional: el turno es de otro rol.' : '';
    let tip: string;
    switch (novelty.kind) {
      case 'ABSENT':
        tip = `Ausencia. Tenía ${original}.${reason}`;
        break;
      case 'COVERING':
        tip = `Reemplazo: cubrió a un compañero con ${final}. En la malla tenía ${original}.${reason}${exceptional}`;
        break;
      case 'SHIFT_CHANGE':
        tip = `Cambio de turno: trabajó ${final} en vez de ${original}.${reason}${exceptional}`;
        break;
      default:
        tip = `Intercambio con un compañero: trabajó ${final} en vez de ${original}.${reason}${exceptional}`;
    }
    return { kind: novelty.kind, ...NOVELTY_STYLE[novelty.kind], tip, reason: novelty.reason ? `Motivo: ${novelty.reason}.` : '' };
  }

  private codeName(code: string): string {
    return this.catalog()[code]?.name ?? code;
  }
}
