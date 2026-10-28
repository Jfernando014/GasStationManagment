import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';

/** Builds a JWT-shaped token; only the `exp` claim of the payload is read. */
function tokenWithExp(expSeconds: number): string {
  const payload = btoa(JSON.stringify({ sub: 'admin', exp: expSeconds }))
    .replace(/=+$/, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `header.${payload}.signature`;
}

describe('AuthService token expiry', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    localStorage.clear();
    http.verify();
  });

  it('keeps a token that has not expired', () => {
    const token = tokenWithExp(Math.floor(Date.now() / 1000) + 3600);
    localStorage.setItem('token', token);

    expect(service.hasToken()).toBe(true);
    expect(service.getToken()).toBe(token);
  });

  it('removes an expired token instead of using it', () => {
    const token = tokenWithExp(Math.floor(Date.now() / 1000) - 60);
    localStorage.setItem('token', token);

    expect(service.getToken()).toBeNull();
    expect(service.hasToken()).toBe(false);
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('removes a token that is not a readable JWT', () => {
    localStorage.setItem('token', 'not-a-jwt');

    expect(service.hasToken()).toBe(false);
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('starts unauthenticated when the stored token is expired', () => {
    TestBed.resetTestingModule();
    localStorage.setItem('token', tokenWithExp(Math.floor(Date.now() / 1000) - 60));
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);

    const fresh = TestBed.inject(AuthService);

    let authenticated: boolean | null = null;
    fresh.isAuthenticated$.subscribe(value => (authenticated = value)).unsubscribe();
    expect(authenticated).toBe(false);
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('stores the token returned by the login', () => {
    const token = tokenWithExp(Math.floor(Date.now() / 1000) + 3600);
    let authenticated = false;
    service.isAuthenticated$.subscribe(value => (authenticated = value));
    service.login({ username: 'admin', password: 'admin123' }).subscribe();

    const call = http.expectOne('http://localhost:8080/api/v1/auth/login');
    call.flush({ token });

    expect(localStorage.getItem('token')).toBe(token);
    expect(authenticated).toBe(true);
  });

  it('clears the session on logout', () => {
    localStorage.setItem('token', tokenWithExp(Math.floor(Date.now() / 1000) + 3600));

    service.logout();

    expect(localStorage.getItem('token')).toBeNull();
    expect(service.hasToken()).toBe(false);
  });
});
