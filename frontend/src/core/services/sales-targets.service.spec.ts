import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SalesTargetsService } from './sales-targets.service';
import { AuthService } from './auth.service';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { errorInterceptor } from '../interceptors/error.interceptor';
import { ApiError } from '../../shared/models/api-error.model';

describe('C-14 SalesTargetsService', () => {
  let service: SalesTargetsService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/api/v1/sales/targets';
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])), provideHttpClientTesting(),
      { provide: AuthService, useValue: { getToken: () => 'test-token' } }
    ] });
    service = TestBed.inject(SalesTargetsService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('sends exact JSON numeric tokens with authentication, without rounding', () => {
    service.create({ year: 2026, category: 'FUEL_AGGREGATE', unit: 'GALLONS',
      value: '999999999999999999.123456789012', maximumSale: '0,000000000001' }).subscribe();
    const request = http.expectOne(url);
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test-token');
    expect(request.request.headers.get('Content-Type')).toBe('application/json');
    expect(request.request.body).toContain('"value":999999999999999999.123456789012');
    expect(request.request.body).toContain('"maximumSale":0.000000000001');
    request.flush('{"id":1,"year":2026,"month":null,"category":"FUEL_AGGREGATE","unit":"GALLONS","value":999999999999999999.123456789012,"maximumSale":1E-12,"createdAt":"2026-10-11T12:00:00Z"}');
  });
  it('queries the year and exact identity, omitting month for annual lookup', () => {
    service.list(2026).subscribe(targets => expect(targets).toEqual([]));
    http.expectOne(`${url}?year=2026`).flush('[]');
    service.findByIdentity(2026, 'BASKET').subscribe();
    const request = http.expectOne(`${url}/by-identity?year=2026&category=BASKET`);
    expect(request.request.params.has('month')).toBe(false);
    request.flush('{"id":2,"year":2026,"month":null,"category":"BASKET","unit":"COP","value":0,"maximumSale":null,"createdAt":"2026-10-11T12:00:00Z"}');
  });
  it('preserves text Problem Details through the existing error interceptor', () => {
    let failure: ApiError | undefined;
    service.list(2026).subscribe({ error: error => { failure = error; } });
    http.expectOne(`${url}?year=2026`).flush('{"detail":"No autorizado","code":"ACCESS_DENIED"}', { status: 403, statusText: 'Forbidden' });
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure?.detail).toBe('No autorizado');
    expect(failure?.status).toBe(403);
  });
  it.each([401, 403, 404])('preserves HTTP %s without substituting errors for an empty list', status => {
    let failure: ApiError | undefined;
    let emitted = false;
    service.list(2026).subscribe({ next: () => { emitted = true; }, error: error => { failure = error; } });
    http.expectOne(`${url}?year=2026`).flush('', { status, statusText: 'Request rejected' });
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure?.status).toBe(status);
    expect(emitted).toBe(false);
  });
  it('preserves a network failure as status zero without emitting invented data', () => {
    let failure: ApiError | undefined;
    let emitted = false;
    service.list(2026).subscribe({ next: () => { emitted = true; }, error: error => { failure = error; } });
    http.expectOne(`${url}?year=2026`).error(new ProgressEvent('error'));
    expect(failure?.status).toBe(0);
    expect(failure?.detail).toBe('No se pudo conectar con el servidor');
    expect(emitted).toBe(false);
  });
});
