import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild,
  viewChildren
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  forkJoin,
  map,
  Observable,
  of,
  switchMap
} from 'rxjs';

import { FuelAutonomyService } from '../../../../core/services/fuel-autonomy.service';
import { FuelVariationsService } from '../../../../core/services/fuel-variations.service';
import { JournalService } from '../../../../core/services/journal.service';
import { ProductStockService } from '../../../../core/services/product-stock.service';
import { TankMeasurementsService } from '../../../../core/services/tank-measurements.service';
import { TanksService } from '../../../../core/services/tanks.service';
import {
  FUEL_COLORS,
  FUEL_LABELS,
  PRODUCT_CATEGORY_LABELS,
  formatDays,
  formatGallons
} from '../../../../shared/helpers/inventory-display.helpers';
import { getErrorMessage, getFieldErrors } from '../../../../shared/helpers/problem-details.helpers';
import { FuelAutonomyResponse } from '../../../../shared/models/dtos/fuel-autonomy.dto';
import { FuelVariationResponse } from '../../../../shared/models/dtos/fuel-variation.dto';
import {
  ProductStockResponse,
  StockTransferRequest
} from '../../../../shared/models/dtos/product-stock.dto';
import { TankMeasurementResponse } from '../../../../shared/models/dtos/tank-measurement.dto';
import { FuelType, TankResponse } from '../../../../shared/models/dtos/tank.dto';
import { JournalResponse, JournalStatus, StepStatus } from '../../../../shared/models/journal.model';

const STEP_NUMBER = 5;
const TOAST_DURATION_MS = 3000;
/** Same as --duration-normal: the dialog is closed once its fade-out has finished. */
const DIALOG_CLOSE_DELAY_MS = 320;

type StepTab = 'tanques' | 'lubricantes';
type ProductFilter = 'movimiento' | 'todos' | 'LUBRICANTE' | 'ADITIVO' | 'OTRO';
type DialogMode = 'initial' | 'entry';

interface DialogContext {
  mode: DialogMode;
  product: ProductStockResponse;
}

/** Paso 5 de la jornada: medición de los seis tanques (5a) y lubricantes y aditivos (5b). */
@Component({
  selector: 'app-journal-step5',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './journal-step5.component.html',
  styleUrl: './journal-step5.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class JournalStep5Component {
  private readonly tanksService = inject(TanksService);
  private readonly measurementsService = inject(TankMeasurementsService);
  private readonly variationsService = inject(FuelVariationsService);
  private readonly autonomyService = inject(FuelAutonomyService);
  private readonly stockService = inject(ProductStockService);
  private readonly journalService = inject(JournalService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = viewChild<ElementRef<HTMLDialogElement>>('dialog');

  readonly heightInputs = viewChildren<ElementRef<HTMLInputElement>>('heightInput');

  readonly journal = toSignal(this.journalService.currentJournal$, { initialValue: null });
  readonly date = computed(() => this.journal()?.date ?? null);
  readonly step = computed(() => this.journal()?.steps.find(item => item.stepNumber === STEP_NUMBER) ?? null);
  readonly completed = computed(() => this.step()?.status === StepStatus.COMPLETED);
  readonly editable = computed(() => {
    const journal = this.journal();
    if (!journal || journal.status !== JournalStatus.OPEN) return false;
    const closing = journal.steps.find(item => item.stepNumber === 6);
    return closing?.status !== StepStatus.COMPLETED;
  });

  readonly tab = signal<StepTab>('tanques');
  readonly tanks = signal<TankResponse[]>([]);
  readonly heights = signal<Record<number, number | null>>({});
  readonly gallons = signal<Record<number, number | null>>({});
  readonly measurement = signal<TankMeasurementResponse | null>(null);
  readonly variations = signal<FuelVariationResponse[]>([]);
  readonly autonomy = signal<FuelAutonomyResponse[]>([]);
  readonly stock = signal<ProductStockResponse[]>([]);
  readonly pendingTransfers = signal<Record<number, number | null>>({});

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly savingHeights = signal(false);
  readonly savingTransfers = signal(false);
  readonly syncing = signal(false);
  readonly formError = signal<string | null>(null);
  readonly unmatchedCodes = signal<string[]>([]);
  readonly toastText = signal('');
  readonly toastVisible = signal(false);

  readonly productFilter = signal<ProductFilter>('movimiento');
  readonly productSearch = signal('');
  readonly dialogContext = signal<DialogContext | null>(null);
  readonly dialogOpen = signal(false);
  readonly dialogError = signal<string | null>(null);
  readonly dialogFieldErrors = signal<Record<string, string>>({});
  readonly dialogSaving = signal(false);
  readonly initialOffice = signal<number | null>(null);
  readonly initialCounter = signal<number | null>(null);
  readonly entryQuantity = signal<number | null>(null);

  readonly fuelLabels = FUEL_LABELS;
  readonly fuelColors = FUEL_COLORS;
  readonly categoryLabels = PRODUCT_CATEGORY_LABELS;
  readonly formatGallons = formatGallons;
  readonly formatDays = formatDays;

  readonly measuredCount = computed(() =>
    this.tanks().filter(tank => this.isValidHeight(tank, this.heights()[tank.id])).length
  );
  readonly allMeasured = computed(() =>
    this.tanks().length > 0 && this.measuredCount() === this.tanks().length
  );
  /** The step can only be completed after the six readings are stored in the backend. */
  readonly savedAllTanks = computed(() => {
    const measurement = this.measurement();
    if (!measurement || this.tanks().length === 0) return false;
    return this.tanks().every(tank => measurement.readings.some(reading => reading.tankId === tank.id));
  });
  readonly canComplete = computed(() =>
    this.editable() && !this.completed() && this.savedAllTanks() && !this.savingHeights()
  );
  readonly canEditHeights = computed(() => this.editable() && !this.completed());

  readonly visibleProducts = computed(() => {
    const query = this.productSearch().trim().toLowerCase();
    const filterBy = this.productFilter();
    return this.stock().filter(product => {
      if (query && !product.productName.toLowerCase().includes(query) && !product.productCode.includes(query)) {
        return false;
      }
      if (filterBy === 'todos') return true;
      if (filterBy === 'movimiento') {
        return product.office.sold + product.counter.sold + product.office.entries + product.office.transfers +
          product.counter.transfers + product.office.adjustments + product.counter.adjustments !== 0;
      }
      return product.category === filterBy;
    });
  });

  private readonly recalculate$ = new Subject<void>();
  private toastTimer?: ReturnType<typeof setTimeout>;
  private dialogTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => {
      clearTimeout(this.toastTimer);
      clearTimeout(this.dialogTimer);
    });

    this.journalService.currentJournal$
      .pipe(
        filter((journal): journal is JournalResponse => journal !== null),
        distinctUntilChanged((a, b) => a.id === b.id && a.date === b.date),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(journal => this.load(journal.date));

    this.recalculate$
      .pipe(
        debounceTime(300),
        switchMap(() => this.calculateGallons()),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(gallons => this.gallons.set(gallons));
  }

  private load(date: string): void {
    this.loading.set(true);
    this.loadError.set(null);
    forkJoin({
      tanks: this.tanksService.list(),
      measurement: this.measurementsService.findByJournalDate(date).pipe(catchError(() => of(null))),
      variations: this.variationsService.findByJournalDate(date).pipe(catchError(() => of([]))),
      autonomy: this.autonomyService.findByJournalDate(date).pipe(catchError(() => of([])))
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.tanks.set(result.tanks);
          this.measurement.set(result.measurement);
          this.variations.set(result.variations);
          this.autonomy.set(result.autonomy);
          this.applyMeasurement(result.measurement, result.tanks);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.loadError.set(getErrorMessage(error));
        }
      });

    this.loadProductStock(date);
  }

  private loadProductStock(date: string): void {
    this.stockService
      .listByJournalDate(date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rows => this.stock.set(rows),
        error: (error: unknown) => this.formError.set(getErrorMessage(error))
      });
  }

  /** Today's saved measurement fills the heights; the gallons come from the backend. */
  private applyMeasurement(measurement: TankMeasurementResponse | null, tanks: TankResponse[]): void {
    const heights: Record<number, number | null> = {};
    const gallons: Record<number, number | null> = {};
    for (const tank of tanks) {
      const reading = measurement?.readings.find(item => item.tankId === tank.id);
      heights[tank.id] = reading ? reading.heightCm : null;
      gallons[tank.id] = reading ? reading.volumeGallons : null;
    }
    this.heights.set(heights);
    this.gallons.set(gallons);
  }

  heightText(tankId: number): string {
    const height = this.heights()[tankId];
    return height === null || height === undefined ? '' : String(height).replace('.', ',');
  }

  isValidHeight(tank: TankResponse, height: number | null | undefined): boolean {
    return height !== null && height !== undefined && height > 0 && height <= tank.maxHeightCm;
  }

  isHeightInvalid(tank: TankResponse): boolean {
    const height = this.heights()[tank.id];
    return height !== null && height !== undefined && !this.isValidHeight(tank, height);
  }

  fillPercent(tank: TankResponse): string {
    const gallons = this.gallons()[tank.id];
    if (gallons === null || gallons === undefined || tank.maxCapacityGallons <= 0) return '—';
    return `${formatGallons((gallons / tank.maxCapacityGallons) * 100, 1)} %`;
  }

  onHeightInput(tank: TankResponse, event: Event): void {
    if (!this.canEditHeights()) return;
    const raw = (event.target as HTMLInputElement).value.replace(',', '.');
    const height = raw.trim() === '' ? null : Number(raw);
    this.heights.update(current => ({
      ...current,
      [tank.id]: height !== null && Number.isFinite(height) ? height : null
    }));
    this.recalculate$.next();
    this.formError.set(null);
  }

  onHeightKeydown(event: KeyboardEvent, index: number): void {
    if (event.key !== 'Enter' && event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;
    event.preventDefault();
    const offset = event.key === 'ArrowUp' || (event.key === 'Enter' && event.shiftKey) ? -1 : 1;
    const next = this.heightInputs()[index + offset]?.nativeElement;
    if (next) {
      next.focus();
      next.select();
    } else if (event.key === 'Enter') {
      (event.target as HTMLInputElement).blur();
    }
  }

  /** Pasting the six heights one per line fills the column from the focused row down. */
  onHeightsPaste(event: ClipboardEvent, index: number): void {
    if (!this.canEditHeights()) return;
    const text = event.clipboardData?.getData('text') ?? '';
    const parts = text.split(/\r?\n|\t/).map(part => part.trim()).filter(part => part !== '');
    if (parts.length <= 1) return;
    event.preventDefault();

    const tanks = this.tanks();
    const heights = { ...this.heights() };
    let pasted = 0;
    parts.forEach((part, offset) => {
      const tank = tanks[index + offset];
      if (!tank) return;
      const height = Number(part.replace(',', '.'));
      if (!Number.isFinite(height)) return;
      heights[tank.id] = height;
      pasted++;
    });
    this.heights.set(heights);
    this.recalculate$.next();
    if (pasted > 0) this.showToast(`${pasted} alturas pegadas.`);
  }

  private calculateGallons(): Observable<Record<number, number | null>> {
    const heights = this.heights();
    const gallons: Record<number, number | null> = {};
    for (const tank of this.tanks()) {
      gallons[tank.id] = null;
    }
    const requests = this.tanks()
      .filter(tank => this.isValidHeight(tank, heights[tank.id]))
      .map(tank => this.measurementsService.volume(tank.id, heights[tank.id] as number).pipe(
        map(result => ({ tankId: result.tankId, gallons: result.volumeGallons })),
        catchError(() => of(null))
      ));
    if (requests.length === 0) return of(gallons);
    return forkJoin(requests).pipe(
      map(results => {
        for (const result of results) {
          if (result) gallons[result.tankId] = result.gallons;
        }
        return gallons;
      })
    );
  }

  saveMeasurements(): void {
    const date = this.date();
    if (!date || this.savingHeights() || !this.canEditHeights()) return;
    if (!this.allMeasured()) {
      this.formError.set('Digita la altura de los 6 tanques antes de guardar la medición.');
      return;
    }
    const readings = this.tanks().map(tank => ({ tankId: tank.id, heightCm: this.heights()[tank.id] as number }));
    this.savingHeights.set(true);
    this.formError.set(null);
    this.measurementsService
      .save(date, { readings })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: measurement => {
          this.savingHeights.set(false);
          this.measurement.set(measurement);
          this.applyMeasurement(measurement, this.tanks());
          this.reloadVariationAndAutonomy(date);
          this.showToast('Medición del día guardada.');
        },
        error: (error: unknown) => {
          this.savingHeights.set(false);
          this.formError.set(getErrorMessage(error));
        }
      });
  }

  private reloadVariationAndAutonomy(date: string): void {
    this.variationsService
      .findByJournalDate(date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(variations => this.variations.set(variations));
    this.autonomyService
      .findByJournalDate(date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(autonomy => this.autonomy.set(autonomy));
  }

  completeStep(): void {
    const journal = this.journal();
    if (!journal || !this.canComplete()) return;
    this.journalService
      .completeStep(journal.id, STEP_NUMBER)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: step => {
          this.journalService.updateCurrentStep(step);
          this.showToast('Paso 5 marcado como completado.');
        },
        error: (error: unknown) => this.formError.set(getErrorMessage(error))
      });
  }

  variationLabel(variation: FuelVariationResponse): string {
    if (variation.status === 'NO_PREVIOUS_MEASUREMENT') return 'Sin medición anterior';
    if (variation.status === 'INCOMPLETE_MEASUREMENT') return 'Medición incompleta';
    if (variation.status === 'NO_SALES_DATA') return 'Sin ventas del paso 2';
    return variation.outOfTolerance ? 'Fuera de tolerancia' : 'Cuadra';
  }

  variationTone(variation: FuelVariationResponse): string {
    if (variation.status !== 'OK') return 'jr-chip--aviso';
    return variation.outOfTolerance ? 'jr-chip--falta' : 'jr-chip--cerrada';
  }

  autonomyLabel(autonomy: FuelAutonomyResponse): string {
    if (autonomy.status === 'INCOMPLETE_MEASUREMENT') return 'Falta medición';
    if (autonomy.status === 'NO_SALES_HISTORY') return 'Sin historial de ventas';
    if (autonomy.level === 'CRITICAL') return 'Crítico';
    if (autonomy.level === 'WARNING') return 'Advertencia';
    return 'Normal';
  }

  autonomyTone(autonomy: FuelAutonomyResponse): string {
    if (autonomy.level === 'CRITICAL') return 'jr-chip--falta';
    if (autonomy.level === 'WARNING') return 'jr-chip--aviso';
    if (autonomy.level === 'OK') return 'jr-chip--cerrada';
    return 'jr-chip--pendiente';
  }

  transferOf(productId: number): number | null {
    return this.pendingTransfers()[productId] ?? null;
  }

  transferInvalid(product: ProductStockResponse): boolean {
    const quantity = this.transferOf(product.productId);
    return quantity !== null && (!Number.isInteger(quantity) || quantity < 0 || quantity > product.office.closing);
  }

  onTransferInput(product: ProductStockResponse, event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    const quantity = raw === '' ? null : Number(raw);
    this.pendingTransfers.update(current => ({
      ...current,
      [product.productId]: quantity !== null && Number.isFinite(quantity) ? quantity : null
    }));
  }

  changeProductFilter(filter: ProductFilter): void {
    this.productFilter.set(filter);
  }

  hasPendingTransfers(): boolean {
    return Object.values(this.pendingTransfers()).some(quantity => quantity !== null && quantity > 0);
  }

  saveTransfers(): void {
    const date = this.date();
    if (!date || this.savingTransfers() || !this.editable()) return;
    const requests: StockTransferRequest[] = this.stock()
      .filter(product => {
        const quantity = this.transferOf(product.productId);
        return quantity !== null && quantity > 0 && Number.isInteger(quantity) && quantity <= product.office.closing;
      })
      .map(product => ({
        productId: product.productId,
        quantity: this.transferOf(product.productId) as number,
        journalDate: date
      }));

    if (requests.length === 0) {
      this.formError.set('No hay traslados válidos para guardar.');
      return;
    }

    this.savingTransfers.set(true);
    this.formError.set(null);
    forkJoin(requests.map(request => this.stockService.registerTransfer(request)))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingTransfers.set(false);
          this.pendingTransfers.set({});
          this.loadProductStock(date);
          this.showToast(`${requests.length} traslado${requests.length === 1 ? '' : 's'} guardado${requests.length === 1 ? '' : 's'}.`);
        },
        error: (error: unknown) => {
          this.savingTransfers.set(false);
          this.loadProductStock(date);
          this.formError.set(getErrorMessage(error));
        }
      });
  }

  syncSales(): void {
    const date = this.date();
    if (!date || this.syncing() || !this.editable()) return;
    this.syncing.set(true);
    this.formError.set(null);
    this.unmatchedCodes.set([]);
    this.stockService
      .syncSales(date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.syncing.set(false);
          this.unmatchedCodes.set(result.unmatchedCodes);
          this.loadProductStock(date);
          this.showToast(`${result.syncedProducts} productos actualizados con las ventas del paso 2.`);
        },
        error: (error: unknown) => {
          this.syncing.set(false);
          this.formError.set(getErrorMessage(error));
        }
      });
  }

  openDialog(product: ProductStockResponse, mode: DialogMode): void {
    if (!this.editable()) return;
    this.dialogContext.set({ product, mode });
    this.initialOffice.set(null);
    this.initialCounter.set(null);
    this.entryQuantity.set(null);
    this.dialogError.set(null);
    this.dialogFieldErrors.set({});
    const dialog = this.dialogRef()?.nativeElement;
    if (dialog && !dialog.open) dialog.showModal();
    clearTimeout(this.dialogTimer);
    requestAnimationFrame(() => this.dialogOpen.set(true));
  }

  closeDialog(): void {
    this.dialogOpen.set(false);
    clearTimeout(this.dialogTimer);
    this.dialogTimer = setTimeout(() => this.dialogRef()?.nativeElement.close(), DIALOG_CLOSE_DELAY_MS);
  }

  onDialogCancel(event: Event): void {
    event.preventDefault();
    this.closeDialog();
  }

  dialogFieldError(field: string): string {
    return this.dialogFieldErrors()[field] ?? '';
  }

  confirmDialog(): void {
    const context = this.dialogContext();
    const date = this.date();
    if (!context || !date || this.dialogSaving()) return;

    this.dialogError.set(null);
    this.dialogFieldErrors.set({});

    if (context.mode === 'initial') {
      const office = this.initialOffice();
      const counter = this.initialCounter();
      if (office === null || counter === null || !Number.isInteger(office) || !Number.isInteger(counter) ||
          office < 0 || counter < 0) {
        this.dialogError.set('Las existencias deben ser números enteros mayores o iguales a cero.');
        return;
      }
    } else {
      const quantity = this.entryQuantity();
      if (quantity === null || !Number.isInteger(quantity) || quantity <= 0) {
        this.dialogError.set('La cantidad debe ser un número entero mayor que cero.');
        return;
      }
    }

    const operation: Observable<unknown> = context.mode === 'initial'
      ? this.stockService.registerInitial({
          productId: context.product.productId,
          officeQuantity: this.initialOffice() as number,
          counterQuantity: this.initialCounter() as number,
          journalDate: date
        })
      : this.stockService.registerEntry({
          productId: context.product.productId,
          quantity: this.entryQuantity() as number,
          journalDate: date
        });

    this.dialogSaving.set(true);
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.dialogSaving.set(false);
        this.closeDialog();
        this.loadProductStock(date);
        this.showToast(context.mode === 'initial' ? 'Existencia inicial registrada.' : 'Entrada registrada.');
      },
      error: (error: unknown) => {
        this.dialogSaving.set(false);
        const mapped = getFieldErrors(error).reduce<Record<string, string>>((result, fieldError) => {
          result[fieldError.field] = fieldError.message;
          return result;
        }, {});
        if (Object.keys(mapped).length > 0) {
          this.dialogFieldErrors.set(mapped);
        } else {
          this.dialogError.set(getErrorMessage(error));
        }
      }
    });
  }

  existingTotal(product: ProductStockResponse): number {
    return product.office.closing + product.counter.closing;
  }

  counterAfterTransfer(product: ProductStockResponse): number {
    return product.counter.closing + (this.transferOf(product.productId) ?? 0);
  }

  fuelLabel(fuelType: FuelType): string {
    return FUEL_LABELS[fuelType];
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
