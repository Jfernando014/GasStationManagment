import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TankMeasurementsService } from './tank-measurements.service';

describe('TankMeasurementsService', () => {
  let service: TankMeasurementsService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/api/v1/inventory/measurements';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(TankMeasurementsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('saves the readings of the day with one PUT', () => {
    const readings = [
      { tankId: 1, heightCm: 120.5 },
      { tankId: 2, heightCm: 98 }
    ];
    service.save('2026-08-05', { readings }).subscribe();

    const call = http.expectOne(`${url}/2026-08-05`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual({ readings });
    call.flush({ id: 1, journalDate: '2026-08-05', measuredAt: '2026-08-05T08:00:00', readings: [] });
  });

  it('reads the previous measurement of the date', () => {
    service.findPrevious('2026-08-05').subscribe();
    http.expectOne(`${url}/previous?journalDate=2026-08-05`).flush({
      id: 2,
      journalDate: '2026-08-04',
      measuredAt: '2026-08-04T18:00:00',
      readings: []
    });
  });

  it('calculates the gallons of one height in one tank', () => {
    service.volume(3, 155.4).subscribe();
    const call = http.expectOne('http://localhost:8080/api/v1/inventory/tanks/3/volume?heightCm=155.4');
    expect(call.request.method).toBe('GET');
    call.flush({ tankId: 3, heightCm: 155.4, volumeGallons: 3970 });
  });
});
