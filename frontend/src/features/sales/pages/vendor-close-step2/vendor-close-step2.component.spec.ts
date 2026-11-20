import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VendorCloseStep2Component } from './vendor-close-step2.component';
import { SalesVendorCloseService } from '../../../../core/services/sales-vendor-close.service';
import { JournalService } from '../../../../core/services/journal.service';
import { JournalStepChangesService } from '../../../../core/services/journal-step-changes.service';
import { ProductsService } from '../../../../core/services/products.service';
import { ApiError } from '../../../../shared/models/api-error.model';
import { JournalResponse, JournalStatus, StepStatus } from '../../../../shared/models/journal.model';
import { VendorShiftCloseBatchRequestDto } from '../../../../shared/models/dtos/vendor-close.dto';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

interface SalesServiceStub {
  getAssignedStaff(): Observable<unknown>;
  getVendorCloses(): Observable<unknown>;
  getDaySummary(): Observable<unknown>;
  getEffectiveTolerance(): Observable<unknown>;
  getCatalogProducts(): unknown[];
  getCalibrationDemo0805(): unknown;
  saveVendorCloses(req: VendorShiftCloseBatchRequestDto): Observable<unknown>;
  resetVendorCloses(date: string): Observable<void>;
  getLastSavedRequest(): VendorShiftCloseBatchRequestDto | null;
}

describe('VendorCloseStep2Component', () => {
  let component: VendorCloseStep2Component;
  let fixture: ComponentFixture<VendorCloseStep2Component>;
  let salesServiceMock: SalesServiceStub;

  const mockStaff = [
    {
      document: '52875796',
      name: 'Mónica Dorado',
      role: 'ISLERO',
      shiftName: '12-7',
      pumpNumber: 3,
      workerCode: 'MON-127'
    },
    {
      document: '1061696674',
      name: 'Carlos Andrés Paredes',
      role: 'ISLERO',
      shiftName: 'DIA6',
      pumpNumber: 1,
      workerCode: 'CAR-D6'
    },
    {
      document: '76326816',
      name: 'Wiltom Andrés Paz',
      role: 'ISLERO',
      shiftName: 'NOCHE',
      pumpNumber: 1,
      workerCode: 'WIL-NOC'
    }
  ];

  beforeEach(async () => {
    let savedRequest: VendorShiftCloseBatchRequestDto | null = null;

    salesServiceMock = {
      getAssignedStaff: () => of(mockStaff),
      getVendorCloses: () => of([]),
      getDaySummary: () => of({}),
      getEffectiveTolerance: () => of({
        effectiveDate: '2026-08-05',
        maxDiscrepancyAmount: 0.5,
        active: true
      }),
      getCatalogProducts: () => [
        { sku: '76102', name: 'Aditivo Gasolina Ruta 75 40ml', category: 'Aditivos', price: 6800.0 },
        { sku: '75089', name: 'Super Concentrado 4x1 Plus 40ml', category: 'Aditivos', price: 7200.0 }
      ],
      getCalibrationDemo0805: () => ({
        fecha: '2026-08-05',
        vendedores: mockStaff,
        capturas: {
          '52875796': {
            b100k: 8, b50k: 24, b20k: 7, b10k: 14, b5k: 13, b2k: 14,
            parciales: 10, pico: 373000,
            ga: 0, credibanco: 1354436, qr: 162606, redenciones: 0,
            galMotor: 223.701, galDiesel: 13.413, galExtra: 2.755, galMaxpro: 0,
            canastilla: 20800, tirilla: 3890028.87
          },
          '1061696674': {
            b100k: 13, b50k: 26, b20k: 0, b10k: 2, b5k: 2, b2k: 24,
            parciales: 9, pico: 47000,
            ga: 0, credibanco: 2019760, qr: 377902, redenciones: 41380,
            galMotor: 229.649, galDiesel: 11.314, galExtra: 13.606, galMaxpro: 7.595,
            canastilla: 25800, tirilla: 4285510.58
          },
          '76326816': {
            b100k: 9, b50k: 29, b20k: 21, b10k: 44, b5k: 10, b2k: 25,
            parciales: 16, pico: 110000,
            ga: 0, credibanco: 2317116, qr: 335500, redenciones: 0,
            galMotor: 331.237, galDiesel: 33.753, galExtra: 0.735, galMaxpro: 7.820,
            canastilla: 34500, tirilla: 5961277.95
          }
        },
        productos: {
          '76102': { '52875796': 2, '1061696674': 3, '76326816': 1 }
        }
      }),
      saveVendorCloses: (req: VendorShiftCloseBatchRequestDto) => {
        savedRequest = req;
        return of([]);
      },
      resetVendorCloses: () => of(void 0),
      getLastSavedRequest: () => savedRequest
    };

    await TestBed.configureTestingModule({
      imports: [VendorCloseStep2Component],
      providers: [
        { provide: SalesVendorCloseService, useValue: salesServiceMock },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ date: '2026-08-05' })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(VendorCloseStep2Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component and initialize assigned vendors', () => {
    expect(component).toBeTruthy();
    expect(component.assignedVendors.length).toBe(3);
    expect(component.tolerance).toBe(0.5);
  });

  it('should accurately compute August 5 calibration numbers and day total cash of $7,530,000 (S2-10)', () => {
    component.loadDemoHoja05(false);

    // Mónica: 10 sobres * 200.000 + 373.000 = $2.373.000
    const monica = component.captures['52875796'];
    expect(monica.efectivo).toBe(2373000);
    expect(monica.totalEntregado).toBe(3890042);
    expect(monica.descuadre).toBe(13.13);
    expect(monica.estadoResultado).toBe('sobra');

    // Carlos: 9 sobres * 200.000 + 47.000 = $1.847.000
    const carlos = component.captures['1061696674'];
    expect(carlos.efectivo).toBe(1847000);
    expect(carlos.totalEntregado).toBe(4286042);
    expect(carlos.descuadre).toBe(531.42);
    expect(carlos.estadoResultado).toBe('sobra');

    // Wiltom: 16 sobres * 200.000 + 110.000 = $3.310.000
    const wiltom = component.captures['76326816'];
    expect(wiltom.efectivo).toBe(3310000);
    expect(wiltom.totalEntregado).toBe(5962616);
    expect(wiltom.descuadre).toBe(1338.05);
    expect(wiltom.estadoResultado).toBe('sobra');

    // Total Cash for the day: 2.373.000 + 1.847.000 + 3.310.000 = $7.530.000 (S2-10)
    expect(component.totalCash()).toBe(7530000);
    expect(component.totalParciales()).toBe(35);
    expect(component.totalPico()).toBe(530000);
  });

  it('should evaluate tolerance rules correctly for CUADRA, SOBRA, and FALTA', () => {
    const doc = '52875796';
    const cap = component.captures[doc];
    cap.conDatos = true;
    cap.parciales = 5; // 5 * 200.000 = 1.000.000
    cap.pico = 0;
    cap.ga = 0;
    cap.credibanco = 0;
    cap.qr = 0;
    cap.redenciones = 0;

    // Case 1: Exact match ($1.000.000 vs $1.000.000) -> CUADRA
    cap.tirilla = 1000000;
    component.recalculate(doc);
    expect(cap.descuadre).toBe(0);
    expect(cap.estadoResultado).toBe('cuadra');

    // Case 2: Discrepancy within tolerance (0.4 <= 0.5) -> CUADRA
    cap.tirilla = 999999.60;
    component.recalculate(doc);
    expect(cap.descuadre).toBe(0.4);
    expect(cap.estadoResultado).toBe('cuadra');

    // Case 3: Sobra exceeding tolerance (+$100 > 0.5) -> SOBRA
    cap.tirilla = 999900;
    component.recalculate(doc);
    expect(cap.descuadre).toBe(100);
    expect(cap.estadoResultado).toBe('sobra');

    // Case 4: Falta exceeding tolerance (-$100 < -0.5) -> FALTA
    cap.tirilla = 1000100;
    component.recalculate(doc);
    expect(cap.descuadre).toBe(-100);
    expect(cap.estadoResultado).toBe('falta');
  });

  it('should flag arqueo discrepancy when bills sum does not match reported cash', () => {
    const doc = '52875796';
    const cap = component.captures[doc];
    cap.conDatos = true;
    cap.parciales = 5;
    cap.pico = 50000; // Efectivo = $1.050.000

    // Arqueo: 10 billetes de 100k = $1.000.000 (discrepancy of $50k)
    cap.denominaciones = {
      b100k: 10,
      b50k: 0,
      b20k: 0,
      b10k: 0,
      b5k: 0,
      b2k: 0
    };
    component.recalculate(doc);

    expect(cap.efectivo).toBe(1050000);
    expect(cap.totalArqueo).toBe(1000000);
    expect(cap.avisoArqueo).toBe(true);

    // Now fix arqueo by adding 1 billete de 50k
    cap.denominaciones.b50k = 1;
    component.recalculate(doc);
    expect(cap.totalArqueo).toBe(1050000);
    expect(cap.avisoArqueo).toBe(false);
  });

  it('should toggle Dominus drawer and fold sections', () => {
    expect(component.isDominusDrawerOpen).toBe(false);
    component.openDominusDrawer();
    expect(component.isDominusDrawerOpen).toBe(true);
    component.closeDominusDrawer();
    expect(component.isDominusDrawerOpen).toBe(false);

    expect(component.folded.arqueo).toBe(true);
    component.toggleFold('arqueo');
    expect(component.folded.arqueo).toBe(false);
  });

  it('should add products from catalog search into visible products', () => {
    component.productSearchInput = 'Aditivo Gasolina';
    component.addProductFromSearch();
    expect(component.visibleProductSkus).toContain('76102');
  });

  it('should save vendor closes batch to backend service', () => {
    component.loadDemoHoja05(false);
    component.saveCloses();

    expect(salesServiceMock.getLastSavedRequest()).toBeTruthy();
    expect(salesServiceMock.getLastSavedRequest()?.closes.length).toBe(3);
    expect(component.isSaving).toBe(false);
    expect(component.saveSuccessMessage).toContain('guardados');
  });

  it('should surface real backend ApiError detail when saveCloses fails', () => {
    salesServiceMock.saveVendorCloses = () =>
      throwError(() => new ApiError(409, { detail: 'La jornada ya está cerrada y no permite modificaciones.' }));

    component.saveCloses();

    expect(component.isSaving).toBe(false);
    expect(component.errorMessage).toContain('La jornada ya está cerrada y no permite modificaciones.');
    expect(component.errorMessage).not.toContain('No se pudo conectar con el servidor');
  });

  it('should mark step 2 completed in JournalService after saving closes', async () => {
    const journal: JournalResponse = {
      id: 34,
      date: '2026-11-19',
      consecutive: 318,
      status: JournalStatus.OPEN,
      steps: [
        { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: '2026-11-19T06:00:00' },
        { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.PENDING, completedAt: null }
      ]
    };
    const journalSubject = new BehaviorSubject<JournalResponse | null>(journal);
    let completedCall: { journalId: number; stepNumber: number } | null = null;
    let updatedStepStatus: StepStatus | null = null;

    const journalServiceStub = {
      currentJournal$: journalSubject.asObservable(),
      getCurrentJournalSnapshot: () => journalSubject.value,
      completeStep: (journalId: number, stepNumber: number) => {
        completedCall = { journalId, stepNumber };
        return of({
          id: 2,
          stepNumber: 2,
          name: 'Vendedores',
          status: StepStatus.COMPLETED,
          completedAt: '2026-11-19T12:00:00'
        });
      },
      updateCurrentStep: (step: { status: StepStatus }) => {
        updatedStepStatus = step.status;
      }
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [VendorCloseStep2Component],
      providers: [
        { provide: SalesVendorCloseService, useValue: salesServiceMock },
        { provide: JournalService, useValue: journalServiceStub },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } }
      ]
    }).compileComponents();

    const localFixture = TestBed.createComponent(VendorCloseStep2Component);
    const localComponent = localFixture.componentInstance;
    localFixture.detectChanges();

    expect(localComponent.isStep1Complete).toBe(true);
    localComponent.saveCloses();

    expect(completedCall).toEqual({ journalId: 34, stepNumber: 2 });
    expect(updatedStepStatus).toBe(StepStatus.COMPLETED);
    expect(localComponent.saveSuccessMessage).toContain('paso 2 completado');
  });

  it('should match demo hoja 05 and backend closes into existing UUID-keyed staff by name without duplicating columns', async () => {
    const uuidStaff = [
      {
        document: '8293f40b-5ecc-487c-a2fb-efd7aec4811c',
        name: 'Mónica Dorado',
        role: 'APOYO',
        shiftName: '12-7',
        pumpNumber: 3,
        workerCode: '12-7'
      },
      {
        document: '43d5960a-ae88-4fce-8848-8ae9ba27a124',
        name: 'Carlos Andrés Paredes',
        role: 'TITULAR',
        shiftName: 'DIA6',
        pumpNumber: 1,
        workerCode: 'DIA6'
      },
      {
        document: 'a99d99d2-8413-4bff-a598-0348db14ff92',
        name: 'Wiltom Andrés Paz',
        role: 'TITULAR',
        shiftName: 'NOCHE',
        pumpNumber: 1,
        workerCode: 'NOCHE'
      }
    ];

    salesServiceMock.getAssignedStaff = () => of(uuidStaff);
    salesServiceMock.getVendorCloses = () => of([
      {
        journalDate: '2026-08-05',
        vendorDocument: '52875796',
        vendorName: 'Mónica Dorado',
        shiftName: '12-7',
        pumpNumber: 3,
        cash: 2373000,
        ga: 0,
        credibanco: 1354436,
        qr: 162606,
        redemptions: 0,
        totalDelivered: 3890042,
        gallonsMotor: 223.701,
        gallonsDiesel: 13.413,
        gallonsExtra: 2.755,
        gallonsMaxpro: 0,
        basketDominusAmount: 20800,
        dominusTirilla: 3890028.87,
        realDiscrepancy: 13.13,
        resultStatus: 'SOBRA',
        products: []
      }
    ]);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [VendorCloseStep2Component],
      providers: [
        { provide: SalesVendorCloseService, useValue: salesServiceMock },
        { provide: ActivatedRoute, useValue: { queryParams: of({ date: '2026-08-05' }) } }
      ]
    }).compileComponents();

    const localFixture = TestBed.createComponent(VendorCloseStep2Component);
    const localComponent = localFixture.componentInstance;
    localFixture.detectChanges();

    expect(localComponent.assignedVendors.length).toBe(3);
    expect(localComponent.captures['8293f40b-5ecc-487c-a2fb-efd7aec4811c'].efectivo).toBe(2373000);

    localComponent.loadDemoHoja05(false);
    expect(localComponent.assignedVendors.length).toBe(3);
    expect(localComponent.captures['43d5960a-ae88-4fce-8848-8ae9ba27a124'].credibanco).toBe(2019760);
    expect(localComponent.captures['a99d99d2-8413-4bff-a598-0348db14ff92'].qr).toBe(335500);
  });

  it('should compute sold units per vendor accurately and reset all fields + step 2 status on confirmReset', async () => {
    const journal: JournalResponse = {
      id: 34,
      date: '2026-11-19',
      consecutive: 318,
      status: JournalStatus.OPEN,
      steps: [
        { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: '2026-11-19T06:00:00' },
        { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.COMPLETED, completedAt: '2026-11-19T12:00:00' }
      ]
    };
    const journalSubject = new BehaviorSubject<JournalResponse | null>(journal);
    let resetCalledWithDate: string | null = null;
    let updatedStepStatus: StepStatus | null = null;

    salesServiceMock.resetVendorCloses = (date: string) => {
      resetCalledWithDate = date;
      return of(void 0);
    };

    const journalServiceStub = {
      currentJournal$: journalSubject.asObservable(),
      getCurrentJournalSnapshot: () => journalSubject.value,
      completeStep: () => of(journal.steps[1]),
      updateCurrentStep: (step: { status: StepStatus }) => {
        updatedStepStatus = step.status;
      }
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [VendorCloseStep2Component],
      providers: [
        { provide: SalesVendorCloseService, useValue: salesServiceMock },
        { provide: JournalService, useValue: journalServiceStub },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } }
      ]
    }).compileComponents();

    const localFixture = TestBed.createComponent(VendorCloseStep2Component);
    const localComponent = localFixture.componentInstance;
    localFixture.detectChanges();

    localComponent.loadDemoHoja05(false);
    expect(localComponent.vendorProductUnits('52875796')).toBe(2);
    expect(localComponent.vendorProductUnits('1061696674')).toBe(3);
    expect(localComponent.vendorProductUnits('76326816')).toBe(1);

    localComponent.requestReset();
    expect(localComponent.showResetConfirm).toBe(true);
    localComponent.confirmReset();

    expect(resetCalledWithDate).toBe('2026-11-19');
    expect(localComponent.showResetConfirm).toBe(false);
    expect(localComponent.totalCash()).toBe(0);
    expect(localComponent.totalDelivered()).toBe(0);
    expect(localComponent.vendorProductUnits('52875796')).toBe(0);
    expect(updatedStepStatus).toBe(StepStatus.PENDING);
  });

  it('should load active catalog from ProductsService and preserve real category in saveCloses()', async () => {
    const productsServiceStub = {
      list: () => of([
        {
          id: 'prod-1',
          code: '76102',
          name: 'Aditivo Gasolina Ruta 75 40ml',
          category: 'ADITIVO',
          price: 6800,
          cost: 4200,
          active: true,
          marginPercent: 38.24
        },
        {
          id: 'prod-2',
          code: '99001',
          name: 'Lubricante Sintético 1L',
          category: 'LUBRICANTE',
          price: 32000,
          cost: 21000,
          active: true,
          marginPercent: 34.38
        }
      ])
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [VendorCloseStep2Component],
      providers: [
        { provide: SalesVendorCloseService, useValue: salesServiceMock },
        { provide: ProductsService, useValue: productsServiceStub },
        { provide: ActivatedRoute, useValue: { queryParams: of({ date: '2026-08-05' }) } }
      ]
    }).compileComponents();

    const localFixture = TestBed.createComponent(VendorCloseStep2Component);
    const localComponent = localFixture.componentInstance;
    localFixture.detectChanges();

    localComponent.captures['52875796'].parciales = 1;
    localComponent.captures['52875796'].productos['76102'] = 2;
    localComponent.saveCloses();

    const sentRequest = salesServiceMock.getLastSavedRequest();
    expect(sentRequest).toBeTruthy();
    const sentProduct = sentRequest!.closes[0].products?.find(p => p.sku === '76102');
    expect(sentProduct?.category).toBe('ADITIVO');
    expect(sentProduct?.category).not.toBe('Canastilla');
  });

  it('should apply effective fuel prices from backend for tirilla mismatch validation', async () => {
    const salesWithPricesMock = {
      ...salesServiceMock,
      getEffectiveFuelPrices: () => of({
        MOTOR: 20000,
        DIESEL: 12000,
        EXTRA: 21000,
        MAX_PRO: 18000
      })
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [VendorCloseStep2Component],
      providers: [
        { provide: SalesVendorCloseService, useValue: salesWithPricesMock },
        { provide: ActivatedRoute, useValue: { queryParams: of({ date: '2026-11-19' }) } }
      ]
    }).compileComponents();

    const localFixture = TestBed.createComponent(VendorCloseStep2Component);
    const localComponent = localFixture.componentInstance;
    localFixture.detectChanges();

    expect(localComponent.fuelPrices['motor']).toBe(20000);
    expect(localComponent.fuelPrices['diesel']).toBe(12000);
    expect(localComponent.fuelPrices['extra']).toBe(21000);
    expect(localComponent.fuelPrices['maxpro']).toBe(18000);

    // 10 galMotor at fallback 15826 = 158260, but backend effective price is 20000 -> 200000
    localComponent.captures['52875796'].galMotor = 10;
    localComponent.captures['52875796'].tirilla = 158260;
    localComponent.onFieldChange('52875796');
    expect(localComponent.captures['52875796'].avisoTirilla).toBeTruthy();

    localComponent.captures['52875796'].tirilla = 200000;
    localComponent.onFieldChange('52875796');
    expect(localComponent.captures['52875796'].avisoTirilla).toBeNull();
  });

  it('should add product selected from datalist "Name · SKU" format via addProductFromSearch', async () => {
    const productsServiceStub = {
      list: () => of([
        {
          id: 'prod-1',
          code: '76102',
          name: 'Aditivo Gasolina Ruta 75 40ml',
          category: 'ADITIVO',
          price: 6800,
          cost: 4200,
          active: true,
          marginPercent: 38.24
        },
        {
          id: 'prod-2',
          code: '99001',
          name: 'Lubricante Sintético 1L',
          category: 'LUBRICANTE',
          price: 32000,
          cost: 21000,
          active: true,
          marginPercent: 34.38
        }
      ])
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [VendorCloseStep2Component],
      providers: [
        { provide: SalesVendorCloseService, useValue: salesServiceMock },
        { provide: ProductsService, useValue: productsServiceStub },
        { provide: ActivatedRoute, useValue: { queryParams: of({ date: '2026-08-05' }) } }
      ]
    }).compileComponents();

    const localFixture = TestBed.createComponent(VendorCloseStep2Component);
    const localComponent = localFixture.componentInstance;
    localFixture.detectChanges();

    expect(localComponent.visibleProductSkus).not.toContain('99001');
    localComponent.productSearchInput = 'Lubricante Sintético 1L · 99001';
    localComponent.addProductFromSearch();

    expect(localComponent.visibleProductSkus).toContain('99001');
    expect(localComponent.folded.productos).toBe(false);
    expect(localComponent.productSearchInput).toBe('');
  });

  it('should load initial data only once when currentJournal$ and route queryParams initialize together', async () => {
    let staffCalls = 0;
    let closesCalls = 0;
    const journal: JournalResponse = {
      id: 34,
      date: '2026-11-19',
      consecutive: 318,
      status: JournalStatus.OPEN,
      steps: [
        { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: '2026-11-19T06:00:00' },
        { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.PENDING, completedAt: null }
      ]
    };
    const journalSubject = new BehaviorSubject<JournalResponse | null>(journal);
    const countingSalesMock = {
      ...salesServiceMock,
      getAssignedStaff: () => {
        staffCalls += 1;
        return of(mockStaff);
      },
      getVendorCloses: () => {
        closesCalls += 1;
        return of([]);
      }
    };
    const journalServiceStub = {
      currentJournal$: journalSubject.asObservable(),
      getCurrentJournalSnapshot: () => journalSubject.value,
      completeStep: () => of(journal.steps[1]),
      updateCurrentStep: () => undefined
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [VendorCloseStep2Component],
      providers: [
        { provide: SalesVendorCloseService, useValue: countingSalesMock },
        { provide: JournalService, useValue: journalServiceStub },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } }
      ]
    }).compileComponents();

    const localFixture = TestBed.createComponent(VendorCloseStep2Component);
    const localComponent = localFixture.componentInstance;
    localFixture.detectChanges();

    expect(localComponent.journalDate).toBe('2026-11-19');
    expect(staffCalls).toBe(1);
    expect(closesCalls).toBe(1);
  });

  it('should not inject demo vendors when getAssignedStaff returns empty on a non-calibration date', async () => {
    const emptyStaffMock = {
      ...salesServiceMock,
      getAssignedStaff: () => of([]),
      getVendorCloses: () => of([])
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [VendorCloseStep2Component],
      providers: [
        { provide: SalesVendorCloseService, useValue: emptyStaffMock },
        { provide: ActivatedRoute, useValue: { queryParams: of({ date: '2026-11-19' }) } }
      ]
    }).compileComponents();

    const localFixture = TestBed.createComponent(VendorCloseStep2Component);
    const localComponent = localFixture.componentInstance;
    localFixture.detectChanges();

    expect(localComponent.assignedVendors.length).toBe(0);
    expect(localComponent.errorMessage).toContain('Paso 1');
  });

  it('tracks unsaved edits on Step 2 and saves via JournalStepChangesService before leaving', () => {
    const stepChanges = TestBed.inject(JournalStepChangesService);
    expect(component.hasUnsavedChanges()).toBe(false);

    const firstDoc = component.assignedVendors[0].document;
    component.captures[firstDoc].pico = 50000;

    expect(component.hasUnsavedChanges()).toBe(true);
    let canLeave = false;
    stepChanges.canDeactivateStep().subscribe(res => (canLeave = res));
    expect(stepChanges.modalOpen()).toBe(true);

    stepChanges.confirmSaveAndLeave();
    expect(canLeave).toBe(true);
    expect(component.hasUnsavedChanges()).toBe(false);
    expect(salesServiceMock.getLastSavedRequest()).toBeTruthy();
  });
});
