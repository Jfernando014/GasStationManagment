import { ApiError, DEFAULT_ERROR_MESSAGE } from '../../../../shared/models/api-error.model';
import {
  FAILED_AUDIT_MESSAGE,
  auditErrorMessage,
  exportErrorMessage,
  restoreErrorMessage
} from './backup-error-messages';

const RAW_RESTORE_ERROR = 'pg_restore: error: could not execute query: DROP SEQUENCE backup_audit_id_seq';
const RAW_EXPORT_ERROR = 'pg_dump: error: connection to server failed';

describe('backup error messages', () => {
  it('hides the raw restore error behind a generic message', () => {
    const message = restoreErrorMessage(new ApiError(500, {
      code: 'RESTORE_FAILED',
      detail: RAW_RESTORE_ERROR
    }));

    expect(message).toBe('No se pudo restaurar la base de datos. Revisa los logs del servidor o intenta de nuevo.');
    expect(message).not.toContain('pg_restore');
  });

  it('keeps a safe message when the migrations fail after restoring', () => {
    const message = restoreErrorMessage(new ApiError(500, {
      code: 'MIGRATION_FAILED',
      detail: 'Flyway failed with SQL syntax error'
    }));

    expect(message).toBe(
      'El respaldo se restauró, pero no se pudieron aplicar las migraciones. Se restauró la base anterior.'
    );
    expect(message).not.toContain('Flyway');
  });

  it('hides the raw export error behind a generic message', () => {
    const message = exportErrorMessage(new ApiError(500, {
      code: 'DUMP_FAILED',
      detail: RAW_EXPORT_ERROR
    }));

    expect(message).toBe('No se pudo generar el respaldo. Revisa los logs del servidor o intenta de nuevo.');
    expect(message).not.toContain('pg_dump');
  });

  it('keeps the safe restore status messages', () => {
    expect(restoreErrorMessage(new ApiError(403, {}))).toBe(
      'La restauración está deshabilitada en este servidor.'
    );
    expect(restoreErrorMessage(new ApiError(409, {}))).toBe(
      'Ya hay otra operación de respaldo en curso. Espera un momento.'
    );
    expect(restoreErrorMessage(new ApiError(413, {}))).toBe(
      'El archivo supera el tamaño máximo permitido (512 MB).'
    );
  });

  it('keeps safe validation details for client errors', () => {
    expect(restoreErrorMessage(new ApiError(400, {
      detail: 'The file is not a PostgreSQL custom dump'
    }))).toBe('The file is not a PostgreSQL custom dump');
    expect(exportErrorMessage(new ApiError(409, {}))).toBe(
      'Ya hay otra operación de respaldo en curso. Espera un momento.'
    );
  });

  it('uses the default message for unknown errors', () => {
    expect(restoreErrorMessage(new Error('boom'))).toBe(
      'No se pudo restaurar la base de datos. Intenta de nuevo.'
    );
    expect(exportErrorMessage(new Error('boom'))).toBe('No se pudo generar el respaldo. Intenta de nuevo.');
    expect(restoreErrorMessage(new ApiError(400, {}))).toBe(DEFAULT_ERROR_MESSAGE);
  });

  it('hides raw audit loading errors', () => {
    const message = auditErrorMessage(new ApiError(500, {
      detail: RAW_RESTORE_ERROR
    }));

    expect(message).toBe('No se pudo cargar el historial. Revisa los logs del servidor o intenta de nuevo.');
    expect(message).not.toContain('pg_restore');
  });

  it('exposes a generic message for failed audit entries', () => {
    expect(FAILED_AUDIT_MESSAGE).toBe('La operación falló. Revisa los logs del servidor.');
  });
});
