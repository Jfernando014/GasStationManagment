import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

/** One row of the backup audit. */
export interface BackupAuditEntry {
  id: number;
  username: string;
  action: 'EXPORT' | 'IMPORT';
  fileName: string | null;
  sizeBytes: number;
  status: 'STARTED' | 'SUCCEEDED' | 'FAILED';
  message: string | null;
  clientIp: string | null;
  startedAt: string;
  finishedAt: string | null;
}

/** Answer of a successful restore. */
export interface RestoreResponse {
  fileName: string;
  sizeBytes: number;
  restartScheduled: boolean;
  message: string;
}

/** Dump ready to be saved by the browser. */
export interface BackupDownload {
  blob: Blob;
  fileName: string;
}

/**
 * Database export and import for administrators.
 * The token is added by authInterceptor.
 *
 * @author javiersolanop777
 */
@Injectable({ providedIn: 'root' })
export class BackupService {
  private readonly atrHttp = inject(HttpClient);
  private readonly atrBaseUrl = 'http://localhost:8080/api/v1/backups';

  /** Downloads a compressed custom dump. */
  exportBackup(): Observable<BackupDownload> {
    return this.atrHttp.get(`${this.atrBaseUrl}/export`, { observe: 'response', responseType: 'blob' }).pipe(
      map(prmResponse => ({
        blob: prmResponse.body ?? new Blob(),
        fileName: this.fileNameFrom(prmResponse.headers.get('Content-Disposition'))
      }))
    );
  }

  /** Uploads a dump. The events include the upload progress. */
  importBackup(prmFile: File, prmConfirmation: string): Observable<HttpEvent<RestoreResponse>> {
    const objForm = new FormData();
    objForm.append('file', prmFile);
    return this.atrHttp.post<RestoreResponse>(`${this.atrBaseUrl}/import`, objForm, {
      headers: { 'X-Confirm-Restore': prmConfirmation },
      observe: 'events',
      reportProgress: true
    });
  }

  /** Last backup operations, newest first. */
  getAudit(): Observable<BackupAuditEntry[]> {
    return this.atrHttp.get<BackupAuditEntry[]>(`${this.atrBaseUrl}/audit`);
  }

  private fileNameFrom(prmHeader: string | null): string {
    if (!prmHeader) {
      return 'gas-station.dump';
    }
    const encoded = /filename\*=UTF-8''([^;]+)/i.exec(prmHeader);
    if (encoded?.[1]) {
      return decodeURIComponent(encoded[1].trim());
    }
    const plain = /filename="?([^";]+)"?/i.exec(prmHeader);
    return plain?.[1]?.trim() ?? 'gas-station.dump';
  }
}
