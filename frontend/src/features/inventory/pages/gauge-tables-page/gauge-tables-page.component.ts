import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { ConfirmModalComponent } from '../../../../atomic-design/organisms/confirm-modal/confirm-modal.component';
import { GaugeTablesService } from '../../../../core/services/gauge-tables.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { getErrorMessage, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { GaugeTableRequest, GaugeTableResponse } from '../../../../shared/models/dtos/gauge-table.dto';
import {
  FUEL_OPTIONS,
  TankField,
  TankFormModel,
  fromTankResponse,
  isTankFieldInvalid,
  isTankFormValid,
  mapServerFieldErrors,
  toFuelType,
  toTankRequest
} from '../../helpers/tank-form.helpers';

interface CalibrationPoint {
  height: number;
  gallons: number;
}

type DeleteModalTarget =
  | { kind: 'table'; table: GaugeTableResponse }
  | { kind: 'tank'; tank: TankFormModel };

const TOAST_DURATION_MS = 3000;

/** Configuración › Tabla de aforo: calibration tables and linked tank management. */
@Component({
  selector: 'app-gauge-tables-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ActionButtonDirective, TagComponent, ConfirmModalComponent],
  templateUrl: './gauge-tables-page.component.html',
  styleUrl: './gauge-tables-page.component.css'
})
export class GaugeTablesPageComponent {
  private readonly gaugeTablesService = inject(GaugeTablesService);
  private readonly tanksService = inject(TanksService);
  private readonly destroyRef = inject(DestroyRef);
  @ViewChild('csvInput') private csvInput?: ElementRef<HTMLInputElement>;

  readonly fuelOptions = FUEL_OPTIONS;
  readonly gaugeTables = signal<GaugeTableResponse[]>([]);
  readonly tanks = signal<TankFormModel[]>([]);
  readonly gaugeTableLoading = signal(true);
  readonly selectedGaugeTableId = signal<number | null>(null);
  readonly editingTableName = signal('');
  readonly newGaugeTableOpen = signal(false);
  readonly gaugeTableSubmitted = signal(false);
  readonly newGaugeTable = signal({ name: '' });
  readonly gaugeDraftPoints = signal<CalibrationPoint[]>([
    { height: 0, gallons: 0 },
    { height: 1, gallons: 1 }
  ]);
  readonly curves = signal<Record<string, CalibrationPoint[]>>({});
  readonly editingTank = signal<TankFormModel | null>(null);
  readonly tankSubmitted = signal(false);
  readonly deleteTarget = signal<DeleteModalTarget | null>(null);
  readonly deleting = signal(false);
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly error = signal('');
  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.loadGaugeTables();
    this.loadTanks();
  }

  get selectedTable(): GaugeTableResponse | undefined {
    return this.gaugeTables().find(table => table.id === this.selectedGaugeTableId());
  }

  get selectedCurve(): CalibrationPoint[] {
    const id = this.selectedGaugeTableId();
    return id === null ? [] : this.curves()[String(id)] ?? [];
  }

  get tanksInSelectedTable(): TankFormModel[] {
    const id = this.selectedGaugeTableId();
    return id === null ? [] : this.tanks().filter(tank => tank.gaugeTableId === id);
  }

  private loadGaugeTables(): void {
    this.gaugeTableLoading.set(true);
    this.gaugeTablesService.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: tables => {
        this.gaugeTables.set(tables);
        if (tables.length > 0) {
          const currentId = this.selectedGaugeTableId();
          const active = tables.find(table => table.id === currentId) ?? tables[0];
          this.selectedGaugeTableId.set(active.id);
          this.editingTableName.set(active.name);
          this.setCurveFromTable(active);
        } else {
          this.selectedGaugeTableId.set(null);
          this.editingTableName.set('');
        }
        this.gaugeTableLoading.set(false);
      },
      error: error => {
        this.gaugeTableLoading.set(false);
        this.setError(getErrorMessage(error));
      }
    });
  }

  private loadTanks(): void {
    this.tanksService.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: tanks => this.tanks.set(tanks.map(fromTankResponse)),
      error: error => this.setError(getErrorMessage(error))
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
    this.editingTableName.set(table?.name ?? '');
    if (table) this.setCurveFromTable(table);
    this.clearServerFieldErrors();
    this.clearFeedback();
  }

  updateNewGaugeTableName(name: string): void {
    this.newGaugeTable.set({ name });
  }

  updateEditingTableName(name: string): void {
    this.editingTableName.set(name);
    this.clearServerFieldErrors();
    this.clearFeedback();
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
        this.editingTableName.set(table.name);
        this.setCurveFromTable(table);
        this.newGaugeTable.set({ name: '' });
        this.gaugeDraftPoints.set([{ height: 0, gallons: 0 }, { height: 1, gallons: 1 }]);
        this.newGaugeTableOpen.set(false);
        this.gaugeTableSubmitted.set(false);
        this.success('Tabla de aforo guardada correctamente.');
      },
      error: error => this.serverFieldErrors.set(mapServerFieldErrors(error, 'name'))
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
    const name = this.editingTableName().trim() || (this.selectedTable?.name ?? '');
    if (!name) {
      this.setError('La tabla de aforo debe tener un nombre válido.');
      return;
    }
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
      name,
      points: curve.map(point => ({ heightCm: point.height, volumeGallons: point.gallons }))
    };
    this.gaugeTablesService.update(tableId, request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: table => {
        this.gaugeTables.update(tables => tables.map(item => item.id === table.id ? table : item));
        this.editingTableName.set(table.name);
        this.setCurveFromTable(table);
        this.gaugeTableSubmitted.set(false);
        this.success('Tabla de aforo actualizada correctamente.');
      },
      error: error => this.serverFieldErrors.set(mapServerFieldErrors(error, 'name'))
    });
  }

  requestDeleteTable(): void {
    const table = this.selectedTable;
    if (!table) return;
    this.clearFeedback();
    this.deleteTarget.set({ kind: 'table', table });
  }

  assignTankGaugeTable(tank: TankFormModel, value: number | string | null): void {
    const nextTableId = value === null || value === '' || Number(value) <= 0 ? null : Number(value);
    if (!tank.backendId) return;
    const updatedTank: TankFormModel = { ...tank, gaugeTableId: nextTableId };
    this.tanksService.update(tank.backendId, toTankRequest(updatedTank))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: saved => {
          const mapped = fromTankResponse(saved);
          this.tanks.update(items => items.map(item => item.id === mapped.id ? mapped : item));
          if (this.editingTank()?.id === mapped.id) {
            this.editingTank.set({ ...mapped });
          }
          this.success(
            mapped.gaugeTableId
              ? `Tabla asignada al tanque ${mapped.code}.`
              : `El tanque ${mapped.code} quedó con tabla Ninguna.`
          );
        },
        error: error => this.setError(getErrorMessage(error))
      });
  }

  editTankFromTable(tank: TankFormModel): void {
    this.editingTank.set({ ...tank });
    this.tankSubmitted.set(false);
    this.clearServerFieldErrors();
    this.clearFeedback();
  }

  cancelTankEdit(): void {
    this.editingTank.set(null);
    this.tankSubmitted.set(false);
    this.clearServerFieldErrors();
  }

  updateEditingTank(field: TankField, value: string | number | null): void {
    const current = this.editingTank();
    if (!current) return;
    if (field === 'fuel') {
      const fuel = String(value ?? '');
      this.editingTank.set({ ...current, fuel, fuelType: toFuelType(fuel) });
      return;
    }
    if (field === 'gaugeTableId') {
      const parsed = value === null || value === '' || Number(value) <= 0 ? null : Number(value);
      this.editingTank.set({ ...current, gaugeTableId: parsed });
      return;
    }
    if (['capacity', 'maxHeight', 'tolerance'].includes(field)) {
      this.editingTank.set({ ...current, [field]: Number(value ?? 0) });
      return;
    }
    this.editingTank.set({ ...current, [field]: String(value ?? '') });
  }

  isEditingTankFieldInvalid(field: TankField): boolean {
    const tank = this.editingTank();
    if (!tank || (!this.tankSubmitted() && !this.fieldError(field))) return false;
    return isTankFieldInvalid(field, tank);
  }

  saveEditingTank(): void {
    const tank = this.editingTank();
    if (!tank?.backendId) return;
    this.clearServerFieldErrors();
    this.tankSubmitted.set(true);
    if (!isTankFormValid(tank)) {
      this.setError('Completa código, nombre, combustible, capacidad, altura y tolerancia del tanque.');
      return;
    }
    this.tanksService.update(tank.backendId, toTankRequest(tank))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: saved => {
          const mapped = fromTankResponse(saved);
          this.tanks.update(items => items.map(item => item.id === mapped.id ? mapped : item));
          this.editingTank.set(null);
          this.tankSubmitted.set(false);
          this.success('Tanque actualizado correctamente.');
        },
        error: error => this.serverFieldErrors.set(mapServerFieldErrors(error, 'code'))
      });
  }

  requestDeleteTank(tank: TankFormModel): void {
    this.clearFeedback();
    this.deleteTarget.set({ kind: 'tank', tank });
  }

  cancelDeleteModal(): void {
    if (!this.deleting()) {
      this.deleteTarget.set(null);
    }
  }

  deleteModalTitle(): string {
    const target = this.deleteTarget();
    if (!target) return 'Confirmar eliminación';
    return target.kind === 'table' ? 'Eliminar tabla de aforo' : 'Eliminar tanque';
  }

  deleteModalMessage(): string {
    const target = this.deleteTarget();
    if (!target) return '';
    if (target.kind === 'table') {
      return `¿Deseas eliminar la tabla «${target.table.name}»? Los tanques asociados pasarán a quedar con tabla «Ninguna».`;
    }
    return `¿Deseas eliminar el tanque «${target.tank.name} (${target.tank.code})»? Esta acción no se puede deshacer.`;
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;
    if (target.kind === 'table') {
      this.executeDeleteTable(target.table);
    } else {
      this.executeDeleteTank(target.tank);
    }
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

  private executeDeleteTable(table: GaugeTableResponse): void {
    this.deleting.set(true);
    this.gaugeTablesService.delete(table.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        const remaining = this.gaugeTables().filter(item => item.id !== table.id);
        this.gaugeTables.set(remaining);
        this.tanks.update(items => items.map(tank =>
          tank.gaugeTableId === table.id ? { ...tank, gaugeTableId: null } : tank
        ));
        if (this.editingTank()?.gaugeTableId === table.id) {
          this.editingTank.update(tank => tank ? { ...tank, gaugeTableId: null } : null);
        }
        const nextSelected = remaining[0] ?? null;
        this.selectedGaugeTableId.set(nextSelected?.id ?? null);
        this.editingTableName.set(nextSelected?.name ?? '');
        if (nextSelected) this.setCurveFromTable(nextSelected);
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.success('Tabla de aforo eliminada. Los tanques vinculados pasaron a Ninguna.');
      },
      error: error => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.setError(getErrorMessage(error));
      }
    });
  }

  private executeDeleteTank(tank: TankFormModel): void {
    if (!tank.backendId) return;
    this.deleting.set(true);
    this.tanksService.delete(tank.backendId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.tanks.update(items => items.filter(item => item.id !== tank.id));
        if (this.editingTank()?.id === tank.id) {
          this.cancelTankEdit();
        }
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.success('Tanque eliminado correctamente.');
      },
      error: error => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.setError(
          isApiError(error) && error.status === 409
            ? 'No se puede eliminar este tanque porque ya tiene mediciones o recepciones registradas.'
            : getErrorMessage(error)
        );
      }
    });
  }

  private clearServerFieldErrors(): void {
    this.serverFieldErrors.set({});
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
