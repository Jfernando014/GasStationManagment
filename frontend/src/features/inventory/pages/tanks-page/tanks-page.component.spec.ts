import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { GaugeTablesService } from '../../../../core/services/gauge-tables.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { ApiError } from '../../../../shared/models/api-error.model';
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
    delete: ReturnType<typeof vi.fn>;
  };
  let gaugeTablesService: { list: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    tanksService = {
      list: vi.fn(() => of([TANK])),
      create: vi.fn((request: unknown) => of({ ...TANK, id: 2, ...request as object })),
      update: vi.fn((_id: number, request: unknown) => of({ ...TANK, ...request as object })),
      delete: vi.fn(() => of(void 0))
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

  it('lists the installed tanks with the name of their gauge table or Ninguna', () => {
    tanksService.list.mockReturnValue(of([TANK, { ...TANK, id: 2, code: 'T2', name: 'Tanque sin tabla', gaugeTableId: null }]));
    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(tanksService.list).toHaveBeenCalled();
    expect(gaugeTablesService.list).toHaveBeenCalled();
    expect(text).toContain('Tanque motor 11');
    expect(text).toContain('Tabla T1 - 250 cm');
    expect(text).toContain('Ninguna');
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

  it('discards unsaved draft changes when canceling edit', () => {
    const component = create().componentInstance;

    component.selectTank('1');
    component.updateTank('1', 'name', 'Temporal');
    component.cancelEdit();

    expect(component.selectedTankId()).toBe('');
    expect(component.tanks()[0].name).toBe('Tanque motor 11');
  });

  it('creates a new tank with Ninguna when no gauge table is selected', () => {
    const component = create().componentInstance;

    component.updateNewTank('code', 'T9');
    component.updateNewTank('name', 'Tanque prueba');
    component.updateNewTank('fuel', 'Diésel ACPM');
    component.updateNewTank('capacity', 2000);
    component.updateNewTank('maxHeight', 200);
    component.updateNewTank('tolerance', 1);
    component.addTank();

    expect(tanksService.create).toHaveBeenCalledWith({
      code: 'T9',
      name: 'Tanque prueba',
      fuelType: 'DIESEL',
      maxCapacityGallons: 2000,
      maxHeightCm: 200,
      toleranceCm: 1,
      gaugeTableId: null
    });
    expect(component.tanks()).toHaveLength(2);
  });

  it('deletes a tank after confirming in the modal and handles 409 conflict', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.requestDeleteTank(component.tanks()[0]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Eliminar tanque');

    component.confirmDeleteTank();
    expect(tanksService.delete).toHaveBeenCalledWith(1);
    expect(component.tanks()).toHaveLength(0);

    tanksService.delete.mockReturnValue(throwError(() => new ApiError(409, { detail: 'Conflict' })));
    component.tanks.set([{ ...component.selectedTank, id: '1', backendId: 1, name: 'Tanque motor 11', code: 'T1' }]);
    component.requestDeleteTank(component.tanks()[0]);
    component.confirmDeleteTank();
    expect(component.error()).toContain('mediciones o recepciones registradas');
  });

  it('shows a non-blocking link hint when there are no gauge tables yet', () => {
    gaugeTablesService.list.mockReturnValue(of([]));

    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Aún no hay tablas de aforo registradas');
    expect(text).toContain('Tabla de aforo');
  });
});
