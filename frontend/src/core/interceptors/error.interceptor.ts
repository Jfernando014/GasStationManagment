import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

import { ApiError, DEFAULT_ERROR_MESSAGE } from '../../shared/models/api-error.model';
import { ProblemDetails } from '../../shared/models/dtos/problem-details.dto';

const CONNECTION_ERROR_MESSAGE = 'No se pudo conectar con el servidor';

/**
 * Turns every HttpErrorResponse into an ApiError and throws it again.
 * It only normalizes the error: each screen decides what to show.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }
      return throwError(() => toApiError(error));
    })
  );

function toApiError(error: HttpErrorResponse): ApiError {
  if (error.status === 0) {
    return new ApiError(0, { detail: CONNECTION_ERROR_MESSAGE });
  }
  if (isProblemDetails(error.error)) {
    return new ApiError(error.status, error.error);
  }
  return new ApiError(error.status, { detail: DEFAULT_ERROR_MESSAGE });
}

function isProblemDetails(body: unknown): body is ProblemDetails {
  if (typeof body !== 'object' || body === null) {
    return false;
  }
  const problem = body as ProblemDetails;
  return typeof problem.detail === 'string' || typeof problem.title === 'string';
}
