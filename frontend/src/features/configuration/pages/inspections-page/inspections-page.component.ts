import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { InspectionsService } from '../../../../core/services/inspections.service';
import { saveBlob } from '../../../../shared/helpers/file-download.helpers';
import {
  INSPECTION_CATEGORY_LABELS,
  INSPECTION_STATUS_LABELS
} from '../../../../shared/helpers/inventory-display.helpers';
import { getErrorMessage, getFieldErrors, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import {
  INSPECTION_CATEGORIES,
  INSPECTION_STATUSES,
  InspectionCategory,
  InspectionRequest,
  InspectionResponse,
  InspectionStatus
} from '../../../../shared/models/dtos/inspection.dto';

const TOAST_DURATION_MS = 3000;

interface InspectionDraft {
  code: string;
  category: InspectionCategory | '';
  target: string;
  dateExecuted: string;
  dateDue: string;
  certifier: string;
  toleranceMeasured: string;
  certDoc: string;
}

/** Configuración › Inspecciones: inspection, hermeticity and ONAC calibration log. */
@Component({
  selector: 'app-inspections-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inspections-page.component.html',
  styleUrl: './inspections-page.component.css'
})
export class InspectionsPageComponent {
  private readonly inspectionsService = inject(InspectionsService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categories = INSPECTION_CATEGORIES;
  readonly statuses = INSPECTION_STATUSES;
  readonly categoryLabels = INSPECTION_CATEGORY_LABELS;
  readonly statusLabels = INSPECTION_STATUS_LABELS;
  readonly today = this.currentDate();

  readonly inspections = signal<InspectionResponse[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly categoryFilter = signal<InspectionCategory | ''>('');
  readonly statusFilter = signal<InspectionStatus | ''>('');

  readonly draft = signal<InspectionDraft>(this.emptyDraft());
  readonly formOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly submitted = signal(false);
  readonly saving = signal(false);
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly formError = signal<string | null>(null);

  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;

  readonly visibleInspections = computed(() => {
    const category = this.categoryFilter();
    const status = this.statusFilter();
    return this.inspections().filter(inspection =>
      (!category || inspection.category === category) &&
      (!status || inspection.status === status)
    );
  });

  readonly counts = computed(() => {
    const inspections = this.inspections();
    return {
      total: inspections.length,
      vigentes: inspections.filter(inspection => inspection.status === 'VIGENTE').length,
      proximos: inspections.filter(inspection => inspection.status === 'PROXIMO').length,
      vencidos: inspections.filter(inspection => inspection.status === 'VENCIDO').length
    };
  });

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.inspectionsService
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: inspections => {
          this.inspections.set(this.sort(inspections));
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.loadError.set(getErrorMessage(error));
        }
      });
  }

  onCategoryFilter(value: string): void {
    this.categoryFilter.set(value as InspectionCategory | '');
  }

  onStatusFilter(value: string): void {
    this.statusFilter.set(value as InspectionStatus | '');
  }

  clearFilters(): void {
    this.categoryFilter.set('');
    this.statusFilter.set('');
  }

  openCreate(): void {
    this.editingId.set(null);
    this.draft.set(this.emptyDraft());
    this.submitted.set(false);
    this.clearErrors();
    this.formOpen.set(true);
  }

  openEdit(inspection: InspectionResponse): void {
    this.editingId.set(inspection.id);
    this.draft.set({
      code: inspection.code,
      category: inspection.category,
      target: inspection.target,
      dateExecuted: inspection.dateExecuted,
      dateDue: inspection.dateDue,
      certifier: inspection.certifier,
      toleranceMeasured: inspection.toleranceMeasured,
      certDoc: inspection.certDoc ?? ''
    });
    this.submitted.set(false);
    this.clearErrors();
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.submitted.set(false);
    this.clearErrors();
  }

  updateDraft(field: keyof InspectionDraft, value: string): void {
    this.draft.update(draft => ({ ...draft, [field]: value ?? '' }));
  }

  fieldError(field: string): string {
    return this.fieldErrors()[field] ?? '';
  }

  isInvalid(field: keyof InspectionDraft): boolean {
    if (!this.submitted() && !this.fieldError(field)) return false;
    const draft = this.draft();
    switch (field) {
      case 'code':
        return !draft.code.trim() || draft.code.trim().length > 20;
      case 'category':
        return !draft.category;
      case 'target':
        return !draft.target.trim() || draft.target.trim().length > 150;
      case 'dateExecuted':
        return !draft.dateExecuted || draft.dateExecuted > this.today;
      case 'dateDue':
        return !draft.dateDue || draft.dateDue <= draft.dateExecuted;
      case 'certifier':
        return !draft.certifier.trim() || draft.certifier.trim().length > 150;
      case 'toleranceMeasured':
        return !draft.toleranceMeasured.trim() || draft.toleranceMeasured.trim().length > 100;
      case 'certDoc':
        return draft.certDoc.trim().length > 200;
      default:
        return false;
    }
  }

  save(): void {
    if (this.saving()) return;
    this.submitted.set(true);
    this.clearErrors();
    const fields: (keyof InspectionDraft)[] = [
      'code', 'category', 'target', 'dateExecuted', 'dateDue',
      'certifier', 'toleranceMeasured', 'certDoc'
    ];
    if (fields.some(field => this.isInvalid(field))) {
      this.formError.set('Revisa los datos marcados para guardar la inspección.');
      return;
    }

    const draft = this.draft();
    const request: InspectionRequest = {
      code: draft.code.trim(),
      category: draft.category as InspectionCategory,
      target: draft.target.trim(),
      dateExecuted: draft.dateExecuted,
      dateDue: draft.dateDue,
      certifier: draft.certifier.trim(),
      toleranceMeasured: draft.toleranceMeasured.trim(),
      certDoc: draft.certDoc.trim() ? draft.certDoc.trim() : null
    };
    const editingId = this.editingId();
    const operation = editingId === null
      ? this.inspectionsService.create(request)
      : this.inspectionsService.update(editingId, request);

    this.saving.set(true);
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.saving.set(false);
        this.inspections.update(inspections => this.sort(editingId === null
          ? [...inspections, saved]
          : inspections.map(inspection => inspection.id === saved.id ? saved : inspection)));
        this.formOpen.set(false);
        this.submitted.set(false);
        this.showToast(editingId === null
          ? 'Inspección registrada correctamente.'
          : 'Inspección actualizada correctamente.');
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.showServerError(error);
      }
    });
  }

  /** ISO date (yyyy-MM-dd) shown as dd/MM/yyyy without shifting it through a time zone. */
  formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return year && month && day ? `${day}/${month}/${year}` : value;
  }

  exportCsv(): void {
    const rows = this.visibleInspections();
    if (rows.length === 0) {
      this.showToast('No hay inspecciones para exportar.');
      return;
    }
    const header = [
      'Código', 'Categoría', 'Equipo o tanque', 'Fecha ejecución', 'Vencimiento',
      'Certificador', 'Tolerancia', 'Estado', 'Documento'
    ];
    const lines = rows.map(inspection => [
      inspection.code,
      this.categoryLabels[inspection.category],
      inspection.target,
      inspection.dateExecuted,
      inspection.dateDue,
      inspection.certifier,
      inspection.toleranceMeasured,
      this.statusLabels[inspection.status],
      inspection.certDoc ?? ''
    ].map(value => this.csvCell(value)).join(';'));
    const content = [header.map(value => this.csvCell(value)).join(';'), ...lines].join('\r\n');
    const blob = new Blob(['\uFEFF' + content], { type: 'text/csv;charset=utf-8;' });
    saveBlob(blob, `Bitacora_Inspecciones_${this.today}.csv`);
    this.showToast('Bitácora de inspecciones descargada.');
  }

  private csvCell(value: string): string {
    return `"${value.replace(/"/g, '""')}"`;
  }

  private sort(inspections: InspectionResponse[]): InspectionResponse[] {
    return [...inspections].sort((a, b) => a.dateDue.localeCompare(b.dateDue) || b.id - a.id);
  }

  private emptyDraft(): InspectionDraft {
    return {
      code: '',
      category: '',
      target: '',
      dateExecuted: this.today,
      dateDue: '',
      certifier: '',
      toleranceMeasured: '',
      certDoc: ''
    };
  }

  private currentDate(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  }

  private showServerError(error: unknown): void {
    const mapped = getFieldErrors(error).reduce<Record<string, string>>((result, fieldError) => {
      result[fieldError.field] = fieldError.message;
      return result;
    }, {});
    if (Object.keys(mapped).length > 0) {
      this.fieldErrors.set(mapped);
      return;
    }
    if (isApiError(error) && error.status === 409) {
      this.fieldErrors.set({ code: 'El código del acta ya está registrado.' });
      return;
    }
    this.formError.set(getErrorMessage(error));
  }

  private clearErrors(): void {
    this.fieldErrors.set({});
    this.formError.set(null);
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
