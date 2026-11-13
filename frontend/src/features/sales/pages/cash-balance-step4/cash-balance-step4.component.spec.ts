import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { CashBalanceStep4Component } from './cash-balance-step4.component';
import { CashBalanceStep4Service } from '../../../../core/services/cash-balance-step4.service';
import { JournalService } from '../../../../core/services/journal.service';
import { JournalResponse, JournalStatus, StepStatus } from '../../../../shared/models/journal.model';
import { CashBalanceStepResponseDto } from '../../../../shared/models/dtos/cash-balance-step4.dto';

describe('CashBalanceStep4Component', () => {
  let fixture: ComponentFixture<CashBalanceStep4Component>;
  let journalSubject: BehaviorSubject<JournalResponse | null>;
  let service: {
    getCashBalance: ReturnType<typeof vi.fn>;
    saveDominusBalance: ReturnType<typeof vi.fn>;
    registerFundPayment: ReturnType<typeof vi.fn>;
    getFundBalance: ReturnType<typeof vi.fn>;
  };
  let journalService: {
    currentJournal$: BehaviorSubject<JournalResponse | null>;
    completeStep: ReturnType<typeof vi.fn>;
    updateCurrentStep: ReturnType<typeof vi.fn>;
  };

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
      expenses: {
        items: [{ date: '2026-08-05', description: 'Compra de insumos', amount: 25_000 }],
        total: 25_000
      },
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

  const openJournal: JournalResponse = {
    id: 5,
    date: '2026-08-05',
    consecutive: 293,
    status: JournalStatus.OPEN,
    steps: [
      { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: null },
      { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.COMPLETED, completedAt: null },
      { id: 3, stepNumber: 3, name: 'Electrónicos', status: StepStatus.COMPLETED, completedAt: null },
      { id: 4, stepNumber: 4, name: 'Caja', status: StepStatus.PENDING, completedAt: null },
      { id: 5, stepNumber: 5, name: 'Inventario', status: StepStatus.PENDING, completedAt: null },
      { id: 6, stepNumber: 6, name: 'Cierre', status: StepStatus.PENDING, completedAt: null }
    ]
  };

  beforeEach(async () => {
    journalSubject = new BehaviorSubject<JournalResponse | null>(openJournal);
    journalService = {
      currentJournal$: journalSubject,
      completeStep: vi.fn().mockReturnValue(
        of({ id: 4, stepNumber: 4, name: 'Caja', status: StepStatus.COMPLETED, completedAt: '2026-08-05T12:00:00Z' })
      ),
      updateCurrentStep: vi.fn()
    };
    service = {
      getCashBalance: vi.fn().mockReturnValue(of(response)),
      saveDominusBalance: vi.fn().mockReturnValue(
        of({
          ...response,
          table: { ...response.table, dominusAccumulatedBalance: 11_500_000, difference: 0 },
          canComplete: true,
          missing: [],
          differenceStatus: 'CUADRA'
        })
      ),
      registerFundPayment: vi.fn().mockReturnValue(of({ id: 1 })),
      getFundBalance: vi.fn().mockReturnValue(of({ date: openJournal.date, balance: 4_000_000 }))
    };

    await TestBed.configureTestingModule({
      imports: [CashBalanceStep4Component],
      providers: [
        provideRouter([]),
        { provide: JournalService, useValue: journalService },
        { provide: CashBalanceStep4Service, useValue: service }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CashBalanceStep4Component);
    fixture.detectChanges();
  });

  it('renders the backend cash balance, fund, expenses, and PDF controls', () => {
    const element: HTMLElement = fixture.nativeElement;

    expect(service.getCashBalance).toHaveBeenCalledWith(openJournal.date);
    expect(element.querySelector('h1')?.textContent).toContain('Consolidado y saldo en caja');
    expect(element.textContent).toContain('Saldo de movimientos de caja');
    expect(element.textContent).toContain('Compra de insumos');
    expect(element.textContent).toContain('Fondo de ahorro para arriendo');
    expect(element.querySelector('#cb-pdf-file')).toBeTruthy();
  });

  it('persists the Dominus amount and shows the returned difference', () => {
    const input = fixture.nativeElement.querySelector('#cb-dominus') as HTMLInputElement;
    input.value = '11.500.000';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    const saveButton = Array.from(buttons).find(
      button => button.textContent?.includes('Guardar saldo')
    ) as HTMLButtonElement;
    saveButton.click();
    fixture.detectChanges();

    expect(service.saveDominusBalance).toHaveBeenCalledWith(openJournal.date, 11_500_000);
    expect(fixture.nativeElement.textContent).toContain('Cuadra');
  });

  it('submits a validated payment and refreshes the backend table', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    const openPayment = Array.from(buttons).find(
      button => button.textContent?.trim() === 'Registrar pago'
    ) as HTMLButtonElement;
    openPayment.click();
    fixture.detectChanges();

    const amount = fixture.nativeElement.querySelector('#cb-pay-amount') as HTMLInputElement;
    amount.value = '500.000';
    amount.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const dialogButtons = fixture.nativeElement.querySelectorAll('.cb-dialog button') as NodeListOf<HTMLButtonElement>;
    const submit = Array.from(dialogButtons).find(
      button => button.textContent?.includes('Registrar pago')
    ) as HTMLButtonElement;
    submit.click();
    fixture.detectChanges();

    expect(service.registerFundPayment).toHaveBeenCalledWith({
      date: openJournal.date,
      amount: 500_000,
      description: 'Arriendo'
    });
    expect(service.getCashBalance).toHaveBeenCalledTimes(2);
  });

  it('disables completion until the backend reports that the step is ready', () => {
    const completeButton = fixture.nativeElement.querySelector('.cb-actions .cb-btn:last-child') as HTMLButtonElement;
    expect(completeButton.disabled).toBe(true);

    journalSubject.next({ ...openJournal, status: JournalStatus.CLOSED });
    fixture.detectChanges();
    const dominusInput = fixture.nativeElement.querySelector('#cb-dominus') as HTMLInputElement;
    expect(dominusInput.disabled).toBe(true);
  });
});
