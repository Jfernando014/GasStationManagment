import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AdministrativeExpensesService } from './administrative-expenses.service';

describe('AdministrativeExpensesService', () => {
  let service: AdministrativeExpensesService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8080/api/v1/sales/administrative-expenses';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdministrativeExpensesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('requests expenses filtered by fortnight and by date range', () => {
    service.listByFortnight(2026, 8, 1).subscribe();
    const fortnightReq = httpMock.expectOne(
      `${baseUrl}?year=2026&month=8&fortnight=1`
    );
    expect(fortnightReq.request.method).toBe('GET');
    fortnightReq.flush({ expenses: [], total: 0 });

    service.listByDateRange('2026-08-01', '2026-08-31').subscribe();
    const rangeReq = httpMock.expectOne(`${baseUrl}?fromDate=2026-08-01&toDate=2026-08-31`);
    expect(rangeReq.request.method).toBe('GET');
    rangeReq.flush({ expenses: [], total: 0 });
  });

  it('registers and deletes administrative expenses', () => {
    service.register({
      date: '2026-08-01',
      amount: 18350,
      name: 'Servientrega S.A',
      documentNumber: '860512330-3',
      description: 'Envío documentos Bogotá'
    }).subscribe();

    const postReq = httpMock.expectOne(baseUrl);
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.name).toBe('Servientrega S.A');
    postReq.flush({
      id: 2,
      date: '2026-08-01',
      amount: 18350,
      name: 'Servientrega S.A',
      documentNumber: '860512330-3',
      description: 'Envío documentos Bogotá',
      createdAt: '2026-08-01T10:00:00Z'
    });

    service.delete(2).subscribe();
    const delReq = httpMock.expectOne(`${baseUrl}/2`);
    expect(delReq.request.method).toBe('DELETE');
    delReq.flush(null);
  });
});
