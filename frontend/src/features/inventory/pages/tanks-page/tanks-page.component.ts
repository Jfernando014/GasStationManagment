import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ConfirmModalComponent } from '../../../../atomic-design/organisms/confirm-modal/confirm-modal.component';
import { GaugeTablesService } from '../../../../core/services/gauge-tables.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { getErrorMessage, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { GaugeTableResponse } from '../../../../shared/models/dtos/gauge-table.dto';
import {
  FUEL_OPTIONS,
  TankField,
  TankFormModel,
  createEmptyTankForm,
  fromTankResponse,
  isTankFieldInvalid,
  isTankFormValid,
  mapServerFieldErrors,
  toFuelType,
  toTankRequest
} from '../../helpers/tank-form.helpers';

const TOAST_DURATION_MS = 3000;

/** Configuración › Tanques: tank registry with optional link to gauge tables. */
@Component({
  selector: 'app-tanks-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ConfirmModalComponent],
  templateUrl: './tanks-page.component.html'
})
export class TanksPageComponent {
  private readonly tanksService = inject(TanksService);
  private readonly gaugeTablesService = inject(GaugeTablesService);
  private readonly destroyRef = inject(DestroyRef);

  readonly fuelOptions = FUEL_OPTIONS;
  readonly tanks = signal<TankFormModel[]>([]);
  readonly gaugeTables = signal<GaugeTableResponse[]>([]);
  readonly selectedTankId = signal('');
  readonly editingTank = signal<TankFormModel | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  readonly newTankOpen = signal(false);
  readonly tankSubmitted = signal(false);
  readonly newTank = signal<TankFormModel>(createEmptyTankForm());
  readonly deleteTarget = signal<TankFormModel | null>(null);
  readonly deleting = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.loadTanks();
    this.loadGaugeTables();
  }

  get selectedTank(): TankFormModel {
    return this.editingTank() ?? createEmptyTankForm();
  }

  gaugeTableName(gaugeTableId: number | null): string {
    if (!gaugeTableId) return 'Ninguna';
    return this.gaugeTables().find(table => table.id === gaugeTableId)?.name ?? 'Ninguna';
  }

  selectTank(id: string): void {
    const tank = this.tanks().find(item => item.id === id);
    if (!tank) return;
    this.selectedTankId.set(id);
    this.editingTank.set({ ...tank });
    this.tankSubmitted.set(false);
    this.clearServerFieldErrors();
    this.clearFeedback();
  }

  cancelEdit(): void {
    this.selectedTankId.set('');
    this.editingTank.set(null);
    this.tankSubmitted.set(false);
    this.clearServerFieldErrors();
    this.clearFeedback();
  }

  openNewTank(): void {
    this.newTankOpen.update(open => !open);
    this.tankSubmitted.set(false);
    this.clearServerFieldErrors();
    this.clearFeedback();
  }

  updateTank(id: string, field: keyof TankFormModel, value: string | number | null): void {
    const current = this.editingTank();
    if (!current || current.id !== id) return;
    this.editingTank.set(this.patchTank(current, field, value));
  }

  saveTank(tank: TankFormModel): void {
    this.clearServerFieldErrors();
    this.tankSubmitted.set(true);
    if (!isTankFormValid(tank)) {
      this.setError('Completa código, nombre, combustible, capacidad, altura y tolerancia.');
      return;
    }
    const request = toTankRequest(tank);
    const operation = tank.backendId === undefined
      ? this.tanksService.create(request)
      : this.tanksService.update(tank.backendId, request);
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.replaceTank(fromTankResponse(saved));
        this.selectedTankId.set('');
        this.editingTank.set(null);
        this.tankSubmitted.set(false);
        this.success('Configuración del tanque guardada correctamente.');
      },
      error: error => this.serverFieldErrors.set(mapServerFieldErrors(error, 'code'))
    });
  }

  addTank(): void {
    this.clearServerFieldErrors();
    this.tankSubmitted.set(true);
    const draft = this.newTank();
    if (!isTankFormValid(draft)) {
      this.setError('Completa código, nombre, combustible, capacidad, altura y tolerancia.');
      return;
    }

    this.tanksService.create(toTankRequest(draft)).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.tanks.update(tanks => [...tanks, fromTankResponse(saved)]);
        this.newTank.set(createEmptyTankForm());
        this.newTankOpen.set(false);
        this.tankSubmitted.set(false);
        this.success('Tanque registrado correctamente.');
      },
      error: error => this.serverFieldErrors.set(mapServerFieldErrors(error, 'code'))
    });
  }

  updateNewTank(field: TankField, value: string | number | null): void {
    this.newTank.update(tank => this.patchTank(tank, field, value));
  }

  isTankFieldInvalid(field: TankField, tank: TankFormModel | null = null): boolean {
    if (!this.tankSubmitted() && !this.fieldError(field)) return false;
    return isTankFieldInvalid(field, tank ?? this.newTank());
  }

  fieldError(field: string): string {
    return this.serverFieldErrors()[field] ?? '';
  }

  requestDeleteTank(tank: TankFormModel): void {
    this.clearFeedback();
    this.deleteTarget.set(tank);
  }

  cancelDeleteTank(): void {
    if (!this.deleting()) {
      this.deleteTarget.set(null);
    }
  }

  deleteConfirmMessage(): string {
    const target = this.deleteTarget();
    return target
      ? `¿Deseas eliminar el tanque «${target.name} (${target.code})»? Esta acción no se puede deshacer.`
      : '';
  }

  confirmDeleteTank(): void {
    const target = this.deleteTarget();
    if (!target?.backendId) return;
    this.deleting.set(true);
    this.tanksService.delete(target.backendId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.tanks.update(items => items.filter(item => item.id !== target.id));
        if (this.selectedTankId() === target.id) {
          this.cancelEdit();
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

  private patchTank(tank: TankFormModel, field: keyof TankFormModel, value: string | number | null): TankFormModel {
    if (field === 'fuel') {
      const fuel = String(value ?? '');
      return { ...tank, fuel, fuelType: toFuelType(fuel) };
    }
    if (field === 'gaugeTableId') {
      const parsed = value === null || value === '' || Number(value) <= 0 ? null : Number(value);
      return { ...tank, gaugeTableId: parsed };
    }
    if (['capacity', 'maxHeight', 'tolerance'].includes(field)) {
      return { ...tank, [field]: Number(value ?? 0) };
    }
    return { ...tank, [field]: String(value ?? '') };
  }

  private loadTanks(): void {
    this.loading.set(true);
    this.tanksService.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: tanks => {
        this.tanks.set(tanks.map(fromTankResponse));
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

  private replaceTank(updated: TankFormModel): void {
    this.tanks.update(tanks => tanks.map(tank => tank.id === updated.id ? updated : tank));
  }

  private clearFeedback(): void {
    this.error.set('');
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
