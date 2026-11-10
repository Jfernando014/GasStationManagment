import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CashBalanceStep4Service } from './cash-balance-step4.service';

describe('CashBalanceStep4Service', () => {
  let service: CashBalanceStep4Service;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), CashBalanceStep4Service]
    });
    service = TestBed.inject(CashBalanceStep4Service);
    service.clearAllStoredData();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    service.clearAllStoredData();
  });

  it('should calculate authentic August 1, 2026 seed data (Planilla 289) faithfully', () => {
    const calc = service.calculateStep4('2026-08-01');
    expect(calc.planilla).toBe(289);
    expect(calc.fondo.dias).toBe(12);
    expect(calc.fondo.acumulado).toBe(3000000);
    expect(calc.saldoAnterior).toBe(9364000);
    expect(calc.origenAnterior).toBe('planilla histórica');
    expect(calc.dinero).toBe(12516000);
    expect(calc.saldoEfectivo).toBe(9364000 + (12516000 - 250000) - 0);
    expect(calc.saldoJustificado).toBe(21630000 + 3000000 + 0);
    expect(calc.saldoDominus).toBe(24629469);
    expect(calc.diferencia).toBe(-531);
    expect(calc.totalEnCaja).toBe(21630000 + 3000000);
  });

  it('should compute savings fund on August 5, 2026 as 16 days ($4,000,000)', () => {
    const fondo = service.fondoArriendoAl('2026-08-05');
    expect(fondo.dias).toBe(16);
    expect(fondo.acumulado).toBe(4000000);
    expect(fondo.ahorroHoy).toBe(250000);
  });

  it('should update cuadre state when consolidadoDominus matches Step 2 tirilla', () => {
    service.saveCapturedData('2026-08-05', { consolidadoDominus: 14136817.4 });
    const calc = service.calculateStep4('2026-08-05');
    expect(calc.cuadre).toBe('cuadra');
    expect(calc.planilla).toBe(293);
  });

  it('should register and remove a fund payment', () => {
    let registeredId = '';
    service.registerFundPayment('2026-08-05', 1000000, 'Arriendo').subscribe(p => {
      registeredId = p.id;
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/sales/savings-fund/payments');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 99 });

    const fondoAfter = service.fondoArriendoAl('2026-08-05');
    expect(fondoAfter.totalPagosHoy).toBe(1000000);
    expect(fondoAfter.acumulado).toBe(3000000);

    service.removeFundPayment(registeredId);
  });
});

