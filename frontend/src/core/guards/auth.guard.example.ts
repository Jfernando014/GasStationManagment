import { CanActivateFn } from '@angular/router';

/**
 * Example functional Auth Guard.
 * Checks for user authentication token in storage.
 */
export const authGuardExample: CanActivateFn = (route, state) => {
  const token = localStorage.getItem('auth_token');
  return Boolean(token);
};
