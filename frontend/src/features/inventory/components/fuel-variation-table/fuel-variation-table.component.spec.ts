import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { FuelVariationResponse } from '../../../../shared/models/dtos/fuel-variation.dto';
import { FuelVariationTableComponent } from './fuel-variation-table.component';

const VARIATIONS: FuelVariationResponse[] = [
  {
    fuelType: 'MOTOR',
    status: 'OK',
    tankIds: [1, 5],
    initialGallons: 3000,
    entriesGallons: 0,
    soldGallons: 500,
    theoreticalGallons: 2500,
    measuredGallons: 2490,
    varianceGallons: -10,
    toleranceGallons: 30,
    outOfTolerance: false
  },
  {
    fuelType: 'EXTRA',
    status: 'NO_PREVIOUS_MEASUREMENT',
    tankIds: [2],
    initialGallons: null,
    entriesGallons: 0,
    soldGallons: null,
    theoreticalGallons: null,
    measuredGallons: 1200,
    varianceGallons: null,
    toleranceGallons: null,
    outOfTolerance: false
  }
];

describe('FuelVariationTableComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FuelVariationTableComponent],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('shows the status of every fuel type', () => {
    const fixture = TestBed.createComponent(FuelVariationTableComponent);
    fixture.componentRef.setInput('variations', VARIATIONS);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Gasolina Corriente');
    expect(text).toContain('Cuadra');
    expect(text).toContain('Sin medición anterior');
  });

  it('marks an out of tolerance variation as an error', () => {
    const component = TestBed.createComponent(FuelVariationTableComponent).componentInstance;

    expect(component.variationTone({ ...VARIATIONS[0], outOfTolerance: true })).toBe('jr-chip--falta');
    expect(component.variationLabel({ ...VARIATIONS[0], outOfTolerance: true })).toBe('Fuera de tolerancia');
  });
});
