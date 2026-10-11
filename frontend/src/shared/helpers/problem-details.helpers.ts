import { FormGroup } from '@angular/forms';

import { ApiError, DEFAULT_ERROR_MESSAGE } from '../models/api-error.model';
import { FieldError } from '../models/dtos/problem-details.dto';
import { SERVER_ERROR_KEY } from './form-control.helpers';

/** True when the error was normalized by the error interceptor. */
export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

/** Message to show to the user: the backend `detail` or a default message. */
export function getErrorMessage(error: unknown): string {
  return isApiError(error) && error.detail ? error.detail : DEFAULT_ERROR_MESSAGE;
}

/** Validation errors per field; empty when the error has none. */
export function getFieldErrors(error: unknown): FieldError[] {
  return isApiError(error) ? error.fieldErrors : [];
}

/**
 * Marks each form control named in the backend errors with the server error and its message.
 * Returns true when at least one control was marked.
 */
export function applyFieldErrors(form: FormGroup, error: unknown): boolean {
  let applied = false;
  for (const fieldError of getFieldErrors(error)) {
    const control = form.get(fieldError.field);
    if (control) {
      control.setErrors({ ...control.errors, [SERVER_ERROR_KEY]: fieldError.message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}
