import { CommonModule } from '@angular/common';
import { HttpEventType } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { AuthService } from '../../../../core/services/auth.service';
import {
  BackupAuditEntry,
  BackupService
} from '../../../../core/services/backup.service';
import { saveBlob } from '../../../../shared/helpers/file-download.helpers';
import {
  FAILED_AUDIT_MESSAGE,
  auditErrorMessage,
  exportErrorMessage,
  restoreErrorMessage
} from './backup-error-messages';

/** Configuración › Respaldos: export and import of the database by an administrator. */
@Component({
  selector: 'app-backup-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ActionButtonDirective],
  templateUrl: './backup-page.component.html',
  styleUrl: './backup-page.component.css'
})
export class BackupPageComponent {
  private readonly atrBackupService = inject(BackupService);
  private readonly atrAuthService = inject(AuthService);
  private readonly atrRouter = inject(Router);
  private readonly atrDestroyRef = inject(DestroyRef);

  readonly phrase = 'RESTAURAR';
  readonly failedAuditMessage = FAILED_AUDIT_MESSAGE;
  readonly audit = signal<BackupAuditEntry[]>([]);
  readonly auditLoading = signal(true);
  readonly auditError = signal<string | null>(null);
  readonly exporting = signal(false);
  readonly exportError = signal<string | null>(null);
  readonly restoring = signal(false);
  readonly restoreError = signal<string | null>(null);
  readonly restoreSuccess = signal<string | null>(null);
  readonly selectedFile = signal<File | null>(null);
  readonly confirmation = signal('');
  readonly uploadProgress = signal(0);
  readonly isRestoreReady = computed(() =>
    this.selectedFile() !== null &&
    this.confirmation().trim().toUpperCase() === this.phrase &&
    !this.restoring()
  );

  private logoutTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.atrDestroyRef.onDestroy(() => clearTimeout(this.logoutTimer));
    this.loadAudit();
  }

  loadAudit(): void {
    this.auditLoading.set(true);
    this.auditError.set(null);
    this.atrBackupService.getAudit().pipe(takeUntilDestroyed(this.atrDestroyRef)).subscribe({
      next: entries => {
        this.audit.set(entries);
        this.auditLoading.set(false);
      },
      error: (error: unknown) => {
        this.auditLoading.set(false);
        this.auditError.set(auditErrorMessage(error));
      }
    });
  }

  exportBackup(): void {
    if (this.exporting()) return;
    this.exporting.set(true);
    this.exportError.set(null);
    this.atrBackupService.exportBackup().pipe(takeUntilDestroyed(this.atrDestroyRef)).subscribe({
      next: file => {
        this.exporting.set(false);
        saveBlob(file.blob, file.fileName);
        this.loadAudit();
      },
      error: (error: unknown) => {
        this.exporting.set(false);
        this.exportError.set(exportErrorMessage(error));
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
    this.restoreError.set(null);
    this.restoreSuccess.set(null);
    this.uploadProgress.set(0);
  }

  restore(): void {
    const file = this.selectedFile();
    if (!file || this.restoring()) return;
    if (this.confirmation().trim().toUpperCase() !== this.phrase) {
      this.restoreError.set(`Escribe ${this.phrase} para confirmar la restauración.`);
      return;
    }

    this.restoring.set(true);
    this.restoreError.set(null);
    this.restoreSuccess.set(null);
    this.uploadProgress.set(0);

    this.atrBackupService.importBackup(file, this.confirmation().trim().toUpperCase())
      .pipe(takeUntilDestroyed(this.atrDestroyRef))
      .subscribe({
        next: event => {
          if (event.type === HttpEventType.UploadProgress) {
            const total = event.total ?? file.size;
            this.uploadProgress.set(total > 0 ? Math.round((event.loaded * 100) / total) : 0);
            return;
          }
          if (event.type === HttpEventType.Response) {
            this.uploadProgress.set(100);
            this.restoring.set(false);
            this.selectedFile.set(null);
            this.confirmation.set('');
            this.restoreSuccess.set(event.body?.message ?? 'Restauración completada correctamente.');
            this.scheduleLogout();
          }
        },
        error: (error: unknown) => {
          this.restoring.set(false);
          this.uploadProgress.set(0);
          this.restoreError.set(restoreErrorMessage(error));
        }
      });
  }

  formatSize(bytes: number): string {
    if (!bytes) return '—';
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = bytes;
    let index = 0;
    while (size >= 1024 && index < units.length - 1) {
      size /= 1024;
      index++;
    }
    return `${new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 }).format(size)} ${units[index]}`;
  }

  formatDate(value: string | null): string {
    if (!value) return '—';
    return new Date(value).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
  }

  actionLabel(action: BackupAuditEntry['action']): string {
    return action === 'EXPORT' ? 'Exportación' : 'Restauración';
  }

  statusLabel(status: BackupAuditEntry['status']): string {
    switch (status) {
      case 'STARTED':
        return 'En curso';
      case 'SUCCEEDED':
        return 'Completado';
      default:
        return 'Fallido';
    }
  }

  private scheduleLogout(): void {
    clearTimeout(this.logoutTimer);
    this.logoutTimer = setTimeout(() => {
      this.atrAuthService.logout();
      this.atrRouter.navigate(['/login']);
    }, 2500);
  }
}
