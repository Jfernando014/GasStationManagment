import { CommonModule } from '@angular/common';
import { ActionButtonDirective } from '../../atoms/action-button/action-button.directive';
import { TagComponent } from '../../atoms/tag/tag.component';
import { TabItem, TabsComponent } from '../../molecules/tabs/tabs.component';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';

import { EmptyStateComponent } from '../../molecules/empty-state/empty-state.component';
import { FuelReceptionsService } from '../../../core/services/fuel-receptions.service';
import { InventoryAlertsService } from '../../../core/services/inventory-alerts.service';
import { InventoryHomeService } from '../../../core/services/inventory-home.service';
import { TankMeasurementsService } from '../../../core/services/tank-measurements.service';
import { TanksService } from '../../../core/services/tanks.service';
import { formatDateTime, formatShortDate, todayCalendarDay } from '../../../shared/helpers/format.helpers';
import {
  autonomyLevelLabel,
  autonomyLevelTone
} from '../../../shared/helpers/inventory-alert-display.helpers';
import {
  formatDays,
  formatGallons,
  formatPercent,
  FUEL_COLORS,
  FUEL_LABELS,
  PRODUCT_CATEGORY_LABELS
} from '../../../shared/helpers/inventory-display.helpers';
import { getErrorMessage, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { AutonomyLevel } from '../../../shared/models/dtos/inventory-alert.dto';
import { DailyControlResponse, ProductControlResponse } from '../../../shared/models/dtos/daily-control.dto';
import { InventoryHomeResponse, TankSnapshotResponse } from '../../../shared/models/dtos/inventory-home.dto';
import { FuelType, TankResponse } from '../../../shared/models/dtos/tank.dto';

type HomeTab = 'resumen' | 'control';

const OPTIMAL_BANNER_KEY = 'eds.home.optimalDismissed';
const TOAST_DURATION_MS = 3200;

function readOptimalDismissed(): boolean {
  try {
    return typeof sessionStorage !== 'undefined' && sessionStorage.getItem(OPTIMAL_BANNER_KEY) === '1';
  } catch {
    return false;
  }
}

function writeOptimalDismissed(dismissed: boolean): void {
  try {
    if (typeof sessionStorage === 'undefined') return;
    if (dismissed) sessionStorage.setItem(OPTIMAL_BANNER_KEY, '1');
    else sessionStorage.removeItem(OPTIMAL_BANNER_KEY);
  } catch {
    // Ignore storage errors in restricted environments.
  }
}

/**
 * Pantalla de inicio del inventario: existencias por combustible y tanque
 * desde la última medición, alertas de autonomía y control del diario por fecha.
 */
@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ActionButtonDirective, TagComponent, TabsComponent, EmptyStateComponent],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'closeActiveModal()'
  }
})
export class HomePageComponent {
  private readonly homeService = inject(InventoryHomeService);
  private readonly alertsService = inject(InventoryAlertsService);
  private readonly tanksService = inject(TanksService, { optional: true });
  private readonly measurementsService = inject(TankMeasurementsService, { optional: true });
  private readonly receptionsService = inject(FuelReceptionsService, { optional: true });
  private readonly destroyRef = inject(DestroyRef);

  readonly home = signal<InventoryHomeResponse | null>(null);
  readonly tankCatalog = signal<TankResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly optimalBannerDismissed = signal(readOptimalDismissed());

  readonly tab = signal<HomeTab>('resumen');
  readonly tabs: TabItem[] = [
    { id: 'resumen', label: 'Resumen', tone: 'green' },
    { id: 'control', label: 'Control del día', tone: 'amber' }
  ];

  readonly controlDate = signal(todayCalendarDay());
  readonly control = signal<DailyControlResponse | null>(null);
  readonly controlLoading = signal(false);
  readonly controlError = signal('');
  readonly controlLoaded = signal(false);

  // Quick measure modal ("Medir Regla")
  readonly measureTarget = signal<TankSnapshotResponse | null>(null);
  readonly measureDate = signal(todayCalendarDay());
  readonly measureHeightText = signal('');
  readonly measurePreviewGallons = signal<number | null>(null);
  readonly measureError = signal<string | null>(null);
  readonly savingMeasure = signal(false);

  // Quick fuel reception modal ("+ Nueva Consignación" / "Reabastecer")
  readonly receptionModalOpen = signal(false);
  readonly receptionTankId = signal<number>(0);
  readonly receptionDate = signal(todayCalendarDay());
  readonly receptionGallonsText = signal('');
  readonly receptionSupplier = signal('');
  readonly receptionError = signal<string | null>(null);
  readonly savingReception = signal(false);

  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;
  private readonly previewVolume$ = new Subject<{ tankId: number; heightCm: number } | null>();

  readonly fuelLabels = FUEL_LABELS;
  readonly fuelColors = FUEL_COLORS;
  readonly categoryLabels = PRODUCT_CATEGORY_LABELS;
  readonly formatGallons = formatGallons;
  readonly formatPercent = formatPercent;
  readonly formatDays = formatDays;
  readonly formatShortDate = formatShortDate;
  readonly formatDateTime = formatDateTime;
  readonly levelLabel = autonomyLevelLabel;
  readonly levelTone = autonomyLevelTone;

  readonly totalGallons = computed(() =>
    this.home()?.fuels.reduce((sum, fuel) => sum + fuel.currentGallons, 0) ?? 0
  );
  readonly totalCapacity = computed(() =>
    this.home()?.fuels.reduce((sum, fuel) => sum + fuel.capacityGallons, 0) ?? 0
  );
  readonly measuredTanks = computed(() =>
    this.home()?.tanks.filter(tank => tank.gallons !== null).length ?? 0
  );
  readonly totalTanks = computed(() => this.home()?.tanks.length ?? 0);
  readonly pendingAlerts = computed(() => this.home()?.pendingAlerts ?? 0);

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.loadTankCatalog();
    this.loadHome();

    this.previewVolume$
      .pipe(
        debounceTime(220),
        distinctUntilChanged((a, b) => a?.tankId === b?.tankId && a?.heightCm === b?.heightCm),
        switchMap(req => {
          if (!req || !this.measurementsService) return of<{ gallons: number | null; error: string | null }>({ gallons: null, error: null });
          return this.measurementsService.volume(req.tankId, req.heightCm).pipe(
            switchMap(res => of({ gallons: res.volumeGallons, error: null })),
            catchError(err => of({
              gallons: null,
              error: isApiError(err) && err.status === 409
                ? 'Este tanque no tiene una tabla de aforo asignada. Configúrala en Tanques.'
                : getErrorMessage(err)
            }))
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(result => {
        if (!this.measureTarget()) return;
        this.measurePreviewGallons.set(result.gallons);
        if (result.error) {
          this.measureError.set(result.error);
        }
      });
  }

  loadHome(): void {
    this.loading.set(true);
    this.error.set('');
    this.homeService.getHome().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: home => {
        this.home.set(home);
        this.alertsService.setPending(home.pendingAlerts);
        if (home.mostCriticalAlert) {
          this.optimalBannerDismissed.set(false);
          writeOptimalDismissed(false);
        }
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.error.set(getErrorMessage(error));
      }
    });
  }

  private loadTankCatalog(): void {
    if (!this.tanksService) return;
    this.tanksService
      .list()
      .pipe(catchError(() => of([])), takeUntilDestroyed(this.destroyRef))
      .subscribe(tanks => this.tankCatalog.set(tanks));
  }

  dismissOptimalBanner(): void {
    this.optimalBannerDismissed.set(true);
    writeOptimalDismissed(true);
  }

  tankMeta(tankId: number): TankResponse | undefined {
    return this.tankCatalog().find(item => item.id === tankId);
  }

  tankDisplayTitle(tank: TankSnapshotResponse): string {
    const meta = this.tankMeta(tank.tankId);
    if (!meta || meta.name.trim().toLowerCase() === tank.tankCode.trim().toLowerCase()) {
      return tank.tankCode;
    }
    return `${meta.name} (${tank.tankCode})`;
  }

  tankStatusLabel(level: AutonomyLevel | null): string {
    if (level === 'CRITICAL') return 'Bajo Nivel';
    if (level === 'WARNING') return 'Advertencia';
    return 'Operativo';
  }

  tankUllage(tank: TankSnapshotResponse): number | null {
    if (tank.gallons === null) return null;
    return Math.max(tank.capacityGallons - tank.gallons, 0);
  }

  clampedPercent(percentage: number | null): number {
    if (percentage === null || !Number.isFinite(percentage)) return 0;
    return Math.min(Math.max(percentage, 0), 100);
  }

  tankFillClass(tank: TankSnapshotResponse): string {
    if (tank.alertLevel === 'CRITICAL') return 'fill-critical-flat';
    if (tank.alertLevel === 'WARNING') return 'fill-warning-flat';
    const byFuel: Record<FuelType, string> = {
      MOTOR: 'fill-corriente-flat',
      EXTRA: 'fill-extra-flat',
      DIESEL: 'fill-diesel-flat',
      MAX_PRO: 'fill-maxpro-flat'
    };
    return byFuel[tank.fuelType];
  }

  openMeasureModal(tank: TankSnapshotResponse): void {
    this.measureTarget.set(tank);
    this.measureDate.set(todayCalendarDay());
    this.measureHeightText.set(tank.heightCm !== null ? String(tank.heightCm) : '');
    this.measurePreviewGallons.set(tank.gallons);
    this.measureError.set(null);
  }

  closeMeasureModal(): void {
    if (this.savingMeasure()) return;
    this.measureTarget.set(null);
    this.measureError.set(null);
    this.previewVolume$.next(null);
  }

  onMeasureHeightInput(value: string | number | null): void {
    const parsed = this.parseDecimalInput(value);
    this.measureHeightText.set(parsed.text);
    this.measureError.set(null);
    const target = this.measureTarget();
    if (!target || parsed.text === '' || !Number.isFinite(parsed.numeric) || parsed.numeric < 0) {
      this.measurePreviewGallons.set(null);
      this.previewVolume$.next(null);
      return;
    }
    this.previewVolume$.next({ tankId: target.tankId, heightCm: parsed.numeric });
  }

  saveQuickMeasurement(): void {
    const target = this.measureTarget();
    if (!target || this.savingMeasure() || !this.measurementsService) return;
    const parsed = this.parseDecimalInput(this.measureHeightText());
    if (!this.measureDate()) {
      this.measureError.set('Selecciona la fecha de medición.');
      return;
    }
    if (parsed.text === '' || !Number.isFinite(parsed.numeric) || parsed.numeric < 0) {
      this.measureError.set('Digita una altura válida en centímetros.');
      return;
    }
    const maxHeight = this.tankMeta(target.tankId)?.maxHeightCm;
    if (maxHeight !== undefined && parsed.numeric > maxHeight) {
      this.measureError.set(`La altura no puede superar el máximo del tanque (${maxHeight} cm).`);
      return;
    }

    this.savingMeasure.set(true);
    this.measureError.set(null);
    this.measurementsService
      .save(this.measureDate(), { readings: [{ tankId: target.tankId, heightCm: parsed.numeric }] })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingMeasure.set(false);
          this.measureTarget.set(null);
          this.showToast(`Medición registrada para ${target.tankCode}.`);
          this.loadHome();
        },
        error: err => {
          this.savingMeasure.set(false);
          this.measureError.set(getErrorMessage(err));
        }
      });
  }

  onReceptionGallonsInput(value: string | number | null): void {
    this.receptionGallonsText.set(this.parseDecimalInput(value).text);
    this.receptionError.set(null);
  }

  openReceptionModal(tank?: TankSnapshotResponse): void {
    const tanks = this.home()?.tanks ?? [];
    const selected = tank?.tankId ?? tanks[0]?.tankId ?? 0;
    this.receptionTankId.set(selected);
    this.receptionDate.set(todayCalendarDay());
    this.receptionGallonsText.set('');
    this.receptionSupplier.set('');
    this.receptionError.set(null);
    this.receptionModalOpen.set(true);
  }

  closeReceptionModal(): void {
    if (this.savingReception()) return;
    this.receptionModalOpen.set(false);
    this.receptionError.set(null);
  }

  selectedReceptionTank(): TankSnapshotResponse | undefined {
    const id = this.receptionTankId();
    return this.home()?.tanks.find(t => t.tankId === id);
  }

  submitReception(): void {
    if (this.savingReception() || !this.receptionsService) return;
    const tankId = Number(this.receptionTankId());
    const date = this.receptionDate();
    const gallons = this.parseDecimalInput(this.receptionGallonsText()).numeric;
    const supplier = String(this.receptionSupplier() ?? '').trim();

    if (!tankId || tankId <= 0 || !date || !Number.isFinite(gallons) || gallons <= 0 || !supplier) {
      this.receptionError.set('Completa el tanque, la fecha, los galones recibidos y el proveedor.');
      return;
    }

    this.savingReception.set(true);
    this.receptionError.set(null);
    this.receptionsService
      .create({ tankId, receptionDate: date, gallons, supplier })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingReception.set(false);
          this.receptionModalOpen.set(false);
          this.showToast('Consignación registrada. Registra la nueva lectura con vara para actualizar existencias.');
          this.loadHome();
        },
        error: err => {
          this.savingReception.set(false);
          this.receptionError.set(getErrorMessage(err));
        }
      });
  }

  closeActiveModal(): void {
    if (this.measureTarget()) this.closeMeasureModal();
    if (this.receptionModalOpen()) this.closeReceptionModal();
  }

  private parseDecimalInput(value: string | number | null | undefined): { text: string; numeric: number } {
    const text = String(value ?? '').trim();
    return {
      text,
      numeric: text === '' ? Number.NaN : Number(text.replace(',', '.'))
    };
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }

  loadControl(): void {
    this.controlLoading.set(true);
    this.controlError.set('');
    this.homeService.getDailyControl(this.controlDate()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: control => {
        this.control.set(control);
        this.controlLoaded.set(true);
        this.controlLoading.set(false);
      },
      error: error => {
        this.control.set(null);
        this.controlLoaded.set(true);
        this.controlLoading.set(false);
        this.controlError.set(getErrorMessage(error));
      }
    });
  }

  changeTab(tab: HomeTab): void {
    this.tab.set(tab);
    if (tab === 'control' && !this.controlLoaded()) {
      this.loadControl();
    }
  }

  onControlDateChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    if (!value) return;
    this.controlDate.set(value);
    this.loadControl();
  }

  variationLabel(status: string): string {
    if (status === 'OK') return 'Calculada';
    if (status === 'NO_PREVIOUS_MEASUREMENT') return 'Sin medición anterior';
    if (status === 'INCOMPLETE_MEASUREMENT') return 'Medición incompleta';
    return 'Sin datos de ventas';
  }

  autonomyLabel(status: string, level: AutonomyLevel): string {
    if (status === 'INCOMPLETE_MEASUREMENT') return 'Falta medición';
    if (status === 'NO_SALES_HISTORY') return 'Sin historial de ventas';
    return autonomyLevelLabel(level);
  }

  productStatusLabel(product: ProductControlResponse): string {
    if (product.status === 'NEGATIVE_STOCK') return 'Stock negativo';
    return product.depleted ? 'Agotado' : 'Normal';
  }

  productStatusTone(product: ProductControlResponse): 'green' | 'amber' | 'red' {
    if (product.status === 'NEGATIVE_STOCK') return 'red';
    return product.depleted ? 'amber' : 'green';
  }
}
