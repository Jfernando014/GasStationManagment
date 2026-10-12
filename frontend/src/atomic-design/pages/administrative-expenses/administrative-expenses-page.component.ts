import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface AdministrativeExpense {
  id: number;
  date: string;
  amount: number;
  description: string;
  createdAt: string;
}

interface ExpenseListResponse {
  expenses: AdministrativeExpense[];
  total: number;
}

@Component({
  selector: 'app-administrative-expenses-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './administrative-expenses-page.component.html',
  styleUrl: './administrative-expenses-page.component.css',
})
export class AdministrativeExpensesPageComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/sales/administrative-expenses';

  expenseDate = this.today();
  amount: number | null = null;
  description = '';
  filterMode: 'fortnight' | 'date-range' = 'fortnight';
  month = this.today().slice(0, 7);
  fortnight = 1;
  dateFrom = `${this.month}-01`;
  dateTo = this.today();
  expenses: AdministrativeExpense[] = [];
  total = 0;
  isLoading = false;
  isSubmitting = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadExpenses();
  }

  submitExpense(): void {
    if (!this.expenseDate || this.amount === null || this.amount <= 0) {
      this.errorMessage = 'Ingresa una fecha válida y un valor mayor que cero.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.http.post<AdministrativeExpense>(this.apiUrl, {
      date: this.expenseDate,
      amount: this.amount,
      description: this.description.trim() || 'Gasto administrativo',
    }).subscribe({
      next: () => {
        this.amount = null;
        this.description = '';
        this.expenseDate = this.today();
        this.isSubmitting = false;
        this.loadExpenses();
      },
      error: () => {
        this.errorMessage = 'No se pudo registrar el gasto.';
        this.isSubmitting = false;
      },
    });
  }

  loadExpenses(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.http.get<ExpenseListResponse>(this.apiUrl, { params: this.buildParams() }).subscribe({
      next: (response) => {
        this.expenses = response.expenses ?? [];
        this.total = Number(response.total ?? 0);
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar los gastos para este filtro.';
        this.isLoading = false;
      },
    });
  }

  deleteExpense(expense: AdministrativeExpense): void {
    if (!window.confirm(`¿Eliminar el gasto “${expense.description}”?`)) {
      return;
    }
    this.http.delete(`${this.apiUrl}/${expense.id}`).subscribe({
      next: () => this.loadExpenses(),
      error: () => this.errorMessage = 'No se pudo eliminar el gasto seleccionado.',
    });
  }

  private buildParams(): HttpParams {
    if (this.filterMode === 'fortnight') {
      const [year, month] = this.month.split('-');
      return new HttpParams()
        .set('year', year)
        .set('month', month)
        .set('fortnight', this.fortnight);
    }
    return new HttpParams().set('fromDate', this.dateFrom).set('toDate', this.dateTo);
  }

  private today(): string {
    const now = new Date();
    const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
    return localDate.toISOString().slice(0, 10);
  }
}