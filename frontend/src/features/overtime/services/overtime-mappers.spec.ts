import { SheetDto, SummaryDto } from '../models/overtime.dto';
import { toCategoryHours, toSheet, toSummary } from './overtime-mappers';

describe('overtime-mappers', () => {
  const zeroHoursDto = { rdf: 0, rno: 0, rnf: 0, hedo: 0, heno: 0, hedf: 0, henf: 0 };

  it('maps lowercase category DTO keys to uppercase CategoryHours', () => {
    expect(toCategoryHours({ rdf: 1, rno: 2, rnf: 3, hedo: 4, heno: 5, hedf: 6, henf: 7 })).toEqual({
      RDF: 1, RNO: 2, RNF: 3, HEDO: 4, HENO: 5, HEDF: 6, HENF: 7
    });
  });

  it('maps a full overtime sheet including sorted versions and adjustment category fallback', () => {
    const dto: SheetDto = {
      worker: { workerId: 'w1', workerName: 'Ana Milena', role: 'TITULAR' },
      fortnight: { month: '2026-08', fortnight: 2, from: '2026-08-16', to: '2026-08-31' },
      days: [
        {
          ...zeroHoursDto,
          rdf: 8,
          date: '2026-08-16',
          dayOfWeek: 'SUNDAY',
          holiday: true,
          workedCode: 'DIA',
          grossHours: 10,
          parametersValidFrom: '2026-08-20'
        }
      ],
      subtotals: { ...zeroHoursDto, rdf: 8 },
      grossHours: 10,
      fixedHedo: { applies: true, hours: 6 },
      adjustments: [
        {
          id: 1,
          workerId: 'w1',
          fortnight: { month: '2026-08', fortnight: 2, from: '2026-08-16', to: '2026-08-31' },
          category: 'UNKNOWN_CAT',
          direction: 'ADD',
          hours: 2,
          reason: 'Apoyo extra',
          createdAt: '2026-08-17T10:00:00Z',
          createdBy: 'admin'
        }
      ],
      adjustmentsNet: { ...zeroHoursDto, hedo: 2 },
      totalToPay: { ...zeroHoursDto, rdf: 8, hedo: 8 },
      incomplete: false,
      noveltiesUnconfirmed: true,
      affectedDates: [{ date: '2026-08-18', reason: 'NOVELTIES_NOT_APPLIED' }],
      parameterVersions: ['2026-08-20', '2026-08-01']
    };

    const sheet = toSheet(dto);
    expect(sheet.worker).toEqual({ id: 'w1', name: 'Ana Milena', role: 'TITULAR' });
    expect(sheet.fortnight).toEqual({ month: '2026-08', number: 2, from: '2026-08-16', to: '2026-08-31' });
    expect(sheet.days[0].weekday).toBe('dom');
    expect(sheet.days[0].hours.RDF).toBe(8);
    expect(sheet.adjustments[0].category).toBe('HEDO');
    expect(sheet.parameterVersions).toEqual(['2026-08-01', '2026-08-20']);
    expect(sheet.noveltiesUnconfirmed).toBe(true);
  });

  it('maps a summary response normalizing fortnight number', () => {
    const dto: SummaryDto = {
      fortnight: { month: '2026-08', fortnight: 1, from: '2026-08-01', to: '2026-08-15' },
      workers: [
        {
          worker: { workerId: 'w2', workerName: 'Carlos', role: null },
          grossHours: 80,
          subtotals: { ...zeroHoursDto, rno: 4 },
          fixedHedo: { applies: false, hours: 0 },
          adjustmentsNet: { ...zeroHoursDto },
          totalToPay: { ...zeroHoursDto, rno: 4 },
          incomplete: true,
          noveltiesUnconfirmed: false
        }
      ],
      incomplete: true,
      noveltiesUnconfirmed: false,
      affectedDates: [{ date: '2026-08-05', reason: 'PENDING' }]
    };

    const summary = toSummary(dto);
    expect(summary.fortnight.number).toBe(1);
    expect(summary.workers).toHaveLength(1);
    expect(summary.workers[0]).toMatchObject({
      worker: { id: 'w2', name: 'Carlos', role: null },
      grossHours: 80,
      incomplete: true
    });
    expect(summary.workers[0].totalToPay.RNO).toBe(4);
    expect(summary.affectedDates).toEqual([{ date: '2026-08-05', reason: 'PENDING' }]);
  });
});
