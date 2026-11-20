import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { InventoryAlertsService } from '../../../core/services/inventory-alerts.service';
import { InventoryHomeService } from '../../../core/services/inventory-home.service';
import { FuelReceptionsService } from '../../../core/services/fuel-receptions.service';
import { TankMeasurementsService } from '../../../core/services/tank-measurements.service';
import { TanksService } from '../../../core/services/tanks.service';
import { DailyControlResponse } from '../../../shared/models/dtos/daily-control.dto';
import { InventoryHomeResponse } from '../../../shared/models/dtos/inventory-home.dto';
import { HomePageComponent } from './home-page.component';

const HOME: InventoryHomeResponse = {
  fuels: [
    {
      fuelType: 'MOTOR',
      currentGallons: 2500,
      capacityGallons: 5000,
      percentage: 50,
      complete: true,
      alertLevel: 'CRITICAL'
    }
  ],
  tanks: [
    {
      tankId: 1,
      tankCode: 'TQ-001',
      fuelType: 'MOTOR',
      heightCm: 120,
      gallons: 2500,
      capacityGallons: 5000,
      percentage: 50,
      measuredOn: '2026-11-06',
      measuredAt: '2026-11-06T18:06:27',
      alertLevel: 'CRITICAL'
    }
  ],
  mostCriticalAlert: {
    id: 9,
    alertType: 'AUTONOMY',
    fuelType: 'MOTOR',
    level: 'CRITICAL',
    status: 'ACTIVE',
    autonomyDays: 1.05,
    openedOn: '2026-11-06',
    createdAt: '2026-11-06T18:06:27',
    readAt: null,
    attendedAt: null,
    attendNote: null,
    resolvedAt: null
  },
  pendingAlerts: 1
};

const CONTROL: DailyControlResponse = {
  journalDate: '2026-11-06',
  fuels: [
    {
      fuelType: 'MOTOR',
      variation: {
        fuelType: 'MOTOR',
        status: 'OK',
        tankIds: [1],
        initialGallons: 2600,
        entriesGallons: 0,
        soldGallons: 100,
        theoreticalGallons: 2500,
        measuredGallons: 2500,
        varianceGallons: 0,
        toleranceGallons: 5,
        outOfTolerance: false
      },
      autonomy: {
        fuelType: 'MOTOR',
        status: 'CALCULATED',
        stockGallons: 2500,
        expectedDailySalesGallons: 2380,
        autonomyDays: 1.05,
        level: 'CRITICAL',
        warningDays: 5,
        criticalDays: 2,
        tanks: []
      },
      estimatedDepletionDate: '2026-11-07'
    }
  ],
  products: [
    {
      productId: 3,
      productCode: 'LUB-01',
      productName: 'Aceite 20W50',
      category: 'LUBRICANTE',
      sold: 4,
      officeStock: 10,
      counterStock: 2,
      status: 'OK',
      depleted: false
    }
  ]
};

describe('HomePageComponent', () => {
  let homeService: {
    getHome: ReturnType<typeof vi.fn>;
    getDailyControl: ReturnType<typeof vi.fn>;
  };
  let alertsService: {
    setPending: ReturnType<typeof vi.fn>;
  };
  let measurementsService: {
    volume: ReturnType<typeof vi.fn>;
    save: ReturnType<typeof vi.fn>;
  };
  let receptionsService: {
    create: ReturnType<typeof vi.fn>;
  };
  let tanksService: {
    list: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    sessionStorage.clear();
    homeService = {
      getHome: vi.fn(() => of(HOME)),
      getDailyControl: vi.fn(() => of(CONTROL))
    };
    alertsService = { setPending: vi.fn() };
    measurementsService = {
      volume: vi.fn(() => of({ tankId: 1, heightCm: 130, volumeGallons: 2700 })),
      save: vi.fn(() => of({ journalDate: '2026-11-19', measuredAt: '2026-11-19T10:00:00', readings: [] }))
    };
    receptionsService = {
      create: vi.fn(() => of({ id: 1, tankId: 1, receptionDate: '2026-11-19', gallons: 1200, supplier: 'Terpel', createdAt: '', updatedAt: '' }))
    };
    tanksService = {
      list: vi.fn(() => of([{ id: 1, code: 'TQ-001', name: 'Tanque 1', fuelType: 'MOTOR', maxCapacityGallons: 5000, maxHeightCm: 230, toleranceCm: 0 }]))
    };

    await TestBed.configureTestingModule({
      imports: [HomePageComponent],
      providers: [
        provideRouter([]),
        { provide: InventoryHomeService, useValue: homeService },
        { provide: InventoryAlertsService, useValue: alertsService },
        { provide: TankMeasurementsService, useValue: measurementsService },
        { provide: FuelReceptionsService, useValue: receptionsService },
        { provide: TanksService, useValue: tanksService }
      ]
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(HomePageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('shows the stock per fuel and tank from the last measurement', () => {
    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(homeService.getHome).toHaveBeenCalled();
    expect(text).toContain('Gasolina Corriente');
    expect(text).toContain('TQ-001');
    expect(text).toContain('2.500');
  });

  it('reuses the pending count of the home response on the bell', () => {
    const component = create().componentInstance;

    expect(alertsService.setPending).toHaveBeenCalledWith(1);
    expect(component.pendingAlerts()).toBe(1);
  });

  it('loads the control of the day with the selected journal date', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.changeTab('control');
    fixture.detectChanges();

    expect(homeService.getDailyControl).toHaveBeenCalledWith(component.controlDate());
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Aceite 20W50');
    expect(text).toContain('Calculada');
  });

  it('allows dismissing the healthy optimal banner but always displays active alerts', () => {
    homeService.getHome.mockReturnValue(of({ ...HOME, mostCriticalAlert: null, pendingAlerts: 0 }));
    const fixture = create();
    const el = fixture.nativeElement as HTMLElement;

    const closeBtn = el.querySelector('.optimal-banner-close') as HTMLButtonElement;
    expect(closeBtn).toBeTruthy();
    closeBtn.click();
    fixture.detectChanges();

    expect(el.querySelector('.optimal-alert-banner')).toBeNull();
  });

  it('saves a quick tank ruler measurement from the interactive tank card modal', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.openMeasureModal(HOME.tanks[0]);
    component.measureHeightText.set('135');
    component.saveQuickMeasurement();

    expect(measurementsService.save).toHaveBeenCalledWith(component.measureDate(), {
      readings: [{ tankId: 1, heightCm: 135 }]
    });
    expect(homeService.getHome).toHaveBeenCalledTimes(2);
  });

  it('submits a fuel reception from the quick consignation modal', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.openReceptionModal(HOME.tanks[0]);
    component.onReceptionGallonsInput(1200 as unknown as string);
    component.receptionSupplier.set('Terpel');
    component.submitReception();

    expect(receptionsService.create).toHaveBeenCalledWith({
      tankId: 1,
      receptionDate: component.receptionDate(),
      gallons: 1200,
      supplier: 'Terpel'
    });
    expect(homeService.getHome).toHaveBeenCalledTimes(2);
  });

  it('handles numeric ngModelChange events cleanly in quick measure modal and renders a link to tanks', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    const el = fixture.nativeElement as HTMLElement;

    const configLink = el.querySelector('.home-tanks-config-btn') as HTMLAnchorElement;
    expect(configLink).toBeTruthy();
    expect(configLink.getAttribute('href')).toContain('/configuracion/tanques');

    component.openMeasureModal(HOME.tanks[0]);
    component.onMeasureHeightInput(23 as unknown as string);
    component.saveQuickMeasurement();

    expect(measurementsService.save).toHaveBeenCalledWith(component.measureDate(), {
      readings: [{ tankId: 1, heightCm: 23 }]
    });
  });
});
