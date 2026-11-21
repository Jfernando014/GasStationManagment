import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { ConfirmModalComponent } from '../../../../atomic-design/organisms/confirm-modal/confirm-modal.component';
import { AutonomyThresholdsService } from '../../../../core/services/autonomy-thresholds.service';
import { getErrorMessage, getFieldErrors, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import {
  AutonomyThresholdRequest,
  AutonomyThresholdResponse
} from '../../../../shared/models/dtos/autonomy-threshold.dto';

type ThresholdField = 'validFrom' | 'warningDays' | 'criticalDays';

interface ThresholdForm {
  validFrom: string;
  warningDays: number | null;
  criticalDays: number | null;
}

const TOAST_DURATION_MS = 3000;

const emptyThresholdForm: ThresholdForm = { validFrom: '', warningDays: null, criticalDays: null };

/** Configuración › Umbrales: days of autonomy that raise a warning or a critical alert. */
@Component({
  selector: 'app-autonomy-thresholds-page',
  standalone: true,
  imports: [FormsModule, RouterLink, ActionButtonDirective, TagComponent, ConfirmModalComponent],
  templateUrl: './autonomy-thresholds-page.component.html'
})
export class AutonomyThresholdsPageComponent {
  private readonly thresholdsService = inject(AutonomyThresholdsService);
  private readonly destroyRef = inject(DestroyRef);

  readonly thresholds = signal<AutonomyThresholdResponse[]>([]);
  readonly form = signal<ThresholdForm>({ ...emptyThresholdForm });
  readonly editingId = signal<number | null>(null);
  readonly deleteTarget = signal<AutonomyThresholdResponse | null>(null);
  readonly deleting = signal(false);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly error = signal('');
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly toastText = signal('');
  readonly toastVisible = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.toastTimer));
    this.loadThresholds();
  }

  updateForm(field: ThresholdField, value: string | number | null): void {
    const numeric = field !== 'validFrom';
    this.form.update(form => ({ ...form, [field]: numeric ? this.toNumber(value) : String(value ?? '') }));
    this.error.set('');
    this.clearServerFieldErrors();
  }

  startEdit(threshold: AutonomyThresholdResponse): void {
    this.editingId.set(threshold.id);
    this.form.set({
      validFrom: threshold.validFrom,
      warningDays: threshold.warningDays,
      criticalDays: threshold.criticalDays
    });
    this.submitted.set(false);
    this.error.set('');
    this.clearServerFieldErrors();
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.form.set({ ...emptyThresholdForm });
    this.submitted.set(false);
    this.error.set('');
    this.clearServerFieldErrors();
  }

  save(): void {
    this.submitted.set(true);
    this.error.set('');
    this.clearServerFieldErrors();

    const request = this.toRequest(this.form());
    if (!request) {
      this.error.set('Completa la fecha de vigencia y verifica que el umbral crítico sea menor que el de advertencia.');
      return;
    }

    const editId = this.editingId();
    const operation = editId === null
      ? this.thresholdsService.create(request)
      : this.thresholdsService.update(editId, request);

    this.saving.set(true);
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.submitted.set(false);
        this.editingId.set(null);
        this.form.set({ ...emptyThresholdForm });
        this.loadThresholds();
        this.success(
          editId === null
            ? 'Umbrales registrados. Aplican desde la fecha de vigencia.'
            : 'Umbrales actualizados correctamente.'
        );
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.setServerError(error);
      }
    });
  }

  requestDelete(threshold: AutonomyThresholdResponse): void {
    this.error.set('');
    this.deleteTarget.set(threshold);
  }

  cancelDelete(): void {
    if (!this.deleting()) {
      this.deleteTarget.set(null);
    }
  }

  deleteConfirmMessage(): string {
    const target = this.deleteTarget();
    return target
      ? `¿Deseas eliminar los umbrales vigentes desde el ${this.formatDate(target.validFrom)}?`
      : '';
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;
    this.deleting.set(true);
    this.thresholdsService.delete(target.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        if (this.editingId() === target.id) {
          this.cancelEdit();
        }
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.loadThresholds();
        this.success('Vigencia de umbrales eliminada correctamente.');
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.error.set(getErrorMessage(error));
      }
    });
  }

  isFieldInvalid(field: ThresholdField): boolean {
    if (!this.submitted() && !this.fieldError(field)) return false;
    return this.validationMessage(field) !== '';
  }

  /** Client-side message; the backend repeats the same rules. */
  validationMessage(field: ThresholdField): string {
    const form = this.form();
    if (field === 'validFrom') {
      return form.validFrom ? '' : 'La fecha de vigencia es obligatoria.';
    }
    const value = field === 'warningDays' ? form.warningDays : form.criticalDays;
    const label = field === 'warningDays' ? 'advertencia' : 'crítico';
    if (value === null) return `El umbral de ${label} es obligatorio.`;
    if (value <= 0) return `El umbral de ${label} debe ser mayor que cero.`;
    if (field === 'criticalDays' && form.warningDays !== null && value >= form.warningDays) {
      return 'El umbral crítico debe ser menor que el de advertencia.';
    }
    return '';
  }

  fieldError(field: string): string {
    return this.serverFieldErrors()[field] ?? '';
  }

  formatDate(isoDate: string): string {
    const [year, month, day] = isoDate.split('-');
    return year && month && day ? `${day}/${month}/${year}` : isoDate;
  }

  private loadThresholds(): void {
    this.loading.set(true);
    this.thresholdsService.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: thresholds => {
        this.thresholds.set(thresholds);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(getErrorMessage(error));
      }
    });
  }

  private toRequest(form: ThresholdForm): AutonomyThresholdRequest | null {
    if (!form.validFrom || form.warningDays === null || form.criticalDays === null) return null;
    if (form.warningDays <= 0 || form.criticalDays <= 0) return null;
    if (form.criticalDays >= form.warningDays) return null;
    return {
      validFrom: form.validFrom,
      warningDays: form.warningDays,
      criticalDays: form.criticalDays
    };
  }

  private toNumber(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const number = Number(value);
    return Number.isNaN(number) ? null : number;
  }

  private setServerError(error: unknown): void {
    const mapped = getFieldErrors(error).reduce<Record<string, string>>((result, fieldError) => {
      result[fieldError.field] = fieldError.message;
      return result;
    }, {});
    if (Object.keys(mapped).length === 0) {
      mapped['validFrom'] = isApiError(error) && error.status === 409
        ? 'Ya existen umbrales con esa fecha de vigencia. Registra una fecha distinta.'
        : getErrorMessage(error);
    }
    this.serverFieldErrors.set(mapped);
  }

  private clearServerFieldErrors(): void {
    this.serverFieldErrors.set({});
  }

  private success(text: string): void {
    this.error.set('');
    this.showToast(text);
  }

  private showToast(text: string): void {
    clearTimeout(this.toastTimer);
    this.toastText.set(text);
    this.toastVisible.set(true);
    this.toastTimer = setTimeout(() => this.toastVisible.set(false), TOAST_DURATION_MS);
  }
}
