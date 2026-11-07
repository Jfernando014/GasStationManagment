import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { InventoryAlertsService } from '../../../core/services/inventory-alerts.service';
import { InventoryHomeService } from '../../../core/services/inventory-home.service';
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

  beforeEach(async () => {
    homeService = {
      getHome: vi.fn(() => of(HOME)),
      getDailyControl: vi.fn(() => of(CONTROL))
    };
    alertsService = { setPending: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [HomePageComponent],
      providers: [
        provideRouter([]),
        { provide: InventoryHomeService, useValue: homeService },
        { provide: InventoryAlertsService, useValue: alertsService }
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
});
