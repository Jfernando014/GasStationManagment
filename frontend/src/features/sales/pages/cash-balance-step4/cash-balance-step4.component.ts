import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';

import { CashBalanceStep4Service } from '../../../../core/services/cash-balance-step4.service';
import { JournalService } from '../../../../core/services/journal.service';
import {
  buildFinalCashBalancePdf,
  downloadCashBalancePdf
} from '../../../../shared/helpers/cash-balance-step4-pdf';
import {
  MAX_FUND_PAYMENT_DESCRIPTION_LENGTH,
  cashBalanceTone,
  cashDifferenceDetail,
  cashDifferenceLabel,
  hasPdfSignature,
  validateCashBalancePdf,
  validateFundPayment
} from '../../../../shared/helpers/cash-balance-step4.helpers';
import { formatAmount, formatSignedCop, parseAmount } from '../../../../shared/helpers/electronic-payments.helpers';
import { formatCop, formatInputDate } from '../../../../shared/helpers/format.helpers';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import { CashBalanceStepResponseDto } from '../../../../shared/models/dtos/cash-balance-step4.dto';
import { JournalStatus, StepStatus } from '../../../../shared/models/journal.model';

const STEP_NUMBER = 4;
const EXPENSES_ROUTE = '/gastos';

@Component({
  selector: 'app-cash-balance-step4',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cash-balance-step4.component.html',
  styleUrl: './cash-balance-step4.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CashBalanceStep4Component {
  private readonly service = inject(CashBalanceStep4Service);
  private readonly journalService = inject(JournalService);
  private requestId = 0;
  private readonly reloadVersion = signal(0);

  readonly expensesRoute = EXPENSES_ROUTE;
  readonly descriptionMax = MAX_FUND_PAYMENT_DESCRIPTION_LENGTH;

  readonly journal = toSignal(this.journalService.currentJournal$, { initialValue: null });
  readonly date = computed(() => this.journal()?.date ?? null);
  readonly dateLabel = computed(() => {
    const date = this.date();
    return date ? formatInputDate(date) : '';
  });
  readonly step = computed(() => this.journal()?.steps.find(item => item.stepNumber === STEP_NUMBER) ?? null);
  readonly completed = computed(() => this.step()?.status === StepStatus.COMPLETED);
  readonly canEdit = computed(() => {
    const journal = this.journal();
    if (!journal || journal.status !== JournalStatus.OPEN || this.completed()) return false;
    return journal.steps.find(item => item.stepNumber === 6)?.status !== StepStatus.COMPLETED;
  });

  readonly result = signal<CashBalanceStepResponseDto | null>(null);
  readonly table = computed(() => this.result()?.table ?? null);
  readonly differenceStatus = computed(() => this.result()?.differenceStatus ?? null);
  readonly canComplete = computed(() => this.result()?.canComplete ?? false);
  readonly missing = computed(() => this.result()?.missing ?? []);
  readonly tone = computed(() => cashBalanceTone(this.differenceStatus()));
  readonly statusLabel = computed(() => cashDifferenceLabel(this.differenceStatus()));
  readonly statusDetail = computed(() => cashDifferenceDetail(this.differenceStatus()));

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly notice = signal<string | null>(null);

  readonly dominusText = signal('');
  readonly dominusError = signal<string | null>(null);
  readonly savingDominus = signal(false);
  readonly dominusDirty = computed(() => {
    const text = this.dominusText();
    if (text.trim() === '') return false;
    const stored = this.table()?.dominusAccumulatedBalance ?? null;
    const parsed = parseAmount(text);
    return stored === null || parsed === null || parsed !== stored;
  });

  readonly completing = signal(false);
  readonly paymentOpen = signal(false);
  readonly paymentDate = signal('');
  readonly paymentAmount = signal('');
  readonly paymentDescription = signal('Arriendo');
  readonly paymentError = signal<string | null>(null);
  readonly savingPayment = signal(false);

  readonly pdfName = signal<string | null>(null);
  readonly pdfSize = signal(0);
  readonly pdfBytes = signal<Uint8Array | null>(null);
  readonly pdfError = signal<string | null>(null);
  readonly dragOver = signal(false);
  readonly generating = signal(false);
  readonly canDownload = computed(
    () => this.pdfBytes() !== null && this.table() !== null && !this.generating()
  );

  constructor() {
    effect(() => {
      const date = this.date();
      this.reloadVersion();
      if (date) {
        untracked(() => this.load(date, true));
      } else {
        this.result.set(null);
        this.loading.set(false);
      }
    });
  }

  money(value: number | null): string {
    return value === null ? '—' : formatCop(value);
  }

  signedMoney(value: number | null): string {
    return value === null ? '—' : formatSignedCop(value);
  }

  shortDate(value: string): string {
    return formatInputDate(value);
  }

  fileSize(bytes: number): string {
    return bytes >= 1024 * 1024
      ? `${(bytes / (1024 * 1024)).toFixed(1)} MB`
      : `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }

  reload(): void {
    this.reloadVersion.update(value => value + 1);
  }

  private load(date: string, resetDraft: boolean): void {
    const requestId = ++this.requestId;
    this.loading.set(true);
    this.loadError.set(null);
    this.service
      .getCashBalance(date)
      .pipe(
        map(response => ({ response, error: null })),
        catchError(error => of({ response: null, error }))
      )
      .subscribe(({ response, error }) => {
        if (requestId !== this.requestId || this.date() !== date) return;
        this.loading.set(false);
        if (error !== null) {
          this.loadError.set(getErrorMessage(error));
          return;
        }
        if (response) this.applyResult(response, resetDraft);
      });
  }

  private applyResult(response: CashBalanceStepResponseDto, resetDraft: boolean): void {
    this.result.set(response);
    if (resetDraft) {
      const stored = response.table.dominusAccumulatedBalance;
      this.dominusText.set(stored === null ? '' : stored === 0 ? '0' : formatAmount(stored));
      this.dominusError.set(null);
    }
  }

  private clearMessages(): void {
    this.actionError.set(null);
    this.notice.set(null);
  }

  private refresh(): void {
    const date = this.date();
    if (date) this.load(date, !this.dominusDirty());
  }

  onDominusInput(event: Event): void {
    this.dominusText.set((event.target as HTMLInputElement).value);
    this.dominusError.set(null);
  }

  onDominusBlur(): void {
    const text = this.dominusText();
    const amount = parseAmount(text);
    if (amount !== null && text.trim() !== '') {
      this.dominusText.set(amount === 0 ? '0' : formatAmount(amount));
    }
  }

  saveDominus(): void {
    const date = this.date();
    if (!date || !this.canEdit() || this.savingDominus()) return;
    const text = this.dominusText();
    const amount = parseAmount(text);
    if (text.trim() === '' || amount === null) {
      this.dominusError.set('Escribe un saldo válido de Dominus, con máximo dos decimales.');
      return;
    }

    this.clearMessages();
    this.savingDominus.set(true);
    this.service.saveDominusBalance(date, amount).subscribe({
      next: response => {
        this.applyResult(response, true);
        this.savingDominus.set(false);
        this.notice.set('Saldo de Dominus guardado.');
      },
      error: error => {
        this.dominusError.set(getErrorMessage(error));
        this.savingDominus.set(false);
      }
    });
  }

  openPayment(): void {
    if (!this.canEdit()) return;
    this.clearMessages();
    this.paymentDate.set(this.date() ?? '');
    this.paymentAmount.set('');
    this.paymentDescription.set('Arriendo');
    this.paymentError.set(null);
    this.paymentOpen.set(true);
    setTimeout(() => document.getElementById('cb-pay-amount')?.focus(), 0);
  }

  closePayment(): void {
    if (!this.savingPayment()) this.paymentOpen.set(false);
  }

  onPaymentDate(event: Event): void {
    this.paymentDate.set((event.target as HTMLInputElement).value);
    this.paymentError.set(null);
  }

  onPaymentAmount(event: Event): void {
    this.paymentAmount.set((event.target as HTMLInputElement).value);
    this.paymentError.set(null);
  }

  onPaymentAmountBlur(): void {
    const amount = parseAmount(this.paymentAmount());
    if (amount !== null && this.paymentAmount().trim() !== '') {
      this.paymentAmount.set(amount === 0 ? '0' : formatAmount(amount));
    }
  }

  onPaymentDescription(event: Event): void {
    this.paymentDescription.set((event.target as HTMLInputElement).value);
  }

  submitPayment(): void {
    if (!this.canEdit() || this.savingPayment()) return;
    const date = this.paymentDate();
    const amountText = this.paymentAmount();
    const firstCheck = validateFundPayment({ date, amountText, balance: null });
    if (firstCheck.error || firstCheck.amount === null) {
      this.paymentError.set(firstCheck.error);
      return;
    }

    const table = this.table();
    const balance$ =
      table && date === this.date()
        ? of(table.savingsFund.accumulatedBalance)
        : this.service.getFundBalance(date).pipe(map(response => response.balance));
    const description = this.paymentDescription().trim();

    this.paymentError.set(null);
    this.savingPayment.set(true);
    balance$
      .pipe(
        switchMap(balance => {
          const validation = validateFundPayment({ date, amountText, balance });
          if (validation.error || validation.amount === null) return of(validation.error);
          return this.service
            .registerFundPayment({
              date,
              amount: validation.amount,
              description: description || undefined
            })
            .pipe(map(() => null));
        })
      )
      .subscribe({
        next: error => {
          this.savingPayment.set(false);
          if (error) {
            this.paymentError.set(error);
            return;
          }
          this.paymentOpen.set(false);
          this.notice.set('Pago registrado desde el fondo.');
          this.refresh();
        },
        error: error => {
          this.savingPayment.set(false);
          this.paymentError.set(getErrorMessage(error));
        }
      });
  }

  completeStep(): void {
    const journal = this.journal();
    if (!journal || !this.canEdit() || !this.canComplete() || this.completing()) return;
    this.clearMessages();
    this.completing.set(true);
    this.journalService.completeStep(journal.id, STEP_NUMBER).subscribe({
      next: step => {
        this.completing.set(false);
        this.journalService.updateCurrentStep(step);
        this.notice.set('Paso 4 completado.');
        this.refresh();
      },
      error: error => {
        this.completing.set(false);
        this.actionError.set(getErrorMessage(error));
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) void this.loadPdf(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) void this.loadPdf(file);
  }

  private async loadPdf(file: File): Promise<void> {
    const validationError = validateCashBalancePdf(file);
    if (validationError) {
      this.pdfError.set(validationError);
      return;
    }
    try {
      const bytes = new Uint8Array(await file.arrayBuffer());
      if (!hasPdfSignature(bytes)) {
        this.pdfError.set('El archivo no contiene la firma de un PDF válido.');
        return;
      }
      this.pdfBytes.set(bytes);
      this.pdfName.set(file.name);
      this.pdfSize.set(file.size);
      this.pdfError.set(null);
    } catch {
      this.pdfError.set('No se pudo leer el archivo. Intenta de nuevo.');
    }
  }

  removePdf(): void {
    this.pdfBytes.set(null);
    this.pdfName.set(null);
    this.pdfSize.set(0);
    this.pdfError.set(null);
  }

  async downloadFinalPdf(): Promise<void> {
    const table = this.table();
    const bytes = this.pdfBytes();
    if (!table || !bytes || this.generating()) return;

    this.clearMessages();
    this.pdfError.set(null);
    this.generating.set(true);
    try {
      const result = await buildFinalCashBalancePdf(bytes, {
        table,
        differenceStatus: this.differenceStatus()
      });
      downloadCashBalancePdf(result.bytes, `Consolidado-Dominus-${table.date}-con-saldo-en-caja.pdf`);
      this.notice.set(`PDF final descargado: ${result.pageCount} páginas, con saldo en caja al final.`);
    } catch {
      this.pdfError.set(
        'No se pudo procesar el PDF. Verifica que sea un archivo válido y que no esté dañado o protegido.'
      );
    } finally {
      this.generating.set(false);
    }
  }
}
