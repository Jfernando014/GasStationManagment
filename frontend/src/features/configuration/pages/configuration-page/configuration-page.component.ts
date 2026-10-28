import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { TanksService } from '../../../../core/services/tanks.service';
import { GaugeTablesService } from '../../../../core/services/gauge-tables.service';
import { GaugeTableRequest, GaugeTableResponse } from '../../../../shared/models/dtos/gauge-table.dto';
import { getErrorMessage, getFieldErrors, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { FuelType, TankRequest, TankResponse } from '../../../../shared/models/dtos/tank.dto';

interface Tank {
  id: string;
  backendId?: number;
  name: string;
  code: string;
  fuel: string;
  fuelType: FuelType;
  capacity: number;
  current: number;
  maxHeight: number;
  tolerance: number;
  gaugeTableId: number;
  version: string;
}

interface CalibrationPoint {
  height: number;
  gallons: number;
}

const TOAST_DURATION_MS = 3000;

const emptyTank: Tank = {
  id: '',
  code: '',
  name: '',
  fuel: '',
  fuelType: 'MOTOR',
  capacity: 0,
  current: 0,
  maxHeight: 0,
  tolerance: 0,
  gaugeTableId: 0,
  version: ''
};

@Component({
  selector: 'app-configuration-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './configuration-page.component.html',
  styleUrl: './configuration-page.component.css'
})
export class ConfigurationPageComponent {
  private readonly tanksService = inject(TanksService);
  private readonly gaugeTablesService = inject(GaugeTablesService);
  private readonly destroyRef = inject(DestroyRef);
  readonly router = inject(Router);
  @ViewChild('csvInput') private csvInput?: ElementRef<HTMLInputElement>;

  readonly tanks = signal<Tank[]>([]);
  readonly curves = signal<Record<string, CalibrationPoint[]>>({});
  readonly selectedTankId = signal('');
  readonly message = signal('');
  readonly error = signal('');
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  readonly newTankOpen = signal(false);
  readonly tankSubmitted = signal(false);
  readonly newTank = signal({ code: '', name: '', fuel: '', capacity: 0, maxHeight: 0, tolerance: 0, gaugeTableId: 0 });
  readonly tab = signal<'tanques' | 'aforo'>('tanques');
  readonly loading = signal(true);
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
  private toastTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.updateTab(this.router.url);
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(event => this.updateTab(event.urlAfterRedirects));
    this.loadTanks();
    this.loadGaugeTables();
  }

  private updateTab(url: string): void {
    this.tab.set(url.includes('/configuracion/aforo') ? 'aforo' : 'tanques');
  }

  private loadTanks(): void {
    this.loading.set(true);
    this.tanksService.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: tanks => {
        const mapped = tanks.map(tank => this.fromResponse(tank));
        this.tanks.set(mapped);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.setError(getErrorMessage(error));
      }
    });
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

  private fromResponse(tank: TankResponse): Tank {
    return {
      id: String(tank.id),
      backendId: tank.id,
      code: tank.code,
      name: tank.name,
      fuel: this.fromFuelType(tank.fuelType),
      fuelType: tank.fuelType,
      capacity: tank.maxCapacityGallons,
      current: 0,
      maxHeight: tank.maxHeightCm,
      tolerance: tank.toleranceCm,
      gaugeTableId: tank.gaugeTableId ?? 0,
      version: 'Configurado'
    };
  }

  private toRequest(tank: Tank): TankRequest {
    return {
      code: tank.code,
      name: tank.name.trim(),
      fuelType: tank.fuelType,
      maxCapacityGallons: tank.capacity,
      maxHeightCm: tank.maxHeight,
      toleranceCm: tank.tolerance,
      gaugeTableId: tank.gaugeTableId
    };
  }

  private replaceTank(updated: Tank): void {
    this.tanks.update(tanks => tanks.map(tank => tank.id === updated.id ? updated : tank));
  }

  private setCurveFromTable(table: GaugeTableResponse): void {
    this.curves.update(curves => ({
      ...curves,
      [String(table.id)]: table.points.map(point => ({ height: point.heightCm, gallons: point.volumeGallons }))
    }));
  }

  private toFuelType(fuel: string): FuelType {
    if (fuel === 'Diésel ACPM') return 'DIESEL';
    if (fuel === 'Gasolina Extra') return 'EXTRA';
    if (fuel === 'Diésel Max Pro') return 'MAX_PRO';
    return 'MOTOR';
  }

  private fromFuelType(fuelType: FuelType): string {
    if (fuelType === 'DIESEL') return 'Diésel ACPM';
    if (fuelType === 'EXTRA') return 'Gasolina Extra';
    if (fuelType === 'MAX_PRO') return 'Diésel Max Pro';
    return 'Gasolina Corriente';
  }

  get selectedTank(): Tank {
    return this.tanks().find(tank => tank.id === this.selectedTankId()) ?? emptyTank;
  }

  get selectedCurve(): CalibrationPoint[] {
    const key = this.selectedGaugeTableId() === null
      ? this.selectedTankId()
      : String(this.selectedGaugeTableId());
    return this.curves()[key] ?? (this.selectedGaugeTableId() === null ? [] : this.gaugeDraftPoints());
  }

  selectTank(id: string): void {
    this.selectedTankId.set(id);
    this.clearFeedback();
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

  isTankFieldInvalid(field: 'code' | 'name' | 'fuel' | 'capacity' | 'maxHeight' | 'tolerance' | 'gaugeTableId', tank: Tank | null = null): boolean {
    if (!this.tankSubmitted() && !this.fieldError(field)) return false;
    const value = tank ? tank[field] : this.newTank()[field];
    if (field === 'code' || field === 'name' || field === 'fuel') return !String(value).trim();
    if (field === 'capacity' || field === 'maxHeight') return Number(value) <= 0;
    if (field === 'gaugeTableId') return Number(value) <= 0;
    return Number(value) < 0;
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

  updateTank(id: string, field: keyof Tank, value: string | number): void {
    const numericField = ['capacity', 'maxHeight', 'tolerance', 'gaugeTableId'].includes(field);
    this.tanks.update(tanks => tanks.map(tank => {
      if (tank.id !== id) return tank;
      if (field === 'fuel') {
        const fuel = String(value);
        return { ...tank, fuel, fuelType: this.toFuelType(fuel) };
      }
      return { ...tank, [field]: numericField ? Number(value) : value };
    }));
  }

  saveTank(tank: Tank): void {
    this.clearServerFieldErrors();
    this.tankSubmitted.set(true);
    if (!tank.code.trim() || !tank.name.trim() || !tank.fuel || tank.capacity <= 0 ||
      tank.maxHeight <= 0 || tank.tolerance < 0 || !tank.gaugeTableId) {
      this.setError('Completa los datos del tanque y selecciona una tabla de aforo.');
      return;
    }
    const request = this.toRequest(tank);
    const operation = tank.backendId === undefined
      ? this.tanksService.create(request)
      : this.tanksService.update(tank.backendId, request);
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.replaceTank(this.fromResponse(saved));
        // Saving closes the editor so the fields stop being editable until "Editar" is pressed again.
        this.selectedTankId.set('');
        this.tankSubmitted.set(false);
        this.success('Configuración del tanque guardada correctamente.');
      },
      error: error => this.setServerError(error, 'code')
    });
  }

  addTank(): void {
    this.clearServerFieldErrors();
    this.tankSubmitted.set(true);
    const draft = this.newTank();
    if (!draft.code.trim() || !draft.name.trim() || !draft.fuel || draft.capacity <= 0 ||
      draft.maxHeight <= 0 || draft.tolerance < 0 || draft.gaugeTableId <= 0 || this.gaugeTables().length === 0) {
      this.setError(this.gaugeTables().length === 0
        ? 'Primero registra una tabla de aforo para poder registrar el tanque.'
        : 'Completa código, nombre, combustible, capacidad, altura, tolerancia y tabla de aforo.');
      return;
    }

    const code = draft.code.trim();
    const request: TankRequest = {
      code,
      name: draft.name.trim(),
      fuelType: this.toFuelType(draft.fuel),
      maxCapacityGallons: draft.capacity,
      maxHeightCm: draft.maxHeight,
      toleranceCm: draft.tolerance,
      gaugeTableId: draft.gaugeTableId
    };
    this.tanksService.create(request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        const tank = this.fromResponse(saved);
        this.tanks.update(tanks => [...tanks, tank]);
        this.curves.update(curves => ({ ...curves, [tank.id]: [{ height: 0, gallons: 0 }, { height: tank.maxHeight, gallons: tank.capacity }] }));
        this.newTank.set({ code: '', name: '', fuel: '', capacity: 0, maxHeight: 0, tolerance: 0, gaugeTableId: 0 });
        this.newTankOpen.set(false);
        this.success('Tanque registrado correctamente.');
      },
      error: error => this.setServerError(error, 'code')
    });
  }

  updateNewTank(field: 'code' | 'name' | 'fuel' | 'capacity' | 'maxHeight' | 'tolerance' | 'gaugeTableId', value: string | number): void {
    const numericField = ['capacity', 'maxHeight', 'tolerance', 'gaugeTableId'].includes(field);
    this.newTank.update(tank => ({ ...tank, [field]: numericField ? Number(value) : value }));
  }

  addPoint(): void {
    const curve = this.selectedCurve;
    const last = curve[curve.length - 1] ?? { height: 0, gallons: 0 };
    const key = this.selectedGaugeTableId() === null ? this.selectedTankId() : String(this.selectedGaugeTableId());
    const next = [...curve, { height: Number((last.height + 1).toFixed(1)), gallons: last.gallons + 15 }];
    if (this.selectedGaugeTableId() === null) {
      this.curves.update(curves => ({ ...curves, [key]: next }));
    } else {
      this.curves.update(curves => ({ ...curves, [key]: next }));
    }
  }

  deletePoint(index: number): void {
    if (this.selectedCurve.length <= 2) {
      this.setError('La tabla de aforo requiere un mínimo de 2 puntos.');
      return;
    }
    const key = this.selectedGaugeTableId() === null ? this.selectedTankId() : String(this.selectedGaugeTableId());
    this.curves.update(curves => ({ ...curves, [key]: this.selectedCurve.filter((_, pointIndex) => pointIndex !== index) }));
  }

  updatePoint(index: number, field: keyof CalibrationPoint, value: string): void {
    const points = this.selectedCurve.map((point, pointIndex) => pointIndex === index ? { ...point, [field]: Number(value) } : point);
    const key = this.selectedGaugeTableId() === null ? this.selectedTankId() : String(this.selectedGaugeTableId());
    this.curves.update(curves => ({ ...curves, [key]: points }));
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
      name: this.gaugeTables().find(table => table.id === tableId)?.name ?? '',
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
        const key = this.selectedGaugeTableId() === null ? this.selectedTankId() : String(this.selectedGaugeTableId());
        this.curves.update(curves => ({ ...curves, [key]: points }));
        this.success(`${points.length} puntos de aforo importados. Valida y guarda los cambios.`);
      }
      input.value = '';
    };
    reader.readAsText(file);
  }

  exportCsv(): void {
    const table = this.gaugeTables().find(item => item.id === this.selectedGaugeTableId());
    const capacity = table?.maxVolumeGallons ?? 0;
    const csv = ['Altura_cm,Volumen_Galones,%_Llenado', ...this.selectedCurve.map(point => `${point.height},${point.gallons},${capacity > 0 ? ((point.gallons / capacity) * 100).toFixed(2) : '0'}%`)].join('\n');
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    link.download = `Aforo_${table?.name ?? this.selectedGaugeTableId()}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
    this.success('Archivo CSV exportado correctamente.');
  }

  openImport(): void { this.csvInput?.nativeElement.click(); }
  clearFeedback(): void { this.message.set(''); this.error.set(''); }
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
    this.message.set('');
    this.error.set(text);
  }
  private success(text: string): void {
    this.error.set('');
    this.message.set('');
    this.showToast(text);
  }
  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
