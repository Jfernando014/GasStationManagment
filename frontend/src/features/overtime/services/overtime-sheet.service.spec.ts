import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { errorInterceptor } from '../../../core/interceptors/error.interceptor';
import { SheetDto, SummaryDto } from '../models/overtime.dto';
import { OvertimeSheetService } from './overtime-sheet.service';

const ZERO = { rdf: 0, rno: 0, rnf: 0, hedo: 0, heno: 0, hedf: 0, henf: 0 };

describe('OvertimeSheetService', () => {
  let service: OvertimeSheetService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(OvertimeSheetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('requests fortnight summary with month and fortnight query params', () => {
    const dto: SummaryDto = {
      fortnight: { month: '2026-08', fortnight: 1, from: '2026-08-01', to: '2026-08-15' },
      workers: [],
      incomplete: false,
      noveltiesUnconfirmed: false,
      affectedDates: []
    };

    let response: unknown;
    service.getSummary('2026-08', 1).subscribe(result => (response = result));

    const req = httpMock.expectOne(
      r => r.method === 'GET' && r.url.endsWith('/api/v1/workers/overtime-sheet')
        && r.params.get('month') === '2026-08' && r.params.get('fortnight') === '1'
    );
    req.flush(dto);
    expect(response).toEqual({ outcome: 'OK', data: { fortnight: { month: '2026-08', number: 1, from: '2026-08-01', to: '2026-08-15' }, workers: [], affectedDates: [] } });
  });

  it('loads a worker sheet and treats malformed id (400) as NOT_FOUND', () => {
    const dto: SheetDto = {
      worker: { workerId: 'abc-1', workerName: 'Ana', role: 'TITULAR' },
      fortnight: { month: '2026-08', fortnight: 2, from: '2026-08-16', to: '2026-08-31' },
      days: [],
      subtotals: ZERO,
      grossHours: 0,
      fixedHedo: { applies: false, hours: 0 },
      adjustments: [],
      adjustmentsNet: ZERO,
      totalToPay: ZERO,
      incomplete: false,
      noveltiesUnconfirmed: false,
      affectedDates: [],
      parameterVersions: []
    };

    let okResult: unknown;
    service.getSheet('abc-1', '2026-08', 2).subscribe(result => (okResult = result));
    const okReq = httpMock.expectOne(
      r => r.method === 'GET' && r.url.endsWith('/api/v1/workers/abc-1/overtime-sheet')
        && r.params.get('month') === '2026-08' && r.params.get('fortnight') === '2'
    );
    okReq.flush(dto);
    expect(okResult).toMatchObject({ outcome: 'OK', data: { worker: { id: 'abc-1', name: 'Ana' } } });

    let badResult: unknown;
    service.getSheet('bad id', '2026-08', 2).subscribe(result => (badResult = result));
    httpMock.expectOne(r => r.method === 'GET' && r.url.endsWith('/api/v1/workers/bad%20id/overtime-sheet'))
      .flush({ detail: 'ID inválido' }, { status: 400, statusText: 'Bad Request' });
    expect(badResult).toEqual({ outcome: 'NOT_FOUND', message: 'ID inválido' });
  });

  it('creates and deletes adjustments while inferring validation field from backend detail', () => {
    let created: unknown;
    service.createAdjustment('w1', '2026-08', 1, {
      category: 'HEDO',
      direction: 'ADD',
      hours: 2,
      reason: '  Turno adicional  '
    }).subscribe(result => (created = result));

    const createReq = httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/api/v1/workers/w1/overtime-adjustments'));
    expect(createReq.request.body).toEqual({
      month: '2026-08',
      fortnight: 1,
      category: 'HEDO',
      direction: 'ADD',
      hours: 2,
      reason: 'Turno adicional'
    });
    createReq.flush({}, { status: 201, statusText: 'Created' });
    expect(created).toEqual({ outcome: 'OK' });

    let hoursError: unknown;
    service.createAdjustment('w1', '2026-08', 1, {
      category: 'HEDO',
      direction: 'ADD',
      hours: 0,
      reason: 'Motivo válido'
    }).subscribe(result => (hoursError = result));
    httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/api/v1/workers/w1/overtime-adjustments'))
      .flush({ detail: 'Las horas deben ser positivas' }, { status: 400, statusText: 'Bad Request' });
    expect(hoursError).toEqual({ outcome: 'ERROR', message: 'Las horas deben ser positivas', field: 'hours' });

    let deleteResult: unknown;
    service.deleteAdjustment(77).subscribe(result => (deleteResult = result));
    const delReq = httpMock.expectOne(r => r.method === 'DELETE' && r.url.endsWith('/api/v1/workers/overtime-adjustments/77'));
    delReq.flush(null, { status: 204, statusText: 'No Content' });
    expect(deleteResult).toEqual({ outcome: 'OK' });
  });
});
