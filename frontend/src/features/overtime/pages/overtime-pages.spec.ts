import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';

import { ShiftsScheduleService } from '../../../core/services/shifts-schedule.service';
import { WorkersService } from '../../../core/services/workers.service';
import { CategoryHours, DayHours, OvertimeSheet, OvertimeSummary, ParameterVersion } from '../models/overtime.model';
import { OvertimeParametersService } from '../services/overtime-parameters.service';
import { OvertimeSheetService } from '../services/overtime-sheet.service';
import { WorkerHoursService } from '../services/worker-hours.service';
import { OvertimeDayPageComponent } from './overtime-day-page/overtime-day-page.component';
import { OvertimeParametersTabComponent } from './overtime-parameters-tab/overtime-parameters-tab.component';
import { OvertimeSheetPageComponent } from './overtime-sheet-page/overtime-sheet-page.component';
import { OvertimeSummaryPageComponent } from './overtime-summary-page/overtime-summary-page.component';

function zeroHours(partial: Partial<CategoryHours> = {}): CategoryHours {
  return { RDF: 0, RNO: 0, RNF: 0, HEDO: 0, HENO: 0, HEDF: 0, HENF: 0, ...partial };
}

function makeSummary(): OvertimeSummary {
  return {
    fortnight: { month: '2026-08', number: 1, from: '2026-08-01', to: '2026-08-15' },
    workers: [
      {
        worker: { id: 'w1', name: 'Ana', role: 'TITULAR' },
        grossHours: 48,
        totalToPay: zeroHours({ RDF: 4 }),
        incomplete: false,
        noveltiesUnconfirmed: false
      },
      {
        worker: { id: 'w2', name: 'Carlos', role: 'TITULAR' },
        grossHours: 40,
        totalToPay: zeroHours(),
        incomplete: false,
        noveltiesUnconfirmed: false
      }
    ],
    affectedDates: []
  };
}

function makeSheet(): OvertimeSheet {
  return {
    worker: { id: 'w1', name: 'Ana', role: 'TITULAR' },
    fortnight: { month: '2026-08', number: 1, from: '2026-08-01', to: '2026-08-15' },
    days: [
      {
        date: '2026-08-01',
        weekday: 'sáb',
        holiday: false,
        workedCode: 'DIA',
        grossHours: 10,
        hours: zeroHours(),
        parametersFrom: '2025-11-01'
      },
      {
        date: '2026-08-10',
        weekday: 'lun',
        holiday: false,
        workedCode: 'DIA',
        grossHours: 10,
        hours: zeroHours({ RNO: 2 }),
        parametersFrom: '2026-08-10'
      }
    ],
    subtotals: zeroHours({ RNO: 2 }),
    grossHours: 20,
    fixedHedo: { applies: true, hours: 6 },
    adjustments: [],
    adjustmentsNet: zeroHours(),
    totalToPay: zeroHours({ RNO: 2, HEDO: 6 }),
    incomplete: false,
    noveltiesUnconfirmed: false,
    affectedDates: [],
    parameterVersions: ['2025-11-01', '2026-08-10']
  };
}

function createRouteStub(params: Record<string, string>, query: Record<string, string>) {
  const paramSubject = new BehaviorSubject<ParamMap>(convertToParamMap(params));
  const querySubject = new BehaviorSubject<ParamMap>(convertToParamMap(query));
  return {
    route: {
      snapshot: {
        get paramMap() {
          return paramSubject.value;
        },
        get queryParamMap() {
          return querySubject.value;
        }
      },
      paramMap: paramSubject.asObservable(),
      queryParamMap: querySubject.asObservable()
    },
    paramSubject,
    querySubject
  };
}

describe('overtime pages', () => {
  it('loads summary for query period and navigates on fortnight change', async () => {
    const summaryService = { getSummary: vi.fn(() => of({ outcome: 'OK' as const, data: makeSummary() })) };
    const { route } = createRouteStub({}, { mes: '2026-08', quincena: '1' });

    await TestBed.configureTestingModule({
      imports: [OvertimeSummaryPageComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: OvertimeSheetService, useValue: summaryService }
      ]
    }).compileComponents();

    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const fixture = TestBed.createComponent(OvertimeSummaryPageComponent);
    fixture.detectChanges();

    expect(summaryService.getSummary).toHaveBeenCalledWith('2026-08', 1);
    expect(fixture.nativeElement.textContent).toContain('Ana');

    (fixture.componentInstance as unknown as { selectFortnight(n: 1 | 2): void }).selectFortnight(2);
    expect(navigateSpy).toHaveBeenCalledWith([], {
      relativeTo: route,
      queryParams: { mes: '2026-08', quincena: 2 },
      replaceUrl: true
    });
  });

  it('loads worker sheet, parameter version numbers, and neighbour pagination in OvertimeSheetPageComponent', async () => {
    const sheetService = {
      getSheet: vi.fn(() => of({ outcome: 'OK' as const, data: makeSheet() })),
      getSummary: vi.fn(() => of({ outcome: 'OK' as const, data: makeSummary() }))
    };
    const paramsService = {
      getHistory: vi.fn(() =>
        of({
          outcome: 'OK' as const,
          data: [
            {
              id: 2, number: 2, validFrom: '2026-08-10', validTo: null, current: true, scheduled: false,
              nightStartHour: 19, restHours: 1, restMinShiftHours: 8, fixedHedoHours: 6,
              fixedHedoRoles: ['TITULAR'], reason: null, createdBy: 'admin', createdAt: '2026-08-10T09:00:00Z'
            },
            {
              id: 1, number: 1, validFrom: '2025-11-01', validTo: '2026-08-09', current: false, scheduled: false,
              nightStartHour: 21, restHours: 1, restMinShiftHours: 8, fixedHedoHours: 6,
              fixedHedoRoles: ['TITULAR'], reason: 'Inicial', createdBy: 'admin', createdAt: '2025-11-01T09:00:00Z'
            }
          ]
        })
      )
    };
    const { route } = createRouteStub({ workerId: 'w1' }, { mes: '2026-08', quincena: '1' });

    await TestBed.configureTestingModule({
      imports: [OvertimeSheetPageComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: OvertimeSheetService, useValue: sheetService },
        { provide: OvertimeParametersService, useValue: paramsService }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(OvertimeSheetPageComponent);
    fixture.detectChanges();

    expect(sheetService.getSheet).toHaveBeenCalledWith('w1', '2026-08', 1);
    expect(paramsService.getHistory).toHaveBeenCalled();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('1 de 2');
    expect(text).toContain('Cambio de parámetros en la quincena');
    expect(text).toContain('v2');
  });

  it('loads pending day with schedule fallback and handles reprocess in OvertimeDayPageComponent', async () => {
    const pendingDay: DayHours = {
      date: '2026-08-05',
      status: 'PENDING',
      noveltiesApplied: false,
      attempts: 1,
      workers: []
    };
    const processedDay: DayHours = {
      date: '2026-08-05',
      status: 'PROCESSED',
      noveltiesApplied: true,
      attempts: 2,
      workers: [
        { workerId: 'w1', name: 'Ana', workedCode: 'DIA', gridCode: 'DIA', hours: 10, absent: false, novelty: null }
      ]
    };

    const hoursService = {
      getDay: vi.fn()
        .mockReturnValueOnce(of({ outcome: 'OK' as const, data: pendingDay }))
        .mockReturnValueOnce(of({ outcome: 'OK' as const, data: processedDay })),
      reprocess: vi.fn(() => of({ outcome: 'PROCESSED' as const }))
    };
    const scheduleService = {
      getSchedule: vi.fn(() => of({
        workers: [
          { workerId: 'w1', fullName: 'Ana', role: 'TITULAR', days: [{ date: '2026-08-05', shiftCode: 'DIA' }] }
        ]
      }))
    };
    const { route } = createRouteStub({ date: '2026-08-05' }, { mes: '2026-08', quincena: '1' });

    await TestBed.configureTestingModule({
      imports: [OvertimeDayPageComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: WorkerHoursService, useValue: hoursService },
        { provide: ShiftsScheduleService, useValue: scheduleService }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(OvertimeDayPageComponent);
    fixture.detectChanges();

    expect(hoursService.getDay).toHaveBeenCalledWith('2026-08-05');
    expect(scheduleService.getSchedule).toHaveBeenCalledWith('2026-08-05', '2026-08-05');
    expect(fixture.nativeElement.textContent).toContain('Ana');

    (fixture.componentInstance as unknown as { reprocess(): void }).reprocess();
    fixture.detectChanges();

    expect(hoursService.reprocess).toHaveBeenCalledWith('2026-08-05');
    expect(fixture.nativeElement.textContent).toContain('Procesado · las horas ya están en la quincena');
  });

  it('selects requested parameter version from query string and diffs against previous version', async () => {
    const versions: ParameterVersion[] = [
      {
        id: 2, number: 2, validFrom: '2026-10-01', validTo: null, current: true, scheduled: false,
        nightStartHour: 19, restHours: 2, restMinShiftHours: 8, fixedHedoHours: 6,
        fixedHedoRoles: ['TITULAR'], reason: 'Reforma', createdBy: 'admin', createdAt: '2026-10-01T12:00:00Z'
      },
      {
        id: 1, number: 1, validFrom: '2025-11-01', validTo: '2026-09-30', current: false, scheduled: false,
        nightStartHour: 21, restHours: 1, restMinShiftHours: 8, fixedHedoHours: 6,
        fixedHedoRoles: ['TITULAR'], reason: 'Inicial', createdBy: 'admin', createdAt: '2025-11-01T12:00:00Z'
      }
    ];
    const paramsService = { getHistory: vi.fn(() => of({ outcome: 'OK' as const, data: versions })) };
    const workersService = { listRoles: vi.fn(() => of([])) };
    const { route } = createRouteStub({}, { desde: '2025-11-01' });

    await TestBed.configureTestingModule({
      imports: [OvertimeParametersTabComponent],
      providers: [
        { provide: ActivatedRoute, useValue: route },
        { provide: OvertimeParametersService, useValue: paramsService },
        { provide: WorkersService, useValue: workersService }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(OvertimeParametersTabComponent);
    fixture.detectChanges();

    const host = fixture.componentInstance as unknown as {
      selected: () => ParameterVersion | null;
      changes: () => unknown;
      select(id: number): void;
    };

    expect(host.selected()?.id).toBe(1);
    expect(host.changes()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Es la primera versión: son los valores iniciales.');

    host.select(2);
    fixture.detectChanges();
    expect(host.selected()?.id).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('La noche empieza · 21:00 → 19:00');
  });
});
