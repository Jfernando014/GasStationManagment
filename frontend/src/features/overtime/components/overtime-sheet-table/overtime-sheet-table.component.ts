import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { CATEGORY_HEADS, formatHours, formatSignedHours } from '../../helpers/overtime-display.helpers';
import { CategoryHours, OVERTIME_CATEGORIES, OvertimeSheet } from '../../models/overtime.model';

interface Cell {
  text: string;
  zero: boolean;
  negative: boolean;
}

interface DayView {
  date: string;
  day: string;
  sunday: boolean;
  holidayTag: boolean;
  holiday: boolean;
  code: string | null;
  rest: boolean;
  hours: number;
  /** "v2", or the start day when the numbers of the versions are not known. */
  version: string;
  versionTitle: string;
  isNewVersion: boolean;
  cells: Cell[];
}

type FooterKind = 'subtotal' | 'fixed' | 'adjustments' | 'total';

interface FooterView {
  kind: FooterKind;
  label: string;
  hours: number | null;
  cells: Cell[];
}

function dayCells(hours: CategoryHours): Cell[] {
  return OVERTIME_CATEGORIES.map(category => ({
    text: hours[category] === 0 ? '·' : formatHours(hours[category]), zero: hours[category] === 0, negative: false
  }));
}

function totalCells(hours: CategoryHours): Cell[] {
  return OVERTIME_CATEGORIES.map(category => ({
    text: formatHours(hours[category]), zero: hours[category] === 0, negative: hours[category] < 0
  }));
}

function netCells(hours: CategoryHours): Cell[] {
  return OVERTIME_CATEGORIES.map(category => ({
    text: hours[category] === 0 ? '—' : formatSignedHours(hours[category]), zero: hours[category] === 0, negative: false
  }));
}

/** Day by day of one fortnight with its subtotals, the fixed HEDO line, the adjustments and the total to pay. */
@Component({
  selector: 'app-overtime-sheet-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InfoTooltipComponent, ShiftChipComponent, TagComponent],
  templateUrl: './overtime-sheet-table.component.html',
  styleUrl: './overtime-sheet-table.component.css'
})
export class OvertimeSheetTableComponent {
  readonly sheet = input.required<OvertimeSheet>();
  /** Number of each version of the parameters by its first day; empty while it is not known. */
  readonly versionNumbers = input<Record<string, number>>({});

  protected readonly heads = CATEGORY_HEADS;

  /** The fortnight used more than one version: only then the table shows which one each day used. */
  protected readonly mixed = computed(() => this.sheet().parameterVersions.length > 1);

  protected readonly days = computed<DayView[]>(() => {
    const sheet = this.sheet();
    const newest = sheet.parameterVersions[sheet.parameterVersions.length - 1];
    return sheet.days.map(day => {
      const number = this.versionNumbers()[day.parametersFrom];
      return {
        date: formatShortDate(day.date, false),
        day: day.weekday,
        sunday: day.weekday === 'dom',
        holidayTag: day.holiday && day.weekday !== 'dom',
        holiday: day.holiday,
        code: day.workedCode,
        rest: day.workedCode === null,
        hours: day.grossHours,
        version: number ? `v${number}` : formatShortDate(day.parametersFrom, false),
        versionTitle: `Calculado con los parámetros vigentes desde ${formatShortDate(day.parametersFrom)}`,
        isNewVersion: day.parametersFrom === newest,
        cells: dayCells(day.hours)
      };
    });
  });

  protected readonly footer = computed<FooterView[]>(() => {
    const sheet = this.sheet();
    const lines: FooterView[] = [
      { kind: 'subtotal', label: 'Subtotal de los días', hours: sheet.grossHours, cells: totalCells(sheet.subtotals) }
    ];
    if (sheet.fixedHedo.applies) {
      const fixed = Object.fromEntries(OVERTIME_CATEGORIES.map(category =>
        [category, category === 'HEDO' ? sheet.fixedHedo.hours : 0])) as CategoryHours;
      lines.push({ kind: 'fixed', label: 'HEDO fijas de la quincena', hours: null, cells: netCells(fixed) });
    }
    lines.push({ kind: 'adjustments', label: 'Ajustes manuales', hours: null, cells: netCells(sheet.adjustmentsNet) });
    lines.push({ kind: 'total', label: 'Total a pagar', hours: sheet.grossHours, cells: totalCells(sheet.totalToPay) });
    return lines;
  });
}
