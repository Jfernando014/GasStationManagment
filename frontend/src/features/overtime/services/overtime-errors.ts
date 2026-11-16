import { getErrorMessage, getFieldErrors, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { ActionResult, LoadResult } from '../models/overtime.model';

const SESSION_MESSAGE = 'Tu sesión ya no es válida. Inicia sesión de nuevo';

/**
 * Error of a read. With `missingIs400` a malformed link (400) counts as "does not exist", like an unknown id (404).
 * No session (403) is told apart because retrying does not help.
 */
export function toLoadError(error: unknown, missingIs400 = false): LoadResult<never> {
  if (isApiError(error)) {
    if (error.status === 404 || (missingIs400 && error.status === 400)) {
      return { outcome: 'NOT_FOUND', message: getErrorMessage(error) };
    }
    if (error.status === 401 || error.status === 403) return { outcome: 'ERROR', message: SESSION_MESSAGE };
  }
  return { outcome: 'ERROR', message: getErrorMessage(error) };
}

/**
 * Error of an action. `fieldByCode` says which field a backend `code` belongs to; a field error of a validation
 * (400 with `errors`) uses its own field. Without a field the message goes to the whole form.
 */
export function toActionError(
  error: unknown, fieldByCode: Record<string, string>, forbiddenMessage: string
): ActionResult {
  if (isApiError(error)) {
    if (error.status === 403) return { outcome: 'ERROR', message: forbiddenMessage, field: null };
    const fieldError = getFieldErrors(error)[0];
    if (fieldError) return { outcome: 'ERROR', message: fieldError.message, field: fieldError.field };
    const field = error.code ? fieldByCode[error.code] ?? null : null;
    return { outcome: 'ERROR', message: getErrorMessage(error), field };
  }
  return { outcome: 'ERROR', message: getErrorMessage(error), field: null };
}
