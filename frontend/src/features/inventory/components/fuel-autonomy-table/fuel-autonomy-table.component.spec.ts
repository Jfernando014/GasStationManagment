import { TestBed } from '@angular/core/testing';

import { FuelAutonomyResponse } from '../../../../shared/models/dtos/fuel-autonomy.dto';
import { FuelAutonomyTableComponent } from './fuel-autonomy-table.component';

const AUTONOMY: FuelAutonomyResponse[] = [
  {
    fuelType: 'DIESEL',
    status: 'CALCULATED',
    stockGallons: 400,
    expectedDailySalesGallons: 200,
    autonomyDays: 2,
    level: 'CRITICAL',
    warningDays: 4,
    criticalDays: 2,
    tanks: []
  },
  {
    fuelType: 'EXTRA',
    status: 'INCOMPLETE_MEASUREMENT',
    stockGallons: 0,
    expectedDailySalesGallons: 0,
    autonomyDays: null,
    level: 'NOT_EVALUATED',
    warningDays: 4,
    criticalDays: 2,
    tanks: []
  }
];

describe('FuelAutonomyTableComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [FuelAutonomyTableComponent] }).compileComponents();
  });

  it('shows the level of every fuel type and the configured thresholds', () => {
    const fixture = TestBed.createComponent(FuelAutonomyTableComponent);
    fixture.componentRef.setInput('autonomy', AUTONOMY);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Crítico');
    expect(text).toContain('Falta medición');
    expect(text).toContain('Advertencia ≤ 4,0 días');
  });

  it('tones the autonomy levels with the shared chips', () => {
    const component = TestBed.createComponent(FuelAutonomyTableComponent).componentInstance;

    expect(component.autonomyTone(AUTONOMY[0])).toBe('jr-chip--falta');
    expect(component.autonomyTone(AUTONOMY[1])).toBe('jr-chip--pendiente');
  });
});
