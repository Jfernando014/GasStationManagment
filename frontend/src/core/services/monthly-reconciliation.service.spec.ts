import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MonthlyReconciliationService } from './monthly-reconciliation.service';

describe('MonthlyReconciliationService', () => {
  let service: MonthlyReconciliationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(MonthlyReconciliationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the selected month from the monthly sales reconciliation endpoint', () => {
    let response: unknown;
    service.getMonthly('2026-08').subscribe(value => response = value);

    const request = http.expectOne('http://localhost:8080/api/v1/sales/reconciliation/monthly/2026-08');
    expect(request.request.method).toBe('GET');
    request.flush({ month: '2026-08' });

    expect(response).toEqual({ month: '2026-08' });
  });

  it('encodes reserved characters in the month path parameter', () => {
    service.getMonthly('2026/08').subscribe();

    const request = http.expectOne('http://localhost:8080/api/v1/sales/reconciliation/monthly/2026%2F08');
    expect(request.request.method).toBe('GET');
    request.flush({});
  });
});
