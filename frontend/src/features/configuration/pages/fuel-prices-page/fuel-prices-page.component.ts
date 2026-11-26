import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Subscription, forkJoin } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { ToastComponent } from '../../../../atomic-design/molecules/toast/toast.component';
import { FuelPricesService } from '../../../../core/services/fuel-prices.service';
import { ToastService } from '../../../../core/services/toast.service';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { FUELS, FUEL_VALUE_LABELS, fuelName, summarizeFuel } from '../../../../shared/helpers/fuel-price.helpers';
import { prefersReducedMotion, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { FuelValueDto, FuelValueKind } from '../../../../shared/models/dtos/fuel-price.dto';
import { FuelType } from '../../../../shared/models/dtos/tank.dto';
import { FuelPriceCardComponent } from '../../components/fuel-price-card/fuel-price-card.component';
import { FuelValueHistoryComponent } from '../../components/fuel-value-history/fuel-value-history.component';
import {
  FuelPanelRequest, FuelValuePanelComponent, FuelValueSaved
} from '../../components/fuel-value-panel/fuel-value-panel.component';

/** How long the row just saved stays highlighted. */
const HIGHLIGHT_MS = 2600;
/** Time the side panel takes to leave before the page scrolls to the saved row. */
const PANEL_LEAVE_MS = 350;
const PHONE_QUERY = '(max-width: 640px)';

const KIND_BY_PARAM: Record<string, FuelValueKind> = { venta: 'sale', compra: 'purchase' };

/**
 * Sale price and purchase cost per gallon of every fuel, with their history. Other screens can open it ready:
 * `?combustible=MAX_PRO&valor=compra&nuevo=1` picks the fuel and the kind and opens the form for a new value.
 */
@Component({
  selector: 'app-fuel-prices-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ActionButtonDirective, SkeletonComponent, EmptyStateComponent, ToastComponent,
    FuelPriceCardComponent, FuelValueHistoryComponent, FuelValuePanelComponent
  ],
  templateUrl: './fuel-prices-page.component.html',
  styleUrl: './fuel-prices-page.component.css'
})
export class FuelPricesPageComponent {
  private readonly service = inject(FuelPricesService);
  private readonly toast = inject(ToastService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly today = stationToday();
  protected readonly skeletonCards = [0, 1, 2, 3];
  protected readonly skeletonRows = [0, 1, 2];

  protected readonly status = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly sales = signal<FuelValueDto[]>([]);
  protected readonly purchases = signal<FuelValueDto[]>([]);
  protected readonly selected = signal<FuelType>('MOTOR');
  protected readonly panelOpen = signal(false);
  protected readonly panelRequest = signal<FuelPanelRequest | null>(null);
  protected readonly highlight = signal<{ kind: FuelValueKind; id: number } | null>(null);
  /** Kind the "Nuevo valor" button starts with. */
  private preferredKind: FuelValueKind = 'sale';
  /** Query parameters waiting for the values to be loaded. */
  private pendingParams: ParamMap | null = null;
  private loadSubscription: Subscription | null = null;
  private highlightTimer: ReturnType<typeof setTimeout> | null = null;
  private scrollTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly summaries = computed(() =>
    FUELS.map(fuel => summarizeFuel(fuel, this.sales(), this.purchases(), this.today)));

  protected readonly selectedName = computed(() => fuelName(this.selected()));
  protected readonly selectedSales = computed(() => this.sales().filter(value => value.fuelType === this.selected()));
  protected readonly selectedPurchases = computed(() =>
    this.purchases().filter(value => value.fuelType === this.selected()));

  constructor() {
    inject(ActivatedRoute).queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.pendingParams = params;
      if (this.status() === 'ready') this.applyParams();
    });
    this.destroyRef.onDestroy(() => {
      this.loadSubscription?.unsubscribe();
      if (this.highlightTimer) clearTimeout(this.highlightTimer);
      if (this.scrollTimer) clearTimeout(this.scrollTimer);
      this.toast.hide();
    });
    this.load();
  }

  protected load(): void {
    this.status.set('loading');
    this.fetch(() => this.status.set('error'));
  }

  /** Reads the histories again without taking the screen away; if it fails, what is shown stays. */
  protected refresh(): void {
    this.fetch(() => undefined);
  }

  protected selectFuel(type: FuelType): void {
    this.selected.set(type);
    if (this.onPhone()) this.scrollTo('.history', 'start');
  }

  protected openNew(fuelType: FuelType = this.selected(), kind: FuelValueKind = this.preferredKind): void {
    this.selected.set(fuelType);
    this.openPanel({ mode: 'new', fuelType, kind });
  }

  protected openEdit(kind: FuelValueKind, value: FuelValueDto): void {
    this.openPanel({ mode: 'edit', kind, value });
  }

  protected openPanel(request: FuelPanelRequest): void {
    this.toast.hide();
    this.panelRequest.set(request);
    this.panelOpen.set(true);
  }

  protected closePanel(): void {
    this.panelOpen.set(false);
  }

  protected onSaved({ kind, value, edited }: FuelValueSaved): void {
    const list = kind === 'sale' ? this.sales : this.purchases;
    list.update(values => [...values.filter(item => item.id !== value.id), value]);
    this.closePanel();
    this.selected.set(value.fuelType);

    const label = FUEL_VALUE_LABELS[kind];
    const what = `${label.charAt(0).toUpperCase()}${label.slice(1)} de ${fuelName(value.fuelType)}`;
    const when = value.validFrom > this.today ? 'Empieza a regir el' : 'Rige desde el';
    this.toast.show(`${what} ${edited ? 'actualizado' : 'guardado'}. ${when} ${formatShortDate(value.validFrom)}.`);

    this.highlight.set({ kind, id: value.id });
    if (this.highlightTimer) clearTimeout(this.highlightTimer);
    this.highlightTimer = setTimeout(() => this.highlight.set(null), HIGHLIGHT_MS);
    this.scrollTo(`[data-history="${kind}"] [data-value-id="${value.id}"]`, 'nearest');
  }

  private fetch(onError: () => void): void {
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = forkJoin({
      sales: this.service.history('sale'),
      purchases: this.service.history('purchase')
    }).subscribe({
      next: ({ sales, purchases }) => {
        this.sales.set(sales);
        this.purchases.set(purchases);
        this.status.set('ready');
        this.applyParams();
      },
      error: onError
    });
  }

  private applyParams(): void {
    const params = this.pendingParams;
    if (!params) return;
    this.pendingParams = null;

    const wantedFuel = params.get('combustible')?.toUpperCase();
    const fuel = FUELS.find(item => item.type === wantedFuel)?.type;
    const kind = KIND_BY_PARAM[params.get('valor')?.toLowerCase() ?? ''];
    if (fuel) this.selected.set(fuel);
    if (kind) this.preferredKind = kind;

    if (['1', 'true', 'si'].includes(params.get('nuevo')?.toLowerCase() ?? '')) {
      this.openNew();
    } else if (kind && this.onPhone()) {
      this.scrollTo(`[data-history="${kind}"]`, 'start');
    }
  }

  private onPhone(): boolean {
    return typeof window !== 'undefined' && !!window.matchMedia?.(PHONE_QUERY).matches;
  }

  /** Waits for the screen to be drawn (and the panel to leave) before bringing the element into view. */
  private scrollTo(selector: string, block: ScrollLogicalPosition): void {
    if (this.scrollTimer) clearTimeout(this.scrollTimer);
    this.scrollTimer = setTimeout(() => {
      this.host.nativeElement.querySelector(selector)?.scrollIntoView?.({
        behavior: prefersReducedMotion() ? 'auto' : 'smooth',
        block
      });
    }, PANEL_LEAVE_MS);
  }
}
