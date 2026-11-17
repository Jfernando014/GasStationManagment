/**
 * Models of the "Descargas del día" component (end-of-day step).
 *
 * @author javiersolanop777
 */

/** Ids of the files offered in the end-of-day step. Same order as the prototype. */
export type DailyDownloadId = 'pdf' | 'planilla' | 'lubricantes' | 'variacion' | 'complemento';

export interface DailyDownloadItem {
  id: DailyDownloadId;
  type: 'PDF' | 'XLSX';
  name: string;
  /** False while the backend does not offer the file yet. */
  enabled: boolean;
}

/** A downloaded file, ready to be saved. */
export interface DownloadedFile {
  fileName: string;
  content: Blob;
}

/**
 * Files of the card, in the order of the prototype.
 * To enable a new file: set enabled to true and add its request in DailyDownloadsComponent.
 */
export const DAILY_DOWNLOAD_ITEMS: readonly DailyDownloadItem[] = [
  { id: 'pdf', type: 'PDF', name: 'Dominus con saldo en caja', enabled: false },
  { id: 'planilla', type: 'XLSX', name: 'PLANILLA VENTAS', enabled: true },
  { id: 'lubricantes', type: 'XLSX', name: 'LUBRICANTES Y VARIOS', enabled: true },
  { id: 'variacion', type: 'XLSX', name: 'VARIACIÓN', enabled: false },
  { id: 'complemento', type: 'XLSX', name: 'COMPLEMENTO DOMINUS', enabled: false }
];
