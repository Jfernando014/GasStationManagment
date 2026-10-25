import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, firstValueFrom, forkJoin, of } from 'rxjs';
import { SalesStatisticsService } from '../../../core/services/sales-statistics.service';
import { addExact, formatStatistics, localDate, monthRange } from '../../../shared/helpers/sales-statistics.helpers';
import { getErrorMessage } from '../../../shared/helpers/problem-details.helpers';
import { StatisticsWorkspace } from '../statistics-workspace';
import { buildStatisticsBalance } from '../statistics-balance';
import { BasketStatisticsResponse } from '../../../shared/models/dtos/basket-statistics.dto';

@Component({ selector: 'app-statistics-balance', standalone: true, imports: [FormsModule],
  templateUrl: './statistics-balance.component.html', styleUrls: ['../statistics.css', '../statistics-visuals.css'] })
export class StatisticsBalanceComponent {
  readonly workspace = inject(StatisticsWorkspace);
  private readonly api = inject(SalesStatisticsService);
  private readonly destroyRef = inject(DestroyRef);
  readonly month = signal(localDate().slice(0, 7));
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly checking = signal(false);
  readonly basketWarning = signal('');
  private readonly periodBasket = signal<BasketStatisticsResponse | null>(null);
  readonly format = formatStatistics;
  readonly periods = computed(() => {
    const now = new Date();
    return [...new Set([this.month(), ...Array.from({ length: 12 }, (_, index) => localDate(new Date(now.getFullYear(), now.getMonth() - index, 1)).slice(0, 7)),
      ...this.workspace.observations().map(row => row.date.slice(0, 7)), ...(this.workspace.basket() ? [this.workspace.basket()!.from.slice(0, 7)] : [])])].sort().reverse();
  });
  readonly observations = computed(() => this.workspace.observations().filter(row => row.date.startsWith(this.month())));
  readonly realBasket = computed(() => { const data = this.periodBasket(); return data?.from.slice(0, 7) === this.month() ? data : null; });
  readonly available = computed(() => this.observations().length > 0 || this.realBasket()?.charts.availability === 'AVAILABLE');
  readonly total = computed(() => this.observations().length ? this.observations().reduce((sum, row) => addExact(sum, row.value), '0') : null);
  readonly slides = [
    { title: 'Portada & Resumen de Despachos', label: '1. Resumen Ejecutivo', description: 'Metas, totales de galonaje y avance mensual.' },
    { title: 'Turnos Día vs. Noche', label: '2. Análisis Operativo', description: 'Gráficas comparativas de flujo por franjas.' },
    { title: 'Ranking de Vendedores', label: '3. Productividad y Canasta', description: 'Cumplimiento de cuota y venta de lubricantes.' },
    { title: 'Conclusiones y Recomendaciones', label: '4. Dictamen de Gestión', description: 'Plan de aprovisionamiento de tanques y metas.' }
  ];
  constructor() {
    effect(onCleanup => {
      const month = this.month(), current = this.workspace.basket();
      this.basketWarning.set(''); this.periodBasket.set(null);
      if (current?.from.slice(0, 7) === month) { this.periodBasket.set(current); this.checking.set(false); return; }
      const dates = monthRange(month); this.checking.set(true);
      const subscription = this.api.basket(dates.from, dates.to).subscribe({
        next: result => { this.periodBasket.set(result); this.checking.set(false); },
        error: (error: unknown) => { this.basketWarning.set(getErrorMessage(error)); this.checking.set(false); }
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }
  label(month: string): string { return new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric' }).format(new Date(Number(month.slice(0, 4)), Number(month.slice(5)) - 1, 1)); }
  async export(): Promise<void> {
    if (!this.available() || this.loading()) return;
    this.loading.set(true); this.error.set(''); this.success.set('');
    const month = this.month(), dates = monthRange(month);
    const sales = { ...dates, category: this.workspace.category(), outputScale: 2, observations: this.observations().map(row => ({ ...row })) };
    try {
      const result = await firstValueFrom(forkJoin({
        averages: sales.observations.length ? this.api.averages(sales) : of(null),
        basket: this.realBasket() ? of(this.realBasket()) : this.api.basket(dates.from, dates.to).pipe(catchError(() => of(null)))
      }).pipe(takeUntilDestroyed(this.destroyRef)));
      const presentation = await buildStatisticsBalance({ month, sales, ...result });
      await presentation.writeFile({ fileName: 'Balance_Estadisticas_Bellavista_' + month + '.pptx', compression: true });
      this.success.set('Presentación generada con cuatro diapositivas y procedencia explícita de los datos.');
    } catch (error: unknown) { this.error.set(getErrorMessage(error)); }
    finally { this.loading.set(false); }
  }
}
