import { Component, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { ColorThemeService } from '../../../../core/services/color-theme.service';
import { SalesVendorCloseService } from '../../../../core/services/sales-vendor-close.service';
import { JournalService } from '../../../../core/services/journal.service';
import { ShiftHoursService } from '../../../../core/services/shift-hours.service';
import { JournalStepChangesService, StepDirtyHandler } from '../../../../core/services/journal-step-changes.service';
import { ProductsService } from '../../../../core/services/products.service';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { FUELS } from '../../../../shared/helpers/fuel-price.helpers';
import { FuelType } from '../../../../shared/models/dtos/tank.dto';
import { JournalCellNoteResponse, JournalStatus, StepStatus } from '../../../../shared/models/journal.model';
import {
  AssignedVendorDto,
  CatalogProductItem,
  DenominationsBreakdown,
  VendorCloseSummaryDto,
  VendorGridCapture,
  VendorShiftCloseBatchRequestDto,
  VendorShiftCloseItemRequestDto
} from '../../../../shared/models/dtos/vendor-close.dto';

export interface ActiveCellNoteTarget {
  columnKey: string;
  vendorName: string;
  fieldKey: string;
  fieldLabel: string;
}

export interface CellNoteSummaryItem {
  columnKey: string;
  vendorName: string;
  fieldKey: string;
  fieldLabel: string;
  note: string;
}

export interface FloatingNoteCallout {
  vendorName: string;
  fieldLabel: string;
  note: string;
  x: number;
  y: number;
  placement: 'above' | 'below';
}

export const STEP2_NOTE_FIELD_LABELS: Record<string, string> = {
  descuadre: 'Descuadre real',
  parciales: 'Parciales',
  pico: 'Pico',
  efectivo: 'Total efectivo',
  ga: 'G.A.',
  credibanco: 'Credibanco',
  qr: 'QR',
  redenciones: 'Redenciones Puntos Colombia',
  totalEntregado: 'Total entregado',
  galMotor: 'Galones Motor',
  galDiesel: 'Galones Diésel',
  galExtra: 'Galones Extra',
  galMaxpro: 'Galones Max Pro',
  canastilla: 'Canastilla',
  tirilla: 'Total (tirilla)'
};

export const DENOMINATIONS: [keyof DenominationsBreakdown, number][] = [
  ['b100k', 100000],
  ['b50k', 50000],
  ['b20k', 20000],
  ['b10k', 10000],
  ['b5k', 5000],
  ['b2k', 2000]
];

/** Name of each fuel of the backend in the effective prices of the grid. */
const FUEL_PRICE_KEYS: Record<FuelType, string> = {
  MOTOR: 'motor',
  DIESEL: 'diesel',
  EXTRA: 'extra',
  MAX_PRO: 'maxpro'
};

/** Why the difference against the slip is not calculated. */
export interface FuelPriceNotice {
  text: string;
  /** Fuel to open in the prices screen; null when the prices could not be read. */
  fuelType: FuelType | null;
}

@Component({
  selector: 'app-vendor-close-step2',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ActionButtonDirective],
  templateUrl: './vendor-close-step2.component.html',
  styleUrls: ['./vendor-close-step2.component.css']
})
export class VendorCloseStep2Component implements OnInit {
  private readonly salesService = inject(SalesVendorCloseService);
  private readonly journalService = inject(JournalService, { optional: true });
  private readonly productsService = inject(ProductsService, { optional: true });
  private readonly stepChangesService = inject(JournalStepChangesService, { optional: true });
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly shiftHours = inject(ShiftHoursService);
  private readonly colorTheme = inject(ColorThemeService, { optional: true });
  private savedInputSnapshot = '';
  private readonly dirtyHandler: StepDirtyHandler = {
    hasUnsavedChanges: () => this.hasUnsavedChanges(),
    saveBeforeExit: () => this.persistVendorCloses()
  };

  readonly denominations = DENOMINATIONS;
  readonly packetValue = 200000;

  vendorColor(vendor: AssignedVendorDto): string | null {
    return this.colorTheme?.resolveWorkerColor({ document: vendor.document, name: vendor.name })?.background ?? null;
  }

  journalDate = '2026-08-05';
  isReadOnly = false;
  assignedVendors: AssignedVendorDto[] = [];
  captures: Record<string, VendorGridCapture> = {};

  tolerance = 0.5;
  /** Sale prices that rule on the journal date; a fuel without one is absent. */
  fuelPrices: Record<string, number> = {};
  private fuelPricesState: 'loading' | 'ready' | 'error' = 'loading';
  catalogProducts: CatalogProductItem[] = [];
  private readonly defaultProductSkus: readonly string[] = ['76102', '75089', '74063', '80266'];
  visibleProductSkus: string[] = [...this.defaultProductSkus];
  productSearchInput = '';

  folded = {
    arqueo: true,
    productos: true
  };

  isDominusDrawerOpen = false;
  isStep1Complete = true;
  isLoading = false;
  isSaving = false;
  isResetting = false;
  showResetConfirm = false;
  saveSuccessMessage = '';
  errorMessage = '';
  toastMessage = '';
  toastVisible = false;
  private toastTimer: ReturnType<typeof setTimeout> | null = null;
  private loadedDate: string | null = null;
  currentJournalId: number | null = null;
  private loadedNotesJournalId: number | null = null;

  readonly maxNoteLength = 500;
  cellNotes: Record<string, string> = {};
  activeNoteTarget: ActiveCellNoteTarget | null = null;
  noteDraft = '';
  isSavingNote = false;
  noteErrorMessage = '';
  highlightedNoteCellKey: string | null = null;
  highlightedNoteFieldKey: string | null = null;
  floatingNoteCallout: FloatingNoteCallout | null = null;
  private highlightTimer: ReturnType<typeof setTimeout> | null = null;
  private calloutRepositionTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.stepChangesService?.register(this.dirtyHandler);
    this.destroyRef.onDestroy(() => {
      this.stepChangesService?.unregister(this.dirtyHandler);
      this.clearNoteHighlightTimers();
    });

    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        if (params['date']) {
          this.journalDate = params['date'];
        } else {
          const snapshot = this.journalService?.getCurrentJournalSnapshot?.();
          if (snapshot?.date) {
            this.journalDate = snapshot.date;
          }
          if (snapshot?.id) {
            this.currentJournalId = snapshot.id;
            this.loadCellNotes(snapshot.id);
          }
        }
        this.ensureDataLoadedForDate();
      });

    if (this.journalService) {
      this.journalService.currentJournal$
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(journal => {
          if (!journal) return;
          this.currentJournalId = journal.id;
          const step1 = journal.steps?.find(s => s.stepNumber === 1);
          this.isStep1Complete = step1?.status === StepStatus.COMPLETED;
          const closingStep = journal.steps?.find(s => s.stepNumber === 6);
          this.isReadOnly = journal.status === JournalStatus.CLOSED || closingStep?.status === StepStatus.COMPLETED;
          if (journal.date && journal.date !== this.journalDate) {
            this.journalDate = journal.date;
          }
          this.ensureDataLoadedForDate();
          this.loadCellNotes(journal.id);
          this.cdr.detectChanges();
        });
    }
  }

  private ensureDataLoadedForDate(): void {
    if (this.loadedDate === this.journalDate) return;
    this.initData();
  }

  initData(): void {
    this.loadedDate = this.journalDate;
    this.catalogProducts = this.salesService.getCatalogProducts();
    this.loadProductCatalog();
    this.loadFuelPrices();
    this.loadTolerance();
    this.loadStaffAndCloses();
  }

  loadProductCatalog(): void {
    if (!this.productsService) return;
    this.productsService.list({ active: true }).subscribe({
      next: products => {
        if (!products || products.length === 0) return;
        this.catalogProducts = products.map(p => ({
          sku: p.code,
          name: p.name,
          category: p.category,
          price: Number(p.price) || 0
        }));
        const catalogSkus = new Set(this.catalogProducts.map(p => p.sku));
        const matchingDefaults = this.defaultProductSkus.filter(sku => catalogSkus.has(sku));
        if (matchingDefaults.length === 0 && Object.values(this.captures).every(c => !c.conDatos)) {
          this.visibleProductSkus = this.catalogProducts.slice(0, 4).map(p => p.sku);
        }
        this.cdr.detectChanges();
      },
      error: () => {
        // Keep local fallback catalog when inventory service has not been populated yet.
      }
    });
  }

  loadFuelPrices(): void {
    this.fuelPrices = {};
    this.fuelPricesState = 'loading';
    const loader = (this.salesService as { getEffectiveFuelPrices?: (date: string) => ReturnType<SalesVendorCloseService['getEffectiveFuelPrices']> }).getEffectiveFuelPrices;
    if (!loader) return;
    const date = this.journalDate;
    loader.call(this.salesService, date).subscribe({
      next: prices => {
        // A late answer for a day that is no longer on screen is ignored.
        if (date !== this.journalDate) return;
        this.applyEffectiveFuelPrices(prices ?? {});
        this.fuelPricesState = 'ready';
        this.recalculateAll();
      },
      error: () => {
        if (date !== this.journalDate) return;
        this.fuelPricesState = 'error';
        this.recalculateAll();
      }
    });
  }

  private applyEffectiveFuelPrices(prices: Record<string, number>): void {
    for (const [rawKey, value] of Object.entries(prices)) {
      const key = FUEL_PRICE_KEYS[rawKey.toUpperCase() as FuelType];
      const numeric = Number(value);
      if (key && Number.isFinite(numeric) && numeric > 0) {
        this.fuelPrices[key] = numeric;
      }
    }
  }

  private missingFuelPrices() {
    return FUELS.filter(fuel => this.fuelPrices[FUEL_PRICE_KEYS[fuel.type]] === undefined);
  }

  /** Note of the "Diferencia contra la tirilla" row; null while loading and when every fuel has a price. */
  get fuelPriceNotice(): FuelPriceNotice | null {
    if (this.fuelPricesState === 'loading') return null;
    if (this.fuelPricesState === 'error') {
      return {
        text: 'No se pudieron consultar los precios de venta, así que no se calcula esta diferencia.',
        fuelType: null
      };
    }
    const missing = this.missingFuelPrices();
    if (missing.length === 0) return null;
    const names = missing.map(fuel => fuel.name);
    const listed = names.length === 1 ? names[0] : `${names.slice(0, -1).join(', ')} y ${names[names.length - 1]}`;
    const which = missing.length === FUELS.length ? '' : ` de ${listed}`;
    return {
      text: `No hay precio de venta${which} cargado para el ${formatShortDate(this.journalDate)}, así que no se calcula esta diferencia.`,
      fuelType: missing[0].type
    };
  }

  loadTolerance(): void {
    this.salesService.getEffectiveTolerance(this.journalDate).subscribe({
      next: (dto) => {
        if (dto && dto.maxDiscrepancyAmount !== undefined) {
          this.tolerance = dto.maxDiscrepancyAmount;
        }
        this.recalculateAll();
        this.cdr.detectChanges();
      },
      error: () => {
        this.tolerance = 0.5;
        this.recalculateAll();
      }
    });
  }

  loadStaffAndCloses(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.salesService.getAssignedStaff(this.journalDate).subscribe({
      next: (staff) => {
        if (staff && staff.length > 0) {
          this.assignedVendors = staff;
        } else if (this.journalDate === '2026-08-05') {
          this.assignedVendors = this.salesService.getCalibrationDemo0805().vendedores;
        } else {
          this.assignedVendors = [];
        }
        this.initGrid();
        this.loadExistingCloses();
      },
      error: () => {
        if (this.journalDate === '2026-08-05') {
          this.assignedVendors = this.salesService.getCalibrationDemo0805().vendedores;
        } else {
          this.assignedVendors = [];
          this.errorMessage = 'No se pudo cargar el personal asignado desde el servidor.';
        }
        this.initGrid();
        this.loadExistingCloses();
      }
    });
  }

  private initGrid(): void {
    this.captures = {};
    for (const vendor of this.assignedVendors) {
      this.captures[vendor.document] = this.createEmptyCapture(vendor);
    }
    this.cdr.detectChanges();
  }

  private createEmptyCapture(vendor: AssignedVendorDto): VendorGridCapture {
    return {
      vendor,
      parciales: 0,
      pico: 0,
      efectivo: 0,
      ga: 0,
      credibanco: 0,
      qr: 0,
      redenciones: 0,
      totalEntregado: 0,
      galMotor: 0,
      galDiesel: 0,
      galExtra: 0,
      galMaxpro: 0,
      canastilla: 0,
      tirilla: 0,
      descuadre: 0,
      estadoResultado: 'vacio',
      denominaciones: {
        b100k: 0,
        b50k: 0,
        b20k: 0,
        b10k: 0,
        b5k: 0,
        b2k: 0
      },
      totalArqueo: 0,
      avisoArqueo: false,
      avisoTirilla: null,
      productos: {},
      conDatos: false
    };
  }

  loadExistingCloses(): void {
    this.salesService.getVendorCloses(this.journalDate).subscribe({
      next: (closes) => {
        this.isLoading = false;
        if (closes && closes.length > 0) {
          this.populateFromBackendCloses(closes);
          this.errorMessage = '';
        } else if (this.journalDate === '2026-08-05') {
          this.loadDemoHoja05(false);
        } else if (this.assignedVendors.length === 0 && !this.errorMessage) {
          this.errorMessage = 'No hay vendedores asignados para esta jornada. Confirma el personal en el Paso 1.';
        }
        this.recalculateAll();
        this.markAsClean();
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        if (this.journalDate === '2026-08-05') {
          this.loadDemoHoja05(false);
        } else if (!this.errorMessage) {
          this.errorMessage = 'No se pudieron consultar los cierres guardados en el servidor.';
        }
        this.recalculateAll();
        this.markAsClean();
        this.cdr.detectChanges();
      }
    });
  }

  private populateFromBackendCloses(closes: VendorCloseSummaryDto[]): void {
    for (const close of closes) {
      let cap = this.captures[close.vendorDocument];
      if (!cap) {
        const matchedVendor = this.findMatchingVendor(close.vendorName, close.shiftName);
        if (matchedVendor) {
          cap = this.captures[matchedVendor.document];
        } else {
          const fallbackVendor: AssignedVendorDto = {
            document: close.vendorDocument,
            name: close.vendorName,
            role: 'ISLERO',
            shiftName: close.shiftName,
            pumpNumber: close.pumpNumber ?? 1,
            workerCode: close.shiftName
          };
          this.assignedVendors.push(fallbackVendor);
          cap = this.createEmptyCapture(fallbackVendor);
          this.captures[fallbackVendor.document] = cap;
        }
      }
      if (!cap) continue;
      cap.conDatos = true;
      cap.efectivo = Number(close.cash || 0);
      cap.parciales = Math.floor(cap.efectivo / this.packetValue);
      cap.pico = cap.efectivo % this.packetValue;
      cap.ga = Number(close.ga || 0);
      cap.credibanco = Number(close.credibanco || 0);
      cap.qr = Number(close.qr || 0);
      cap.redenciones = Number(close.redemptions || 0);
      cap.galMotor = Number(close.gallonsMotor || 0);
      cap.galDiesel = Number(close.gallonsDiesel || 0);
      cap.galExtra = Number(close.gallonsExtra || 0);
      cap.galMaxpro = Number(close.gallonsMaxpro || 0);
      cap.canastilla = Number(close.basketDominusAmount || 0);
      cap.tirilla = Number(close.dominusTirilla || 0);

      if (close.products) {
        for (const p of close.products) {
          cap.productos[p.sku] = p.unitsSold;
          if (!this.visibleProductSkus.includes(p.sku)) {
            this.visibleProductSkus.push(p.sku);
          }
        }
      }
    }
  }

  private findMatchingVendor(name: string, shiftName?: string): AssignedVendorDto | undefined {
    const targetName = this.normalizeName(name);
    if (!targetName) return undefined;
    const sameName = this.assignedVendors.filter(v => this.normalizeName(v.name) === targetName);
    if (sameName.length === 0) return undefined;
    if (shiftName) {
      const sameShift = sameName.find(v => v.shiftName === shiftName);
      if (sameShift) return sameShift;
    }
    return sameName[0];
  }

  private normalizeName(value: string | null | undefined): string {
    if (!value) return '';
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, ' ')
      .trim()
      .toLowerCase();
  }

  loadDemoHoja05(notify = true): void {
    const demo = this.salesService.getCalibrationDemo0805();
    const docTargetMap: Record<string, string> = {};
    const skippedVendors: string[] = [];

    for (const [doc, data] of Object.entries(demo.capturas)) {
      let targetDoc = doc;
      if (!this.captures[targetDoc]) {
        const foundStaff = demo.vendedores.find(v => v.document === doc);
        const existingByName = foundStaff
          ? this.findMatchingVendor(foundStaff.name, foundStaff.shiftName)
          : undefined;
        if (existingByName && this.captures[existingByName.document]) {
          targetDoc = existingByName.document;
        } else if (foundStaff) {
          // A vendor who is not in the staff of the day gets no column: its close could not be registered
          skippedVendors.push(foundStaff.name);
        }
      }
      docTargetMap[doc] = targetDoc;
      const cap = this.captures[targetDoc];
      if (cap) {
        cap.conDatos = true;
        cap.parciales = data.parciales;
        cap.pico = data.pico;
        cap.ga = data.ga;
        cap.credibanco = data.credibanco;
        cap.qr = data.qr;
        cap.redenciones = data.redenciones;
        cap.galMotor = data.galMotor;
        cap.galDiesel = data.galDiesel;
        cap.galExtra = data.galExtra;
        cap.galMaxpro = data.galMaxpro;
        cap.canastilla = data.canastilla;
        cap.tirilla = data.tirilla;
        cap.denominaciones = {
          b100k: data.b100k,
          b50k: data.b50k,
          b20k: data.b20k,
          b10k: data.b10k,
          b5k: data.b5k,
          b2k: data.b2k
        };
      }
    }

    for (const [sku, vendorUnits] of Object.entries(demo.productos)) {
      if (!this.visibleProductSkus.includes(sku)) {
        this.visibleProductSkus.push(sku);
      }
      for (const [doc, units] of Object.entries(vendorUnits)) {
        const targetDoc = docTargetMap[doc] ?? doc;
        if (this.captures[targetDoc]) {
          this.captures[targetDoc].productos[sku] = units;
        }
      }
    }

    this.recalculateAll();
    if (notify) {
      this.showToast(skippedVendors.length > 0
        ? `Hoja 05 cargada. Sin columna en esta jornada (no están en el personal del día): ${skippedVendors.join(', ')}`
        : 'Hoja 05 de calibración cargada exitosamente');
    }
  }

  onInputFocus(event: FocusEvent): void {
    const input = event.target as HTMLInputElement | null;
    input?.select();
  }

  onFieldChange(doc: string): void {
    if (this.isReadOnly) return;
    const cap = this.captures[doc];
    if (cap) {
      cap.conDatos = true;
      this.recalculate(doc);
    }
  }

  recalculate(doc: string): void {
    const cap = this.captures[doc];
    if (!cap) return;

    // Planilla física
    cap.efectivo = (Number(cap.parciales) || 0) * this.packetValue + (Number(cap.pico) || 0);

    // Total entregado
    cap.totalEntregado = cap.efectivo +
      (Number(cap.ga) || 0) +
      (Number(cap.credibanco) || 0) +
      (Number(cap.qr) || 0) +
      (Number(cap.redenciones) || 0);

    // Arqueo
    let sumArqueo = 0;
    for (const [key, denom] of this.denominations) {
      sumArqueo += (Number(cap.denominaciones[key]) || 0) * denom;
    }
    cap.totalArqueo = sumArqueo;
    cap.avisoArqueo = sumArqueo > 0 && sumArqueo !== cap.efectivo;

    // Descuadre real
    const tirilla = Number(cap.tirilla) || 0;
    if (!cap.conDatos || (cap.totalEntregado === 0 && tirilla === 0)) {
      cap.descuadre = 0;
      cap.estadoResultado = 'vacio';
    } else {
      cap.descuadre = Math.round((cap.totalEntregado - tirilla) * 100) / 100;
      if (Math.abs(cap.descuadre) <= this.tolerance) {
        cap.estadoResultado = 'cuadra';
      } else if (cap.descuadre > this.tolerance) {
        cap.estadoResultado = 'sobra';
      } else {
        cap.estadoResultado = 'falta';
      }
    }

    // Validación tirilla vs galones: solo con el precio de venta de todos los combustibles
    if (tirilla > 0 && this.missingFuelPrices().length === 0) {
      const fuelSales = (Number(cap.galMotor) || 0) * this.fuelPrices['motor'] +
        (Number(cap.galDiesel) || 0) * this.fuelPrices['diesel'] +
        (Number(cap.galExtra) || 0) * this.fuelPrices['extra'] +
        (Number(cap.galMaxpro) || 0) * this.fuelPrices['maxpro'] +
        (Number(cap.canastilla) || 0);

      if (Math.abs(fuelSales - tirilla) > 100) {
        cap.avisoTirilla = `La tirilla difiere de galones × precio (${this.formatCurrency(fuelSales)}). Revisa lo pegado.`;
      } else {
        cap.avisoTirilla = null;
      }
    } else {
      cap.avisoTirilla = null;
    }
  }

  recalculateAll(): void {
    for (const doc of Object.keys(this.captures)) {
      this.recalculate(doc);
    }
    this.cdr.detectChanges();
  }

  toggleFold(section: 'arqueo' | 'productos'): void {
    this.folded[section] = !this.folded[section];
    if (section === 'productos' && !this.folded.productos && !this.folded.arqueo) {
      this.folded.arqueo = true;
    }
  }

  addProductFromSearch(): void {
    const query = this.productSearchInput.trim();
    if (!query) return;

    const skuPart = query.includes('·') ? query.split('·').pop()?.trim() ?? '' : query;
    const found =
      this.catalogProducts.find(p => p.sku === query || (skuPart !== '' && p.sku === skuPart)) ??
      this.catalogProducts.find(p => p.name.toLowerCase().includes(query.toLowerCase()));

    if (found) {
      if (!this.visibleProductSkus.includes(found.sku)) {
        this.visibleProductSkus.push(found.sku);
        this.folded.productos = false;
        this.folded.arqueo = true;
        this.showToast(`Producto agregado: ${found.name}`);
      } else {
        this.showToast('Ese producto ya está visible en la lista');
      }
    } else {
      this.showToast('Producto no encontrado en el catálogo');
    }
    this.productSearchInput = '';
  }

  getProductName(sku: string): string {
    const found = this.catalogProducts.find(p => p.sku === sku);
    return found ? found.name : `Producto SKU ${sku}`;
  }

  getProductCategory(sku: string): string {
    const found = this.catalogProducts.find(p => p.sku === sku);
    return found?.category ? found.category : 'OTRO';
  }

  getProductPrice(sku: string): number {
    const found = this.catalogProducts.find(p => p.sku === sku);
    return found ? found.price : 0;
  }

  // --- Keyboard Navigation & Multi-Row Paste ---
  onKeyDown(event: KeyboardEvent, fila: string, colDoc: string): void {
    if (event.key === 'Enter' || event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const currentInput = event.target as HTMLInputElement;
      const inputsInCol = Array.from(document.querySelectorAll(`input[data-col="${colDoc}"]`)) as HTMLInputElement[];
      const currentIndex = inputsInCol.indexOf(currentInput);
      const isUp = event.key === 'ArrowUp' || (event.key === 'Enter' && event.shiftKey);
      const targetInput = inputsInCol[isUp ? currentIndex - 1 : currentIndex + 1];
      if (targetInput) {
        targetInput.focus();
        targetInput.select();
      }
    } else if (event.key === 'Tab') {
      const currentInput = event.target as HTMLInputElement;
      const fila = currentInput.getAttribute('data-fila');
      const docs = this.assignedVendors.map(v => v.document);
      const colIndex = docs.indexOf(colDoc);
      const nextColIndex = colIndex + (event.shiftKey ? -1 : 1);
      if (nextColIndex >= 0 && nextColIndex < docs.length) {
        const nextDoc = docs[nextColIndex];
        const nextInput = document.querySelector(`input[data-fila="${fila}"][data-col="${nextDoc}"]`) as HTMLInputElement;
        if (nextInput) {
          event.preventDefault();
          nextInput.focus();
          nextInput.select();
        }
      }
    }
  }

  onPaste(event: ClipboardEvent, startFila: string, colDoc: string): void {
    const clipboardData = event.clipboardData || (window as Window & { clipboardData?: DataTransfer }).clipboardData;
    if (!clipboardData) return;

    const pastedText = clipboardData.getData('text');
    const lines = pastedText.split(/\r?\n|\t/).map((s: string) => s.trim()).filter((s: string) => s.length > 0);
    if (lines.length <= 1) return; // Single line paste handled normally

    event.preventDefault();
    const inputsInCol = Array.from(document.querySelectorAll(`input[data-col="${colDoc}"]`)) as HTMLInputElement[];
    const currentInput = event.target as HTMLInputElement;
    const startIndex = inputsInCol.indexOf(currentInput);

    let pastedCount = 0;
    for (let i = 0; i < lines.length && (startIndex + i) < inputsInCol.length; i++) {
      const input = inputsInCol[startIndex + i];
      const parsedNum = this.parseNumber(lines[i]);
      if (parsedNum !== null && !isNaN(parsedNum)) {
        input.value = String(parsedNum);
        input.dispatchEvent(new Event('input'));
        pastedCount++;
      }
    }

    this.recalculate(colDoc);
    this.showToast(`Se pegaron ${pastedCount} valores en la columna`);
  }

  parseNumber(str: string): number | null {
    if (!str) return null;
    const s = String(str).replace(/[\s$.]/g, '').replace(',', '.');
    const n = parseFloat(s);
    return isNaN(n) ? null : n;
  }

  // --- Day Summary Calculations ---
  totalCash(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (c.efectivo || 0), 0);
  }

  totalParciales(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.parciales) || 0), 0);
  }

  totalPico(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.pico) || 0), 0);
  }

  totalGa(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.ga) || 0), 0);
  }

  totalCredibanco(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.credibanco) || 0), 0);
  }

  totalQr(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.qr) || 0), 0);
  }

  totalRedenciones(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.redenciones) || 0), 0);
  }

  totalDelivered(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (c.totalEntregado || 0), 0);
  }

  totalGalMotor(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.galMotor) || 0), 0);
  }

  totalGalDiesel(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.galDiesel) || 0), 0);
  }

  totalGalExtra(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.galExtra) || 0), 0);
  }

  totalGalMaxpro(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.galMaxpro) || 0), 0);
  }

  totalCanastilla(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.canastilla) || 0), 0);
  }

  totalDominusTirilla(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.tirilla) || 0), 0);
  }

  totalDiscrepancy(): number {
    return Math.round((this.totalDelivered() - this.totalDominusTirilla()) * 100) / 100;
  }

  totalDenomination(key: keyof DenominationsBreakdown): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.denominaciones[key]) || 0), 0);
  }

  totalArqueo(): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (c.totalArqueo || 0), 0);
  }

  totalProductUnits(sku: string): number {
    return Object.values(this.captures).reduce((sum, c) => sum + (Number(c.productos[sku]) || 0), 0);
  }

  vendorProductUnits(doc: string): number {
    const products = this.captures[doc]?.productos;
    if (!products) return 0;
    return Object.values(products).reduce((sum, units) => sum + (Number(units) || 0), 0);
  }

  allProductUnits(): number {
    return this.visibleProductSkus.reduce((sum, sku) => sum + this.totalProductUnits(sku), 0);
  }

  hasDiscrepancies(): boolean {
    return Object.values(this.captures).some(c => c.conDatos && c.estadoResultado !== 'cuadra');
  }

  discrepanciesSummary(): { text: string; status: 'cuadra' | 'sobra' | 'falta' | 'vacio' } {
    const withData = Object.values(this.captures).filter(c => c.conDatos);
    if (withData.length === 0) {
      return { text: 'Sin datos', status: 'vacio' };
    }
    const nFalta = withData.filter(c => c.estadoResultado === 'falta').length;
    const nSobra = withData.filter(c => c.estadoResultado === 'sobra').length;
    if (nFalta > 0) {
      const text = `${nFalta} ${nFalta === 1 ? 'falta' : 'faltan'}${nSobra ? ` · ${nSobra} ${nSobra === 1 ? 'sobra' : 'sobran'}` : ''}`;
      return { text, status: 'falta' };
    }
    if (nSobra > 0) {
      const text = `${nSobra} ${nSobra === 1 ? 'sobra' : 'sobran'}`;
      return { text, status: 'sobra' };
    }
    return { text: 'Todos cuadran', status: 'cuadra' };
  }

  // --- Reset Fields & Persisted Closes ---
  requestReset(): void {
    if (this.isReadOnly || this.isSaving || this.isResetting) return;
    this.showResetConfirm = true;
    this.errorMessage = '';
    this.saveSuccessMessage = '';
    this.cdr.detectChanges();
  }

  cancelReset(): void {
    this.showResetConfirm = false;
    this.cdr.detectChanges();
  }

  confirmReset(): void {
    if (this.isReadOnly || this.isSaving || this.isResetting) return;
    this.showResetConfirm = false;
    this.isResetting = true;
    this.errorMessage = '';
    this.saveSuccessMessage = '';

    this.salesService.resetVendorCloses(this.journalDate).subscribe({
      next: () => {
        this.isResetting = false;
        this.visibleProductSkus = [...this.defaultProductSkus];
        this.initGrid();
        this.recalculateAll();
        this.markStep2PendingLocally();
        this.markAsClean();
        this.saveSuccessMessage = 'Los campos y cierres guardados de esta jornada fueron restablecidos.';
        this.showToast('Campos y cierres restablecidos');
        this.cdr.detectChanges();
      },
      error: (error: unknown) => {
        this.isResetting = false;
        this.errorMessage = getErrorMessage(error);
        this.showToast('Error al restablecer los cierres');
        this.cdr.detectChanges();
      }
    });
  }

  private markStep2PendingLocally(): void {
    if (!this.journalService) return;
    const journal = this.journalService.getCurrentJournalSnapshot();
    const step2 = journal?.steps?.find(s => s.stepNumber === 2);
    if (!journal || !step2 || step2.status === StepStatus.PENDING) return;
    this.journalService.updateCurrentStep({
      ...step2,
      status: StepStatus.PENDING,
      completedAt: null
    });
  }

  hasUnsavedChanges(): boolean {
    if (this.isReadOnly || this.isLoading || !this.savedInputSnapshot) return false;
    return this.serializeCapturesInputState() !== this.savedInputSnapshot;
  }

  private markAsClean(): void {
    this.savedInputSnapshot = this.serializeCapturesInputState();
  }

  private serializeCapturesInputState(): string {
    const keys = Object.keys(this.captures).sort();
    return JSON.stringify(
      keys.map(doc => {
        const cap = this.captures[doc];
        const positiveProducts = Object.entries(cap.productos || {})
          .filter(([, qty]) => Number(qty) > 0)
          .sort(([a], [b]) => a.localeCompare(b));
        return [
          doc,
          cap.parciales,
          cap.pico,
          cap.efectivo,
          cap.ga,
          cap.credibanco,
          cap.qr,
          cap.redenciones,
          cap.canastilla,
          cap.galMotor,
          cap.galDiesel,
          cap.galExtra,
          cap.galMaxpro,
          cap.tirilla,
          cap.denominaciones,
          positiveProducts
        ];
      })
    );
  }

  // --- Save to Backend ---
  saveCloses(): void {
    this.persistVendorCloses().subscribe();
  }

  private persistVendorCloses(): Observable<boolean> {
    if (this.isReadOnly) return of(true);
    this.showResetConfirm = false;
    this.isSaving = true;
    this.saveSuccessMessage = '';
    this.errorMessage = '';

    const closesDto: VendorShiftCloseItemRequestDto[] = this.assignedVendors.map(v => {
      const cap = this.captures[v.document];
      const productsDto = Object.entries(cap.productos)
        .filter(([, units]) => units > 0)
        .map(([sku, units]) => {
          const price = this.getProductPrice(sku);
          return {
            sku,
            productName: this.getProductName(sku),
            category: this.getProductCategory(sku),
            unitsSold: units,
            unitPrice: price,
            totalAmount: units * price
          };
        });

      return {
        vendorDocument: v.document,
        vendorName: v.name,
        shiftName: v.shiftName,
        pumpNumber: v.pumpNumber,
        cash: cap.efectivo,
        ga: cap.ga,
        credibanco: cap.credibanco,
        qr: cap.qr,
        redemptions: cap.redenciones,
        totalDelivered: cap.totalEntregado,
        gallonsMotor: cap.galMotor,
        gallonsDiesel: cap.galDiesel,
        gallonsExtra: cap.galExtra,
        gallonsMaxpro: cap.galMaxpro,
        basketDominusAmount: cap.canastilla,
        dominusTirilla: cap.tirilla,
        products: productsDto
      };
    });

    const batchRequest: VendorShiftCloseBatchRequestDto = {
      journalDate: this.journalDate,
      closes: closesDto
    };

    return this.salesService.saveVendorCloses(batchRequest).pipe(
      switchMap(() => this.markStep2Completed()),
      map(marked => {
        this.isSaving = false;
        this.markAsClean();
        this.saveSuccessMessage = marked
          ? 'Cierres de turno guardados y paso 2 completado en el servidor.'
          : 'Cierres de turno guardados en el servidor. El avance de la jornada se actualizará al recargar.';
        this.showToast('Cierres de vendedores conciliados y guardados');
        this.cdr.detectChanges();
        return true;
      }),
      catchError((error: unknown) => {
        this.isSaving = false;
        this.errorMessage = `${getErrorMessage(error)} Los datos permanecen en pantalla.`;
        this.showToast('Error al guardar cierres en servidor');
        this.cdr.detectChanges();
        return of(false);
      })
    );
  }

  private markStep2Completed(): Observable<boolean> {
    if (!this.journalService) return of(true);
    const journal = this.journalService.getCurrentJournalSnapshot();
    const step2 = journal?.steps?.find(s => s.stepNumber === 2);
    if (!journal || !step2 || step2.status === StepStatus.COMPLETED) return of(true);
    return this.journalService.completeStep(journal.id, 2).pipe(
      tap(step => this.journalService?.updateCurrentStep(step)),
      switchMap(() => of(true)),
      catchError(() => of(false))
    );
  }

  // --- Excel-style Cell Notes (SR-08) ---
  private loadCellNotes(journalId: number): void {
    if (this.loadedNotesJournalId === journalId) return;
    this.loadedNotesJournalId = journalId;
    this.cellNotes = {};
    this.activeNoteTarget = null;
    const loader = this.journalService?.getCellNotes;
    if (typeof loader !== 'function') return;

    loader
      .call(this.journalService, journalId, 2)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (notes: JournalCellNoteResponse[]) => {
          const mapByCell: Record<string, string> = {};
          for (const item of notes ?? []) {
            if (item.columnKey && item.fieldKey && item.note?.trim()) {
              mapByCell[this.cellNoteKey(item.columnKey, item.fieldKey)] = item.note.trim();
            }
          }
          this.cellNotes = mapByCell;
          this.cdr.detectChanges();
        },
        error: () => {
          // Keep grid usable even if cell notes fail to load.
        }
      });
  }

  private cellNoteKey(columnKey: string, fieldKey: string): string {
    return `${columnKey}::${fieldKey}`;
  }

  getCellNote(columnKey: string, fieldKey: string): string | null {
    return this.cellNotes[this.cellNoteKey(columnKey, fieldKey)] ?? null;
  }

  hasCellNote(columnKey: string, fieldKey: string): boolean {
    return Boolean(this.getCellNote(columnKey, fieldKey));
  }

  isHighlightedNoteCell(columnKey: string, fieldKey: string): boolean {
    return this.highlightedNoteCellKey === this.cellNoteKey(columnKey, fieldKey);
  }

  locateCellNote(columnKey: string, fieldKey: string): void {
    const key = this.cellNoteKey(columnKey, fieldKey);
    const noteText = this.getCellNote(columnKey, fieldKey);
    const vendorName = this.assignedVendors.find(v => v.document === columnKey)?.name ?? columnKey;
    const fieldLabel = STEP2_NOTE_FIELD_LABELS[fieldKey] ?? fieldKey;

    this.clearNoteHighlightTimers();
    this.highlightedNoteCellKey = key;
    this.highlightedNoteFieldKey = fieldKey;

    const findTargetCell = (): HTMLElement | undefined =>
      Array.from(document.querySelectorAll<HTMLElement>('[data-note-cell]')).find(
        el => el.getAttribute('data-note-cell') === key
      );

    const updateCalloutPosition = (): void => {
      if (!noteText) return;
      const cell = findTargetCell();
      if (!cell) return;
      const rect = cell.getBoundingClientRect();
      const viewportWidth = typeof window !== 'undefined' ? window.innerWidth : 1024;
      const clampedX = Math.min(Math.max(rect.left + rect.width / 2, 136), Math.max(136, viewportWidth - 136));
      const showBelow = rect.top < 88;
      this.floatingNoteCallout = {
        vendorName,
        fieldLabel,
        note: noteText,
        x: clampedX,
        y: showBelow ? rect.bottom + 8 : rect.top - 8,
        placement: showBelow ? 'below' : 'above'
      };
      this.cdr.detectChanges();
    };

    const targetCell = findTargetCell();
    targetCell?.scrollIntoView?.({ behavior: 'smooth', block: 'center', inline: 'center' });
    updateCalloutPosition();

    // Re-align callout once smooth scroll settles
    this.calloutRepositionTimer = setTimeout(() => {
      this.calloutRepositionTimer = null;
      if (this.highlightedNoteCellKey === key) {
        updateCalloutPosition();
      }
    }, 280);

    this.highlightTimer = setTimeout(() => {
      this.clearNoteHighlight();
    }, 3000);
    this.cdr.detectChanges();
  }

  private clearNoteHighlightTimers(): void {
    if (this.highlightTimer) {
      clearTimeout(this.highlightTimer);
      this.highlightTimer = null;
    }
    if (this.calloutRepositionTimer) {
      clearTimeout(this.calloutRepositionTimer);
      this.calloutRepositionTimer = null;
    }
  }

  private clearNoteHighlight(): void {
    this.clearNoteHighlightTimers();
    this.highlightedNoteCellKey = null;
    this.highlightedNoteFieldKey = null;
    this.floatingNoteCallout = null;
    this.cdr.detectChanges();
  }

  getCellTooltip(columnKey: string, fieldKey: string, fallback?: string | null): string | null {
    const note = this.getCellNote(columnKey, fieldKey);
    if (note && fallback) {
      return `${fallback}\nNota: ${note}`;
    }
    if (note) {
      return `Nota: ${note}`;
    }
    return fallback ?? null;
  }

  get cellNotesList(): CellNoteSummaryItem[] {
    const items: CellNoteSummaryItem[] = [];
    for (const [compoundKey, note] of Object.entries(this.cellNotes)) {
      const separatorIndex = compoundKey.indexOf('::');
      if (separatorIndex <= 0) continue;
      const columnKey = compoundKey.slice(0, separatorIndex);
      const fieldKey = compoundKey.slice(separatorIndex + 2);
      const vendor = this.assignedVendors.find(v => v.document === columnKey);
      items.push({
        columnKey,
        vendorName: vendor?.name ?? columnKey,
        fieldKey,
        fieldLabel: STEP2_NOTE_FIELD_LABELS[fieldKey] ?? fieldKey,
        note
      });
    }
    return items.sort((a, b) =>
      a.vendorName.localeCompare(b.vendorName, 'es') || a.fieldLabel.localeCompare(b.fieldLabel, 'es')
    );
  }

  onCellContextMenu(event: MouseEvent, vendor: AssignedVendorDto, fieldKey: string): void {
    if (this.isReadOnly && !this.hasCellNote(vendor.document, fieldKey)) {
      return;
    }
    event.preventDefault();
    this.openCellNote(vendor, fieldKey, event);
  }

  openCellNote(vendor: AssignedVendorDto, fieldKey: string, event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    if (this.isReadOnly && !this.hasCellNote(vendor.document, fieldKey)) {
      return;
    }
    this.openCellNoteByKey(vendor.document, vendor.name, fieldKey);
  }

  openCellNoteByKey(columnKey: string, vendorName: string, fieldKey: string): void {
    this.clearNoteHighlightTimers();
    this.floatingNoteCallout = null;
    this.activeNoteTarget = {
      columnKey,
      vendorName,
      fieldKey,
      fieldLabel: STEP2_NOTE_FIELD_LABELS[fieldKey] ?? fieldKey
    };
    this.noteDraft = this.getCellNote(columnKey, fieldKey) ?? '';
    this.noteErrorMessage = '';
    this.isSavingNote = false;
    this.cdr.detectChanges();
  }

  closeCellNote(): void {
    this.activeNoteTarget = null;
    this.noteDraft = '';
    this.noteErrorMessage = '';
    this.isSavingNote = false;
    this.cdr.detectChanges();
  }

  onNoteKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.closeCellNote();
      return;
    }
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      this.saveActiveCellNote();
    }
  }

  saveActiveCellNote(): void {
    const target = this.activeNoteTarget;
    if (!target || this.isReadOnly || this.isSavingNote) return;

    const journalId = this.currentJournalId ?? this.journalService?.getCurrentJournalSnapshot?.()?.id ?? null;
    const saver = this.journalService?.saveCellNote;
    if (!journalId || typeof saver !== 'function') {
      this.noteErrorMessage = 'No se encontró una jornada activa para guardar la nota.';
      this.cdr.detectChanges();
      return;
    }

    const normalized = this.noteDraft.trim();
    if (normalized.length > this.maxNoteLength) {
      this.noteErrorMessage = `La nota no puede superar los ${this.maxNoteLength} caracteres.`;
      this.cdr.detectChanges();
      return;
    }

    const payloadNote = normalized.length > 0 ? normalized : null;
    this.isSavingNote = true;
    this.noteErrorMessage = '';

    saver
      .call(this.journalService, journalId, {
        stepNumber: 2,
        columnKey: target.columnKey,
        fieldKey: target.fieldKey,
        note: payloadNote
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: saved => {
          const key = this.cellNoteKey(target.columnKey, target.fieldKey);
          const nextNotes = { ...this.cellNotes };
          if (saved?.note) {
            nextNotes[key] = saved.note;
          } else if (payloadNote) {
            nextNotes[key] = payloadNote;
          } else {
            delete nextNotes[key];
          }
          this.cellNotes = nextNotes;
          this.closeCellNote();
          this.showToast(payloadNote ? 'Nota guardada en la celda' : 'Nota eliminada');
        },
        error: (error: unknown) => {
          this.isSavingNote = false;
          this.noteErrorMessage = getErrorMessage(error) || 'No se pudo guardar la nota de la celda.';
          this.cdr.detectChanges();
        }
      });
  }

  deleteActiveCellNote(): void {
    if (!this.activeNoteTarget || this.isReadOnly || this.isSavingNote) return;
    this.noteDraft = '';
    this.saveActiveCellNote();
  }

  // --- Dominus Lateral Drawer ---
  openDominusDrawer(): void {
    this.isDominusDrawerOpen = true;
  }

  closeDominusDrawer(): void {
    this.isDominusDrawerOpen = false;
  }

  copyToClipboard(val: number | string, label: string): void {
    const text = String(val);
    navigator.clipboard.writeText(text).then(() => {
      this.showToast(`Copiado: ${label} (${this.formatCurrency(Number(val))})`);
    }).catch(() => {
      this.showToast(`Valor copiado: ${val}`);
    });
  }

  showToast(msg: string): void {
    this.toastMessage = msg;
    this.toastVisible = true;
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.toastVisible = false;
      this.cdr.detectChanges();
    }, 3200);
    this.cdr.detectChanges();
  }

  /** Turno del cierre como se lee: el código, o las horas si es un horario personalizado. */
  shiftText(shiftName: string): string {
    return this.shiftHours.text(shiftName);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0
    }).format(amount || 0);
  }

  formatNumber(n: number, decimals = 0): string {
    if (n === null || n === undefined || isNaN(n)) return '0';
    return new Intl.NumberFormat('es-CO', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    }).format(n);
  }
}
