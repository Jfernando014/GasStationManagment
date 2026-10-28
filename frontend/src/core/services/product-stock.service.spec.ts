import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ProductStockService } from './product-stock.service';

describe('ProductStockService', () => {
  let service: ProductStockService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/api/v1/inventory/product-stock';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ProductStockService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('asks for the stock of a journal date, with an optional category', () => {
    service.listByJournalDate('2026-08-05').subscribe();
    http.expectOne(`${url}?journalDate=2026-08-05`).flush([]);

    service.listByJournalDate('2026-08-05', 'ADITIVO').subscribe();
    http.expectOne(`${url}?journalDate=2026-08-05&category=ADITIVO`).flush([]);
  });

  it('registers the opening stock of a product', () => {
    const body = { productId: 4, officeQuantity: 12, counterQuantity: 5, journalDate: '2026-08-05' };
    service.registerInitial(body).subscribe();
    const call = http.expectOne(`${url}/initial`);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(body);
    call.flush([]);
  });

  it('moves units from the office to the counter', () => {
    const body = { productId: 4, quantity: 3, journalDate: '2026-08-05' };
    service.registerTransfer(body).subscribe();
    const call = http.expectOne(`${url}/transfers`);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(body);
    call.flush([]);
  });

  it('synchronizes the sales of step 2 and exposes the unmatched codes', () => {
    let unmatched: string[] = [];
    service.syncSales('2026-08-05').subscribe(result => (unmatched = result.unmatchedCodes));

    const call = http.expectOne(`${url}/sales/2026-08-05`);
    expect(call.request.method).toBe('PUT');
    call.flush({ journalDate: '2026-08-05', syncedProducts: 3, unmatchedCodes: ['99999'] });

    expect(unmatched).toEqual(['99999']);
  });
});
