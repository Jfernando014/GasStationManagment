import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { GaugeTablesService } from '../../../../core/services/gauge-tables.service';
import { GaugeTableResponse } from '../../../../shared/models/dtos/gauge-table.dto';
import { GaugeTablesPageComponent } from './gauge-tables-page.component';

const TABLE: GaugeTableResponse = {
  id: 4,
  name: 'Tabla T1 - 250 cm',
  points: [{ heightCm: 0, volumeGallons: 0 }, { heightCm: 250, volumeGallons: 5000 }],
  minHeightCm: 0,
  maxHeightCm: 250,
  maxVolumeGallons: 5000,
  tanks: []
};

describe('GaugeTablesPageComponent', () => {
  let gaugeTablesService: {
    list: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    gaugeTablesService = {
      list: vi.fn(() => of([TABLE])),
      create: vi.fn((request: { name: string }) => of({ ...TABLE, id: 5, name: request.name })),
      update: vi.fn(() => of(TABLE))
    };

    await TestBed.configureTestingModule({
      imports: [GaugeTablesPageComponent],
      providers: [
        provideRouter([]),
        { provide: GaugeTablesService, useValue: gaugeTablesService }
      ]
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(GaugeTablesPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('loads the first gauge table and its calibration points', () => {
    const component = create().componentInstance;

    expect(gaugeTablesService.list).toHaveBeenCalled();
    expect(component.selectedGaugeTableId()).toBe(4);
    expect(component.selectedCurve).toEqual([{ height: 0, gallons: 0 }, { height: 250, gallons: 5000 }]);
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

  it('does not create a table without a name', () => {
    const component = create().componentInstance;

    component.createGaugeTable();

    expect(gaugeTablesService.create).not.toHaveBeenCalled();
    expect(component.error()).toBe('La tabla debe tener un nombre y al menos dos puntos.');
  });
});
