import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { EmptyStateComponent } from '../../molecules/empty-state/empty-state.component';
import { InventoryAlertsService } from '../../../core/services/inventory-alerts.service';
import { InventoryHomeService } from '../../../core/services/inventory-home.service';
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
import { getErrorMessage } from '../../../shared/helpers/problem-details.helpers';
import { AutonomyLevel } from '../../../shared/models/dtos/inventory-alert.dto';
import { DailyControlResponse, ProductControlResponse } from '../../../shared/models/dtos/daily-control.dto';
import { InventoryHomeResponse } from '../../../shared/models/dtos/inventory-home.dto';

type HomeTab = 'resumen' | 'control';

/**
 * Pantalla de inicio del inventario (HU-7 / HU-29): existencias por combustible y tanque
 * desde la última medición, alerta más crítica y control del diario de una fecha.
 */
@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [RouterLink, EmptyStateComponent],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomePageComponent {
  private readonly homeService = inject(InventoryHomeService);
  private readonly alertsService = inject(InventoryAlertsService);
  private readonly destroyRef = inject(DestroyRef);

  readonly home = signal<InventoryHomeResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly tab = signal<HomeTab>('resumen');

  readonly controlDate = signal(todayCalendarDay());
  readonly control = signal<DailyControlResponse | null>(null);
  readonly controlLoading = signal(false);
  readonly controlError = signal('');
  readonly controlLoaded = signal(false);

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
    this.loadHome();
  }

  loadHome(): void {
    this.loading.set(true);
    this.error.set('');
    this.homeService.getHome().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: home => {
        this.home.set(home);
        this.alertsService.setPending(home.pendingAlerts);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.error.set(getErrorMessage(error));
      }
    });
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

  productStatusTone(product: ProductControlResponse): 'optimal' | 'warning' | 'critical' {
    if (product.status === 'NEGATIVE_STOCK') return 'critical';
    return product.depleted ? 'warning' : 'optimal';
  }
}
