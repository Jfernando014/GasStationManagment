import { Component, Input, inject, signal } from '@angular/core';
import { Observable, finalize } from 'rxjs';
import { DailyDownloadService } from '../../../core/services/daily-download.service';
import {
  DAILY_DOWNLOAD_ITEMS,
  DailyDownloadId,
  DailyDownloadItem,
  DownloadedFile
} from '../../../shared/models/daily-download.model';
import {
  DOWNLOAD_ERROR_MESSAGES,
  getDownloadErrorMessage,
  saveBlob
} from '../../../shared/helpers/file-download.helpers';

interface DownloadMessage {
  text: string;
  isError: boolean;
}

/**
 * "Descargas del día" card of the end-of-day step. Reusable: it only needs the journal date.
 * Usage: <app-daily-downloads [atrDate]="journal.date"></app-daily-downloads>
 *
 * @author javiersolanop777
 */
@Component({
  selector: 'app-daily-downloads',
  standalone: true,
  templateUrl: './daily-downloads.component.html',
  styleUrls: ['./daily-downloads.component.css']
})
export class DailyDownloadsComponent {
  /** Date of the journal (yyyy-MM-dd). */
  @Input() atrDate = '';

  protected readonly atrItems = DAILY_DOWNLOAD_ITEMS;
  protected readonly atrBusyIds = signal<readonly DailyDownloadId[]>([]);
  protected readonly atrMessage = signal<DownloadMessage | null>(null);
  private readonly atrService = inject(DailyDownloadService);

  protected isBusy(prmId: DailyDownloadId): boolean {
    return this.atrBusyIds().includes(prmId);
  }

  protected isDisabled(prmItem: DailyDownloadItem): boolean {
    return !prmItem.enabled || !this.atrDate || this.isBusy(prmItem.id);
  }

  protected download(prmItem: DailyDownloadItem): void {
    if (this.isDisabled(prmItem)) {
      return;
    }
    const objRequest = this.requestFor(prmItem.id);
    if (!objRequest) {
      return;
    }
    this.atrMessage.set(null);
    this.atrBusyIds.update((prmIds) => [...prmIds, prmItem.id]);
    objRequest
      .pipe(finalize(() => this.atrBusyIds.update((prmIds) => prmIds.filter((prmId) => prmId !== prmItem.id))))
      .subscribe({
        next: (objFile) => this.save(prmItem, objFile),
        error: (objError: unknown) =>
          this.atrMessage.set({ text: getDownloadErrorMessage(objError), isError: true })
      });
  }

  private save(prmItem: DailyDownloadItem, prmFile: DownloadedFile): void {
    try {
      saveBlob(prmFile.content, prmFile.fileName);
      this.atrMessage.set({ text: `${prmItem.name} descargado`, isError: false });
    } catch {
      this.atrMessage.set({ text: DOWNLOAD_ERROR_MESSAGES.save, isError: true });
    }
  }

  /** Request of each file. Files without a request are not offered by the backend yet. */
  private requestFor(prmId: DailyDownloadId): Observable<DownloadedFile> | null {
    switch (prmId) {
      case 'planilla':
        return this.atrService.downloadSalesSheet(this.atrDate);
      case 'lubricantes':
        return this.atrService.downloadLubricantsAndMisc(this.atrDate);
      default:
        return null;
    }
  }
}
