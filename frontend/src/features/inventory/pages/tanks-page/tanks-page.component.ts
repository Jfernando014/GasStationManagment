import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { GaugeTablesService } from '../../../../core/services/gauge-tables.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { getErrorMessage, getFieldErrors, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { GaugeTableResponse } from '../../../../shared/models/dtos/gauge-table.dto';
import { FuelType, TankRequest, TankResponse } from '../../../../shared/models/dtos/tank.dto';

interface Tank {
  id: string;
  backendId?: number;
  name: string;
  code: string;
  fuel: string;
  fuelType: FuelType;
  capacity: number;
  maxHeight: number;
  tolerance: number;
  gaugeTableId: number;
}

type TankField = 'code' | 'name' | 'fuel' | 'capacity' | 'maxHeight' | 'tolerance' | 'gaugeTableId';

const TOAST_DURATION_MS = 3000;

const emptyTank: Tank = {
  id: '',
  code: '',
  name: '',
  fuel: '',
  fuelType: 'MOTOR',
  capacity: 0,
  maxHeight: 0,
  tolerance: 0,
  gaugeTableId: 0
};

const emptyTankDraft = { code: '', name: '', fuel: '', capacity: 0, maxHeight: 0, tolerance: 0, gaugeTableId: 0 };

/** Configuración › Tanques: tank registry linked to the existing gauge tables. */
@Component({
  selector: 'app-tanks-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './tanks-page.component.html'
})
export class TanksPageComponent {
  private readonly tanksService = inject(TanksService);
  private readonly gaugeTablesService = inject(GaugeTablesService);
  private readonly destroyRef = inject(DestroyRef);

  readonly tanks = signal<Tank[]>([]);
  readonly gaugeTables = signal<GaugeTableResponse[]>([]);
  readonly selectedTankId = signal('');
  readonly loading = signal(true);
  readonly error = signal('');
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  readonly newTankOpen = signal(false);
  readonly tankSubmitted = signal(false);
  readonly newTank = signal({ ...emptyTankDraft });
  private toastTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.loadTanks();
    this.loadGaugeTables();
  }

  get selectedTank(): Tank {
    return this.tanks().find(tank => tank.id === this.selectedTankId()) ?? emptyTank;
  }

  gaugeTableName(gaugeTableId: number): string {
    return this.gaugeTables().find(table => table.id === gaugeTableId)?.name ?? 'Sin tabla';
  }

  selectTank(id: string): void {
    this.selectedTankId.set(id);
    this.clearFeedback();
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

    const request: TankRequest = {
      code: draft.code.trim(),
      name: draft.name.trim(),
      fuelType: this.toFuelType(draft.fuel),
      maxCapacityGallons: draft.capacity,
      maxHeightCm: draft.maxHeight,
      toleranceCm: draft.tolerance,
      gaugeTableId: draft.gaugeTableId
    };
    this.tanksService.create(request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.tanks.update(tanks => [...tanks, this.fromResponse(saved)]);
        this.newTank.set({ ...emptyTankDraft });
        this.newTankOpen.set(false);
        this.success('Tanque registrado correctamente.');
      },
      error: error => this.setServerError(error, 'code')
    });
  }

  updateNewTank(field: TankField, value: string | number): void {
    const numericField = ['capacity', 'maxHeight', 'tolerance', 'gaugeTableId'].includes(field);
    this.newTank.update(tank => ({ ...tank, [field]: numericField ? Number(value) : value }));
  }

  isTankFieldInvalid(field: TankField, tank: Tank | null = null): boolean {
    if (!this.tankSubmitted() && !this.fieldError(field)) return false;
    const value = tank ? tank[field] : this.newTank()[field];
    if (field === 'code' || field === 'name' || field === 'fuel') return !String(value).trim();
    if (field === 'capacity' || field === 'maxHeight') return Number(value) <= 0;
    if (field === 'gaugeTableId') return Number(value) <= 0;
    return Number(value) < 0;
  }

  fieldError(field: string): string {
    return this.serverFieldErrors()[field] ?? '';
  }

  private loadTanks(): void {
    this.loading.set(true);
    this.tanksService.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: tanks => {
        this.tanks.set(tanks.map(tank => this.fromResponse(tank)));
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.setError(getErrorMessage(error));
      }
    });
  }

  private loadGaugeTables(): void {
    this.gaugeTablesService.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: tables => this.gaugeTables.set(tables),
      error: error => this.setError(getErrorMessage(error))
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
      maxHeight: tank.maxHeightCm,
      tolerance: tank.toleranceCm,
      gaugeTableId: tank.gaugeTableId ?? 0
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

  private clearFeedback(): void {
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
