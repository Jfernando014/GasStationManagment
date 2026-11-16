import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { errorInterceptor } from '../../../core/interceptors/error.interceptor';
import { DayHoursDto } from '../models/worker-hours.dto';
import { WorkerHoursService } from './worker-hours.service';

describe('WorkerHoursService', () => {
  let service: WorkerHoursService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(WorkerHoursService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads day hours and translates novelty texts', () => {
    const dto: DayHoursDto = {
      sync: { date: '2026-08-05', status: 'PROCESSED', noveltiesApplied: true, attempts: 1, lastError: null },
      workers: [
        {
          workerId: 'w1',
          workerName: 'Mónica',
          workedCode: 'DIA',
          gridCode: 'DESCANSO',
          totalHours: 12,
          absent: false,
          noveltyType: 'REPLACEMENT',
          replacedWorkerName: 'Ana',
          reason: 'Permiso'
        },
        {
          workerId: 'w2',
          workerName: 'Ana',
          workedCode: null,
          gridCode: 'DIA',
          totalHours: 0,
          absent: true,
          noveltyType: 'ABSENCE',
          replacedWorkerName: null,
          reason: 'Cita'
        }
      ]
    };

    let result: unknown;
    service.getDay('2026-08-05').subscribe(response => (result = response));

    httpMock.expectOne(r => r.method === 'GET' && r.url.endsWith('/api/v1/workers/hours/2026-08-05')).flush(dto);
    expect(result).toEqual({
      outcome: 'OK',
      data: {
        date: '2026-08-05',
        status: 'PROCESSED',
        noveltiesApplied: true,
        attempts: 1,
        workers: [
          { workerId: 'w1', name: 'Mónica', workedCode: 'DIA', gridCode: 'DESCANSO', hours: 12, absent: false, novelty: 'Reemplazo de Ana' },
          { workerId: 'w2', name: 'Ana', workedCode: null, gridCode: 'DIA', hours: 0, absent: true, novelty: 'Ausencia' }
        ]
      }
    });

    let missing: unknown;
    service.getDay('2026-08-99').subscribe(response => (missing = response));
    httpMock.expectOne(r => r.method === 'GET' && r.url.endsWith('/api/v1/workers/hours/2026-08-99'))
      .flush({ detail: 'Sin registro' }, { status: 404, statusText: 'Not Found' });
    expect(missing).toEqual({ outcome: 'NOT_FOUND', message: 'Sin registro' });
  });

  it('reprocesses a day and maps known error codes and 403', () => {
    let ok: unknown;
    service.reprocess('2026-08-05').subscribe(response => (ok = response));
    httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/api/v1/workers/hours/2026-08-05/reprocess'))
      .flush({ sync: { date: '2026-08-05', status: 'PROCESSED', noveltiesApplied: true, attempts: 2, lastError: null }, workers: [] });
    expect(ok).toEqual({ outcome: 'PROCESSED' });

    let knownCode: unknown;
    service.reprocess('2026-08-05').subscribe(response => (knownCode = response));
    httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/api/v1/workers/hours/2026-08-05/reprocess'))
      .flush({ detail: 'Raw', code: 'DAY_HOURS_NOT_REGISTERED' }, { status: 404, statusText: 'Not Found' });
    expect(knownCode).toEqual({ outcome: 'ERROR', message: 'Ese día no tiene registro de horas' });

    let forbidden: unknown;
    service.reprocess('2026-08-05').subscribe(response => (forbidden = response));
    httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/api/v1/workers/hours/2026-08-05/reprocess'))
      .flush({ detail: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
    expect(forbidden).toEqual({ outcome: 'ERROR', message: 'No tienes permiso para reprocesar' });
  });
});
