import { Component, computed, inject, signal } from '@angular/core';
import { formatStatistics, weekday } from '../../../shared/helpers/sales-statistics.helpers';
import { basketSellers } from '../../../shared/helpers/statistics-contracts.helpers';
import { StatisticsChartSeries, StatisticsTab } from '../../../shared/models/dtos/sales-statistics.dto';
import { StatisticsWorkspace } from '../statistics-workspace';
import { ShiftAveragesComponent } from '../components/shift-averages.component';
import { ScenariosComponent } from '../components/scenarios.component';
import { BasketStatisticsComponent } from '../components/basket-statistics.component';
import { CompetitionStatisticsComponent } from '../components/competition-statistics.component';
import { StatisticsDialogComponent } from '../components/statistics-dialog.component';
import { StatisticsChartComponent } from '../components/statistics-chart.component';
import { ObservationsEditorComponent } from '../components/observations-editor.component';
import { StatisticsBalanceComponent } from '../components/statistics-balance.component';

@Component({ selector: 'app-statistics-page', standalone: true,
  imports: [ShiftAveragesComponent, ScenariosComponent, BasketStatisticsComponent, CompetitionStatisticsComponent,
    StatisticsDialogComponent, StatisticsChartComponent, ObservationsEditorComponent, StatisticsBalanceComponent],
  providers: [StatisticsWorkspace], templateUrl: './statistics-page.component.html', styleUrls: ['../statistics.css', '../statistics-visuals.css']
})
export class StatisticsPageComponent {
  readonly workspace = inject(StatisticsWorkspace);
  readonly format = formatStatistics;
  readonly tab = signal<StatisticsTab>('promedios');
  readonly tabs: { id: StatisticsTab; label: string }[] = [
    { id: 'promedios', label: 'Promedios por turno' }, { id: 'escenarios', label: 'Escenarios' },
    { id: 'canastas', label: 'Lubricantes y otros' }, { id: 'competencia', label: 'Competencia' }
  ];
  readonly dialog = signal<'charts' | 'balance' | 'observations' | null>(null);
  readonly chartKind = signal<'bars' | 'lines'>('bars');
  readonly chartUnit = computed(() => this.tab() === 'competencia' ? 'COP / galón' : this.tab() === 'canastas' ? 'COP' : this.workspace.unit());
  readonly chartSource = computed(() => this.tab() === 'competencia' ? 'Sondeos registrados y precio vigente Bellavista' : this.tab() === 'canastas' ? 'Ventas históricas reales' : 'Observaciones proporcionadas por el cliente');
  readonly day = computed(() => this.workspace.averages()?.series.find(s => s.journey === 'DAY')?.average ?? null);
  readonly night = computed(() => this.workspace.averages()?.series.find(s => s.journey === 'NIGHT')?.average ?? null);
  readonly analysis = computed(() => this.tab() === 'promedios' ? this.workspace.averages() : this.tab() === 'escenarios' ? this.workspace.comparison() : null);
  readonly chartTitle = computed(() => `Visor Analítico · ${this.tabs.find(tab => tab.id === this.tab())?.label ?? 'Estadísticas'}`);
  readonly chartSeries = computed<StatisticsChartSeries[]>(() => {
    if (this.tab() === 'competencia') return this.chartKind() === 'bars' ? this.workspace.competitionBars() : this.workspace.competitionLines();
    if (this.tab() === 'canastas') {
      const data = this.workspace.basket(); if (!data || data.charts.availability !== 'AVAILABLE') return [];
      return this.chartKind() === 'bars' ? basketSellers(data).map((s, index) => ({ label: s.name, color: index % 2 ? '#0B192C' : '#0284C7', points: [{ label: s.name, value: s.amount }] }))
        : data.charts.series.filter(s => s.chartType === 'LINE' && s.metric === 'RECORDED_AMOUNT').map((s, index) => ({ label: s.label, color: index % 2 ? '#0B192C' : '#0284C7', points: s.points.map(p => ({ label: p.periodStart ?? p.key, value: p.value })) }));
    }
    const analysis = this.analysis();
    if (!analysis) return [];
    return analysis.series.filter(row => row.available && row.average !== null).map((row, index) => ({
      label: row.label, color: index === 0 ? '#0284C7' : '#0B192C',
      points: this.chartKind() === 'bars' ? [{ label: row.label, value: row.average! }]
        : this.workspace.selected().filter(point => point.journey === row.journey && (row.dayOfWeek === null || weekday(point.date) === row.dayOfWeek))
          .sort((a, b) => a.date.localeCompare(b.date)).map(point => ({ label: point.date, value: point.value }))
    }));
  });
  selectTab(tab: StatisticsTab, event: Event): void {
    this.tab.set(tab);
    (event.currentTarget as HTMLElement).scrollIntoView?.({ block: 'nearest', inline: 'nearest' });
  }
  tabKey(event: KeyboardEvent, index: number): void {
    const key = event.key;
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(key)) return;
    event.preventDefault();
    const next = key === 'Home' ? 0 : key === 'End' ? this.tabs.length - 1 : (index + (key === 'ArrowRight' ? 1 : -1) + this.tabs.length) % this.tabs.length;
    this.tab.set(this.tabs[next].id);
    (event.target as HTMLElement).parentElement?.querySelectorAll<HTMLButtonElement>('[role=tab]')[next].focus();
  }
}
