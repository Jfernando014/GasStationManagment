import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { GaugeTablesService } from '../../../../core/services/gauge-tables.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { GaugeTableResponse } from '../../../../shared/models/dtos/gauge-table.dto';
import { TankResponse } from '../../../../shared/models/dtos/tank.dto';
import { GaugeTablesPageComponent } from './gauge-tables-page.component';

const TABLE: GaugeTableResponse = {
  id: 4,
  name: 'Tabla T1 - 250 cm',
  points: [{ heightCm: 0, volumeGallons: 0 }, { heightCm: 250, volumeGallons: 5000 }],
  minHeightCm: 0,
  maxHeightCm: 250,
  maxVolumeGallons: 5000,
  tanks: [{ id: 1, code: 'T1', name: 'Tanque motor 11' }]
};

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

describe('GaugeTablesPageComponent', () => {
  let gaugeTablesService: {
    list: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
  let tanksService: {
    list: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    gaugeTablesService = {
      list: vi.fn(() => of([TABLE])),
      create: vi.fn((request: { name: string }) => of({ ...TABLE, id: 5, name: request.name })),
      update: vi.fn((_id: number, request: { name: string }) => of({ ...TABLE, name: request.name })),
      delete: vi.fn(() => of(void 0))
    };
    tanksService = {
      list: vi.fn(() => of([TANK])),
      update: vi.fn((_id: number, request: unknown) => of({ ...TANK, ...request as object })),
      delete: vi.fn(() => of(void 0))
    };

    await TestBed.configureTestingModule({
      imports: [GaugeTablesPageComponent],
      providers: [
        provideRouter([]),
        { provide: GaugeTablesService, useValue: gaugeTablesService },
        { provide: TanksService, useValue: tanksService }
      ]
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(GaugeTablesPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('loads the first gauge table, calibration points, and associated tanks', () => {
    const component = create().componentInstance;

    expect(gaugeTablesService.list).toHaveBeenCalled();
    expect(tanksService.list).toHaveBeenCalled();
    expect(component.selectedGaugeTableId()).toBe(4);
    expect(component.editingTableName()).toBe('Tabla T1 - 250 cm');
    expect(component.selectedCurve).toEqual([{ height: 0, gallons: 0 }, { height: 250, gallons: 5000 }]);
    expect(component.tanksInSelectedTable).toHaveLength(1);
  });

  it('keeps at least two points in the table', () => {
    const component = create().componentInstance;

    component.deletePoint(0);

    expect(component.selectedCurve).toHaveLength(2);
    expect(component.error()).toBe('La tabla de aforo requiere un mínimo de 2 puntos.');
  });

  it('appends a point with an increasing height', () => {
    const component = create().componentInstance;

    component.addPoint();

    expect(component.selectedCurve).toHaveLength(3);
    expect(component.selectedCurve[2]).toEqual({ height: 251, gallons: 5015 });
  });

  it('registers a new gauge table and selects it', () => {
    const component = create().componentInstance;

    component.updateNewGaugeTableName('Tabla T2 - 300 cm');
    component.createGaugeTable();

    expect(gaugeTablesService.create).toHaveBeenCalledWith({
      name: 'Tabla T2 - 300 cm',
      points: [{ heightCm: 0, volumeGallons: 0 }, { heightCm: 1, volumeGallons: 1 }]
    });
    expect(component.gaugeTables()).toHaveLength(2);
    expect(component.selectedGaugeTableId()).toBe(5);
    expect(component.newGaugeTableOpen()).toBe(false);
  });

  it('renames and saves the selected gauge table curve', () => {
    const component = create().componentInstance;

    component.updateEditingTableName('Tabla T1 Renovada');
    component.saveCurve();

    expect(gaugeTablesService.update).toHaveBeenCalledWith(4, {
      name: 'Tabla T1 Renovada',
      points: [{ heightCm: 0, volumeGallons: 0 }, { heightCm: 250, volumeGallons: 5000 }]
    });
    expect(component.gaugeTables()[0].name).toBe('Tabla T1 Renovada');
  });

  it('deletes the selected gauge table and unassigns linked tanks to Ninguna', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.requestDeleteTable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Eliminar tabla de aforo');

    component.confirmDelete();

    expect(gaugeTablesService.delete).toHaveBeenCalledWith(4);
    expect(component.gaugeTables()).toHaveLength(0);
    expect(component.tanks()[0].gaugeTableId).toBeNull();
  });

  it('updates a tank gauge table to Ninguna and edits tank fields directly from gauge tables', () => {
    const component = create().componentInstance;

    component.assignTankGaugeTable(component.tanks()[0], null);
    expect(tanksService.update).toHaveBeenCalledWith(1, expect.objectContaining({ gaugeTableId: null }));
    expect(component.tanks()[0].gaugeTableId).toBeNull();

    component.editTankFromTable(component.tanks()[0]);
    component.updateEditingTank('name', 'Tanque actualizado desde aforo');
    component.saveEditingTank();

    expect(tanksService.update).toHaveBeenLastCalledWith(
      1,
      expect.objectContaining({ name: 'Tanque actualizado desde aforo', gaugeTableId: null })
    );
    expect(component.editingTank()).toBeNull();
  });

  it('does not create a table without a name', () => {
    const component = create().componentInstance;

    component.createGaugeTable();

    expect(gaugeTablesService.create).not.toHaveBeenCalled();
    expect(component.error()).toBe('La tabla debe tener un nombre y al menos dos puntos.');
  });
});
