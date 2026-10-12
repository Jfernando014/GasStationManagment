import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { vi } from 'vitest';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard (HU-1, S1-12)', () => {
  const authService = { hasToken: vi.fn() };
  const router = { navigate: vi.fn() };

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/jornada' } as RouterStateSnapshot;

  beforeEach(() => {
    authService.hasToken.mockReset();
    router.navigate.mockReset();

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    });
  });

  it('should allow access when the user has an active token', () => {
    authService.hasToken.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should deny access and redirect to /login when user has no token', () => {
    authService.hasToken.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
