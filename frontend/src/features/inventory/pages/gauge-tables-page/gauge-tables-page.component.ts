import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { GaugeTablesService } from '../../../../core/services/gauge-tables.service';
import { getErrorMessage, getFieldErrors, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { GaugeTableRequest, GaugeTableResponse } from '../../../../shared/models/dtos/gauge-table.dto';

interface CalibrationPoint {
  height: number;
  gallons: number;
}

const TOAST_DURATION_MS = 3000;

/** Configuración › Tabla de aforo: calibration points of every tank model. */
@Component({
  selector: 'app-gauge-tables-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './gauge-tables-page.component.html',
  styleUrl: './gauge-tables-page.component.css'
})
export class GaugeTablesPageComponent {
  private readonly gaugeTablesService = inject(GaugeTablesService);
  private readonly destroyRef = inject(DestroyRef);
  @ViewChild('csvInput') private csvInput?: ElementRef<HTMLInputElement>;

  readonly gaugeTables = signal<GaugeTableResponse[]>([]);
  readonly gaugeTableLoading = signal(true);
  readonly selectedGaugeTableId = signal<number | null>(null);
  readonly newGaugeTableOpen = signal(false);
  readonly gaugeTableSubmitted = signal(false);
  readonly newGaugeTable = signal({ name: '' });
  readonly gaugeDraftPoints = signal<CalibrationPoint[]>([
    { height: 0, gallons: 0 },
    { height: 1, gallons: 1 }
  ]);
  readonly curves = signal<Record<string, CalibrationPoint[]>>({});
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly error = signal('');
  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.loadGaugeTables();
  }

  get selectedTable(): GaugeTableResponse | undefined {
    return this.gaugeTables().find(table => table.id === this.selectedGaugeTableId());
  }

  get selectedCurve(): CalibrationPoint[] {
    const id = this.selectedGaugeTableId();
    return id === null ? [] : this.curves()[String(id)] ?? [];
  }

  private loadGaugeTables(): void {
    this.gaugeTableLoading.set(true);
    this.gaugeTablesService.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: tables => {
        this.gaugeTables.set(tables);
        if (tables.length > 0 && this.selectedGaugeTableId() === null) {
          this.selectedGaugeTableId.set(tables[0].id);
          this.setCurveFromTable(tables[0]);
        }
        this.gaugeTableLoading.set(false);
      },
      error: error => {
        this.gaugeTableLoading.set(false);
        this.setError(getErrorMessage(error));
      }
    });
  }

  private setCurveFromTable(table: GaugeTableResponse): void {
    this.curves.update(curves => ({
      ...curves,
      [String(table.id)]: table.points.map(point => ({ height: point.heightCm, gallons: point.volumeGallons }))
    }));
  }

  selectGaugeTable(id: string): void {
    const tableId = Number(id);
    const table = this.gaugeTables().find(item => item.id === tableId);
    this.selectedGaugeTableId.set(tableId);
    if (table) this.setCurveFromTable(table);
    this.clearFeedback();
  }

  updateNewGaugeTableName(name: string): void {
    this.newGaugeTable.set({ name });
  }

  isRequiredTextInvalid(value: string): boolean {
    return this.gaugeTableSubmitted() && !value.trim();
  }

  fieldError(field: string): string {
    return this.serverFieldErrors()[field] ?? '';
  }

  isPointInvalid(point: CalibrationPoint): boolean {
    return this.gaugeTableSubmitted() && (!Number.isFinite(point.height) || !Number.isFinite(point.gallons) ||
      point.height < 0 || point.gallons < 0);
  }

  createGaugeTable(): void {
    this.clearServerFieldErrors();
    this.gaugeTableSubmitted.set(true);
    const name = this.newGaugeTable().name.trim();
    const points = this.gaugeDraftPoints().map(point => ({ heightCm: point.height, volumeGallons: point.gallons }));
    if (!name || points.length < 2) {
      this.setError('La tabla debe tener un nombre y al menos dos puntos.');
      return;
    }
    const request: GaugeTableRequest = { name, points };
    this.gaugeTablesService.create(request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: table => {
        this.gaugeTables.update(tables => [...tables, table]);
        this.selectedGaugeTableId.set(table.id);
        this.setCurveFromTable(table);
        this.newGaugeTable.set({ name: '' });
        this.gaugeDraftPoints.set([{ height: 0, gallons: 0 }, { height: 1, gallons: 1 }]);
        this.newGaugeTableOpen.set(false);
        this.success('Tabla de aforo guardada correctamente.');
      },
      error: error => this.setServerError(error, 'name')
    });
  }

  addPoint(): void {
    const curve = this.selectedCurve;
    const last = curve[curve.length - 1] ?? { height: 0, gallons: 0 };
    this.curves.update(curves => ({
      ...curves,
      [String(this.selectedGaugeTableId())]: [...curve, { height: Number((last.height + 1).toFixed(1)), gallons: last.gallons + 15 }]
    }));
  }

  deletePoint(index: number): void {
    if (this.selectedCurve.length <= 2) {
      this.setError('La tabla de aforo requiere un mínimo de 2 puntos.');
      return;
    }
    this.curves.update(curves => ({
      ...curves,
      [String(this.selectedGaugeTableId())]: this.selectedCurve.filter((_, pointIndex) => pointIndex !== index)
    }));
  }

  updatePoint(index: number, field: keyof CalibrationPoint, value: string): void {
    const points = this.selectedCurve.map((point, pointIndex) =>
      pointIndex === index ? { ...point, [field]: Number(value) } : point
    );
    this.curves.update(curves => ({ ...curves, [String(this.selectedGaugeTableId())]: points }));
  }

  saveCurve(): void {
    this.clearServerFieldErrors();
    this.gaugeTableSubmitted.set(true);
    const curve = this.selectedCurve;
    const invalid = curve.length < 2 || curve.some(point => point.height < 0 || point.gallons < 0) ||
      curve.some((point, index) => index > 0 && (point.height <= curve[index - 1].height || point.gallons < curve[index - 1].gallons));
    if (invalid) {
      this.setError('Los puntos deben tener altura ascendente y volumen no decreciente; no se permiten valores negativos.');
      return;
    }
    const tableId = this.selectedGaugeTableId();
    if (tableId === null) {
      this.setError('Selecciona una tabla de aforo.');
      return;
    }
    const request: GaugeTableRequest = {
      name: this.selectedTable?.name ?? '',
      points: curve.map(point => ({ heightCm: point.height, volumeGallons: point.gallons }))
    };
    this.gaugeTablesService.update(tableId, request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: table => {
        this.gaugeTables.update(tables => tables.map(item => item.id === table.id ? table : item));
        this.setCurveFromTable(table);
        this.success('Tabla de aforo actualizada correctamente.');
      },
      error: error => this.setServerError(error, 'name')
    });
  }

  importCsv(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const points = String(reader.result).split(/\r?\n/).map(line => line.trim()).filter(Boolean).slice(1)
        .map(line => line.split(/[,;\t]/).map(value => Number(value.replace(',', '.'))))
        .filter(values => values.length >= 2 && values.every(value => Number.isFinite(value)))
        .map(([height, gallons]) => ({ height, gallons }));
      if (points.length < 2) {
        this.setError('El archivo debe contener al menos dos pares de altura y volumen.');
      } else {
        this.curves.update(curves => ({ ...curves, [String(this.selectedGaugeTableId())]: points }));
        this.success(`${points.length} puntos de aforo importados. Valida y guarda los cambios.`);
      }
      input.value = '';
    };
    reader.readAsText(file);
  }

  exportCsv(): void {
    const table = this.selectedTable;
    const capacity = table?.maxVolumeGallons ?? 0;
    const csv = ['Altura_cm,Volumen_Galones,%_Llenado', ...this.selectedCurve.map(point =>
      `${point.height},${point.gallons},${capacity > 0 ? ((point.gallons / capacity) * 100).toFixed(2) : '0'}%`
    )].join('\n');
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    link.download = `Aforo_${table?.name ?? this.selectedGaugeTableId()}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
    this.success('Archivo CSV exportado correctamente.');
  }

  openImport(): void {
    this.csvInput?.nativeElement.click();
  }

  clearFeedback(): void {
    this.error.set('');
  }

  private clearServerFieldErrors(): void {
    this.serverFieldErrors.set({});
  }

  private setServerError(error: unknown, fallbackField: string): void {
    const fields = getFieldErrors(error);
    const mapped = fields.reduce<Record<string, string>>((result, fieldError) => {
      result[fieldError.field] = fieldError.message;
      return result;
    }, {});
    if (Object.keys(mapped).length === 0) {
      const message = isApiError(error) && error.status === 409
        ? (fallbackField === 'code'
          ? 'El código ya se encuentra registrado.'
          : 'El nombre ya se encuentra registrado.')
        : getErrorMessage(error);
      mapped[fallbackField] = message;
    }
    this.serverFieldErrors.set(mapped);
  }

  private setError(text: string): void {
    this.error.set(text);
  }

  private success(text: string): void {
    this.error.set('');
    this.showToast(text);
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
