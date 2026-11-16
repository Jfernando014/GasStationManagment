import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SellerPerformanceService } from './seller-performance.service';
import { AuthService } from './auth.service';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { errorInterceptor } from '../interceptors/error.interceptor';
import { ApiError } from '../../shared/models/api-error.model';
import { emptyPerformanceFixture, performanceFixture, performanceQuery } from '../../shared/helpers/seller-performance.test-data';
import { fullPerformanceFixture } from '../../shared/helpers/performance-report.test-data';

describe('SCRUM-108 SellerPerformanceService', () => {
  let service: SellerPerformanceService, http: HttpTestingController;
  const url = 'http://localhost:8080/api/v1/statistics/seller-performance';
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])), provideHttpClientTesting(),
      { provide: AuthService, useValue: { getToken: () => 'test-token', logout: () => undefined } }
    ] });
    service = TestBed.inject(SellerPerformanceService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('reuses the endpoint and JWT for the full C-09 report without filtering the station population', () => {
    service.readReport(performanceQuery).subscribe(data => expect(data.normalization.sellers[0].normalizedSales).toBe('180'));
    const call = http.expectOne(request => request.url === url);
    expect(call.request.method).toBe('GET'); expect(call.request.responseType).toBe('text');
    expect(call.request.headers.get('Authorization')).toBe('Bearer test-token');
    expect(call.request.params.keys()).toEqual(['from', 'to', 'measure', 'outputScale']);
    call.flush(JSON.stringify(fullPerformanceFixture()));
  });
  it('queries confirmed history with inclusive dates, JWT and exact text responses, without writes', () => {
    service.read(performanceQuery).subscribe(data => expect(data.compliance.sellers[0].percent).toBe('120'));
    const call = http.expectOne(`${url}?from=2026-08-03&to=2026-08-03&measure=FUEL_GALLONS&outputScale=2`);
    expect(call.request.method).toBe('GET');
    expect(call.request.responseType).toBe('text');
    expect(call.request.body).toBeNull();
    expect(call.request.headers.get('Authorization')).toBe('Bearer test-token');
    expect(call.request.params.has('workerId')).toBe(false);
    call.flush(JSON.stringify(performanceFixture()));
  });
  it('supports the actual basket metric and confirmed empty history', () => {
    const query = { ...performanceQuery, measure: 'BASKET_COP' as const };
    service.read(query).subscribe(data => expect(data.compliance.availability).toBe('NO_DATA'));
    http.expectOne(request => request.params.get('measure') === 'BASKET_COP').flush(JSON.stringify(emptyPerformanceFixture(query)));
  });
  it('rejects invalid queries before HTTP and mismatched successful responses', () => {
    expect(() => service.read({ ...performanceQuery, to: '2026-08-02' })).toThrow();
    expect(() => service.read({ ...performanceQuery, outputScale: 13 })).toThrow();
    let failed = false;
    service.read(performanceQuery).subscribe({ error: () => failed = true });
    const data = performanceFixture(); data.compliance.window.from = '2026-08-01';
    http.expectOne(request => request.url === url).flush(JSON.stringify(data));
    expect(failed).toBe(true);
  });
  it.each([0, 400, 401, 403, 404, 502])('preserves HTTP/network error %s without an empty success', status => {
    let failure: ApiError | undefined, emitted = false;
    service.read(performanceQuery).subscribe({ next: () => emitted = true, error: error => failure = error });
    const call = http.expectOne(request => request.url === url);
    if (status === 0) call.error(new ProgressEvent('error'));
    else call.flush('{"detail":"Source failure","code":"PERFORMANCE_SOURCE_FAILURE"}', { status, statusText: 'Error' });
    expect(failure?.status).toBe(status);
    if (status) expect(failure?.code).toBe('PERFORMANCE_SOURCE_FAILURE');
    expect(emitted).toBe(false);
  });
});
