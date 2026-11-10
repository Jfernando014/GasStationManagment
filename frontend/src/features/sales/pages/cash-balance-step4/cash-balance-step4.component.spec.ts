import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { CashBalanceStep4Component } from './cash-balance-step4.component';
import { JournalService } from '../../../../core/services/journal.service';
import { CashBalanceStep4Service } from '../../../../core/services/cash-balance-step4.service';
import { ToastService } from '../../../../core/services/toast.service';
import { JournalResponse, JournalStatus, StepStatus } from '../../../../shared/models/journal.model';

describe('CashBalanceStep4Component', () => {
  let component: CashBalanceStep4Component;
  let fixture: ComponentFixture<CashBalanceStep4Component>;
  let journalSubject: BehaviorSubject<JournalResponse | null>;
  let step4Service: CashBalanceStep4Service;
  let toastService: ToastService;

  const mockOpenJournal: JournalResponse = {
    id: 5,
    date: '2026-08-05',
    consecutive: 293,
    status: JournalStatus.OPEN,
    steps: [
      { id: 1, stepNumber: 1, name: 'Personal del día', status: StepStatus.COMPLETED, completedAt: '2026-08-05T10:00:00Z' },
      { id: 2, stepNumber: 2, name: 'Cierre por vendedor', status: StepStatus.COMPLETED, completedAt: '2026-08-05T10:30:00Z' },
      { id: 3, stepNumber: 3, name: 'Medios electrónicos', status: StepStatus.COMPLETED, completedAt: '2026-08-05T11:00:00Z' },
      { id: 4, stepNumber: 4, name: 'Consolidado y saldo en caja', status: StepStatus.PENDING, completedAt: null },
      { id: 5, stepNumber: 5, name: 'Inventario y tanques', status: StepStatus.PENDING, completedAt: null },
      { id: 6, stepNumber: 6, name: 'Cierre de la jornada', status: StepStatus.PENDING, completedAt: null }
    ]
  };

  beforeEach(async () => {
    journalSubject = new BehaviorSubject<JournalResponse | null>(mockOpenJournal);

    await TestBed.configureTestingModule({
      imports: [CashBalanceStep4Component],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: JournalService,
          useValue: {
            currentJournal$: journalSubject.asObservable(),
            getSelectedDate: () => '2026-08-05',
            completeStep: () => of({ stepNumber: 4, status: StepStatus.COMPLETED, completedAt: '2026-08-05T12:00:00Z' }),
            updateCurrentStep: () => undefined
          }
        }
      ]
    }).compileComponents();

    step4Service = TestBed.inject(CashBalanceStep4Service);
    step4Service.clearAllStoredData();
    toastService = TestBed.inject(ToastService);
    vi.spyOn(step4Service, 'fetchDayContext').mockReturnValue(
      of({ vendorCloses: [], backendExpenses: null, backendStep4: null })
    );

    fixture = TestBed.createComponent(CashBalanceStep4Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    step4Service.clearAllStoredData();
  });

  it('should render Step 4 prototype structure with header, cuadre, cash balance table, savings fund, expenses, and PDF dropzone', () => {
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('.jr-p4')).toBeTruthy();
    expect(el.querySelector('.jr-paso-cabecera h1')?.textContent).toContain('Consolidado y saldo en caja');
    expect(el.querySelector('#jr-p4-consolidado')).toBeTruthy();
    expect(el.querySelector('#jr-p4-tabla-saldo')).toBeTruthy();
    expect(el.querySelectorAll('#jr-p4-tabla-saldo tr').length).toBe(9);
    expect(el.querySelector('#jr-p4-fondo')).toBeTruthy();
    expect(el.querySelector('#jr-p4-gastos')).toBeTruthy();
    expect(el.querySelector('#jr-p4-archivo-pdf')).toBeTruthy();
  });

  it('should update Dominus consolidation and compute cuadre state when input changes', () => {
    const input = fixture.nativeElement.querySelector('#jr-p4-consolidado') as HTMLInputElement;
    input.value = '14.136.817,40';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.calc.consolidado).toBeCloseTo(14136817.4, 2);
    expect(component.calc.cuadre).toBe('cuadra');
    expect(component.consolidadoIndicatorLabel()).toBe('Cuadra');
  });

  it('should open and submit the savings fund payment dialog', () => {
    const origRegister = step4Service.registerFundPayment.bind(step4Service);
    vi.spyOn(step4Service, 'registerFundPayment').mockImplementation((fecha, valor, concepto = 'Arriendo') => {
      origRegister(fecha, valor, concepto);
      return of({ id: 'p1', fecha, valor, concepto });
    });

    const openBtn = fixture.nativeElement.querySelector('#jr-p4-btn-pago-fondo') as HTMLButtonElement;
    openBtn.click();
    fixture.detectChanges();

    expect(component.isPaymentDialogOpen).toBe(true);
    expect(fixture.nativeElement.querySelector('#jr-p4-dialogo-pago')).toBeTruthy();

    component.paymentDate = '2026-08-05';
    component.paymentAmountInput = '500.000';
    component.paymentConcept = 'Arriendo';
    component.savePayment();
    fixture.detectChanges();

    expect(component.isPaymentDialogOpen).toBe(false);
    expect(component.calc.fondo.totalPagosHoy).toBe(500000);
    expect(component.calc.fondo.acumulado).toBe(3500000);
    expect(toastService.message()).toContain('registrado desde el fondo');
  });

  it('should apply read-only mode (.jr-solo-lectura) when the journal is CLOSED', () => {
    journalSubject.next({
      ...mockOpenJournal,
      status: JournalStatus.CLOSED
    });
    fixture.detectChanges();

    const section = fixture.nativeElement.querySelector('.jr-p4') as HTMLElement;
    expect(section.classList.contains('jr-solo-lectura')).toBe(true);
    expect(fixture.nativeElement.querySelector('#jr-p4-btn-pago-fondo')).toBeNull();
  });
});

