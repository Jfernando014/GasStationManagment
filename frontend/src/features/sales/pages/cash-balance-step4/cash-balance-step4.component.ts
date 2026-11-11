import { Component, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { JournalService } from '../../../../core/services/journal.service';
import { CashBalanceStep4Service } from '../../../../core/services/cash-balance-step4.service';
import { ToastService } from '../../../../core/services/toast.service';
import { JournalResponse, JournalStatus, StepStatus } from '../../../../shared/models/journal.model';
import { DailyExpenseItem, Step4CalculationResult } from '../../../../shared/models/dtos/cash-balance-step4.dto';
import { VendorCloseSummaryDto } from '../../../../shared/models/dtos/vendor-close.dto';

@Component({
  selector: 'app-cash-balance-step4',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './cash-balance-step4.component.html',
  styleUrls: ['./cash-balance-step4.component.css']
})
export class CashBalanceStep4Component implements OnInit {
  private readonly journalService = inject(JournalService);
  private readonly step4Service = inject(CashBalanceStep4Service);
  private readonly toastService = inject(ToastService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  journal: JournalResponse | null = null;
  currentDate = '2026-08-05';
  vendorCloses: VendorCloseSummaryDto[] = [];
  backendExpenses: DailyExpenseItem[] | null = null;

  calc!: Step4CalculationResult;
  readonly toleranciaSaldo = 0;

  consolidadoInputText = '';
  saldoAnteriorInputText = '';
  saldoDominusInputText = '';

  consolidadoInvalid = false;
  saldoAnteriorInvalid = false;
  saldoDominusInvalid = false;

  loadedPdf: { name: string; bytes: Uint8Array; file: File } | null = null;
  isDraggingPdf = false;
  isCompleting = false;

  isPaymentDialogOpen = false;
  paymentDate = '';
  paymentAmountInput = '';
  paymentConcept = 'Arriendo';
  paymentError = '';

  constructor() {
    this.recalculate(true);
  }

  ngOnInit(): void {
    this.journalService.currentJournal$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(journal => {
        if (journal) {
          this.journal = journal;
          this.currentDate = journal.date;
          this.loadDayData(journal.date);
        } else {
          const selected = this.journalService.getSelectedDate() || '2026-08-05';
          this.currentDate = selected;
          this.loadDayData(selected);
        }
      });
  }

  loadDayData(fecha: string): void {
    this.loadedPdf = this.step4Service.getPdfInMemory(fecha);
    this.recalculate(true);

    this.step4Service
      .fetchDayContext(fecha)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ vendorCloses, backendExpenses, backendStep4 }) => {
        this.vendorCloses = vendorCloses;
        this.backendExpenses = backendExpenses;
        if (backendStep4?.table?.dominusAccumulatedBalance !== null && backendStep4?.table?.dominusAccumulatedBalance !== undefined) {
          const existing = this.step4Service.getCapturedData(fecha);
          if (existing.saldoDominus === undefined) {
            this.step4Service.saveCapturedData(fecha, {
              saldoDominus: Number(backendStep4.table.dominusAccumulatedBalance)
            });
          }
        }
        this.recalculate(true);
        this.cdr.detectChanges();
      });
  }

  recalculate(syncInputs = false): void {
    this.calc = this.step4Service.calculateStep4(this.currentDate, {
      journalConsecutive: this.journal?.consecutive,
      vendorCloses: this.vendorCloses,
      backendExpenses: this.backendExpenses
    });

    if (syncInputs) {
      const captured = this.step4Service.getCapturedData(this.currentDate);
      this.consolidadoInputText =
        typeof captured.consolidadoDominus === 'number'
          ? this.formatNumber(captured.consolidadoDominus, Number.isInteger(captured.consolidadoDominus) ? 0 : 2)
          : '';
      this.saldoAnteriorInputText =
        typeof captured.saldoAnterior === 'number'
          ? this.formatNumber(captured.saldoAnterior, 0)
          : '';
      this.saldoDominusInputText =
        this.calc.saldoDominus === null
          ? ''
          : this.formatNumber(
              this.calc.saldoDominus,
              Number.isInteger(this.calc.saldoDominus) ? 0 : 2
            );
    }
  }

  isClosed(): boolean {
    return this.journal?.status === JournalStatus.CLOSED;
  }

  isStep4Completed(): boolean {
    const step4 = this.journal?.steps?.find(s => s.stepNumber === 4);
    return step4?.status === StepStatus.COMPLETED;
  }

  consolidadoIndicatorLabel(): string {
    const labels: Record<string, string> = {
      cuadra: 'Cuadra',
      difiere: 'Difiere',
      pendiente: 'Por pegar'
    };
    return labels[this.calc.cuadre] || 'Por pegar';
  }

  consolidadoIndicatorState(): string {
    const states: Record<string, string> = {
      cuadra: 'cuadra',
      difiere: 'falta',
      pendiente: 'aviso'
    };
    return states[this.calc.cuadre] || 'aviso';
  }

  shortDayMonth(): string {
    const parts = this.currentDate.split('-');
    if (parts.length < 3) return this.currentDate;
    return `${parts[2]}/${parts[1]}`;
  }

  totalGastosDia(): number {
    return this.calc.gastos.reduce((sum, g) => sum + g.valor, 0);
  }

  formatDaysList(lista: number[]): string {
    if (lista.length <= 12) return lista.join(' ');
    return `${lista.slice(0, 5).join(' ')} … ${lista.slice(-5).join(' ')}`;
  }

  pdfEmptyHelpText(): string {
    const captured = this.step4Service.getCapturedData(this.currentDate);
    if (captured.pdf?.nombre && !this.loadedPdf) {
      return `Se adjuntó "${captured.pdf.nombre}", pero no se conserva al recargar: vuelve a cargarlo`;
    }
    return 'Arrastra el archivo aquí · se le agrega la tabla de saldo en caja como última página';
  }

  parseNumero(raw: string): number | null {
    const trimmed = (raw || '').replace(/\$/g, '').replace(/\s+/g, '').trim();
    if (!trimmed) return null;
    // Soporta formato es-CO (ej. 14.136.817,40 o 9.364.000) y decimal con punto
    let normalized = trimmed;
    if (normalized.includes(',')) {
      normalized = normalized.replace(/\./g, '').replace(',', '.');
    } else {
      const dotParts = normalized.split('.');
      if (dotParts.length > 2 || (dotParts.length === 2 && dotParts[1].length === 3)) {
        normalized = normalized.replace(/\./g, '');
      }
    }
    const num = Number(normalized);
    return Number.isFinite(num) ? num : Number.NaN;
  }

  onConsolidadoInput(event: Event): void {
    if (this.isClosed()) return;
    const val = (event.target as HTMLInputElement).value;
    this.consolidadoInputText = val;
    const parsed = this.parseNumero(val);
    this.consolidadoInvalid = Number.isNaN(parsed);
    this.step4Service.saveCapturedData(this.currentDate, {
      consolidadoDominus: this.consolidadoInvalid ? null : parsed
    });
    this.recalculate(false);
  }

  onConsolidadoBlur(): void {
    const parsed = this.parseNumero(this.consolidadoInputText);
    if (parsed !== null && !Number.isNaN(parsed)) {
      this.consolidadoInputText = this.formatNumber(parsed, Number.isInteger(parsed) ? 0 : 2);
    }
  }

  onSaldoAnteriorInput(event: Event): void {
    if (this.isClosed()) return;
    const val = (event.target as HTMLInputElement).value;
    this.saldoAnteriorInputText = val;
    const parsed = this.parseNumero(val);
    this.saldoAnteriorInvalid = Number.isNaN(parsed);
    this.step4Service.saveCapturedData(this.currentDate, {
      saldoAnterior: this.saldoAnteriorInvalid ? null : parsed
    });
    this.recalculate(false);
  }

  onSaldoAnteriorBlur(): void {
    const parsed = this.parseNumero(this.saldoAnteriorInputText);
    if (parsed !== null && !Number.isNaN(parsed)) {
      this.saldoAnteriorInputText = this.formatNumber(parsed, 0);
    }
  }

  onSaldoDominusInput(event: Event): void {
    if (this.isClosed()) return;
    const val = (event.target as HTMLInputElement).value;
    this.saldoDominusInputText = val;
    const parsed = this.parseNumero(val);
    this.saldoDominusInvalid = Number.isNaN(parsed);
    const validValue = this.saldoDominusInvalid ? null : parsed;
    this.step4Service.saveCapturedData(this.currentDate, {
      saldoDominus: validValue
    });
    this.recalculate(false);
  }

  onSaldoDominusBlur(): void {
    const parsed = this.parseNumero(this.saldoDominusInputText);
    if (parsed !== null && !Number.isNaN(parsed)) {
      this.saldoDominusInputText = this.formatNumber(parsed, Number.isInteger(parsed) ? 0 : 2);
      this.step4Service
        .saveDominusBalanceToBackend(this.currentDate, parsed)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe();
    }
  }

  openPaymentDialog(): void {
    if (this.isClosed()) return;
    this.paymentDate = this.currentDate;
    this.paymentAmountInput = '';
    this.paymentConcept = 'Arriendo';
    this.paymentError = '';
    this.isPaymentDialogOpen = true;
  }

  closePaymentDialog(): void {
    this.isPaymentDialogOpen = false;
    this.paymentError = '';
  }

  onScrimClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).id === 'jr-p4-dialogo-pago') {
      this.closePaymentDialog();
    }
  }

  formattedPaymentDate(): string {
    const d = this.paymentDate || this.currentDate;
    return d.split('-').reverse().join('/');
  }

  savePayment(): void {
    if (!this.paymentDate) {
      this.paymentError = 'Elige la fecha del pago.';
      return;
    }
    const valor = this.parseNumero(this.paymentAmountInput);
    if (valor === null || Number.isNaN(valor) || valor <= 0) {
      this.paymentError = 'Escribe un valor mayor que cero.';
      return;
    }
    const saldo = this.step4Service.fondoArriendoAl(this.paymentDate).acumulado;
    if (valor > saldo) {
      this.paymentError = `El pago supera el saldo del fondo (${this.formatCOP(saldo)}).`;
      return;
    }

    this.step4Service
      .registerFundPayment(this.paymentDate, valor, this.paymentConcept)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.closePaymentDialog();
        this.toastService.show(`Pago de ${this.formatCOP(valor)} registrado desde el fondo`);
        this.recalculate(false);
        this.cdr.markForCheck();
      });
  }

  removePayment(id: string): void {
    if (this.isClosed()) return;
    this.step4Service.removeFundPayment(id);
    this.toastService.show('Pago quitado del fondo');
    this.recalculate(false);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (!this.isClosed()) {
      this.isDraggingPdf = true;
    }
  }

  onDragLeave(): void {
    this.isDraggingPdf = false;
  }

  onDropPdf(event: DragEvent): void {
    event.preventDefault();
    this.isDraggingPdf = false;
    if (this.isClosed()) return;
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.loadPdfFile(file);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.loadPdfFile(file);
    }
    input.value = '';
  }

  loadPdfFile(file: File): void {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      this.toastService.show('Formato no admitido. Usa .pdf');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const bytes = new Uint8Array(reader.result as ArrayBuffer);
      this.step4Service.setPdfInMemory(this.currentDate, file, bytes);
      this.loadedPdf = this.step4Service.getPdfInMemory(this.currentDate);
      this.cdr.detectChanges();
    };
    reader.readAsArrayBuffer(file);
  }

  clearPdf(): void {
    if (this.isClosed()) return;
    this.step4Service.clearPdfInMemory(this.currentDate);
    this.loadedPdf = null;
  }

  downloadFinalPdf(): void {
    if (!this.loadedPdf) {
      this.toastService.show('Primero adjunta el PDF del consolidado de Dominus');
      return;
    }
    const blob = new Blob([this.loadedPdf.bytes.buffer as ArrayBuffer], { type: 'application/pdf' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Consolidado-Dominus-${this.currentDate}-con-saldo-en-caja.pdf`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
    this.toastService.show('PDF final listo (con saldo en caja)');
  }

  completeStep4(): void {
    if (!this.journal || this.isClosed() || this.isCompleting) return;
    this.isCompleting = true;
    this.journalService
      .completeStep(this.journal.id, 4)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: step => {
          this.isCompleting = false;
          this.journalService.updateCurrentStep(step);
          this.toastService.show('Paso 4 completo');
          this.cdr.detectChanges();
        },
        error: () => {
          this.isCompleting = false;
          this.toastService.show('Paso 4 guardado localmente');
          this.cdr.detectChanges();
        }
      });
  }

  abs(val: number): number {
    return Math.abs(val);
  }

  formatNumber(value: number | null | undefined, decimals = 0): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return new Intl.NumberFormat('es-CO', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    }).format(value);
  }

  formatCOP(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return `$ ${this.formatNumber(Math.round(value), 0)}`;
  }
}

