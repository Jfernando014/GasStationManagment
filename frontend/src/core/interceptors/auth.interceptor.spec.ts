import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor (HU-1, S1-12, S2-06)', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  const authService = { getToken: vi.fn(), logout: vi.fn() };
  const router = { navigate: vi.fn() };

  beforeEach(() => {
    authService.getToken.mockReset();
    authService.logout.mockReset();
    router.navigate.mockReset();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should attach Authorization Bearer header when token is present', () => {
    authService.getToken.mockReturnValue('valid-session-jwt');

    http.get('/api/test').subscribe();

    const req = httpTesting.expectOne('/api/test');
    expect(req.request.headers.has('Authorization')).toBe(true);
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid-session-jwt');
    req.flush({});
  });

  it('should not attach Authorization header when no token exists', () => {
    authService.getToken.mockReturnValue(null);

    http.get('/api/test').subscribe();

    const req = httpTesting.expectOne('/api/test');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should logout and redirect to login with expired flag on 401 for non-login endpoint', () => {
    authService.getToken.mockReturnValue('expired-token');

    let errorReceived = false;
    http.get('/api/v1/protected/resource').subscribe({
      error: () => { errorReceived = true; }
    });

    const req = httpTesting.expectOne('/api/v1/protected/resource');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(errorReceived).toBe(true);
    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { expired: 'true' } });
  });

  it('should not redirect when 401 occurs on /login endpoint', () => {
    authService.getToken.mockReturnValue(null);

    let errorReceived = false;
    http.post('/api/v1/auth/login', {}).subscribe({
      error: () => { errorReceived = true; }
    });

    const req = httpTesting.expectOne('/api/v1/auth/login');
    req.flush({ message: 'Bad credentials' }, { status: 401, statusText: 'Unauthorized' });

    expect(errorReceived).toBe(true);
    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
