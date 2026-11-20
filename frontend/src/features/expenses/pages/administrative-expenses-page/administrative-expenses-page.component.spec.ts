import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AdministrativeExpensesService } from '../../../../core/services/administrative-expenses.service';
import { JournalService } from '../../../../core/services/journal.service';
import { ApiError } from '../../../../shared/models/api-error.model';
import { AdministrativeExpenseDto } from '../../../../shared/models/dtos/administrative-expense.dto';
import { AdministrativeExpensesPageComponent } from './administrative-expenses-page.component';

const SEEDED_Q1: AdministrativeExpenseDto[] = [
  {
    id: 1,
    date: '2026-08-01',
    amount: 6000,
    name: 'Ofimatic@.NET',
    documentNumber: '25275622-2',
    description: '20 IMPRESIONES B/N',
    createdAt: '2026-08-01T10:00:00Z'
  },
  {
    id: 2,
    date: '2026-08-01',
    amount: 18350,
    name: 'Servientrega S.A',
    documentNumber: '860512330-3',
    description: 'Envío documentos Bogotá',
    createdAt: '2026-08-01T12:00:00Z'
  }
];

describe('AdministrativeExpensesPageComponent', () => {
  let expensesService: {
    listByFortnight: ReturnType<typeof vi.fn>;
    listByDateRange: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    expensesService = {
      listByFortnight: vi.fn(() => of({ expenses: SEEDED_Q1, total: 24350 })),
      listByDateRange: vi.fn(() => of({ expenses: SEEDED_Q1, total: 24350 })),
      register: vi.fn((req: unknown) => of({
        id: 99,
        createdAt: '2026-08-18T10:00:00Z',
        ...(req as object)
      })),
      delete: vi.fn(() => of(void 0))
    };

    await TestBed.configureTestingModule({
      imports: [AdministrativeExpensesPageComponent],
      providers: [
        { provide: AdministrativeExpensesService, useValue: expensesService },
        { provide: JournalService, useValue: { getSelectedDate: () => '2026-11-20' } }
      ]
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(AdministrativeExpensesPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('loads the fortnight expenses and computes petty cash available dynamically', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    const text = fixture.nativeElement.textContent as string;

    expect(expensesService.listByFortnight).toHaveBeenCalledWith(2026, 8, 1);
    expect(component.expenseDate()).toBe('2026-11-20');
    expect(component.pettyCashAvailable()).toBe(500_000 - 24_350);
    expect(text).toContain('Ofimatic@.NET');
    expect(text).toContain('25275622-2');
    expect(text).toContain('475.650');
  });

  it('switches to full month view with date range query and average expense indicator', () => {
    expensesService.listByDateRange.mockReturnValue(of({ expenses: SEEDED_Q1, total: 100_000 }));
    const fixture = create();
    const component = fixture.componentInstance;

    component.onPeriodChange('MES');
    fixture.detectChanges();

    expect(expensesService.listByDateRange).toHaveBeenCalledWith('2026-08-01', '2026-08-31');
    expect(component.averageExpense()).toBe(50_000);
    expect(fixture.nativeElement.textContent).toContain('Total del mes');
    expect(fixture.nativeElement.textContent).toContain('Promedio por gasto');
  });

  it('validates required name, description, and positive amount including numeric input events', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.submitExpense();
    expect(component.isFieldInvalid('name')).toBe(true);
    expect(component.isFieldInvalid('description')).toBe(true);
    expect(component.isFieldInvalid('amount')).toBe(true);
    expect(expensesService.register).not.toHaveBeenCalled();

    component.updateDate('2026-08-18');
    component.updateName('Casa Yale');
    component.updateDescription('Arreglo chapa');
    component.updateAmount(20000 as unknown as string);
    component.documentNumber.set('10536131-4');
    component.submitExpense();

    expect(expensesService.register).toHaveBeenCalledWith({
      date: '2026-08-18',
      amount: 20000,
      name: 'Casa Yale',
      documentNumber: '10536131-4',
      description: 'Arreglo chapa'
    });
    expect(component.period()).toBe('Q2');
    expect(component.toastText()).toContain('paso 4b del 18/08');
  });

  it('deletes an expense after confirming in the blurred modal and surfaces server errors cleanly', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.requestDelete(SEEDED_Q1[0]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Eliminar gasto administrativo');

    component.confirmDelete();
    expect(expensesService.delete).toHaveBeenCalledWith(1);
    expect(component.toastText()).toContain('eliminado');

    expensesService.delete.mockReturnValue(throwError(() => new ApiError(404, { detail: 'No encontrado' })));
    component.requestDelete(SEEDED_Q1[1]);
    component.confirmDelete();
    expect(component.errorMessage()).toContain('No encontrado');
  });
});
