import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { FuelReceptionsService } from '../../../../core/services/fuel-receptions.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { FUEL_LABELS, formatGallons } from '../../../../shared/helpers/inventory-display.helpers';
import { getErrorMessage, getFieldErrors } from '../../../../shared/helpers/problem-details.helpers';
import { FuelReceptionFilters, FuelReceptionResponse } from '../../../../shared/models/dtos/fuel-reception.dto';
import { TankResponse } from '../../../../shared/models/dtos/tank.dto';

const TOAST_DURATION_MS = 3000;

interface ReceptionDraft {
  tankId: number;
  receptionDate: string;
  gallons: number | null;
  supplier: string;
}

/** Configuración › Recepciones: fuel that arrived at each tank, with its supplier. */
@Component({
  selector: 'app-receptions-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './receptions-page.component.html',
  styleUrl: './receptions-page.component.css'
})
export class ReceptionsPageComponent {
  private readonly receptionsService = inject(FuelReceptionsService);
  private readonly tanksService = inject(TanksService);
  private readonly destroyRef = inject(DestroyRef);

  readonly fuelLabels = FUEL_LABELS;
  readonly formatGallons = formatGallons;

  readonly tanks = signal<TankResponse[]>([]);
  readonly receptions = signal<FuelReceptionResponse[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly filters = signal<FuelReceptionFilters>({});

  readonly draft = signal<ReceptionDraft>(this.emptyDraft());
  readonly submitted = signal(false);
  readonly saving = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly formError = signal<string | null>(null);

  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;

  readonly totalGallons = computed(() =>
    this.receptions().reduce((total, reception) => total + reception.gallons, 0)
  );

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.loadTanks();
    this.load();
  }

  private emptyDraft(): ReceptionDraft {
    return { tankId: 0, receptionDate: this.today(), gallons: null, supplier: '' };
  }

  private today(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  }

  private loadTanks(): void {
    this.tanksService
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: tanks => this.tanks.set(tanks),
        error: (error: unknown) => this.formError.set(getErrorMessage(error))
      });
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.receptionsService
      .list(this.filters())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: receptions => {
          this.receptions.set(
            [...receptions].sort((a, b) => b.receptionDate.localeCompare(a.receptionDate) || b.id - a.id)
          );
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.loadError.set(getErrorMessage(error));
        }
      });
  }

  onTankFilter(value: string): void {
    this.filters.update(filters => ({ ...filters, tankId: value ? Number(value) : null }));
    this.load();
  }

  onDateFilter(field: 'from' | 'to', value: string): void {
    this.filters.update(filters => ({ ...filters, [field]: value || null }));
    this.load();
  }

  clearFilters(): void {
    this.filters.set({});
    this.load();
  }

  updateDraft(field: keyof ReceptionDraft, value: string | number | null): void {
    this.draft.update(draft => {
      if (field === 'tankId') return { ...draft, tankId: Number(value) };
      if (field === 'gallons') {
        const gallons = value === null || value === '' ? null : Number(value);
        return { ...draft, gallons: gallons !== null && Number.isFinite(gallons) ? gallons : null };
      }
      return { ...draft, [field]: String(value) };
    });
  }

  fieldError(field: string): string {
    return this.fieldErrors()[field] ?? '';
  }

  isInvalid(field: keyof ReceptionDraft): boolean {
    if (!this.submitted() && !this.fieldError(field)) return false;
    const draft = this.draft();
    if (field === 'tankId') return draft.tankId <= 0;
    if (field === 'receptionDate') return !draft.receptionDate;
    if (field === 'gallons') return draft.gallons === null || draft.gallons <= 0;
    return !draft.supplier.trim();
  }

  save(): void {
    if (this.saving()) return;
    this.submitted.set(true);
    this.fieldErrors.set({});
    this.formError.set(null);
    if (this.isInvalid('tankId') || this.isInvalid('receptionDate') || this.isInvalid('gallons') || this.isInvalid('supplier')) {
      this.formError.set('Completa el tanque, la fecha, los galones y el proveedor.');
      return;
    }
    const draft = this.draft();
    this.saving.set(true);
    this.receptionsService
      .create({
        tankId: draft.tankId,
        receptionDate: draft.receptionDate,
        gallons: Number(draft.gallons),
        supplier: draft.supplier.trim()
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: reception => {
          this.saving.set(false);
          this.receptions.update(receptions => [reception, ...receptions]);
          this.draft.update(current => ({ ...current, gallons: null, supplier: '' }));
          this.submitted.set(false);
          this.showToast('Recepción registrada correctamente.');
        },
        error: (error: unknown) => {
          this.saving.set(false);
          const mapped = getFieldErrors(error).reduce<Record<string, string>>((result, fieldError) => {
            result[fieldError.field] = fieldError.message;
            return result;
          }, {});
          if (Object.keys(mapped).length > 0) {
            this.fieldErrors.set(mapped);
          } else {
            this.formError.set(getErrorMessage(error));
          }
        }
      });
  }

  tankName(tankId: number): string {
    const tank = this.tanks().find(item => item.id === tankId);
    return tank ? `${tank.name} (${tank.code} · ${this.fuelLabels[tank.fuelType]})` : `Tanque ${tankId}`;
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
