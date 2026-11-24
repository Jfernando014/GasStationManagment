import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginPageComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

describe('LoginPageComponent (HU-1, S1-12, S2-06)', () => {
  let component: LoginPageComponent;
  let fixture: ComponentFixture<LoginPageComponent>;
  const authService = { login: vi.fn(), hasToken: vi.fn() };
  const router = { navigate: vi.fn() };
  let paramMap = convertToParamMap({});

  beforeEach(async () => {
    authService.login.mockReset();
    authService.hasToken.mockReset();
    router.navigate.mockReset();
    paramMap = convertToParamMap({});

    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (key: string) => paramMap.get(key)
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component and initialize login form with empty values', () => {
    expect(component).toBeTruthy();
    expect(component.loginForm.value).toEqual({ username: '', password: '' });
    expect(component.loginForm.valid).toBe(false);
    expect(component.isAccountLocked).toBe(false);
  });

  it('should mark all fields as touched and not submit when form is invalid', () => {
    component.onSubmit();

    expect(component.loginForm.get('username')?.touched).toBe(true);
    expect(component.loginForm.get('password')?.touched).toBe(true);
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('should navigate to root route upon successful authentication with token', () => {
    authService.login.mockReturnValue(of({ token: 'mock-jwt-token' }));
    authService.hasToken.mockReturnValue(true);

    component.loginForm.setValue({ username: 'admin', password: 'password123' });
    component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({ username: 'admin', password: 'password123' });
    expect(router.navigate).toHaveBeenCalledWith(['/']);
    expect(component.isLoading).toBe(true);
    expect(component.errorMessage).toBe('');
  });

  it('should display error message with lockout warning when login fails with 401 Unauthorized', () => {
    authService.login.mockReturnValue(throwError(() => ({ status: 401 })));

    component.loginForm.setValue({ username: 'admin', password: 'wrongPassword' });
    component.onSubmit();

    expect(authService.login).toHaveBeenCalled();
    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toContain('Credenciales inválidas');
    expect(component.errorMessage).toContain('5 intentos fallidos');
    expect(component.isAccountLocked).toBe(false);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should display account locked banner and remaining minutes when login fails with 423 Locked', () => {
    authService.login.mockReturnValue(throwError(() => ({
      status: 423,
      error: {
        error: 'ACCOUNT_LOCKED',
        message: 'Account is temporarily locked',
        minutesRemaining: 14
      }
    })));

    component.loginForm.setValue({ username: 'admin', password: 'wrongPassword' });
    component.onSubmit();

    expect(component.isLoading).toBe(false);
    expect(component.isAccountLocked).toBe(true);
    expect(component.lockoutRemainingMinutes).toBe(14);
    expect(component.errorMessage).toContain('bloqueada por seguridad');

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const lockoutBanner = compiled.querySelector('.login-alert-lockout');
    expect(lockoutBanner).toBeTruthy();
    expect(lockoutBanner?.textContent).toContain('14 minutos');
  });

  it('should display connection error message when server is unreachable', () => {
    authService.login.mockReturnValue(throwError(() => ({ status: 0 })));

    component.loginForm.setValue({ username: 'admin', password: 'password123' });
    component.onSubmit();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toContain('Error de conexión');
  });

  it('should display session expired banner when expired query parameter is present', async () => {
    paramMap = convertToParamMap({ expired: 'true' });
    const expiredFixture = TestBed.createComponent(LoginPageComponent);
    const expiredComp = expiredFixture.componentInstance;
    expiredFixture.detectChanges();

    expect(expiredComp.sessionExpiredMessage).toContain('sesión ha expirado');
    const compiled = expiredFixture.nativeElement as HTMLElement;
    const infoBanner = compiled.querySelector('.login-alert-info');
    expect(infoBanner).toBeTruthy();
  });

  it('should toggle between split view and simplified view when clicking EDS Bellavista brand button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const shell = compiled.querySelector('#view-login');
    expect(shell?.classList.contains('is-simplified')).toBe(false);

    const splitBrandBtn = compiled.querySelector('#btn-brand-split') as HTMLButtonElement;
    splitBrandBtn.click();
    fixture.detectChanges();

    expect(component.isSimplifiedView()).toBe(true);
    expect(shell?.classList.contains('is-simplified')).toBe(true);

    const simplifiedBrandBtn = compiled.querySelector('#btn-brand-simplified') as HTMLButtonElement;
    simplifiedBrandBtn.click();
    fixture.detectChanges();

    expect(component.isSimplifiedView()).toBe(false);
    expect(shell?.classList.contains('is-simplified')).toBe(false);
  });

  it('should open and close the blurred Soporte técnico operativo modal via link, Entendido button, and Escape key', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#support-modal')).toBeNull();

    const supportTrigger = compiled.querySelector('#btn-open-support-modal') as HTMLButtonElement;
    supportTrigger.click();
    fixture.detectChanges();

    const modal = compiled.querySelector('#support-modal');
    expect(modal).toBeTruthy();
    expect(modal?.textContent).toContain('Soporte técnico operativo');
    expect(modal?.textContent).toContain('jfportilla@unicauca.edu.co');
    expect(modal?.textContent).toContain('+57 3128883241');

    const okBtn = compiled.querySelector('#btn-support-understood') as HTMLButtonElement;
    okBtn.click();
    fixture.detectChanges();
    expect(compiled.querySelector('#support-modal')).toBeNull();

    // Open again and verify Escape closes it
    supportTrigger.click();
    fixture.detectChanges();
    expect(compiled.querySelector('#support-modal')).toBeTruthy();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(compiled.querySelector('#support-modal')).toBeNull();
  });

  it('should not log authentication response tokens to console on login', () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined);
    authService.login.mockReturnValue(of({ token: 'secret-jwt-token' }));
    authService.hasToken.mockReturnValue(true);

    component.loginForm.setValue({ username: 'admin', password: 'password123' });
    component.onSubmit();

    expect(logSpy).not.toHaveBeenCalled();
    logSpy.mockRestore();
  });

  it('should expose accessible appButton actions and keep simplified brand visible on compact viewports', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const submitBtn = compiled.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(submitBtn.classList.contains('ab--primary')).toBe(true);
    expect(submitBtn.classList.contains('ab--block')).toBe(true);

    component.isCompactViewport.set(true);
    fixture.detectChanges();

    const simplifiedBrandBtn = compiled.querySelector('#btn-brand-simplified') as HTMLButtonElement;
    expect(simplifiedBrandBtn.getAttribute('aria-hidden')).toBeNull();
    expect(simplifiedBrandBtn.tabIndex).toBe(0);
  });
});
