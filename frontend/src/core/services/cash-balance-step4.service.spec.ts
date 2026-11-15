import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CashBalanceStep4Service } from './cash-balance-step4.service';
import { CashBalanceStepResponseDto } from '../../shared/models/dtos/cash-balance-step4.dto';

const response: CashBalanceStepResponseDto = {
  table: {
    date: '2026-08-05',
    journalConsecutive: 293,
    journalStatus: 'OPEN',
    savingsFund: {
      previousBalance: 3_750_000,
      contribution: 250_000,
      payments: 0,
      accumulatedBalance: 4_000_000
    },
    expenses: { items: [], total: 0 },
    cashBalance: 9_000_000,
    dailyCash: 2_500_000,
    planillaCashBalance: 11_500_000,
    dominusAccumulatedBalance: null,
    difference: null
  },
  canComplete: false,
  missing: ['Falta el saldo acumulado de Dominus.'],
  differenceStatus: null
};

describe('CashBalanceStep4Service', () => {
  let service: CashBalanceStep4Service;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), CashBalanceStep4Service]
    });
    service = TestBed.inject(CashBalanceStep4Service);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads the cash-balance table from the backend', () => {
    service.getCashBalance('2026-08-05').subscribe(result => expect(result).toEqual(response));

    const request = httpMock.expectOne('http://localhost:8080/api/v1/sales/cash-balance/2026-08-05');
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('saves the Dominus amount and returns the recalculated table', () => {
    service.saveDominusBalance('2026-08-05', 11_500_000).subscribe();

    const request = httpMock.expectOne('http://localhost:8080/api/v1/sales/cash-balance/2026-08-05/dominus');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ accumulatedBalance: 11_500_000 });
    request.flush(response);
  });

  it('registers a savings-fund payment', () => {
    service
      .registerFundPayment({ date: '2026-08-05', amount: 500_000, description: 'Arriendo' })
      .subscribe();

    const request = httpMock.expectOne('http://localhost:8080/api/v1/sales/savings-fund/payments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      date: '2026-08-05',
      amount: 500_000,
      description: 'Arriendo'
    });
    request.flush({ id: 1, type: 'PAYMENT', date: '2026-08-05', amount: 500_000, description: 'Arriendo' });
  });

  it('loads the fund balance for the requested date', () => {
    service.getFundBalance('2026-08-05').subscribe(result => expect(result.balance).toBe(4_000_000));

    const request = httpMock.expectOne(
      request => request.url === 'http://localhost:8080/api/v1/sales/savings-fund/balance'
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('date')).toBe('2026-08-05');
    request.flush({ date: '2026-08-05', balance: 4_000_000 });
  });
});
