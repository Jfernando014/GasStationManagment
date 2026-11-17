import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { WorkerPerformanceService } from './worker-performance.service';
import { AuthService } from './auth.service';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { errorInterceptor } from '../interceptors/error.interceptor';
import { ApiError } from '../../shared/models/api-error.model';
import { WorkerPerformance } from '../../shared/models/dtos/worker-profile.dto';

describe('WorkerPerformanceService', () => {
  let service: WorkerPerformanceService;
  let http: HttpTestingController;

  const baseUrl = 'http://localhost:8080/api/v1/statistics/workers';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getToken: () => 'test-token', logout: () => undefined } }
      ]
    });
    service = TestBed.inject(WorkerPerformanceService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('requests real worker performance with JWT from statistics endpoint', () => {
    const expected: WorkerPerformance = {
      periodFrom: '2026-08-03',
      periodTo: '2026-08-10',
      closedShifts: { total: 2, asHolder: 1, asSupport: 1 },
      byWeekday: [
        {
          weekday: 'MONDAY',
          asHolder: { shifts: 1, averageGallons: 120, performancePercent: 120 },
          asSupport: { shifts: 0, averageGallons: null, performancePercent: null }
        },
        { weekday: 'TUESDAY', asHolder: { shifts: 0, averageGallons: null, performancePercent: null }, asSupport: { shifts: 0, averageGallons: null, performancePercent: null } },
        { weekday: 'WEDNESDAY', asHolder: { shifts: 0, averageGallons: null, performancePercent: null }, asSupport: { shifts: 0, averageGallons: null, performancePercent: null } },
        { weekday: 'THURSDAY', asHolder: { shifts: 0, averageGallons: null, performancePercent: null }, asSupport: { shifts: 0, averageGallons: null, performancePercent: null } },
        { weekday: 'FRIDAY', asHolder: { shifts: 0, averageGallons: null, performancePercent: null }, asSupport: { shifts: 0, averageGallons: null, performancePercent: null } },
        { weekday: 'SATURDAY', asHolder: { shifts: 0, averageGallons: null, performancePercent: null }, asSupport: { shifts: 0, averageGallons: null, performancePercent: null } },
        { weekday: 'SUNDAY', asHolder: { shifts: 0, averageGallons: null, performancePercent: null }, asSupport: { shifts: 0, averageGallons: null, performancePercent: null } },
        {
          weekday: 'HOLIDAY',
          asHolder: { shifts: 0, averageGallons: null, performancePercent: null },
          asSupport: { shifts: 1, averageGallons: 95.5, performancePercent: 100 }
        }
      ],
      maxSale: { gallons: 120, date: '2026-08-03', shiftCode: 'DIA' }
    };

    let actual: WorkerPerformance | undefined;
    service.getPerformance('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa').subscribe(response => {
      actual = response;
    });

    const req = http.expectOne(`${baseUrl}/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa/performance`);
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush(expected);

    expect(actual).toEqual(expected);
  });

  it('encodes workerId in the request URL', () => {
    service.getPerformance('id/with spaces').subscribe();

    const req = http.expectOne(`${baseUrl}/id%2Fwith%20spaces/performance`);
    expect(req.request.method).toBe('GET');
    req.flush({
      periodFrom: null,
      periodTo: null,
      closedShifts: { total: 0, asHolder: 0, asSupport: 0 },
      byWeekday: [],
      maxSale: null
    });
  });

  it('propagates HTTP failures instead of falling back to placeholder data', () => {
    let error: ApiError | undefined;
    service.getPerformance('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa').subscribe({
      error: err => {
        error = err;
      }
    });

    const req = http.expectOne(`${baseUrl}/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa/performance`);
    req.flush({ detail: 'Source failure', code: 'PERFORMANCE_SOURCE_FAILURE' }, { status: 502, statusText: 'Bad Gateway' });

    expect(error?.status).toBe(502);
    expect(error?.code).toBe('PERFORMANCE_SOURCE_FAILURE');
  });
});
