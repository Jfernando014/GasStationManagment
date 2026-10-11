/**
 * Error body returned by the backend (RFC 9457, application/problem+json).
 * `code` identifies the error for the screens; `errors` only comes with validation errors (400).
 */
export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: FieldError[];
}

/** Validation error for one field of the request. */
export interface FieldError {
  field: string;
  message: string;
}
