import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { ToastService } from '../../../core/services/toast.service';
import { WorkersService } from '../../../core/services/workers.service';
import { CategoryHours, OvertimeSheet, OvertimeSummaryRow, ParameterVersion } from '../models/overtime.model';
import { OvertimeSheetService } from '../services/overtime-sheet.service';
import { OvertimeParametersService } from '../services/overtime-parameters.service';
import { AdjustmentsPanelComponent } from './adjustments-panel/adjustments-panel.component';
import { OvertimeNoticeComponent } from './overtime-notice/overtime-notice.component';
import { OvertimeSheetTableComponent } from './overtime-sheet-table/overtime-sheet-table.component';
import { OvertimeSummaryTableComponent } from './overtime-summary-table/overtime-summary-table.component';
import { ParameterVersionDrawerComponent } from './parameter-version-drawer/parameter-version-drawer.component';

function hours(partial: Partial<CategoryHours> = {}): CategoryHours {
  return { RDF: 0, RNO: 0, RNF: 0, HEDO: 0, HENO: 0, HEDF: 0, HENF: 0, ...partial };
}

function makeSheet(overrides: Partial<OvertimeSheet> = {}): OvertimeSheet {
  return {
    worker: { id: 'w1', name: 'Ana Milena', role: 'TITULAR' },
    fortnight: { month: '2026-08', number: 1, from: '2026-08-01', to: '2026-08-15' },
    days: [
      {
        date: '2026-08-02',
        weekday: 'dom',
        holiday: true,
        workedCode: 'DIA',
        grossHours: 10,
        hours: hours({ RDF: 8 }),
        parametersFrom: '2025-11-01'
      },
      {
        date: '2026-08-03',
        weekday: 'lun',
        holiday: true,
        workedCode: null,
        grossHours: 0,
        hours: hours(),
        parametersFrom: '2026-08-03'
      }
    ],
    subtotals: hours({ RDF: 8 }),
    grossHours: 10,
    fixedHedo: { applies: true, hours: 6 },
    adjustments: [],
    adjustmentsNet: hours({ HEDO: -2 }),
    totalToPay: hours({ RDF: 8, HEDO: 4 }),
    incomplete: false,
    noveltiesUnconfirmed: false,
    affectedDates: [],
    parameterVersions: ['2025-11-01', '2026-08-03'],
    ...overrides
  };
}

function makeVersion(overrides: Partial<ParameterVersion> = {}): ParameterVersion {
  return {
    id: 1,
    number: 1,
    validFrom: '2025-11-01',
    validTo: null,
    current: true,
    scheduled: false,
    nightStartHour: 21,
    restHours: 1,
    restMinShiftHours: 8,
    fixedHedoHours: 6,
    fixedHedoRoles: ['TITULAR'],
    reason: 'Inicial',
    createdBy: 'admin',
    createdAt: '2025-11-01T10:00:00Z',
    ...overrides
  };
}

describe('overtime components', () => {
  it('renders OvertimeNoticeComponent open for <=5 dates and folded for >5 dates', async () => {
    await TestBed.configureTestingModule({
      imports: [OvertimeNoticeComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    const fixture = TestBed.createComponent(OvertimeNoticeComponent);
    fixture.componentRef.setInput('month', '2026-08');
    fixture.componentRef.setInput('fortnight', 1);
    fixture.componentRef.setInput('dates', [{ date: '2026-08-05', reason: 'PENDING' }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('1 día con datos incompletos');
    expect((fixture.componentInstance as unknown as { open: () => boolean }).open()).toBe(true);

    fixture.componentRef.setInput('dates', [
      { date: '2026-08-01', reason: 'PENDING' },
      { date: '2026-08-02', reason: 'MISSING_RECORD' },
      { date: '2026-08-03', reason: 'NOVELTIES_NOT_APPLIED' },
      { date: '2026-08-04', reason: 'PENDING' },
      { date: '2026-08-05', reason: 'PENDING' },
      { date: '2026-08-06', reason: 'PENDING' }
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('6 días con datos incompletos');
    expect((fixture.componentInstance as unknown as { open: () => boolean }).open()).toBe(false);
  });

  it('computes mixed-version rows, fixed HEDO line, and negative cells in OvertimeSheetTableComponent', async () => {
    await TestBed.configureTestingModule({
      imports: [OvertimeSheetTableComponent]
    }).compileComponents();

    const fixture = TestBed.createComponent(OvertimeSheetTableComponent);
    fixture.componentRef.setInput('sheet', makeSheet({ totalToPay: hours({ RDF: 8, HEDO: -1 }) }));
    fixture.componentRef.setInput('versionNumbers', { '2025-11-01': 1, '2026-08-03': 2 });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('HEDO fijas de la quincena');
    expect(text).toContain('+6');
    expect(text).toContain('−2');
    expect(text).toContain('v1');
    expect(text).toContain('v2');
    expect(text).toContain('−1');
  });

  it('aggregates column totals and worker statuses in OvertimeSummaryTableComponent', async () => {
    await TestBed.configureTestingModule({
      imports: [OvertimeSummaryTableComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    const rows: OvertimeSummaryRow[] = [
      {
        worker: { id: 'w1', name: 'Ana', role: 'TITULAR' },
        grossHours: 40,
        totalToPay: hours({ RDF: 4, HEDO: 6 }),
        incomplete: false,
        noveltiesUnconfirmed: false
      },
      {
        worker: { id: 'w2', name: 'Carlos', role: 'TITULAR' },
        grossHours: 36,
        totalToPay: hours({ RDF: 2, RNO: 3 }),
        incomplete: true,
        noveltiesUnconfirmed: false
      }
    ];

    const fixture = TestBed.createComponent(OvertimeSummaryTableComponent);
    fixture.componentRef.setInput('rows', rows);
    fixture.componentRef.setInput('month', '2026-08');
    fixture.componentRef.setInput('fortnight', 1);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Ana');
    expect(text).toContain('Completa');
    expect(text).toContain('Incompleta');
    expect(text).toContain('76');
  });

  it('creates and removes adjustments in AdjustmentsPanelComponent and reports server field errors', async () => {
    const sheetService = {
      createAdjustment: vi.fn(() => of({ outcome: 'OK' })),
      deleteAdjustment: vi.fn(() => of({ outcome: 'OK' }))
    };
    const toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [AdjustmentsPanelComponent],
      providers: [
        { provide: OvertimeSheetService, useValue: sheetService },
        { provide: ToastService, useValue: toast }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(AdjustmentsPanelComponent);
    const changed = vi.fn();
    fixture.componentInstance.changed.subscribe(changed);
    fixture.componentRef.setInput('adjustments', [
      {
        id: 10,
        category: 'HEDO',
        direction: 'ADD',
        hours: 2,
        reason: 'Cierre extendido',
        createdAt: '2026-08-05T15:00:00Z',
        createdBy: 'admin'
      }
    ]);
    fixture.componentRef.setInput('workerId', 'w1');
    fixture.componentRef.setInput('month', '2026-08');
    fixture.componentRef.setInput('fortnight', 1);
    fixture.detectChanges();

    const host = fixture.componentInstance as unknown as {
      openForm(): void;
      save(): void;
      askRemove(id: number): void;
      remove(id: number): void;
      form: { controls: { category: { setValue(v: string): void }; hours: { setValue(v: string): void }; reason: { setValue(v: string): void } } };
    };

    host.openForm();
    host.form.controls.category.setValue('RNO');
    host.form.controls.hours.setValue('3');
    host.form.controls.reason.setValue('Cobertura nocturna');
    host.save();

    expect(sheetService.createAdjustment).toHaveBeenCalledWith('w1', '2026-08', 1, {
      category: 'RNO',
      direction: 'ADD',
      hours: 3,
      reason: 'Cobertura nocturna'
    });
    expect(toast.show).toHaveBeenCalledWith('Ajuste registrado');
    expect(changed).toHaveBeenCalledTimes(1);

    host.askRemove(10);
    host.remove(10);
    expect(sheetService.deleteAdjustment).toHaveBeenCalledWith(10);
    expect(toast.show).toHaveBeenCalledWith('Ajuste eliminado');
    expect(changed).toHaveBeenCalledTimes(2);
  });

  it('preloads base version in ParameterVersionDrawerComponent and creates a new version', async () => {
    const paramsService = { create: vi.fn(() => of({ outcome: 'OK' })) };
    const workersService = { listRoles: vi.fn(() => of([{ id: 'r1', name: 'TITULAR' }, { id: 'r2', name: 'APOYO' }])) };
    const toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ParameterVersionDrawerComponent],
      providers: [
        { provide: OvertimeParametersService, useValue: paramsService },
        { provide: WorkersService, useValue: workersService },
        { provide: ToastService, useValue: toast }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(ParameterVersionDrawerComponent);
    const created = vi.fn();
    const closed = vi.fn();
    fixture.componentInstance.created.subscribe(created);
    fixture.componentInstance.closed.subscribe(closed);
    fixture.componentRef.setInput('versions', [makeVersion()]);
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    const host = fixture.componentInstance as unknown as {
      toggleRole(role: string, checked: boolean): void;
      submit(): void;
      form: {
        controls: {
          validFrom: { setValue(v: string): void };
          nightStartHour: { setValue(v: string): void };
          reason: { setValue(v: string): void };
        };
      };
    };

    host.form.controls.validFrom.setValue('2026-12-01');
    host.form.controls.nightStartHour.setValue('19');
    host.form.controls.reason.setValue('Ley 2466');
    host.toggleRole('APOYO', true);
    host.submit();

    expect(paramsService.create).toHaveBeenCalledWith({
      validFrom: '2026-12-01',
      nightStartHour: 19,
      restHours: 1,
      restMinShiftHours: 8,
      fixedHedoHours: 6,
      fixedHedoRoles: ['TITULAR', 'APOYO'],
      reason: 'Ley 2466'
    });
    expect(toast.show).toHaveBeenCalledWith('Versión 2 creada');
    expect(created).toHaveBeenCalledTimes(1);
    expect(closed).toHaveBeenCalledTimes(1);
  });
});
