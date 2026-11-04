import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
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

import { FuelAutonomyTableComponent } from '../../components/fuel-autonomy-table/fuel-autonomy-table.component';
import { FuelVariationTableComponent } from '../../components/fuel-variation-table/fuel-variation-table.component';
import { LubricantStockTableComponent } from '../../components/lubricant-stock-table/lubricant-stock-table.component';
import { StockMovementDialogComponent } from '../../components/stock-movement-dialog/stock-movement-dialog.component';
import { TankMeasurementsComponent } from '../../components/tank-measurements/tank-measurements.component';
import { FuelAutonomyService } from '../../../../core/services/fuel-autonomy.service';
import { FuelVariationsService } from '../../../../core/services/fuel-variations.service';
import { JournalService } from '../../../../core/services/journal.service';
import { ProductStockService } from '../../../../core/services/product-stock.service';
import { TankMeasurementsService } from '../../../../core/services/tank-measurements.service';
import { TanksService } from '../../../../core/services/tanks.service';
import { getErrorMessage, getFieldErrors } from '../../../../shared/helpers/problem-details.helpers';
import { isValidTankHeight } from '../../../../shared/helpers/inventory-rules.helpers';
import { FuelAutonomyResponse } from '../../../../shared/models/dtos/fuel-autonomy.dto';
import { FuelVariationResponse } from '../../../../shared/models/dtos/fuel-variation.dto';
import { ProductStockResponse, StockTransferRequest } from '../../../../shared/models/dtos/product-stock.dto';
import { TankMeasurementResponse } from '../../../../shared/models/dtos/tank-measurement.dto';
import { TankResponse } from '../../../../shared/models/dtos/tank.dto';
import { JournalResponse, JournalStatus, StepStatus } from '../../../../shared/models/journal.model';
import {
  MovementTarget,
  ProductFilter,
  StepTab,
  StockMovementRequest,
  TankHeightChange,
  TransferChange
} from '../../models/step5.model';

const STEP_NUMBER = 5;
const TOAST_DURATION_MS = 3000;

/** Paso 5 de la jornada: medición de los seis tanques (5a) y lubricantes y aditivos (5b). */
@Component({
  selector: 'app-journal-step5',
  standalone: true,
  imports: [
    FuelAutonomyTableComponent,
    FuelVariationTableComponent,
    LubricantStockTableComponent,
    StockMovementDialogComponent,
    TankMeasurementsComponent
  ],
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

  private readonly movementDialog = viewChild(StockMovementDialogComponent);

  readonly journal = toSignal(this.journalService.currentJournal$, { initialValue: null });
  readonly date = computed(() => this.journal()?.date ?? null);
  readonly step = computed(() => this.journal()?.steps.find(item => item.stepNumber === STEP_NUMBER) ?? null);
  readonly completed = computed(() => this.step()?.status === StepStatus.COMPLETED);
  readonly salesCompleted = computed(() =>
    this.journal()?.steps.find(item => item.stepNumber === 2)?.status === StepStatus.COMPLETED
  );
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
  readonly dialogError = signal<string | null>(null);
  readonly dialogFieldErrors = signal<Record<string, string>>({});
  readonly dialogSaving = signal(false);

  readonly measuredCount = computed(() =>
    this.tanks().filter(tank => isValidTankHeight(tank, this.heights()[tank.id])).length
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
    this.editable() && !this.completed() && this.salesCompleted() && this.savedAllTanks() && !this.savingHeights()
  );
  readonly canEditHeights = computed(() => this.editable() && !this.completed());
  readonly hasPendingTransfers = computed(() =>
    Object.values(this.pendingTransfers()).some(quantity => quantity !== null && quantity > 0)
  );

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

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));

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

  onHeightChanged(change: TankHeightChange): void {
    this.heights.update(current => ({ ...current, [change.tankId]: change.height }));
    this.recalculate$.next();
    this.formError.set(null);
  }

  onHeightsPasted(heights: Record<number, number | null>): void {
    const pasted = Object.keys(heights).length;
    if (pasted === 0) return;
    this.heights.update(current => ({ ...current, ...heights }));
    this.recalculate$.next();
    this.showToast(`${pasted} alturas pegadas.`);
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

  onTransferChanged(change: TransferChange): void {
    this.pendingTransfers.update(current => ({ ...current, [change.productId]: change.quantity }));
  }

  changeProductFilter(filterBy: ProductFilter): void {
    this.productFilter.set(filterBy);
  }

  setProductSearch(search: string): void {
    this.productSearch.set(search);
  }

  saveTransfers(): void {
    const date = this.date();
    if (!date || this.savingTransfers() || !this.editable()) return;
    const requests: StockTransferRequest[] = this.stock()
      .filter(product => {
        const quantity = this.pendingTransfers()[product.productId] ?? null;
        return quantity !== null && quantity > 0 && Number.isInteger(quantity) && quantity <= product.office.closing;
      })
      .map(product => ({
        productId: product.productId,
        quantity: this.pendingTransfers()[product.productId] as number,
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

  openMovement({ product, mode }: MovementTarget): void {
    if (!this.editable()) return;
    this.dialogError.set(null);
    this.dialogFieldErrors.set({});
    this.movementDialog()?.show({ mode, product });
  }

  onMovementConfirmed(request: StockMovementRequest): void {
    const date = this.date();
    if (!date || this.dialogSaving()) return;

    this.dialogError.set(null);
    this.dialogFieldErrors.set({});

    const { context, values } = request;
    const operation: Observable<unknown> = context.mode === 'initial'
      ? this.stockService.registerInitial({
          productId: context.product.productId,
          officeQuantity: values.officeQuantity as number,
          counterQuantity: values.counterQuantity as number,
          journalDate: date
        })
      : this.stockService.registerEntry({
          productId: context.product.productId,
          quantity: values.quantity as number,
          journalDate: date
        });

    this.dialogSaving.set(true);
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.dialogSaving.set(false);
        this.movementDialog()?.close();
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

  private calculateGallons(): Observable<Record<number, number | null>> {
    const heights = this.heights();
    const gallons: Record<number, number | null> = {};
    for (const tank of this.tanks()) {
      gallons[tank.id] = null;
    }
    const requests = this.tanks()
      .filter(tank => isValidTankHeight(tank, heights[tank.id]))
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

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
