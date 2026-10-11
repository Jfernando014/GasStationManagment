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

@Component({
  selector: 'app-administrative-expenses-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './administrative-expenses-page.component.html',
  styles: [`
    :host {
      display: block;
      padding: 1.5rem;
      color: #18212f;
    }

    .expenses-shell {
      display: grid;
      grid-template-columns: 360px minmax(0, 1fr);
      gap: 1.5rem;
    }

    .panel {
      background: #ffffff;
      border: 1px solid #e8edf3;
      border-radius: 18px;
      box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
      padding: 1.25rem;
    }

    .panel h2, .panel h3 {
      margin: 0 0 1rem;
      font-size: 1.1rem;
      color: #1f2937;
    }

    .field {
      display: flex;
      flex-direction: column;
      gap: 0.45rem;
      margin-bottom: 1rem;
    }

    .field label {
      font-size: 0.8rem;
      font-weight: 600;
      color: #4b5563;
    }

    .field input,
    .field textarea,
    .field select {
      border: 1px solid #dbe3ee;
      border-radius: 10px;
      padding: 0.75rem 0.9rem;
      font: inherit;
      background: #f8fafc;
    }

    .field textarea {
      min-height: 90px;
      resize: vertical;
    }

    .inline-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
    }

    .primary-btn,
    .secondary-btn,
    .danger-btn {
      border: none;
      border-radius: 10px;
      padding: 0.75rem 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: opacity 0.2s ease;
    }

    .primary-btn {
      background: #1d4ed8;
      color: white;
    }

    .secondary-btn {
      background: #e2e8f0;
      color: #0f172a;
    }

    .danger-btn {
      background: #fef2f2;
      color: #b91c1c;
      border: 1px solid #fecaca;
      padding: 0.5rem 0.75rem;
    }

    .toolbar {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      align-items: end;
      margin-bottom: 1rem;
    }

    .toolbar .field {
      margin-bottom: 0;
      min-width: 140px;
      flex: 1;
    }

    .totals {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.8rem;
      margin-bottom: 1rem;
    }

    .metric {
      background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
      border: 1px solid #dbeafe;
      border-radius: 12px;
      padding: 0.9rem;
    }

    .metric-label {
      display: block;
      font-size: 0.72rem;
      color: #64748b;
      margin-bottom: 0.35rem;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .metric-value {
      font-size: 1.35rem;
      font-weight: 700;
      color: #0f172a;
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    th, td {
      padding: 0.8rem 0.6rem;
      border-bottom: 1px solid #edf2f7;
      text-align: left;
      font-size: 0.92rem;
    }

    th {
      font-size: 0.74rem;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: #64748b;
      background: #f8fafc;
    }

    .amount {
      font-weight: 700;
      color: #0f172a;
      text-align: right;
      white-space: nowrap;
    }

    .empty-state {
      padding: 1.2rem;
      border: 1px dashed #dbe3ee;
      border-radius: 12px;
      color: #64748b;
      background: #f8fafc;
      text-align: center;
    }

    .alert {
      margin-bottom: 1rem;
      padding: 0.85rem 1rem;
      border-radius: 10px;
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #991b1b;
      font-weight: 600;
    }

    @media (max-width: 980px) {
      .expenses-shell {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class AdministrativeExpensesPageComponent implements OnInit {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/v1/sales/administrative-expenses';

  expenseDate = this.today();
  amount = 0;
  description = '';
  filterMode: 'fortnight' | 'date-range' = 'fortnight';
  month = '2026-08';
  fortnight = 1;
  dateFrom = '2026-08-01';
  dateTo = '2026-08-15';
  expenses: AdministrativeExpense[] = [];
  total = 0;
  isLoading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadExpenses();
  }

  submitExpense(): void {
    if (!this.expenseDate || !this.amount || this.amount <= 0) {
      this.errorMessage = 'El gasto debe tener una fecha válida y un valor mayor a cero.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.http.post<AdministrativeExpense>(this.apiUrl, {
      date: this.expenseDate,
      amount: this.amount,
      description: this.description?.trim() || 'Gasto administrativo'
    }).subscribe({
      next: () => {
        this.amount = 0;
        this.description = '';
        this.expenseDate = this.today();
        this.loadExpenses();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo registrar el gasto. Verifica los datos del formulario.';
        this.isLoading = false;
      }
    });
  }

  loadExpenses(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const params = this.buildParams();
    this.http.get<AdministrativeExpense[]>(this.apiUrl, { params }).subscribe({
      next: (items) => {
        this.expenses = items ?? [];
        this.total = this.expenses.reduce((sum, item) => sum + Number(item.amount || 0), 0);
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar los gastos. Revisa el filtro o el backend.';
        this.isLoading = false;
      }
    });
  }

  deleteExpense(id: number): void {
    this.http.delete(`${this.apiUrl}/${id}`).subscribe({
      next: () => this.loadExpenses(),
      error: () => this.errorMessage = 'No se pudo eliminar el gasto seleccionado.'
    });
  }

  private buildParams(): HttpParams {
    let params = new HttpParams();

    if (this.filterMode === 'fortnight') {
      params = params.set('year', this.month.slice(0, 4));
      params = params.set('month', this.month.slice(5, 7));
      params = params.set('fortnight', String(this.fortnight));
    } else {
      params = params.set('fromDate', this.dateFrom);
      params = params.set('toDate', this.dateTo);
    }

    return params;
  }

  private today(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
