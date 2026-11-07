import { ReportAdaptationError } from './daily-report-adapter.helpers';
import { isApiError } from './problem-details.helpers';

/**
 * Helpers to save downloaded files and to explain download errors.
 *
 * @author javiersolanop777
 */

export const DOWNLOAD_ERROR_MESSAGES = {
  badRequest: 'La fecha de la jornada no es válida.',
  unauthorized: 'Tu sesión expiró. Inicia sesión de nuevo.',
  forbidden: 'No tienes permiso para descargar este archivo.',
  noData: 'Esta jornada aún no tiene cierres de vendedores para descargar.',
  default: 'No se pudo generar el archivo. Intenta de nuevo.',
  save: 'No se pudo guardar el archivo en tu equipo.',
  format: 'El archivo recibido del servidor no tiene el formato esperado.'
} as const;

/** Saves a blob in the computer of the user with a temporary link. */
export function saveBlob(prmContent: Blob, prmFileName: string): void {
  const varUrl = URL.createObjectURL(prmContent);
  const objLink = document.createElement('a');
  objLink.href = varUrl;
  objLink.download = prmFileName;
  document.body.appendChild(objLink);
  objLink.click();
  document.body.removeChild(objLink);
  URL.revokeObjectURL(varUrl);
}

/**
 * Message for the user when a download fails. The backend answer of a file request arrives as a
 * blob, so the message is chosen by the HTTP status and not by the text of the server.
 */
export function getDownloadErrorMessage(prmError: unknown): string {
  if (prmError instanceof ReportAdaptationError) {
    return DOWNLOAD_ERROR_MESSAGES.format;
  }
  if (!isApiError(prmError)) {
    return DOWNLOAD_ERROR_MESSAGES.default;
  }
  switch (prmError.status) {
    case 0:
      return prmError.detail ?? DOWNLOAD_ERROR_MESSAGES.default;
    case 400:
      return DOWNLOAD_ERROR_MESSAGES.badRequest;
    case 401:
      return DOWNLOAD_ERROR_MESSAGES.unauthorized;
    case 403:
      return DOWNLOAD_ERROR_MESSAGES.forbidden;
    case 404:
      return DOWNLOAD_ERROR_MESSAGES.noData;
    default:
      return DOWNLOAD_ERROR_MESSAGES.default;
  }
}
