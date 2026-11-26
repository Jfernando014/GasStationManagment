import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { TagComponent, TagVariant } from '../../../../atomic-design/atoms/tag/tag.component';
import {
  DotValueItem, DotValueListComponent
} from '../../../../atomic-design/molecules/dot-value-list/dot-value-list.component';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { dayLabel } from '../../helpers/overtime-display.helpers';
import {
  extraItems, hoursShort, JournalState, journalState, lacksHours, surchargeItems
} from '../../helpers/overtime-view.helpers';
import { IncompleteReason, OvertimeDay, OvertimeSheet } from '../../models/overtime.model';

/** The version of the parameters changes from this row on. */
interface VersionRow {
  kind: 'version';
  key: string;
  /** "v2"; empty while the numbers of the versions are not known. */
  badge: string;
  since: string;
  from: string;
}

interface ChipView {
  text: string;
  variant: TagVariant;
}

interface RowBase {
  key: string;
  label: string;
  sunday: boolean;
  holidayTag: boolean;
  /** Day to open for more detail, when its hours are pending or unconfirmed. */
  link: string | null;
}

/** A day with its hours. */
interface DayRow extends RowBase {
  kind: 'day';
  code: string | null;
  hours: string;
  surcharges: DotValueItem[];
  extras: DotValueItem[];
  note: string;
}

/** One day, or a run of consecutive days, whose hours are not there yet. */
interface WaitingRow extends RowBase {
  kind: 'waiting';
  state: JournalState;
  chip: ChipView;
  message: string;
  /** First and last day and how many, to merge a run. */
  first: string;
  count: number;
}

type Row = VersionRow | DayRow | WaitingRow;

interface WaitingView {
  chip: ChipView;
  message: string;
  /** What the chip of a run says after the number of days; empty when the state is never grouped. */
  many: string;
}

const WAITING: Record<Exclude<JournalState, 'closed'>, WaitingView> = {
  'no-hours': { chip: { text: 'Cerrada', variant: 'amber' }, message: 'Sin registro de horas', many: '' },
  open: { chip: { text: 'Sin cerrar', variant: 'amber' }, message: 'Se calcula al cerrar', many: 'sin cerrar' },
  today: { chip: { text: 'Hoy', variant: 'orange' }, message: 'En curso', many: '' },
  future: { chip: { text: 'Por venir', variant: 'gray' }, message: '', many: 'por venir' },
  unknown: { chip: { text: 'Sin registro', variant: 'gray' }, message: 'Sin registro de horas', many: 'sin registro' }
};

/** A closed day whose hours failed to be calculated; it can be reprocessed from its detail. */
const FAILED: WaitingView = { chip: { text: 'Pendiente', variant: 'amber' }, message: 'Horas sin calcular', many: '' };

/** "lun 16" */
const shortDay = (date: string) => dayLabel(date).split(' ').slice(0, 2).join(' ');

function hasData(day: OvertimeDay): boolean {
  return day.workedCode !== null || day.grossHours > 0 || Object.values(day.hours).some(hours => hours !== 0);
}

/** What a day with hours needs to be told about them. */
function noteOf(state: JournalState, reason: IncompleteReason | null): { text: string; open: boolean } {
  if (reason === 'PENDING') return { text: 'Horas pendientes', open: true };
  if (reason === 'NOVELTIES_NOT_APPLIED') return { text: 'Sin novedades confirmadas', open: true };
  return { text: state === 'open' ? 'Jornada sin cerrar: estas horas ya cuentan' : '', open: false };
}

function toRow(
  day: OvertimeDay, reason: IncompleteReason | null, today: string, closed: ReadonlySet<string> | null
): DayRow | WaitingRow {
  const base: RowBase = {
    key: day.date,
    label: dayLabel(day.date),
    sunday: day.weekday === 'dom',
    holidayTag: day.holiday && day.weekday !== 'dom',
    link: null
  };
  const state = journalState(day.date, reason, today, closed);
  // Calculated hours are always shown, whatever the state of the journal.
  if (state !== 'closed' && !hasData(day) && lacksHours(reason)) {
    const { chip, message } = state === 'no-hours' && reason === 'PENDING' ? FAILED : WAITING[state];
    return {
      ...base, kind: 'waiting', state, chip, message, link: state === 'no-hours' ? day.date : null,
      first: day.date, count: 1
    };
  }
  const note = noteOf(state, reason);
  return {
    ...base,
    kind: 'day',
    code: day.workedCode,
    hours: hoursShort(day.grossHours),
    surcharges: surchargeItems(day.hours),
    extras: extraItems(day.hours),
    note: note.text,
    link: note.open ? day.date : null
  };
}

/** Joins a day without hours to the run before it when both are in the same state and that state can be grouped. */
function merged(run: WaitingRow, next: WaitingRow): WaitingRow | null {
  const many = next.state === 'closed' ? '' : WAITING[next.state].many;
  if (run.state !== next.state || !many) return null;
  const count = run.count + 1;
  return {
    ...run,
    label: `${shortDay(run.first)} – ${shortDay(next.first)}`,
    sunday: false,
    holidayTag: false,
    chip: { ...run.chip, text: `${count} ${many}` },
    count
  };
}

/** Day by day where the hours of one fortnight come from. Days without hours are grouped unless asked apart. */
@Component({
  selector: 'app-overtime-sheet-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ShiftChipComponent, TagComponent, DotValueListComponent],
  templateUrl: './overtime-sheet-table.component.html',
  styleUrl: './overtime-sheet-table.component.css'
})
export class OvertimeSheetTableComponent {
  readonly sheet = input.required<OvertimeSheet>();
  /** Number of each version of the parameters by its first day; empty while it is not known. */
  readonly versionNumbers = input<Record<string, number>>({});
  /** Closed journals of the month; null while they are not known. */
  readonly closedDays = input<ReadonlySet<string> | null>(null);
  /** Month and fortnight on screen: the detail of a day brings the user back to them. */
  readonly queryParams = input<Record<string, string | number>>({});

  private readonly today = stationToday();
  protected readonly expanded = signal(false);

  private readonly allRows = computed<Row[]>(() => {
    const sheet = this.sheet();
    const reasons = new Map(sheet.affectedDates.map(item => [item.date, item.reason]));
    return sheet.days.flatMap((day, index) => {
      const row = toRow(day, reasons.get(day.date) ?? null, this.today, this.closedDays());
      const previous = sheet.days[index - 1];
      if (!previous || previous.parametersFrom === day.parametersFrom) return [row];
      const number = this.versionNumbers()[day.parametersFrom];
      const change: VersionRow = {
        kind: 'version',
        key: `version-${day.date}`,
        badge: number ? `v${number}` : '',
        since: formatShortDate(day.date, false),
        from: day.parametersFrom
      };
      return [change, row];
    });
  });

  private readonly groupedRows = computed<Row[]>(() => this.allRows().reduce<Row[]>((rows, row) => {
    const last = rows[rows.length - 1];
    const run = last?.kind === 'waiting' && row.kind === 'waiting' ? merged(last, row) : null;
    if (run) rows[rows.length - 1] = run;
    else rows.push(row);
    return rows;
  }, []));

  protected readonly canExpand = computed(() => this.groupedRows().length < this.allRows().length);
  protected readonly rows = computed(() => (this.expanded() ? this.allRows() : this.groupedRows()));
  protected readonly dayCount = computed(() => this.sheet().days.length);
}
