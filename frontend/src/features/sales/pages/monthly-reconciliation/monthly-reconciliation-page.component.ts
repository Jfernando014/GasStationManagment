import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, map, switchMap, tap } from 'rxjs';
import { MonthlyReconciliationService } from '../../../../core/services/monthly-reconciliation.service';
import { ApiError } from '../../../../shared/models/api-error.model';
import { MonthlyReconciliation } from '../../../../shared/models/dtos/monthly-reconciliation.dto';

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

@Component({
  selector: 'app-monthly-reconciliation-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './monthly-reconciliation-page.component.html',
  styleUrl: './monthly-reconciliation-page.component.css'
})
export class MonthlyReconciliationPageComponent {
  private readonly service = inject(MonthlyReconciliationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<string>();

  readonly month = signal('2026-08');
  readonly data = signal<MonthlyReconciliation | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly currency = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 2
  });

  constructor() {
    this.requests.pipe(
      tap(() => {
        this.loading.set(true);
        this.error.set(null);
        this.data.set(null);
      }),
      switchMap(month => this.service.getMonthly(month).pipe(
        map(result => ({ result })),
        catchError((error: unknown) => {
          this.error.set(this.errorMessage(error));
          this.loading.set(false);
          return EMPTY;
        })
      )),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(({ result }) => {
      this.data.set(result);
      this.loading.set(false);
    });

    this.consult();
  }

  consult(): void {
    if (!MONTH_PATTERN.test(this.month())) {
      this.data.set(null);
      this.error.set('Selecciona un mes válido para consultar las conciliaciones.');
      return;
    }
    this.requests.next(this.month());
  }

  moveMonth(offset: number): void {
    if (!MONTH_PATTERN.test(this.month())) return;
    const [year, month] = this.month().split('-').map(Number);
    const date = new Date(Date.UTC(year, month - 1 + offset, 1));
    this.month.set(`${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}`);
    this.consult();
  }

  formatCurrency(amount: number): string {
    return this.currency.format(amount);
  }

  formatDay(date: string): string {
    const formatted = new Intl.DateTimeFormat('es-CO', {
      weekday: 'short',
      day: '2-digit',
      month: 'short',
      timeZone: 'UTC'
    }).format(new Date(`${date}T00:00:00Z`));
    return formatted.charAt(0).toLocaleUpperCase('es-CO') + formatted.slice(1);
  }

  monthLabel(month: string | null = this.data()?.month ?? this.month()): string {
    if (!month || !MONTH_PATTERN.test(month)) return 'Mes seleccionado';
    const [year, monthNumber] = month.split('-').map(Number);
    const label = new Intl.DateTimeFormat('es-CO', {
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(Date.UTC(year, monthNumber - 1, 1)));
    return label.charAt(0).toLocaleUpperCase('es-CO') + label.slice(1);
  }

  private errorMessage(error: unknown): string {
    if (error instanceof ApiError) {
      if (error.status === 401) return 'Tu sesión venció. Inicia sesión nuevamente para consultar.';
      if (error.status === 403) return 'No tienes permiso para consultar las conciliaciones del mes.';
      if (error.detail) return error.detail;
    }
    return 'No fue posible consultar las conciliaciones. Verifica la conexión e inténtalo de nuevo.';
  }
}
