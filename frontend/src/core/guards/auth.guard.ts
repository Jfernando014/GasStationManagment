import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasToken()) {
    return true;
  }

  // Si no está logueado, se envía a /login
  router.navigate(['/login']);
  return false;
};
