import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, switchMap } from 'rxjs';
import { DownloadedFile } from '../../shared/models/daily-download.model';
import { adaptLubricantsFile, adaptSalesSheet } from '../../shared/helpers/daily-report-adapter.helpers';

/**
 * Downloads the daily Excel files of one journal (end-of-day step).
 * The backend file is adapted to the layout of the prototype before it is returned.
 * The token is added by authInterceptor.
 *
 * @author javiersolanop777
 */
@Injectable({
  providedIn: 'root'
})
export class DailyDownloadService {
  private readonly atrHttp = inject(HttpClient);
  private readonly atrBaseUrl = 'http://localhost:8080/api/v1/sales/downloads';

  /** PLANILLA VENTAS of the journal of a date (yyyy-MM-dd), in the layout of the prototype. */
  downloadSalesSheet(prmDate: string): Observable<DownloadedFile> {
    return this.fetchBackendFile(prmDate, 'sales-sheet').pipe(
      switchMap((prmContent) => from(adaptSalesSheet(prmContent, prmDate)))
    );
  }

  /** LUBRICANTES Y VARIOS of the journal of a date (yyyy-MM-dd), in the layout of the prototype. */
  downloadLubricantsAndMisc(prmDate: string): Observable<DownloadedFile> {
    return this.fetchBackendFile(prmDate, 'lubricants-and-misc').pipe(
      switchMap((prmContent) => from(adaptLubricantsFile(prmContent, prmDate)))
    );
  }

  private fetchBackendFile(prmDate: string, prmPath: string): Observable<Blob> {
    return this.atrHttp.get(`${this.atrBaseUrl}/${encodeURIComponent(prmDate)}/${prmPath}`, { responseType: 'blob' });
  }
}
