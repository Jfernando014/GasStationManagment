import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { FuelReceptionsService } from '../../../../core/services/fuel-receptions.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { ApiError } from '../../../../shared/models/api-error.model';
import { FuelReceptionResponse } from '../../../../shared/models/dtos/fuel-reception.dto';
import { TankResponse } from '../../../../shared/models/dtos/tank.dto';
import { ReceptionsPageComponent } from './receptions-page.component';

const TANKS: TankResponse[] = [
  {
    id: 1,
    code: 'T1',
    name: 'Tanque 1',
    fuelType: 'MOTOR',
    maxCapacityGallons: 5000,
    maxHeightCm: 200,
    toleranceCm: 1,
    gaugeTableId: 1
  }
];

const RECEPTIONS: FuelReceptionResponse[] = [
  {
    id: 2,
    tankId: 1,
    receptionDate: '2026-08-04',
    gallons: 1000,
    supplier: 'Terpel',
    createdAt: '2026-08-04T08:00:00',
    updatedAt: '2026-08-04T08:00:00'
  },
  {
    id: 1,
    tankId: 1,
    receptionDate: '2026-08-05',
    gallons: 500,
    supplier: 'Biomax',
    createdAt: '2026-08-05T08:00:00',
    updatedAt: '2026-08-05T08:00:00'
  }
];

describe('ReceptionsPageComponent', () => {
  let receptionsService: { list: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn> };
  let tanksService: { list: ReturnType<typeof vi.fn> };

  function create() {
    const fixture = TestBed.createComponent(ReceptionsPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    receptionsService = {
      list: vi.fn(() => of(RECEPTIONS)),
      create: vi.fn()
    };
    tanksService = { list: vi.fn(() => of(TANKS)) };

    await TestBed.configureTestingModule({
      imports: [ReceptionsPageComponent],
      providers: [
        { provide: FuelReceptionsService, useValue: receptionsService },
        { provide: TanksService, useValue: tanksService }
      ]
    }).compileComponents();
  });

  it('loads the history sorted from newest to oldest and sums the gallons', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    expect(component.receptions().map(reception => reception.id)).toEqual([1, 2]);
    expect(component.totalGallons()).toBe(1500);
    expect(component.tankName(1)).toBe('Tanque 1 (T1 · Gasolina Corriente)');
    expect(fixture.nativeElement.textContent).toContain('Biomax');
  });

  it('asks the backend for the selected tank and dates', () => {
    const component = create().componentInstance;

    component.onTankFilter('1');
    expect(receptionsService.list).toHaveBeenLastCalledWith({ tankId: 1 });

    component.onDateFilter('from', '2026-08-01');
    expect(receptionsService.list).toHaveBeenLastCalledWith({ tankId: 1, from: '2026-08-01' });

    component.clearFilters();
    expect(receptionsService.list).toHaveBeenLastCalledWith({});
  });

  it('does not send the form when a field is missing', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.save();
    fixture.detectChanges();

    expect(receptionsService.create).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Completa el tanque, la fecha, los galones y el proveedor.');
    expect(fixture.nativeElement.textContent).toContain('Selecciona el tanque que recibe el combustible');
  });

  it('registers the reception and keeps the tank and date for the next one', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    const created: FuelReceptionResponse = {
      id: 3,
      tankId: 1,
      receptionDate: '2026-08-06',
      gallons: 800,
      supplier: 'Terpel',
      createdAt: '2026-08-06T08:00:00',
      updatedAt: '2026-08-06T08:00:00'
    };
    receptionsService.create.mockReturnValue(of(created));

    component.draft.set({ tankId: 1, receptionDate: '2026-08-06', gallons: 800, supplier: 'Terpel' });
    component.save();

    expect(receptionsService.create).toHaveBeenCalledWith({
      tankId: 1,
      receptionDate: '2026-08-06',
      gallons: 800,
      supplier: 'Terpel'
    });
    expect(component.receptions()[0]).toEqual(created);
    expect(component.draft().tankId).toBe(1);
    expect(component.draft().gallons).toBeNull();
    expect(component.draft().supplier).toBe('');
  });

  it('shows the backend field error next to the supplier', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    receptionsService.create.mockReturnValue(
      throwError(() => new ApiError(400, {
        message: 'Invalid data',
        fields: [{ field: 'supplier', message: 'The supplier is required' }]
      }))
    );

    component.draft.set({ tankId: 1, receptionDate: '2026-08-06', gallons: 800, supplier: 'Terpel' });
    component.save();
    fixture.detectChanges();

    expect(component.fieldError('supplier')).toBe('The supplier is required');
    expect(fixture.nativeElement.textContent).toContain('The supplier is required');
  });
});
