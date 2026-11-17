import { convertToParamMap } from '@angular/router';

import {
  CATEGORY_HEADS,
  dayLabel,
  formatHours,
  formatSignedHours,
  fortnightButtonLabel,
  fortnightEnd,
  fortnightFromQuery,
  fortnightLabel,
  fortnightOf,
  hourOfDay,
  hoursText,
  negativeCategories,
  parameterChanges,
  parseFortnightParam,
  parseMonthParam,
  rolesText,
  weekdayName
} from './overtime-display.helpers';

describe('overtime-display.helpers', () => {
  it('exposes the seven overtime categories with descriptions', () => {
    expect(CATEGORY_HEADS.map(head => head.category)).toEqual([
      'RDF', 'RNO', 'RNF', 'HEDO', 'HENO', 'HEDF', 'HENF'
    ]);
    expect(CATEGORY_HEADS.every(head => head.tooltip.length > 0)).toBe(true);
  });

  it('formats weekdays, hours and signed adjustments', () => {
    expect(weekdayName('MONDAY')).toBe('lun');
    expect(weekdayName('SUNDAY')).toBe('dom');
    expect(weekdayName('CUSTOM')).toBe('cus');

    expect(formatHours(0)).toBe('0');
    expect(formatHours(5)).toBe('5');
    expect(formatHours(-4)).toBe('−4');

    expect(formatSignedHours(3)).toBe('+3');
    expect(formatSignedHours(0)).toBe('0');
    expect(formatSignedHours(-2)).toBe('−2');
  });

  it('parses month and fortnight query parameters with safe bounds', () => {
    expect(parseMonthParam('2026-08')).toBe('2026-08');
    expect(parseMonthParam('2026-13')).toBeNull();
    expect(parseMonthParam(null)).toBeNull();

    expect(parseFortnightParam('1')).toBe(1);
    expect(parseFortnightParam('2')).toBe(2);
    expect(parseFortnightParam('3')).toBeNull();

    expect(fortnightOf('2026-08-01')).toEqual({ month: '2026-08', number: 1 });
    expect(fortnightOf('2026-08-15')).toEqual({ month: '2026-08', number: 1 });
    expect(fortnightOf('2026-08-16')).toEqual({ month: '2026-08', number: 2 });

    expect(fortnightFromQuery(convertToParamMap({}), '2026-08-20')).toEqual({ month: '2026-08', number: 2 });
    expect(fortnightFromQuery(convertToParamMap({ mes: '2026-06' }), '2026-08-20')).toEqual({ month: '2026-06', number: 1 });
    expect(fortnightFromQuery(convertToParamMap({ mes: '2026-06', quincena: '2' }), '2026-08-20'))
      .toEqual({ month: '2026-06', number: 2 });
    expect(fortnightFromQuery(convertToParamMap({ mes: '2024-01', quincena: '1' }), '2026-08-20'))
      .toEqual({ month: '2026-08', number: 1 });
    expect(fortnightFromQuery(convertToParamMap({ mes: '2026-12', quincena: '2' }), '2026-08-20'))
      .toEqual({ month: '2026-08', number: 2 });
  });

  it('formats fortnight dates, labels and negative categories', () => {
    expect(fortnightEnd('2026-08', 1)).toBe('2026-08-15');
    expect(fortnightEnd('2026-02', 2)).toBe('2026-02-28');
    expect(fortnightEnd('2024-02', 2)).toBe('2024-02-29');

    expect(fortnightButtonLabel('2026-08', 1)).toBe('1 al 15');
    expect(fortnightButtonLabel('2026-08', 2)).toBe('16 al 31');
    expect(fortnightLabel({ from: '2026-08-01', to: '2026-08-15' })).toBe('1 al 15 ago 2026');
    expect(dayLabel('2026-08-22')).toBe('sáb 22 ago');

    expect(negativeCategories({ RDF: 1, RNO: -2, RNF: 0, HEDO: -1, HENO: 0, HEDF: 0, HENF: 0 }))
      .toEqual(['RNO', 'HEDO']);
  });

  it('describes parameter texts and computes changes between versions', () => {
    expect(hoursText(1)).toBe('1 hora');
    expect(hoursText(8)).toBe('8 horas');
    expect(hourOfDay(7)).toBe('07:00');
    expect(hourOfDay(19)).toBe('19:00');
    expect(rolesText([])).toBe('ninguno');
    expect(rolesText(['TITULAR', 'APOYO'])).toBe('Apoyo y Titular');

    const before = {
      nightStartHour: 21,
      restHours: 1,
      restMinShiftHours: 8,
      fixedHedoHours: 6,
      fixedHedoRoles: ['TITULAR']
    };
    expect(parameterChanges(before, { ...before, fixedHedoRoles: ['TITULAR'] })).toEqual([]);
    expect(parameterChanges(before, { ...before, nightStartHour: Number.NaN })).toEqual([]);
    expect(parameterChanges(before, {
      nightStartHour: 19,
      restHours: 2,
      restMinShiftHours: 10,
      fixedHedoHours: 4,
      fixedHedoRoles: ['APOYO', 'TITULAR']
    })).toEqual([
      { label: 'La noche empieza', from: '21:00', to: '19:00' },
      { label: 'Descanso', from: '1 hora', to: '2 horas' },
      { label: 'Mínimo del turno para descontar el descanso', from: '8 horas', to: '10 horas' },
      { label: 'HEDO fijas por quincena', from: '6 horas', to: '4 horas' },
      { label: 'Roles con HEDO fijas', from: 'Titular', to: 'Apoyo y Titular' }
    ]);
  });
});
