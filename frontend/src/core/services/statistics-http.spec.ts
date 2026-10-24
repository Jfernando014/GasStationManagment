import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SalesStatisticsService } from './sales-statistics.service';
import { CompetitionService } from './competition.service';
import { AuthService } from './auth.service';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { errorInterceptor } from '../interceptors/error.interceptor';
import { averagesFixture, basketFixture, observationRequest } from '../../features/statistics/statistics-test-data';
import { ApiError } from '../../shared/models/api-error.model';

describe('SCRUM-84 HTTP contracts', () => {
  let http: HttpTestingController, sales: SalesStatisticsService, competition: CompetitionService;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])), provideHttpClientTesting(), { provide: AuthService, useValue: { getToken: () => 'test-token' } }] });
    http = TestBed.inject(HttpTestingController); sales = TestBed.inject(SalesStatisticsService); competition = TestBed.inject(CompetitionService);
  });
  afterEach(() => http.verify());
  it('posts exact observation amounts with JWT to the existing SCRUM-40 averages endpoint', () => {
    const request = { ...observationRequest, observations: [{ ...observationRequest.observations[0], value: '999999999999999999.123456789012' }] };
    sales.averages(request).subscribe(); const call = http.expectOne('http://localhost:8080/api/v1/sales/averages/explicit');
    expect(call.request.headers.get('Authorization')).toBe('Bearer test-token'); expect(call.request.body).toContain('"value":999999999999999999.123456789012');
    call.flush(JSON.stringify(averagesFixture()));
  });
  it('reuses scenario enums and the nested sales request', () => {
    sales.compare({ sales: observationRequest, first: { dayOfWeek: 'MONDAY', journey: 'DAY' }, second: { dayOfWeek: 'MONDAY', journey: 'NIGHT' } }).subscribe();
    const call = http.expectOne('http://localhost:8080/api/v1/sales/scenarios/compare/explicit'); expect(call.request.body).toContain('"dayOfWeek":"MONDAY"'); call.flush(JSON.stringify(averagesFixture()));
  });
  it('sends inclusive basket dates and preserves UNAVAILABLE', () => {
    sales.basket('2026-08-01', '2026-08-31').subscribe(data => expect(data.charts.availability).toBe('UNAVAILABLE'));
    const call = http.expectOne('http://localhost:8080/api/v1/sales/statistics/basket?from=2026-08-01&to=2026-08-31&period=DAY&outputScale=2'); call.flush(JSON.stringify(basketFixture()));
  });
  it('rejects invalid basket dates before HTTP and mismatched response periods', () => {
    expect(() => sales.basket('2026-02-30', '2026-03-01')).toThrow();
    let failure: Error | undefined;
    sales.basket('2026-07-01', '2026-07-31').subscribe({ error: error => { failure = error; } });
    http.expectOne('http://localhost:8080/api/v1/sales/statistics/basket?from=2026-07-01&to=2026-07-31&period=DAY&outputScale=2').flush(JSON.stringify(basketFixture()));
    expect(failure?.message).toContain('período');
  });
  it('registers a competitor and an exact price observation using the real public DTO', () => {
    competition.register({ name: 'Estación de prueba', brand: 'Prueba', location: 'Ubicación de prueba' }).subscribe();
    http.expectOne('http://localhost:8080/api/v1/competition/competitors').flush('{"id":1,"name":"Estación de prueba","brand":"Prueba","location":"Ubicación de prueba","active":true}');
    competition.observe({ competitorId: '9007199254740993', fuelType: 'MOTOR', pricePerGallon: '16330.25', observedAt: '2026-08-01T12:00:00Z' }).subscribe();
    const call = http.expectOne('http://localhost:8080/api/v1/competition/observations'); expect(call.request.body).toContain('"competitorId":9007199254740993');
    expect(call.request.body).toContain('"pricePerGallon":16330.25');
    call.flush('{"id":1,"competitorId":9007199254740993,"fuelType":"MOTOR","pricePerGallon":16330.25,"observedAt":"2026-08-01T12:00:00Z","createdAt":"2026-08-01T12:00:00Z"}');
  });
  it.each([null, '0'])('preserves optional simulation cost %s without treating price difference as margin', cost => {
    competition.simulate({ fuelType: 'DIESEL', proposedPrice: '100', acquisitionCost: cost }).subscribe(data => {
      expect(data.versusAverage.absoluteCopPerGallon).toBe('-10');
      expect(data.acquisitionCost).toBe(cost);
      expect(data.estimatedMargin.grossCopPerGallon).toBe(cost === null ? null : '100');
      expect(data.estimatedMargin.grossMarginPercentage).toBe(cost === null ? null : '100');
    });
    const call = http.expectOne('http://localhost:8080/api/v1/competition/simulation'); expect(call.request.body).toContain('"acquisitionCost":' + (cost ?? 'null'));
    call.flush(JSON.stringify({ asOf: '2026-08-01T12:00:00Z', fuelType: 'DIESEL', unit: 'COP_PER_GALLON', proposedPrice: 100,
      acquisitionCost: cost === null ? null : 0, competition: { competitorCount: 1, average: 110, minimum: 110 },
      versusAverage: { absoluteCopPerGallon: -10, percentage: -9.090909 }, versusMinimum: { absoluteCopPerGallon: -10, percentage: -9.090909 },
      estimatedMargin: { grossCopPerGallon: cost === null ? null : 100, grossMarginPercentage: cost === null ? null : 100 }, observations: [] }));
  });
  it.each([401, 403, 404, 500])('preserves HTTP %s instead of converting it to an empty market', status => {
    let error: ApiError | undefined;
    competition.compare().subscribe({ error: failure => { error = failure; } });
    http.expectOne('http://localhost:8080/api/v1/competition/comparison').flush('{"detail":"Consulta no disponible"}', { status, statusText: 'Error' }); expect(error?.status).toBe(status);
  });
});
