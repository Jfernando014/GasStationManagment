import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { InventoryAlertsService } from './inventory-alerts.service';

describe('InventoryAlertsService', () => {
  let service: InventoryAlertsService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/api/v1/inventory/alerts';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(InventoryAlertsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the open alerts when no status is given', () => {
    service.list().subscribe();

    const call = http.expectOne(url);
    expect(call.request.method).toBe('GET');
    expect(call.request.params.has('status')).toBe(false);
    call.flush([]);
  });

  it('sends the status filter when one is given', () => {
    service.list('RESOLVED').subscribe();

    const call = http.expectOne(request => request.url === url && request.params.get('status') === 'RESOLVED');
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('reads the pending counter and publishes it', () => {
    let pending = -1;
    service.pending$.subscribe(value => (pending = value));

    service.refreshPending();
    const call = http.expectOne(`${url}/count`);
    expect(call.request.method).toBe('GET');
    call.flush({ pending: 3 });

    expect(pending).toBe(3);
  });

  it('marks an alert as read with a PUT without a body', () => {
    service.markRead(9).subscribe();

    const call = http.expectOne(`${url}/9/read`);
    expect(call.request.method).toBe('PUT');
    call.flush({});
  });

  it('attends an alert with the optional note', () => {
    service.attend(9, { note: 'Reabastecimiento programado' }).subscribe();

    const call = http.expectOne(`${url}/9/attend`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual({ note: 'Reabastecimiento programado' });
    call.flush({});
  });

  it('reuses a pending value already calculated by another screen', () => {
    let pending = -1;
    service.pending$.subscribe(value => (pending = value));

    service.setPending(5);

    expect(pending).toBe(5);
  });
});
