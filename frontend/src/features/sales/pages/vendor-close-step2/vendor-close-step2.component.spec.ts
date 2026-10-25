import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VendorCloseStep2Component } from './vendor-close-step2.component';
import { SalesVendorCloseService } from '../../../../core/services/sales-vendor-close.service';
import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

describe('VendorCloseStep2Component', () => {
  let component: VendorCloseStep2Component;
  let fixture: ComponentFixture<VendorCloseStep2Component>;
  let salesServiceMock: any;

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
    let savedRequest: any = null;

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
      saveVendorCloses: (req: any) => {
        savedRequest = req;
        return of([]);
      },
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
    expect(salesServiceMock.getLastSavedRequest().closes.length).toBe(3);
    expect(component.isSaving).toBe(false);
    expect(component.saveSuccessMessage).toContain('guardados');
  });
});
