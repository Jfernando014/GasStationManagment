import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';

import { FuelAutonomyService } from '../../../../core/services/fuel-autonomy.service';
import { FuelVariationsService } from '../../../../core/services/fuel-variations.service';
import { JournalService } from '../../../../core/services/journal.service';
import { ProductStockService } from '../../../../core/services/product-stock.service';
import { TankMeasurementsService } from '../../../../core/services/tank-measurements.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { JournalResponse, JournalStatus, JournalStepResponse, StepStatus } from '../../../../shared/models/journal.model';
import { ProductStockResponse } from '../../../../shared/models/dtos/product-stock.dto';
import { TankResponse } from '../../../../shared/models/dtos/tank.dto';
import { TankMeasurementsComponent } from '../../components/tank-measurements/tank-measurements.component';
import { JournalStep5Component } from './journal-step5.component';

const JOURNAL_DATE = '2026-08-05';

const STEPS: JournalStepResponse[] = Array.from({ length: 6 }, (_, index) => ({
  id: index + 1,
  stepNumber: index + 1,
  name: `Paso ${index + 1}`,
  status: index + 1 === 2 ? StepStatus.COMPLETED : StepStatus.PENDING,
  completedAt: index + 1 === 2 ? '2026-08-05T07:00:00' : null
}));

const JOURNAL: JournalResponse = {
  id: 10,
  date: JOURNAL_DATE,
  consecutive: 42,
  status: JournalStatus.OPEN,
  steps: STEPS
};

const TANKS: TankResponse[] = ['MOTOR', 'EXTRA', 'DIESEL', 'MAX_PRO', 'MOTOR', 'EXTRA'].map((fuelType, index) => ({
  id: index + 1,
  code: `T${index + 1}`,
  name: `Tanque ${index + 1}`,
  fuelType: fuelType as TankResponse['fuelType'],
  maxCapacityGallons: 5000,
  maxHeightCm: 200,
  toleranceCm: 1,
  gaugeTableId: 1
}));

const STOCK: ProductStockResponse[] = [
  {
    productId: 7,
    productCode: '80216',
    productName: 'Terpel Celerity 4T',
    category: 'LUBRICANTE',
    active: true,
    office: { opening: 10, initial: 0, entries: 0, transfers: 0, adjustments: 0, sold: 2, closing: 10 },
    counter: { opening: 6, initial: 0, entries: 0, transfers: 0, adjustments: 0, sold: 4, closing: 2 },
    status: 'NEGATIVE_STOCK'
  }
];

describe('JournalStep5Component', () => {
  let journalSubject: BehaviorSubject<JournalResponse | null>;
  let tanksService: { list: ReturnType<typeof vi.fn> };
  let measurementsService: {
    findByJournalDate: ReturnType<typeof vi.fn>;
    save: ReturnType<typeof vi.fn>;
    volume: ReturnType<typeof vi.fn>;
  };
  let variationsService: { findByJournalDate: ReturnType<typeof vi.fn> };
  let autonomyService: { findByJournalDate: ReturnType<typeof vi.fn> };
  let stockService: {
    listByJournalDate: ReturnType<typeof vi.fn>;
    registerTransfer: ReturnType<typeof vi.fn>;
    syncSales: ReturnType<typeof vi.fn>;
    registerInitial: ReturnType<typeof vi.fn>;
    registerEntry: ReturnType<typeof vi.fn>;
    registerAdjustment: ReturnType<typeof vi.fn>;
  };
  let journalService: {
    currentJournal$: BehaviorSubject<JournalResponse | null>;
    completeStep: ReturnType<typeof vi.fn>;
    updateCurrentStep: ReturnType<typeof vi.fn>;
  };

  function create() {
    const fixture = TestBed.createComponent(JournalStep5Component);
    fixture.detectChanges();
    return fixture;
  }

  /** Pastes through the measurements child, as the user does over the height column. */
  function pasteHeights(fixture: ComponentFixture<JournalStep5Component>, text: string): ClipboardEvent {
    const measurements = fixture.debugElement.query(By.directive(TankMeasurementsComponent))
      .componentInstance as TankMeasurementsComponent;
    const paste = {
      clipboardData: { getData: () => text },
      preventDefault: vi.fn()
    } as unknown as ClipboardEvent;
    measurements.onHeightsPaste(paste, 0);
    return paste;
  }

  beforeEach(async () => {
    journalSubject = new BehaviorSubject<JournalResponse | null>(JOURNAL);
    tanksService = { list: vi.fn(() => of(TANKS)) };
    measurementsService = {
      findByJournalDate: vi.fn(() => of(null)),
      save: vi.fn(),
      volume: vi.fn((tankId: number, heightCm: number) => of({ tankId, heightCm, volumeGallons: heightCm * 20 }))
    };
    variationsService = { findByJournalDate: vi.fn(() => of([])) };
    autonomyService = { findByJournalDate: vi.fn(() => of([])) };
    stockService = {
      listByJournalDate: vi.fn(() => of(STOCK)),
      registerTransfer: vi.fn(() => of([])),
      syncSales: vi.fn(),
      registerInitial: vi.fn(),
      registerEntry: vi.fn(),
      registerAdjustment: vi.fn()
    };
    journalService = {
      currentJournal$: journalSubject,
      completeStep: vi.fn(),
      updateCurrentStep: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [JournalStep5Component],
      providers: [
        provideRouter([]),
        { provide: TanksService, useValue: tanksService },
        { provide: TankMeasurementsService, useValue: measurementsService },
        { provide: FuelVariationsService, useValue: variationsService },
        { provide: FuelAutonomyService, useValue: autonomyService },
        { provide: ProductStockService, useValue: stockService },
        { provide: JournalService, useValue: journalService }
      ]
    }).compileComponents();
  });

  it('loads the six tanks, the saved measurement and the product stock', () => {
    measurementsService.findByJournalDate.mockReturnValue(of({
      id: 1,
      journalDate: JOURNAL_DATE,
      measuredAt: '2026-08-05T08:00:00',
      readings: [{ tankId: 1, heightCm: 120, volumeGallons: 2400 }]
    }));

    const fixture = create();
    const component = fixture.componentInstance;

    expect(tanksService.list).toHaveBeenCalled();
    expect(measurementsService.findByJournalDate).toHaveBeenCalledWith(JOURNAL_DATE);
    expect(component.tanks()).toHaveLength(6);
    expect(component.heights()[1]).toBe(120);
    expect(component.gallons()[1]).toBe(2400);
    expect(component.stock()).toHaveLength(1);
    expect(component.measuredCount()).toBe(1);
  });

  it('pasting the six heights fills every tank and asks for the gallons', async () => {
    const fixture = create();
    const component = fixture.componentInstance;
    const paste = pasteHeights(fixture, '100\n110\n120\n130\n140\n150');
    await new Promise(resolve => setTimeout(resolve, 400));

    expect(paste.preventDefault).toHaveBeenCalled();
    expect(component.measuredCount()).toBe(6);
    expect(component.allMeasured()).toBe(true);
    expect(component.heights()[6]).toBe(150);
    expect(measurementsService.volume).toHaveBeenCalledTimes(6);
    expect(component.gallons()[3]).toBe(2400);
  });

  it('saves the six readings and reloads the variation', async () => {
    const fixture = create();
    const component = fixture.componentInstance;
    measurementsService.save.mockReturnValue(of({
      id: 2,
      journalDate: JOURNAL_DATE,
      measuredAt: '2026-08-05T09:00:00',
      readings: TANKS.map(tank => ({ tankId: tank.id, heightCm: 100, volumeGallons: 2000 }))
    }));

    pasteHeights(fixture, '100\n100\n100\n100\n100\n100');
    await new Promise(resolve => setTimeout(resolve, 400));

    component.saveMeasurements();

    expect(measurementsService.save).toHaveBeenCalledWith(JOURNAL_DATE, {
      readings: TANKS.map(tank => ({ tankId: tank.id, heightCm: 100 }))
    });
    expect(component.allMeasured()).toBe(true);
    expect(component.measurement()?.id).toBe(2);
    expect(variationsService.findByJournalDate).toHaveBeenCalledTimes(2);
  });

  it('enables completion only after the six readings are saved', async () => {
    const fixture = create();
    const component = fixture.componentInstance;

    expect(component.canComplete()).toBe(false);

    pasteHeights(fixture, '100\n110\n120\n130\n140\n150');
    await new Promise(resolve => setTimeout(resolve, 400));

    expect(component.allMeasured()).toBe(true);
    expect(component.canComplete()).toBe(false);

    measurementsService.save.mockReturnValue(of({
      id: 5,
      journalDate: JOURNAL_DATE,
      measuredAt: '2026-08-05T09:30:00',
      readings: TANKS.map(tank => ({ tankId: tank.id, heightCm: 100, volumeGallons: 2000 }))
    }));
    component.saveMeasurements();

    expect(component.canComplete()).toBe(true);

    journalService.completeStep.mockReturnValue(of({
      id: 5,
      stepNumber: 5,
      name: 'Paso 5',
      status: StepStatus.COMPLETED,
      completedAt: '2026-08-05T10:00:00'
    }));
    component.completeStep();

    expect(journalService.completeStep).toHaveBeenCalledWith(10, 5);
    expect(journalService.updateCurrentStep).toHaveBeenCalled();
  });

  it('does not allow completion while step 2 has not been closed', async () => {
    journalSubject.next({
      ...JOURNAL,
      steps: STEPS.map(step => step.stepNumber === 2
        ? { ...step, status: StepStatus.PENDING, completedAt: null }
        : step)
    });

    const fixture = create();
    const component = fixture.componentInstance;

    pasteHeights(fixture, '100\n100\n100\n100\n100\n100');
    await new Promise(resolve => setTimeout(resolve, 400));

    measurementsService.save.mockReturnValue(of({
      id: 6,
      journalDate: JOURNAL_DATE,
      measuredAt: '2026-08-05T09:45:00',
      readings: TANKS.map(tank => ({ tankId: tank.id, heightCm: 100, volumeGallons: 2000 }))
    }));
    component.saveMeasurements();

    expect(component.salesCompleted()).toBe(false);
    expect(component.canComplete()).toBe(false);
    expect(journalService.completeStep).not.toHaveBeenCalled();
  });

  it('does not allow editing when step 6 is completed', () => {
    journalSubject.next({
      ...JOURNAL,
      steps: STEPS.map(step => step.stepNumber === 6 ? { ...step, status: StepStatus.COMPLETED } : step)
    });

    const component = create().componentInstance;

    expect(component.editable()).toBe(false);
    expect(component.canComplete()).toBe(false);
  });

  it('saves only the valid transfers to the counter', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.onTransferChanged({ productId: 7, quantity: 3 });
    component.saveTransfers();

    expect(stockService.registerTransfer).toHaveBeenCalledWith({
      productId: 7,
      quantity: 3,
      journalDate: JOURNAL_DATE
    });
    expect(component.pendingTransfers()).toEqual({});
  });

  it('rejects a transfer larger than the office stock', () => {
    const component = create().componentInstance;

    component.onTransferChanged({ productId: 7, quantity: 40 });
    expect(component.hasPendingTransfers()).toBe(true);

    component.saveTransfers();
    expect(stockService.registerTransfer).not.toHaveBeenCalled();
    expect(component.formError()).toBe('No hay traslados válidos para guardar.');
  });

  it('shows the unmatched codes as a warning after synchronizing sales', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    stockService.syncSales.mockReturnValue(of({
      journalDate: JOURNAL_DATE,
      syncedProducts: 1,
      unmatchedCodes: ['99999']
    }));

    component.tab.set('lubricantes');
    component.syncSales();
    fixture.detectChanges();

    expect(component.unmatchedCodes()).toEqual(['99999']);
    expect(fixture.nativeElement.textContent).toContain('99999');
    expect(fixture.nativeElement.textContent).toContain('No se bloquearon las ventas');
  });
});
