import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { InventoryHomeService } from './inventory-home.service';

describe('InventoryHomeService', () => {
  let service: InventoryHomeService;
  let http: HttpTestingController;
  const homeUrl = 'http://localhost:8080/api/v1/inventory/home';
  const controlUrl = 'http://localhost:8080/api/v1/inventory/daily-control';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(InventoryHomeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads the inventory summary of the home screen', () => {
    service.getHome().subscribe();

    const call = http.expectOne(homeUrl);
    expect(call.request.method).toBe('GET');
    call.flush({ fuels: [], tanks: [], mostCriticalAlert: null, pendingAlerts: 0 });
  });

  it('reads the control of the day for a journal date', () => {
    service.getDailyControl('2026-11-06').subscribe();

    const call = http.expectOne(request => request.url === controlUrl);
    expect(call.request.method).toBe('GET');
    expect(call.request.params.get('journalDate')).toBe('2026-11-06');
    call.flush({ journalDate: '2026-11-06', fuels: [], products: [] });
  });
});
