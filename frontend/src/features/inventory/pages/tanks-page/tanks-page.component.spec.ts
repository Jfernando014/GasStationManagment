import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { GaugeTablesService } from '../../../../core/services/gauge-tables.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { GaugeTableResponse } from '../../../../shared/models/dtos/gauge-table.dto';
import { TankResponse } from '../../../../shared/models/dtos/tank.dto';
import { TanksPageComponent } from './tanks-page.component';

const TABLES: GaugeTableResponse[] = [
  {
    id: 4,
    name: 'Tabla T1 - 250 cm',
    points: [{ heightCm: 0, volumeGallons: 0 }, { heightCm: 250, volumeGallons: 5000 }],
    minHeightCm: 0,
    maxHeightCm: 250,
    maxVolumeGallons: 5000,
    tanks: []
  }
];

const TANK: TankResponse = {
  id: 1,
  code: 'T1',
  name: 'Tanque motor 11',
  fuelType: 'MOTOR',
  maxCapacityGallons: 5000,
  maxHeightCm: 250,
  toleranceCm: 1,
  gaugeTableId: 4
};

describe('TanksPageComponent', () => {
  let tanksService: {
    list: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let gaugeTablesService: { list: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    tanksService = {
      list: vi.fn(() => of([TANK])),
      create: vi.fn(),
      update: vi.fn((_id: number, request: unknown) => of({ ...TANK, ...request as object }))
    };
    gaugeTablesService = { list: vi.fn(() => of(TABLES)) };

    await TestBed.configureTestingModule({
      imports: [TanksPageComponent],
      providers: [
        provideRouter([]),
        { provide: TanksService, useValue: tanksService },
        { provide: GaugeTablesService, useValue: gaugeTablesService }
      ]
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(TanksPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('lists the installed tanks with the name of their gauge table', () => {
    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(tanksService.list).toHaveBeenCalled();
    expect(gaugeTablesService.list).toHaveBeenCalled();
    expect(text).toContain('Tanque motor 11');
    expect(text).toContain('Tabla T1 - 250 cm');
  });

  it('closes the editor after saving so the fields become read only', () => {
    const component = create().componentInstance;

    component.selectTank('1');
    component.updateTank('1', 'name', 'Tanque editado');
    component.saveTank(component.selectedTank);

    expect(tanksService.update).toHaveBeenCalledWith(1, {
      code: 'T1',
      name: 'Tanque editado',
      fuelType: 'MOTOR',
      maxCapacityGallons: 5000,
      maxHeightCm: 250,
      toleranceCm: 1,
      gaugeTableId: 4
    });
    expect(component.selectedTankId()).toBe('');
    expect(component.tanks()[0].name).toBe('Tanque editado');
  });

  it('shows the gauge table link when there are no tables yet', () => {
    gaugeTablesService.list.mockReturnValue(of([]));

    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Primero registra una tabla de aforo');
  });
});
