import { Component, computed, ElementRef, input, signal, viewChild } from '@angular/core';
import { EmptyStateComponent } from '../../../atomic-design/molecules/empty-state/empty-state.component';
import { addExact, chartRatio, difference, divideExact, formatStatistics } from '../../../shared/helpers/sales-statistics.helpers';
import { StatisticsChartSeries } from '../../../shared/models/dtos/sales-statistics.dto';

@Component({ selector: 'app-statistics-chart', standalone: true, imports: [EmptyStateComponent],
  templateUrl: './statistics-chart.component.html', styleUrls: ['../statistics.css', '../statistics-visuals.css'] })
export class StatisticsChartComponent {
  readonly series = input.required<StatisticsChartSeries[]>();
  readonly kind = input<'bars' | 'lines'>('bars');
  readonly unit = input('Gal');
  readonly sourceLabel = input('Observaciones proporcionadas por el cliente');
  readonly format = formatStatistics;
  readonly error = signal('');
  private readonly svg = viewChild<ElementRef<SVGSVGElement>>('svg');
  readonly points = computed(() => this.series().flatMap(series => series.points.map(point => ({ ...point, color: series.color, series: series.label }))));
  readonly labels = computed(() => [...new Set(this.points().map(point => point.label))].sort());
  readonly maximum = computed(() => this.points().reduce((maximum, point) => difference(point.value, maximum).startsWith('-') ? maximum : point.value, '0'));
  readonly minimum = computed(() => this.points().reduce<string | null>((minimum, point) => minimum === null || difference(point.value, minimum).startsWith('-') ? point.value : minimum, null));
  readonly mean = computed(() => this.points().length ? divideExact(this.points().reduce((sum, point) => addExact(sum, point.value), '0'), String(this.points().length)) : null);
  readonly barWidth = computed(() => Math.min(64, 750 / Math.max(1, this.points().length) * .55));
  barX(index: number): number { return 65 + (index + .5) * 770 / Math.max(1, this.points().length); }
  x(label: string): number { return this.labels().length === 1 ? 450 : 65 + this.labels().indexOf(label) * 770 / (this.labels().length - 1); }
  shortLabel(label: string): string { return label.length > 22 ? label.slice(0, 21) + '…' : label; }
  y(value: string): number { return 260 - chartRatio(value, this.maximum()) * 200; }
  line(series: StatisticsChartSeries): string { return series.points.map(point => `${this.x(point.label)},${this.y(point.value)}`).join(' '); }
  async download(): Promise<void> {
    const element = this.svg()?.nativeElement;
    if (!element || !this.points().length) return;
    this.error.set('');
    const kind = this.kind();
    const url = URL.createObjectURL(new Blob([new XMLSerializer().serializeToString(element)], { type: 'image/svg+xml;charset=utf-8' }));
    try {
      const image = new Image(); image.src = url; await image.decode();
      const canvas = document.createElement('canvas'); canvas.width = 1800; canvas.height = 640;
      const context = canvas.getContext('2d'); if (!context) throw new Error('Canvas unavailable');
      context.fillStyle = '#FFFFFF'; context.fillRect(0, 0, 1800, 640); context.drawImage(image, 0, 0, 1800, 640);
      const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob(value => value ? resolve(value) : reject(new Error('PNG unavailable')), 'image/png'));
      const downloadUrl = URL.createObjectURL(blob); const link = document.createElement('a');
      link.href = downloadUrl; link.download = `bellavista-estadisticas-${kind}.png`; link.click();
      setTimeout(() => URL.revokeObjectURL(downloadUrl), 1000);
    } catch { this.error.set('No se pudo descargar la gráfica. Intenta nuevamente.'); }
    finally { URL.revokeObjectURL(url); }
  }
}
