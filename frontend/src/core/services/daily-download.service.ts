import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, map, switchMap } from 'rxjs';
import { DownloadedFile } from '../../shared/models/daily-download.model';
import { adaptLubricantsFile, adaptSalesSheet } from '../../shared/helpers/daily-report-adapter.helpers';
import { getApiBaseUrl } from '../config/api-base';

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
  private readonly atrBaseUrl = `${getApiBaseUrl()}/api/v1/sales/downloads`;
  private readonly atrReportsBaseUrl = `${getApiBaseUrl()}/api/v1/reports`;

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

  /** VARIACIÓN of one day (yyyy-MM-dd). */
  downloadVariation(prmDate: string): Observable<DownloadedFile> {
    return this.fetchReportFile(
      `/variation/${encodeURIComponent(prmDate)}`,
      `variacion-${prmDate}.xlsx`
    );
  }

  /** COMPLEMENTO DOMINUS of one day (yyyy-MM-dd). */
  downloadDominusComplement(prmDate: string): Observable<DownloadedFile> {
    return this.fetchReportFile(
      `/dominus-complement/${encodeURIComponent(prmDate)}`,
      `complemento-dominus-${prmDate}.xlsx`
    );
  }

  /** Executive PowerPoint balance of the inclusive period from..to (yyyy-MM-dd). */
  downloadBalance(prmFrom: string, prmTo: string): Observable<DownloadedFile> {
    return this.fetchReportFile(
      `/balance?from=${encodeURIComponent(prmFrom)}&to=${encodeURIComponent(prmTo)}`,
      `balance-operativo-${prmFrom}_${prmTo}.pptx`
    );
  }

  private fetchBackendFile(prmDate: string, prmPath: string): Observable<Blob> {
    return this.atrHttp.get(`${this.atrBaseUrl}/${encodeURIComponent(prmDate)}/${prmPath}`, { responseType: 'blob' });
  }

  private fetchReportFile(prmPath: string, prmFileName: string): Observable<DownloadedFile> {
    return this.atrHttp
      .get(`${this.atrReportsBaseUrl}${prmPath}`, { responseType: 'blob' })
      .pipe(map((prmContent) => ({ fileName: prmFileName, content: prmContent })));
  }
}
