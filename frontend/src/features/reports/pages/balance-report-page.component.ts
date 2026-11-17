import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { DailyDownloadService } from '../../../core/services/daily-download.service';
import { DOWNLOAD_ERROR_MESSAGES, getDownloadErrorMessage, saveBlob } from '../../../shared/helpers/file-download.helpers';

interface ReportMessage {
  text: string;
  isError: boolean;
}

/**
 * Page that downloads the executive PowerPoint balance of a date range.
 *
 * @author javiersolanop777
 */
@Component({
  selector: 'app-balance-report-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './balance-report-page.component.html',
  styleUrl: './balance-report-page.component.css'
})
export class BalanceReportPageComponent {
  private readonly atrService = inject(DailyDownloadService);

  protected readonly atrFrom = signal(this.firstDayOfMonth());
  protected readonly atrTo = signal(new Date().toISOString().slice(0, 10));
  protected readonly atrBusy = signal(false);
  protected readonly atrMessage = signal<ReportMessage | null>(null);

  protected download(): void {
    if (!this.atrFrom() || !this.atrTo()) {
      this.atrMessage.set({ text: 'Selecciona las dos fechas del período.', isError: true });
      return;
    }
    if (this.atrFrom() > this.atrTo()) {
      this.atrMessage.set({ text: 'La fecha inicial no puede ser posterior a la fecha final.', isError: true });
      return;
    }
    this.atrBusy.set(true);
    this.atrMessage.set(null);
    this.atrService
      .downloadBalance(this.atrFrom(), this.atrTo())
      .pipe(finalize(() => this.atrBusy.set(false)))
      .subscribe({
        next: (prmFile) => {
          try {
            saveBlob(prmFile.content, prmFile.fileName);
            this.atrMessage.set({ text: 'Balance descargado', isError: false });
          } catch {
            this.atrMessage.set({ text: DOWNLOAD_ERROR_MESSAGES.save, isError: true });
          }
        },
        error: (prmError: unknown) =>
          this.atrMessage.set({ text: getDownloadErrorMessage(prmError), isError: true })
      });
  }

  private firstDayOfMonth(): string {
    const today = new Date();
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-01`;
  }
}
