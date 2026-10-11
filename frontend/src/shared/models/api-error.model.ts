import { FieldError, ProblemDetails } from './dtos/problem-details.dto';

/** Message shown when an error has no detail to show. */
export const DEFAULT_ERROR_MESSAGE = 'Ocurrió un error inesperado. Intenta de nuevo';

/**
 * Normalized HTTP error. The error interceptor throws it for every failed request,
 * whether the backend answered with Problem Details or with no recognizable body.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly title?: string;
  readonly detail?: string;
  readonly fieldErrors: FieldError[];

  constructor(status: number, problem: ProblemDetails) {
    super(problem.detail ?? problem.title ?? DEFAULT_ERROR_MESSAGE);
    this.name = 'ApiError';
    this.status = status;
    this.code = problem.code;
    this.title = problem.title;
    this.detail = problem.detail;
    this.fieldErrors = problem.errors ?? [];
  }
}
