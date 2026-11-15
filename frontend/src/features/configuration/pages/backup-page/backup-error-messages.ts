import { getErrorMessage, isApiError } from '../../../../shared/helpers/problem-details.helpers';

/** Generic text shown for failed entries in the backup history. */
export const FAILED_AUDIT_MESSAGE = 'La operación falló. Revisa los logs del servidor.';

/** Safe message for restore failures, never exposing the backend detail. */
export function restoreErrorMessage(error: unknown): string {
  if (!isApiError(error)) {
    return 'No se pudo restaurar la base de datos. Intenta de nuevo.';
  }
  switch (error.status) {
    case 403:
      return 'La restauración está deshabilitada en este servidor.';
    case 409:
      return 'Ya hay otra operación de respaldo en curso. Espera un momento.';
    case 413:
      return 'El archivo supera el tamaño máximo permitido (512 MB).';
    case 500:
      return internalErrorMessage(error.code, 'restore');
    default:
      return getErrorMessage(error);
  }
}

/** Safe message for export failures, never exposing the backend detail. */
export function exportErrorMessage(error: unknown): string {
  if (!isApiError(error)) {
    return 'No se pudo generar el respaldo. Intenta de nuevo.';
  }
  switch (error.status) {
    case 409:
      return 'Ya hay otra operación de respaldo en curso. Espera un momento.';
    case 500:
      return internalErrorMessage(error.code, 'export');
    default:
      return getErrorMessage(error);
  }
}

/** Safe message for audit loading failures. */
export function auditErrorMessage(error: unknown): string {
  if (isApiError(error) && error.status === 500) {
    return 'No se pudo cargar el historial. Revisa los logs del servidor o intenta de nuevo.';
  }
  return getErrorMessage(error);
}

function internalErrorMessage(code: string | undefined, action: 'restore' | 'export'): string {
  if (action === 'restore' && code === 'MIGRATION_FAILED') {
    return 'El respaldo se restauró, pero no se pudieron aplicar las migraciones. Se restauró la base anterior.';
  }
  return action === 'export'
    ? 'No se pudo generar el respaldo. Revisa los logs del servidor o intenta de nuevo.'
    : 'No se pudo restaurar la base de datos. Revisa los logs del servidor o intenta de nuevo.';
}
