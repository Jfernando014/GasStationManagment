import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { InventoryAlertsService } from '../../../../core/services/inventory-alerts.service';
import {
  alertStatusLabel,
  alertStatusTone,
  alertTypeLabel,
  autonomyLevelLabel,
  autonomyLevelTone
} from '../../../../shared/helpers/inventory-alert-display.helpers';
import { formatDateTime, formatShortDate } from '../../../../shared/helpers/format.helpers';
import { formatDays, FUEL_LABELS } from '../../../../shared/helpers/inventory-display.helpers';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import { AlertStatus, InventoryAlertResponse } from '../../../../shared/models/dtos/inventory-alert.dto';

/** "OPEN" groups the alerts that are not resolved yet. */
type AlertFilter = 'OPEN' | AlertStatus;

const DIALOG_CLOSE_DELAY_MS = 320;
const MAX_NOTE_LENGTH = 200;

/**
 * Bandeja de alertas de inventario (HU-29): las alertas de autonomía que el sistema
 * detectó, con su nivel, su estado y las acciones de leer y atender.
 */
@Component({
  selector: 'app-inventory-alerts-page',
  standalone: true,
  imports: [FormsModule, RouterLink, EmptyStateComponent],
  templateUrl: './inventory-alerts-page.component.html',
  styleUrl: './inventory-alerts-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InventoryAlertsPageComponent {
  private readonly alertsService = inject(InventoryAlertsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly attendDialog = viewChild<ElementRef<HTMLDialogElement>>('attendDialog');

  readonly alerts = signal<InventoryAlertResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly savingId = signal<number | null>(null);

  readonly filter = signal<AlertFilter>('OPEN');
  readonly filters: { value: AlertFilter; label: string }[] = [
    { value: 'OPEN', label: 'Abiertas' },
    { value: 'ACTIVE', label: 'Activas' },
    { value: 'READ', label: 'Leídas' },
    { value: 'ATTENDED', label: 'Atendidas' },
    { value: 'RESOLVED', label: 'Resueltas' }
  ];

  readonly attendTarget = signal<InventoryAlertResponse | null>(null);
  readonly attendNote = signal('');
  readonly attendVisible = signal(false);
  readonly attendSaving = signal(false);
  readonly attendError = signal('');
  readonly maxNoteLength = MAX_NOTE_LENGTH;
  private dialogTimer?: ReturnType<typeof setTimeout>;

  readonly fuelLabels = FUEL_LABELS;
  readonly formatDays = formatDays;
  readonly formatShortDate = formatShortDate;
  readonly formatDateTime = formatDateTime;
  readonly statusLabel = alertStatusLabel;
  readonly statusTone = alertStatusTone;
  readonly levelLabel = autonomyLevelLabel;
  readonly levelTone = autonomyLevelTone;
  readonly typeLabel = alertTypeLabel;

  readonly visibleAlerts = computed(() => {
    const filter = this.filter();
    const alerts = this.alerts();
    return filter === 'OPEN'
      ? alerts.filter(alert => alert.status !== 'RESOLVED')
      : alerts.filter(alert => alert.status === filter);
  });

  readonly counts = computed(() => {
    const alerts = this.alerts();
    const open = alerts.filter(alert => alert.status !== 'RESOLVED');
    return {
      pending: alerts.filter(alert => alert.status === 'ACTIVE' || alert.status === 'READ').length,
      critical: open.filter(alert => alert.level === 'CRITICAL').length,
      warning: open.filter(alert => alert.level === 'WARNING').length,
      attended: alerts.filter(alert => alert.status === 'ATTENDED').length,
      resolved: alerts.filter(alert => alert.status === 'RESOLVED').length
    };
  });

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.dialogTimer));
    this.loadAlerts();
  }

  loadAlerts(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      open: this.alertsService.list(),
      resolved: this.alertsService.list('RESOLVED')
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ open, resolved }) => {
          this.alerts.set(this.sortAlerts([...open, ...resolved]));
          this.alertsService.setPending(this.counts().pending);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.error.set(getErrorMessage(error));
        }
      });
  }

  setFilter(value: AlertFilter): void {
    this.filter.set(value);
  }

  markRead(alert: InventoryAlertResponse): void {
    if (alert.status !== 'ACTIVE' || this.savingId() !== null) return;
    this.savingId.set(alert.id);
    this.actionError.set('');
    this.alertsService
      .markRead(alert.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.replaceAlert(updated);
          this.savingId.set(null);
        },
        error: (error: unknown) => {
          this.savingId.set(null);
          this.actionError.set(getErrorMessage(error));
        }
      });
  }

  openAttend(alert: InventoryAlertResponse): void {
    if (alert.status === 'RESOLVED') return;
    this.attendTarget.set(alert);
    this.attendNote.set(alert.attendNote ?? '');
    this.attendError.set('');
    const dialog = this.attendDialog()?.nativeElement;
    if (dialog && !dialog.open) dialog.showModal();
    clearTimeout(this.dialogTimer);
    requestAnimationFrame(() => this.attendVisible.set(true));
  }

  closeAttend(): void {
    this.attendVisible.set(false);
    clearTimeout(this.dialogTimer);
    this.dialogTimer = setTimeout(() => this.attendDialog()?.nativeElement.close(), DIALOG_CLOSE_DELAY_MS);
  }

  onAttendCancel(event: Event): void {
    event.preventDefault();
    this.closeAttend();
  }

  confirmAttend(): void {
    const target = this.attendTarget();
    if (!target || this.attendSaving()) return;

    const note = this.attendNote().trim();
    if (note.length > MAX_NOTE_LENGTH) {
      this.attendError.set(`La nota no puede superar los ${MAX_NOTE_LENGTH} caracteres.`);
      return;
    }

    this.attendSaving.set(true);
    this.attendError.set('');
    this.alertsService
      .attend(target.id, note ? { note } : {})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.replaceAlert(updated);
          this.attendSaving.set(false);
          this.closeAttend();
        },
        error: (error: unknown) => {
          this.attendSaving.set(false);
          this.attendError.set(getErrorMessage(error));
        }
      });
  }

  isCritical(alert: InventoryAlertResponse): boolean {
    return alert.level === 'CRITICAL';
  }

  private replaceAlert(updated: InventoryAlertResponse): void {
    this.alerts.update(alerts => this.sortAlerts(alerts.map(alert => (alert.id === updated.id ? updated : alert))));
    this.alertsService.setPending(this.counts().pending);
  }

  /** Critical first, then the shortest autonomy; resolved ones last by detection date. */
  private sortAlerts(alerts: InventoryAlertResponse[]): InventoryAlertResponse[] {
    return [...alerts].sort((a, b) => {
      const aOpen = a.status !== 'RESOLVED';
      const bOpen = b.status !== 'RESOLVED';
      if (aOpen !== bOpen) return aOpen ? -1 : 1;
      if (aOpen && a.level !== b.level) return a.level === 'CRITICAL' ? -1 : 1;
      if (aOpen && a.autonomyDays !== b.autonomyDays) return a.autonomyDays - b.autonomyDays;
      return b.createdAt.localeCompare(a.createdAt);
    });
  }
}
