import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { SalesVendorCloseService } from '../../../../core/services/sales-vendor-close.service';
import {
  AssignedVendorDto,
  CatalogProductItem,
  DenominationsBreakdown,
  VendorCloseSummaryDto,
  VendorGridCapture,
  VendorShiftCloseBatchRequestDto,
  VendorShiftCloseItemRequestDto
} from '../../../../shared/models/dtos/vendor-close.dto';

export const DENOMINATIONS: Array<[keyof DenominationsBreakdown, number]> = [
  ['b100k', 100000],
  ['b50k', 50000],
  ['b20k', 20000],
  ['b10k', 10000],
  ['b5k', 5000],
  ['b2k', 2000]
];

export const FUEL_PRICES: Record<string, number> = {
  motor: 16330,
  diesel: 11930,
  extra: 20390,
  maxpro: 12790
};

@Component({
  selector: 'app-vendor-close-step2',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './vendor-close-step2.component.html',
  styleUrls: ['./vendor-close-step2.component.css']
})
export class VendorCloseStep2Component implements OnInit {
  private readonly salesService = inject(SalesVendorCloseService);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly denominations = DENOMINATIONS;
  readonly packetValue = 200000;

  journalDate = '2026-08-05';
  assignedVendors: AssignedVendorDto[] = [];
  captures: Record<string, VendorGridCapture> = {};

  tolerance = 0.5;
  catalogProducts: CatalogProductItem[] = [];
  visibleProductSkus: string[] = ['76102', '75089', '74063', '80266'];
  productSearchInput = '';

  folded = {
    arqueo: true,
    productos: true
  };

  isDominusDrawerOpen = false;
  isStep1Complete = true;
  isLoading = false;
  isSaving = false;
  saveSuccessMessage = '';
  errorMessage = '';
  toastMessage = '';
  toastVisible = false;
  private toastTimer: any = null;

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['date']) {
        this.journalDate = params['date'];
      }
      this.initData();
    });
  }

  initData(): void {
    this.catalogProducts = this.salesService.getCatalogProducts();
    this.loadTolerance();
    this.loadStaffAndCloses();
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
    this.salesService.getAssignedStaff(this.journalDate).subscribe({
      next: (staff) => {
        if (staff && staff.length > 0) {
          this.assignedVendors = staff;
          this.initGrid();
        } else {
          // If backend staff query returned empty, initialize from demo for seamless UX
          const demo = this.salesService.getCalibrationDemo0805();
          this.assignedVendors = demo.vendedores;
          this.initGrid();
        }
        this.loadExistingCloses();
      },
      error: () => {
        // Fallback to demo staff
        const demo = this.salesService.getCalibrationDemo0805();
        this.assignedVendors = demo.vendedores;
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
        } else {
          // Auto-load 05/08 demo if date matches calibration date
          if (this.journalDate === '2026-08-05') {
            this.loadDemoHoja05(false);
          }
        }
        this.recalculateAll();
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        if (this.journalDate === '2026-08-05') {
          this.loadDemoHoja05(false);
        }
        this.recalculateAll();
        this.cdr.detectChanges();
      }
    });
  }

  private populateFromBackendCloses(closes: VendorCloseSummaryDto[]): void {
    for (const close of closes) {
      const cap = this.captures[close.vendorDocument];
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

  loadDemoHoja05(notify = true): void {
    const demo = this.salesService.getCalibrationDemo0805();
    for (const [doc, data] of Object.entries(demo.capturas)) {
      if (!this.captures[doc]) {
        const foundStaff = demo.vendedores.find(v => v.document === doc);
        if (foundStaff) {
          this.assignedVendors.push(foundStaff);
          this.captures[doc] = this.createEmptyCapture(foundStaff);
        }
      }
      const cap = this.captures[doc];
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
        if (this.captures[doc]) {
          this.captures[doc].productos[sku] = units;
        }
      }
    }

    this.recalculateAll();
    if (notify) {
      this.showToast('Hoja 05 de calibración cargada exitosamente');
    }
  }

  onFieldChange(doc: string): void {
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

    // Validación tirilla vs galones
    if (tirilla > 0) {
      const fuelSales = (Number(cap.galMotor) || 0) * FUEL_PRICES['motor'] +
        (Number(cap.galDiesel) || 0) * FUEL_PRICES['diesel'] +
        (Number(cap.galExtra) || 0) * FUEL_PRICES['extra'] +
        (Number(cap.galMaxpro) || 0) * FUEL_PRICES['maxpro'] +
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

    let found = this.catalogProducts.find(p => p.sku === query);
    if (!found) {
      found = this.catalogProducts.find(p => p.name.toLowerCase().includes(query.toLowerCase()));
    }

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
    const clipboardData = event.clipboardData || (window as any).clipboardData;
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
    let s = String(str).replace(/[\s\$\.]/g, '').replace(',', '.');
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

  // --- Save to Backend ---
  saveCloses(): void {
    this.isSaving = true;
    this.saveSuccessMessage = '';
    this.errorMessage = '';

    const closesDto: VendorShiftCloseItemRequestDto[] = this.assignedVendors.map(v => {
      const cap = this.captures[v.document];
      const productsDto = Object.entries(cap.productos)
        .filter(([_, units]) => units > 0)
        .map(([sku, units]) => {
          const price = this.getProductPrice(sku);
          return {
            sku,
            productName: this.getProductName(sku),
            category: 'Canastilla',
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

    this.salesService.saveVendorCloses(batchRequest).subscribe({
      next: (res) => {
        this.isSaving = false;
        this.saveSuccessMessage = 'Cierres de turno guardados y conciliados exitosamente en el servidor.';
        this.showToast('Cierres de vendedores conciliados y guardados');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = 'No se pudo conectar con el servidor para persistir los cierres. Los datos permanecen en pantalla.';
        this.showToast('Error al guardar cierres en servidor');
        this.cdr.detectChanges();
      }
    });
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
