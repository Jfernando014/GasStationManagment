import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { ChangePasswordRequest } from '../../shared/models/dtos/auth-security.dto';

describe('AuthService (S2-05, S2-06)', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('should be created and report initial auth state based on localStorage', () => {
    expect(service).toBeTruthy();
    expect(service.hasToken()).toBe(false);
    expect(service.getToken()).toBeNull();
  });

  it('should save token in localStorage and notify isAuthenticated$ on successful login', () => {
    let authStatus = false;
    service.isAuthenticated$.subscribe(status => authStatus = status);

    service.login({ username: 'admin', password: 'password123' }).subscribe(response => {
      expect(response.token).toBe('mock.jwt.token');
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'admin', password: 'password123' });
    req.flush({ token: 'mock.jwt.token' });

    expect(service.hasToken()).toBe(true);
    expect(service.getToken()).toBe('mock.jwt.token');
    expect(authStatus).toBe(true);
  });

  it('should remove token from localStorage and emit false on logout', () => {
    localStorage.setItem('token', 'mock.jwt.token');
    expect(service.hasToken()).toBe(true);

    let authStatus = true;
    service.isAuthenticated$.subscribe(status => authStatus = status);

    service.logout();

    expect(service.hasToken()).toBe(false);
    expect(service.getToken()).toBeNull();
    expect(authStatus).toBe(false);
  });

  it('should send POST request to /api/v1/auth/change-password with change password payload', () => {
    const payload: ChangePasswordRequest = {
      currentPassword: 'oldPassword123',
      newPassword: 'newPassword456',
      confirmPassword: 'newPassword456'
    };

    service.changePassword(payload).subscribe(response => {
      expect(response.message).toBe('Password changed successfully');
    });

    const req = httpTesting.expectOne('http://localhost:8080/api/v1/auth/change-password');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ message: 'Password changed successfully' });
  });
});
