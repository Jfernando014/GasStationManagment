import { AbstractControl } from '@angular/forms';

/** Error key that holds a message sent by the backend for a field. */
export const SERVER_ERROR_KEY = 'server';

/**
 * Message to show under an invalid control.
 * The backend message is always shown. The other errors only once the user changed the field and left it,
 * or tried to submit (the form marks every control as dirty and touched), so just visiting a field
 * does not show errors. Custom validators can return their message as the error value
 * (e.g. `{ format: 'Solo números' }`). Returns null when there is nothing to show.
 */
export function getControlErrorMessage(control: AbstractControl): string | null {
  if (!control.invalid) {
    return null;
  }
  const errors = control.errors ?? {};
  if (typeof errors[SERVER_ERROR_KEY] === 'string') {
    return errors[SERVER_ERROR_KEY];
  }
  if (!control.touched || !control.dirty) {
    return null;
  }
  if (errors['required']) {
    return 'Este campo es obligatorio';
  }
  if (errors['minlength']) {
    return `Mínimo ${errors['minlength'].requiredLength} caracteres`;
  }
  if (errors['maxlength']) {
    return `Máximo ${errors['maxlength'].requiredLength} caracteres`;
  }
  const customMessage = Object.values(errors).find(value => typeof value === 'string');
  return customMessage ?? 'El valor no es válido';
}
