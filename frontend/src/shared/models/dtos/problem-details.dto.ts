/**
 * Error body returned by the backend (RFC 9457, application/problem+json).
 * `code` identifies the error for the screens; `errors` only comes with validation errors (400).
 *
 * The inventory module still answers with its own envelope
 * (`{ timestamp, status, error, message, path, fields }`), so both shapes are accepted.
 */
export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: FieldError[];
  /** Inventory envelope: short error name, for example "Invalid data". */
  error?: string;
  /** Inventory envelope: message for the user. */
  message?: string;
  /** Inventory envelope: validation errors per field. */
  fields?: FieldError[];
}

/** Validation error for one field of the request. */
export interface FieldError {
  field: string;
  message: string;
}
