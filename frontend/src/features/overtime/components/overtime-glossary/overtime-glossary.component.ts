import { toSignal } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { catchError, map, of } from 'rxjs';

import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { ShiftsScheduleService } from '../../../../core/services/shifts-schedule.service';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import {
  CATEGORY_HELP, EXTRA_CATEGORIES, rolesText, SURCHARGE_CATEGORIES
} from '../../helpers/overtime-display.helpers';
import { hoursShort } from '../../helpers/overtime-view.helpers';
import { CategoryHours, OvertimeCategory } from '../../models/overtime.model';
import { OvertimeParametersService } from '../../services/overtime-parameters.service';

/** The night always ends at this hour; only its start is a parameter. */
const NIGHT_END_HOUR = 6;
/** Order of the titular codes; any other one follows them. */
const SHIFT_ORDER = ['DIA', 'DIA6', '9-6', 'NOCHE'];

interface CategoryRow {
  sigla: OvertimeCategory;
  title: string;
  help: string;
  total: string;
  zero: boolean;
}

interface ShiftRow {
  code: string;
  name: string;
  help: string;
}

/** "7 p. m." */
function clockLabel(hour: number): string {
  return `${hour % 12 || 12} ${hour < 12 ? 'a. m.' : 'p. m.'}`;
}

const twoDigits = (hour: number) => String(hour).padStart(2, '0');

function worksAtNight(shift: ShiftCode, nightStart: number): boolean {
  return shift.schedule.segments.some(segment => {
    const length = (segment.endHour - segment.startHour + 24) % 24 || 24;
    return Array.from({ length }, (_, index) => (segment.startHour + index) % 24)
      .some(hour => hour >= nightStart || hour < NIGHT_END_HOUR);
  });
}

/** What each sigla and shift code means, with the hours of every category in the fortnight on screen. */
@Component({
  selector: 'app-overtime-glossary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ShiftChipComponent],
  templateUrl: './overtime-glossary.component.html',
  styleUrl: './overtime-glossary.component.css'
})
export class OvertimeGlossaryComponent {
  /** Hours of the fortnight per category, all workers together; null while they are not known. */
  readonly totals = input<CategoryHours | null>(null);
  /** Last day of the fortnight: the parameters in force that day are the ones explained. */
  readonly until = input.required<string>();

  private readonly versions = toSignal(inject(OvertimeParametersService).getHistory().pipe(
    map(result => (result.outcome === 'OK' ? result.data : []))
  ), { initialValue: [] });

  private readonly catalog = toSignal(inject(ShiftsScheduleService).getCatalog().pipe(
    map(codes => codes.filter(code => code.active)),
    catchError(() => of([] as ShiftCode[]))
  ));

  /** The newest version that had started by the end of the fortnight. */
  private readonly parameters = computed(() => {
    const versions = this.versions();
    return versions.find(version => version.validFrom <= this.until()) ?? versions[versions.length - 1] ?? null;
  });

  protected readonly surcharges = computed(() => {
    const start = this.parameters()?.nightStartHour;
    const night = start === undefined ? 'de noche' : `de ${clockLabel(start)} a ${clockLabel(NIGHT_END_HOUR)}`;
    return this.rows(SURCHARGE_CATEGORIES, {
      RNO: `Horas normales ${night}`,
      RDF: 'Horas normales de día en domingo o festivo',
      RNF: 'De noche en domingo o festivo'
    });
  });

  protected readonly extras = computed(() => this.rows(EXTRA_CATEGORIES, {
    HEDO: 'Hora de más, de día, en día normal',
    HENO: 'Hora de más, de noche, en día normal',
    HEDF: 'Hora de más, de día, en domingo o festivo',
    HENF: 'Hora de más, de noche, en domingo o festivo'
  }));

  protected readonly catalogLoading = computed(() => this.catalog() === undefined);

  protected readonly titularShifts = computed<ShiftRow[]>(() => {
    const start = this.parameters()?.nightStartHour;
    const rank = (code: string) => (SHIFT_ORDER.indexOf(code) + 1) || SHIFT_ORDER.length + 1;
    return (this.catalog() ?? [])
      .filter(shift => shift.role?.name === 'TITULAR')
      .sort((a, b) => rank(a.code) - rank(b.code))
      .map(shift => {
        const hours = shift.schedule.segments
          .map(segment => `${twoDigits(segment.startHour)}–${twoDigits(segment.endHour)}`).join(' y ');
        const night = start !== undefined && worksAtNight(shift, start) ? ' · genera recargo nocturno' : '';
        return { code: shift.code, name: shift.name, help: `${hours} · ${shift.schedule.totalHours} h${night}` };
      });
  });

  /** One support code of the catalog stands for all of them. */
  protected readonly supportSample = computed(() =>
    (this.catalog() ?? []).find(shift => shift.role?.name === 'APOYO' && !shift.code.startsWith('AP_'))?.code ?? null);

  protected readonly fixedText = computed(() => {
    const parameters = this.parameters();
    if (!parameters || parameters.fixedHedoHours === 0 || parameters.fixedHedoRoles.length === 0) return '';
    return `${parameters.fixedHedoHours} h extra diurnas por quincena a cada ${rolesText(parameters.fixedHedoRoles).toLowerCase()}, aunque no trabaje de más`;
  });

  private rows(categories: OvertimeCategory[], help: Partial<Record<OvertimeCategory, string>>): CategoryRow[] {
    const totals = this.totals();
    return categories.map(category => ({
      sigla: category,
      title: CATEGORY_HELP[category].title,
      help: help[category] ?? '',
      total: totals ? hoursShort(totals[category]) : '…',
      zero: totals?.[category] === 0
    }));
  }
}
