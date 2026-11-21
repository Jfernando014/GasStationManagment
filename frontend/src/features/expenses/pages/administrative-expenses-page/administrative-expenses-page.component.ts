import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { ConfirmModalComponent } from '../../../../atomic-design/organisms/confirm-modal/confirm-modal.component';
import { AdministrativeExpensesService } from '../../../../core/services/administrative-expenses.service';
import { JournalService } from '../../../../core/services/journal.service';
import { downloadCashBalancePdf } from '../../../../shared/helpers/cash-balance-step4-pdf';
import { formatCop, todayCalendarDay } from '../../../../shared/helpers/format.helpers';
import { getErrorMessage, getFieldErrors } from '../../../../shared/helpers/problem-details.helpers';
import {
  AdministrativeExpenseDto,
  AdministrativeExpenseListDto
} from '../../../../shared/models/dtos/administrative-expense.dto';
import {
  PETTY_CASH_FUND_COP,
  buildReimbursementPdf
} from '../../helpers/administrative-expenses-pdf';

export type ExpensePeriod = 'Q1' | 'Q2' | 'MES';
type ExpenseField = 'date' | 'name' | 'description' | 'amount';

const TOAST_DURATION_MS = 3200;

/** Gastos administrativos (centro de costo 761 / caja menor): registro por fecha y control quincenal/mensual. */
@Component({
  selector: 'app-administrative-expenses-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ActionButtonDirective, ConfirmModalComponent],
  templateUrl: './administrative-expenses-page.component.html',
  styleUrl: './administrative-expenses-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdministrativeExpensesPageComponent {
  private readonly expensesService = inject(AdministrativeExpensesService);
  private readonly journalService = inject(JournalService, { optional: true });
  private readonly destroyRef = inject(DestroyRef);

  readonly pettyCashFund = PETTY_CASH_FUND_COP;
  readonly formatCop = formatCop;

  // Registro rápido de gasto
  readonly expenseDate = signal(this.initialExpenseDate());
  readonly expenseName = signal('');
  readonly description = signal('');
  readonly amountText = signal('');
  readonly documentNumber = signal('');
  readonly submitted = signal(false);
  readonly submitting = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});

  // Filtro y resultados del periodo
  readonly month = signal('2026-08');
  readonly period = signal<ExpensePeriod>('Q1');
  readonly expenses = signal<AdministrativeExpenseDto[]>([]);
  readonly total = signal(0);
  readonly loading = signal(true);
  readonly errorMessage = signal('');

  // Eliminación con confirmación modal y descarga de reembolso
  readonly deleteTarget = signal<AdministrativeExpenseDto | null>(null);
  readonly deleting = signal(false);
  readonly generatingPdf = signal(false);
  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;

  readonly sortedExpenses = computed(() =>
    [...this.expenses()].sort((a, b) => a.date.localeCompare(b.date) || a.id - b.id)
  );
  readonly pettyCashAvailable = computed(() => Math.max(this.pettyCashFund - this.total(), 0));
  readonly averageExpense = computed(() => {
    const count = this.expenses().length;
    return count > 0 ? Math.round(this.total() / count) : null;
  });
  readonly averageExpenseDisplay = computed(() => {
    const avg = this.averageExpense();
    return avg !== null ? formatCop(avg) : '—';
  });

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.loadExpenses();
  }

  onMonthChange(value: string): void {
    if (!value) return;
    this.month.set(value);
    this.loadExpenses();
  }

  onPeriodChange(value: ExpensePeriod): void {
    this.period.set(value);
    this.loadExpenses();
  }

  updateAmount(value: string | number | null): void {
    this.amountText.set(value === null || value === undefined ? '' : String(value));
    this.clearErrorFor('amount');
  }

  updateName(value: string): void {
    this.expenseName.set(value);
    this.clearErrorFor('name');
  }

  updateDescription(value: string): void {
    this.description.set(value);
    this.clearErrorFor('description');
  }

  updateDate(value: string): void {
    this.expenseDate.set(value);
    this.clearErrorFor('date');
  }

  isFieldInvalid(field: ExpenseField): boolean {
    if (this.fieldErrors()[field]) return true;
    if (!this.submitted()) return false;
    if (field === 'date') return !this.expenseDate().trim();
    if (field === 'name') return !this.expenseName().trim();
    if (field === 'description') return !this.description().trim();
    const numeric = this.parsedAmount();
    return numeric === null || numeric <= 0;
  }

  fieldErrorMessage(field: ExpenseField): string {
    const serverError = this.fieldErrors()[field];
    if (serverError) return serverError;
    if (field === 'amount') return 'Digita un valor mayor a cero.';
    return 'Este campo es obligatorio.';
  }

  loadExpenses(): void {
    const currentMonth = this.month();
    if (!/^\d{4}-\d{2}$/.test(currentMonth)) {
      this.errorMessage.set('Selecciona un mes válido.');
      return;
    }

    const [yearText, monthText] = currentMonth.split('-');
    const year = Number(yearText);
    const monthNumber = Number(monthText);
    const selectedPeriod = this.period();

    this.loading.set(true);
    this.errorMessage.set('');

    const request$ = selectedPeriod === 'MES'
      ? this.expensesService.listByDateRange(
        `${currentMonth}-01`,
        `${currentMonth}-${String(new Date(year, monthNumber, 0).getDate()).padStart(2, '0')}`
      )
      : this.expensesService.listByFortnight(year, monthNumber, selectedPeriod === 'Q1' ? 1 : 2);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (response: AdministrativeExpenseListDto) => {
        this.expenses.set(response.expenses ?? []);
        this.total.set(Number(response.total ?? 0));
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  submitExpense(): void {
    this.submitted.set(true);
    this.errorMessage.set('');
    this.fieldErrors.set({});

    const date = this.expenseDate().trim();
    const name = this.expenseName().trim();
    const concept = this.description().trim();
    const numericAmount = this.parsedAmount();

    if (!date || !name || !concept || numericAmount === null || numericAmount <= 0) {
      this.errorMessage.set('Completa la fecha, el nombre, el concepto y un valor mayor a cero.');
      return;
    }

    const doc = this.documentNumber().trim();
    this.submitting.set(true);

    this.expensesService.register({
      date,
      amount: numericAmount,
      name,
      documentNumber: doc || null,
      description: concept
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitted.set(false);
        this.expenseName.set('');
        this.description.set('');
        this.amountText.set('');
        this.documentNumber.set('');

        // Move table filter to the expense period so it is immediately visible after creation.
        this.month.set(date.slice(0, 7));
        if (this.period() !== 'MES') {
          this.period.set(Number(date.slice(8, 10)) <= 15 ? 'Q1' : 'Q2');
        }
        this.loadExpenses();
        this.showToast(`Gasto de ${formatCop(numericAmount)} registrado. Aparece en el paso 4b del ${this.shortDayMonth(date)}.`);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const mapped = getFieldErrors(error).reduce<Record<string, string>>((acc, item) => {
          acc[item.field] = item.message;
          return acc;
        }, {});
        if (Object.keys(mapped).length > 0) {
          this.fieldErrors.set(mapped);
        } else {
          this.errorMessage.set(getErrorMessage(error));
        }
      }
    });
  }

  requestDelete(expense: AdministrativeExpenseDto): void {
    this.errorMessage.set('');
    this.deleteTarget.set(expense);
  }

  cancelDelete(): void {
    if (!this.deleting()) {
      this.deleteTarget.set(null);
    }
  }

  deleteModalMessage(): string {
    const target = this.deleteTarget();
    if (!target) return '';
    const supplier = target.name?.trim() || 'Gasto administrativo';
    return `¿Deseas eliminar el gasto «${supplier} — ${target.description}» por ${formatCop(target.amount)}?`;
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;
    this.deleting.set(true);
    this.expensesService.delete(target.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.loadExpenses();
        this.showToast('Gasto eliminado correctamente.');
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.errorMessage.set(getErrorMessage(error));
      }
    });
  }

  async exportReimbursement(): Promise<void> {
    if (this.generatingPdf() || this.sortedExpenses().length === 0) return;
    this.generatingPdf.set(true);
    try {
      const selectedPeriod = this.period();
      const periodLabel = selectedPeriod === 'Q1'
        ? 'Quincena 1 (1 al 15)'
        : selectedPeriod === 'Q2'
          ? 'Quincena 2 (16 al fin de mes)'
          : 'Mes completo';
      const bytes = await buildReimbursementPdf({
        month: this.month(),
        periodLabel,
        isFortnight: selectedPeriod !== 'MES',
        expenses: this.sortedExpenses(),
        total: this.total()
      });
      downloadCashBalancePdf(bytes, `Planilla-Reembolso-${this.month()}-${selectedPeriod}.pdf`);
      this.showToast('Planilla de reembolso de caja menor generada.');
    } catch {
      this.errorMessage.set('No se pudo generar el PDF de la planilla de reembolso.');
    } finally {
      this.generatingPdf.set(false);
    }
  }

  shortDayMonth(isoDate: string): string {
    const [, month, day] = isoDate.split('-');
    return day && month ? `${day}/${month}` : isoDate;
  }

  private initialExpenseDate(): string {
    const selected = this.journalService?.getSelectedDate?.();
    return selected && /^\d{4}-\d{2}-\d{2}$/.test(selected) ? selected : todayCalendarDay();
  }

  private parsedAmount(): number | null {
    const raw = this.amountText().trim().replace(',', '.');
    if (!raw) return null;
    const value = Number(raw);
    return Number.isFinite(value) ? value : null;
  }

  private clearErrorFor(field: ExpenseField): void {
    this.errorMessage.set('');
    if (this.fieldErrors()[field]) {
      this.fieldErrors.update(current => {
        const next = { ...current };
        delete next[field];
        return next;
      });
    }
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
