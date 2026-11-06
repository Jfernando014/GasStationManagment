import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, catchError, concatMap, distinctUntilChanged, filter, map, of, switchMap } from 'rxjs';

import { ElectronicPaymentsService } from '../../../../core/services/electronic-payments.service';
import { JournalService } from '../../../../core/services/journal.service';
import {
  assignmentReason,
  buildStepSummary,
  countMatchingVendors,
  credibancoTone,
  formatAmount,
  formatSignedCop,
  formatTime,
  parseAmount,
  pointsTone,
  qrTone,
  validateRedebanFile,
  vendorLabel,
  voucherValues
} from '../../../../shared/helpers/electronic-payments.helpers';
import { formatCop, formatInputDate } from '../../../../shared/helpers/format.helpers';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import {
  ElectronicPaymentsStatus,
  ElectronicVendorRow,
  RedebanImportResponse,
  VoucherField,
  VoucherRequest
} from '../../../../shared/models/dtos/electronic-payments.dto';
import { JournalResponse, JournalStatus, StepStatus } from '../../../../shared/models/journal.model';

const STEP_NUMBER = 3;
const TOAST_DURATION_MS = 3000;

export type StepTab = 'credibanco' | 'qr' | 'puntos';

interface SaveJob {
  date: string;
  workerId: string;
  body: VoucherRequest;
}

type SaveResult = { ok: true; status: ElectronicPaymentsStatus } | { ok: false; error: unknown };

interface SummaryRequest {
  date: string;
  workerId: string;
}

/**
 * Paso 3 de la jornada: medios electrónicos.
 * Compara lo declarado en las planillas del paso 2 con los comprobantes de Credibanco, el CSV de Redeban (QR)
 * y el total de Puntos Colombia. El servidor calcula todo; aquí solo se digita, se sube el archivo y se lee el resultado.
 */
@Component({
  selector: 'app-electronic-payments-step3',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './electronic-payments-step3.component.html',
  styleUrl: './electronic-payments-step3.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ElectronicPaymentsStep3Component {
  private readonly service = inject(ElectronicPaymentsService);
  private readonly journalService = inject(JournalService);
  private readonly destroyRef = inject(DestroyRef);

  /** Las ediciones de comprobantes se envían una por una y en orden, para que ninguna pise a otra. */
  private readonly saveQueue$ = new Subject<SaveJob>();
  private readonly summaryRequest$ = new Subject<SummaryRequest | null>();
  private toastTimer?: ReturnType<typeof setTimeout>;
  private pointsDirty = false;
  private loadedDate: string | null = null;

  readonly voucherColumns: readonly { field: VoucherField; label: string }[] = [
    { field: 'mastercard', label: 'Mastercard' },
    { field: 'visa', label: 'Visa' },
    { field: 'amex', label: 'Amex' },
    { field: 'mastercardDawn', label: 'Mastercard madrugada' },
    { field: 'visaDawn', label: 'Visa madrugada' },
    { field: 'amexDawn', label: 'Amex madrugada' }
  ];

  // Funciones de formato usadas por la plantilla.
  readonly money = formatCop;
  readonly signed = formatSignedCop;
  readonly amount = formatAmount;
  readonly time = formatTime;
  readonly reason = assignmentReason;

  readonly journal = toSignal(this.journalService.currentJournal$, { initialValue: null });
  readonly date = computed(() => this.journal()?.date ?? null);
  readonly dateLabel = computed(() => {
    const date = this.date();
    return date ? formatInputDate(date) : '';
  });
  readonly step = computed(() => this.journal()?.steps.find(item => item.stepNumber === STEP_NUMBER) ?? null);
  readonly completed = computed(() => this.step()?.status === StepStatus.COMPLETED);
  readonly salesCompleted = computed(
    () => this.journal()?.steps.find(item => item.stepNumber === 2)?.status === StepStatus.COMPLETED
  );
  readonly editable = computed(() => {
    const journal = this.journal();
    if (!journal || journal.status !== JournalStatus.OPEN) return false;
    const closing = journal.steps.find(item => item.stepNumber === 6);
    return closing?.status !== StepStatus.COMPLETED;
  });
  /** Se digita solo con la jornada abierta y el paso sin completar. */
  readonly canEdit = computed(() => this.editable() && !this.completed());

  readonly status = signal<ElectronicPaymentsStatus | null>(null);
  /** Copia editable de las filas: se actualiza al digitar y se resincroniza cuando termina el último guardado. */
  readonly rows = signal<ElectronicVendorRow[]>([]);
  readonly tab = signal<StepTab>('credibanco');

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly formError = signal<string | null>(null);
  readonly pendingSaves = signal(0);
  readonly invalidCells = signal<ReadonlySet<string>>(new Set<string>());
  readonly completing = signal(false);

  readonly uploading = signal(false);
  readonly dragging = signal(false);
  readonly importResult = signal<RedebanImportResponse | null>(null);
  readonly importedFileName = signal<string | null>(null);

  readonly pointsDraft = signal('');
  readonly savingPoints = signal(false);

  readonly whatsappWorkerId = signal<string | null>(null);
  readonly whatsappText = signal('');
  readonly whatsappLoading = signal(false);

  readonly toastText = signal('');
  readonly toastVisible = signal(false);

  readonly saving = computed(() => this.pendingSaves() > 0);
  readonly credibanco = computed(() => this.status()?.credibanco ?? null);
  readonly qr = computed(() => this.status()?.qr ?? null);
  readonly points = computed(() => this.status()?.points ?? null);

  readonly tones = computed(() => {
    const status = this.status();
    if (!status) return null;
    return {
      credibanco: credibancoTone(status.credibanco),
      qr: qrTone(status.qr),
      puntos: pointsTone(status.points)
    };
  });

  /** Cada fila editable junto a su comparación contra la planilla. */
  readonly credibancoRows = computed(() => {
    const status = this.status();
    if (!status) return [];
    const checks = new Map(status.credibanco.vendors.map(check => [check.workerId, check]));
    return this.rows().map(row => ({ row, check: checks.get(row.workerId) ?? null }));
  });

  readonly credibancoTotals = computed(() => {
    const checks = this.credibancoRows().flatMap(item => (item.check ? [item.check] : []));
    return {
      vouchers: checks.reduce((sum, check) => sum + check.vouchersTotal, 0),
      planilla: checks.reduce((sum, check) => sum + check.planillaCredibanco, 0),
      difference: checks.reduce((sum, check) => sum + check.difference, 0)
    };
  });

  readonly qrVendorsMatching = computed(() => this.qr()?.vendors.filter(vendor => vendor.matches).length ?? 0);
  readonly qrVendorsState = computed<'ok' | 'error' | 'pending'>(() => {
    const qr = this.qr();
    if (!qr || !qr.perVendorReliable) return 'pending';
    return qr.vendors.every(vendor => vendor.matches) ? 'ok' : 'error';
  });

  readonly importSummary = computed(() => {
    const result = this.importResult();
    if (!result) return '';
    const ignored: string[] = [];
    if (result.ignoredNotQr > 0) ignored.push(`${result.ignoredNotQr} que no son QR`);
    if (result.ignoredNotApproved > 0) ignored.push(`${result.ignoredNotApproved} no aprobadas`);
    if (result.ignoredOtherDay > 0) ignored.push(`${result.ignoredOtherDay} de otro día`);
    const base = `${result.accepted} de ${result.totalRows} filas sumadas`;
    return ignored.length > 0 ? `${base}; se ignoraron ${ignored.join(', ')}.` : `${base}.`;
  });

  readonly headerChip = computed(() => {
    if (this.completed()) return { css: 'jr-chip--cerrada', label: 'Paso completado' };
    if (!this.editable()) return { css: 'jr-chip--pendiente', label: 'Solo lectura' };
    const status = this.status();
    if (!status) return null;
    const total = status.vendors.length;
    if (total === 0) return { css: 'jr-chip--aviso', label: 'Sin vendedores del paso 2' };
    return {
      css: status.canComplete ? 'jr-chip--cuadra' : 'jr-chip--aviso',
      label: `${countMatchingVendors(status)} de ${total} vendedores cuadran`
    };
  });

  readonly canComplete = computed(
    () =>
      !!this.status()?.canComplete &&
      this.canEdit() &&
      this.salesCompleted() &&
      !this.saving() &&
      !this.completing() &&
      this.invalidCells().size === 0
  );

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));

    this.journalService.currentJournal$
      .pipe(
        filter((journal): journal is JournalResponse => journal !== null),
        distinctUntilChanged((a, b) => a.id === b.id && a.date === b.date),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(journal => this.load(journal.date));

    this.saveQueue$
      .pipe(
        concatMap(job =>
          this.service.saveVouchers(job.date, job.workerId, job.body).pipe(
            map((status): SaveResult => ({ ok: true, status })),
            catchError((error: unknown) => of<SaveResult>({ ok: false, error }))
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(result => {
        this.pendingSaves.update(count => Math.max(0, count - 1));
        if (result.ok) {
          this.applyStatus(result.status);
          return;
        }
        this.formError.set(getErrorMessage(result.error));
        const date = this.date();
        if (date && this.pendingSaves() === 0) this.refresh(date);
      });

    this.summaryRequest$
      .pipe(
        switchMap(request => {
          if (!request) return of<string | null>('');
          this.whatsappLoading.set(true);
          return this.service.getQrSummary(request.date, request.workerId).pipe(
            map(response => response.text),
            catchError((error: unknown) => {
              this.formError.set(getErrorMessage(error));
              return of<string | null>('');
            })
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(text => {
        this.whatsappText.set(text ?? '');
        this.whatsappLoading.set(false);
      });
  }

  // ---------------------------------------------------------------- pestañas

  setTab(tab: StepTab): void {
    this.tab.set(tab);
    if (tab === 'qr') this.syncWhatsapp();
  }

  rowLabel(row: ElectronicVendorRow): string {
    return vendorLabel(row.workerName, row.shiftCode);
  }

  checkLabel(name: string | null, shiftCode: string | null): string {
    return vendorLabel(name, shiftCode);
  }

  // ------------------------------------------------------------ comprobantes

  isInvalid(workerId: string, field: VoucherField): boolean {
    return this.invalidCells().has(`${workerId}:${field}`);
  }

  selectAll(event: Event): void {
    (event.target as HTMLInputElement).select();
  }

  /** Se dispara al salir del campo o con Enter: valida, actualiza la fila y encola el guardado. */
  onVoucherChange(workerId: string, field: VoucherField, event: Event): void {
    if (!this.canEdit()) return;
    const input = event.target as HTMLInputElement;
    const key = `${workerId}:${field}`;
    const value = parseAmount(input.value);

    if (value === null) {
      this.markInvalid(key, true);
      this.formError.set('Digita un importe válido: sin negativos y con máximo 2 decimales.');
      return;
    }
    this.markInvalid(key, false);
    this.formError.set(null);
    input.value = formatAmount(value);

    const current = this.rows().find(row => row.workerId === workerId);
    const date = this.date();
    if (!current || !date || current[field] === value) return;

    const updated: ElectronicVendorRow = { ...current, [field]: value };
    this.rows.update(rows => rows.map(row => (row.workerId === workerId ? updated : row)));
    this.pendingSaves.update(count => count + 1);
    this.saveQueue$.next({ date, workerId, body: voucherValues(updated) });
  }

  // ------------------------------------------------------------------- QR

  onDragOver(event: DragEvent): void {
    if (!this.canEdit()) return;
    event.preventDefault();
    this.dragging.set(true);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    if (!this.canEdit()) return;
    const file = event.dataTransfer?.files?.[0];
    if (file) this.importFile(file);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) this.importFile(file);
  }

  private importFile(file: File): void {
    const date = this.date();
    if (!date || !this.canEdit() || this.uploading()) return;

    const problem = validateRedebanFile(file);
    if (problem) {
      this.formError.set(problem);
      return;
    }

    this.formError.set(null);
    this.uploading.set(true);
    this.service
      .importRedeban(date, file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.uploading.set(false);
          this.importResult.set(result);
          this.importedFileName.set(file.name);
          this.showToast('Archivo de Redeban cargado.');
          this.refresh(date);
        },
        error: (error: unknown) => {
          this.uploading.set(false);
          this.formError.set(getErrorMessage(error));
        }
      });
  }

  selectWhatsappVendor(event: Event): void {
    this.whatsappWorkerId.set((event.target as HTMLSelectElement).value || null);
    this.requestWhatsapp();
  }

  async copyWhatsapp(): Promise<void> {
    await this.copyText(this.whatsappText(), 'Texto del QR copiado.');
  }

  async copyStepSummary(): Promise<void> {
    const status = this.status();
    if (!status) return;
    await this.copyText(buildStepSummary(status), 'Resumen del paso 3 copiado.');
  }

  // ---------------------------------------------------------------- puntos

  onPointsInput(event: Event): void {
    this.pointsDirty = true;
    this.pointsDraft.set((event.target as HTMLInputElement).value);
  }

  savePoints(): void {
    const date = this.date();
    if (!date || !this.canEdit() || this.savingPoints()) return;

    const text = this.pointsDraft();
    const total = parseAmount(text);
    if (text.trim() === '' || total === null) {
      this.formError.set('Digita el total de Puntos Colombia como un importe válido.');
      return;
    }

    this.formError.set(null);
    this.savingPoints.set(true);
    this.service
      .savePoints(date, total)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingPoints.set(false);
          this.pointsDirty = false;
          this.showToast('Total de Puntos Colombia guardado.');
          this.refresh(date);
        },
        error: (error: unknown) => {
          this.savingPoints.set(false);
          this.formError.set(getErrorMessage(error));
        }
      });
  }

  // --------------------------------------------------------- completar paso

  completeStep(): void {
    const journal = this.journal();
    if (!journal || !this.canComplete()) return;

    this.completing.set(true);
    this.formError.set(null);
    this.journalService
      .completeStep(journal.id, STEP_NUMBER)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: step => {
          this.completing.set(false);
          this.journalService.updateCurrentStep(step);
          this.showToast('Paso 3 marcado como completado.');
        },
        error: (error: unknown) => {
          this.completing.set(false);
          this.formError.set(getErrorMessage(error));
          this.refresh(journal.date);
        }
      });
  }

  closeLoadError(): void {
    this.loadError.set(null);
  }

  // --------------------------------------------------------------- privados

  private load(date: string): void {
    if (this.loadedDate !== date) {
      this.loadedDate = date;
      this.pointsDirty = false;
      this.pointsDraft.set('');
      this.importResult.set(null);
      this.importedFileName.set(null);
      this.whatsappWorkerId.set(null);
    }
    this.loading.set(true);
    this.loadError.set(null);
    this.service
      .getStatus(date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: status => {
          this.applyStatus(status);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(getErrorMessage(error));
          this.loading.set(false);
        }
      });
  }

  /** Recarga sin pantalla de carga, después de guardar algo. */
  private refresh(date: string): void {
    this.service
      .getStatus(date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: status => this.applyStatus(status),
        error: (error: unknown) => this.formError.set(getErrorMessage(error))
      });
  }

  private applyStatus(status: ElectronicPaymentsStatus): void {
    this.status.set(status);
    if (this.pendingSaves() === 0) {
      this.rows.set(status.vendors);
      this.invalidCells.set(new Set<string>());
    }
    if (!this.pointsDirty) {
      const total = status.points.pointsTotal;
      this.pointsDraft.set(total === null ? '' : formatAmount(total) || '0');
    }
    if (this.tab() === 'qr') this.syncWhatsapp();
  }

  /** Mantiene seleccionado un vendedor válido y pide su texto de WhatsApp. */
  private syncWhatsapp(): void {
    const qr = this.status()?.qr;
    if (!qr || !qr.redebanLoaded || qr.vendors.length === 0) {
      this.whatsappWorkerId.set(null);
      this.summaryRequest$.next(null);
      return;
    }
    const current = this.whatsappWorkerId();
    if (!current || !qr.vendors.some(vendor => vendor.workerId === current)) {
      this.whatsappWorkerId.set(qr.vendors[0].workerId);
    }
    this.requestWhatsapp();
  }

  private requestWhatsapp(): void {
    const date = this.date();
    const workerId = this.whatsappWorkerId();
    if (!date || !workerId || !this.status()?.qr.redebanLoaded) {
      this.summaryRequest$.next(null);
      return;
    }
    this.summaryRequest$.next({ date, workerId });
  }

  private markInvalid(key: string, invalid: boolean): void {
    this.invalidCells.update(current => {
      const next = new Set(current);
      if (invalid) next.add(key);
      else next.delete(key);
      return next;
    });
  }

  private async copyText(text: string, successMessage: string): Promise<void> {
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      this.showToast(successMessage);
    } catch {
      if (this.fallbackCopy(text)) {
        this.showToast(successMessage);
      } else {
        this.formError.set('No se pudo copiar el texto. Selecciónalo y cópialo manualmente.');
      }
    }
  }

  private fallbackCopy(text: string): boolean {
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', '');
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();
    let copied = false;
    try {
      copied = document.execCommand('copy');
    } catch {
      copied = false;
    }
    document.body.removeChild(area);
    return copied;
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
