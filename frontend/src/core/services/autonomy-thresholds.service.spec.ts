import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AutonomyThresholdsService } from './autonomy-thresholds.service';

describe('AutonomyThresholdsService', () => {
  let service: AutonomyThresholdsService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/api/v1/inventory/autonomy-thresholds';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AutonomyThresholdsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the registered thresholds', () => {
    service.list().subscribe();
    const call = http.expectOne(url);
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('registers a new validity date', () => {
    const body = { validFrom: '2026-09-01', warningDays: 5, criticalDays: 2 };
    service.create(body).subscribe();
    const call = http.expectOne(url);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(body);
    call.flush({ id: 1, ...body, createdAt: '2026-08-30T08:00:00' });
  });
});
