import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChangePasswordModalComponent } from './change-password-modal.component';
import { AuthService } from '../../../core/services/auth.service';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

describe('ChangePasswordModalComponent (S2-06)', () => {
  let component: ChangePasswordModalComponent;
  let fixture: ComponentFixture<ChangePasswordModalComponent>;
  const authService = { changePassword: vi.fn() };

  beforeEach(async () => {
    authService.changePassword.mockReset();

    await TestBed.configureTestingModule({
      imports: [ChangePasswordModalComponent],
      providers: [
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChangePasswordModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component with empty form and closed by default', () => {
    expect(component).toBeTruthy();
    expect(component.open()).toBe(false);
    expect(component.passwordForm.valid).toBe(false);
    expect(component.isLoading).toBe(false);
  });

  it('should require currentPassword, newPassword, and confirmPassword', () => {
    const current = component.passwordForm.get('currentPassword');
    const newPass = component.passwordForm.get('newPassword');
    const confirm = component.passwordForm.get('confirmPassword');

    expect(current?.valid).toBe(false);
    expect(newPass?.valid).toBe(false);
    expect(confirm?.valid).toBe(false);

    current?.setValue('oldPass123');
    expect(current?.valid).toBe(true);
  });

  it('should enforce minimum length of 6 for new password', () => {
    const newPass = component.passwordForm.get('newPassword');

    newPass?.setValue('12345');
    expect(newPass?.errors?.['minlength']).toBeTruthy();

    newPass?.setValue('123456');
    expect(newPass?.errors?.['minlength']).toBeFalsy();
  });

  it('should invalidate form with passwordMismatch when passwords do not match', () => {
    component.passwordForm.setValue({
      currentPassword: 'currentPassword123',
      newPassword: 'newPassword123',
      confirmPassword: 'differentPassword123'
    });

    expect(component.passwordForm.errors?.['passwordMismatch']).toBe(true);
    expect(component.passwordForm.valid).toBe(false);
  });

  it('should validate successfully when new and confirm passwords match', () => {
    component.passwordForm.setValue({
      currentPassword: 'currentPassword123',
      newPassword: 'newPassword123',
      confirmPassword: 'newPassword123'
    });

    expect(component.passwordForm.errors).toBeNull();
    expect(component.passwordForm.valid).toBe(true);
  });

  it('should toggle password visibility flags', () => {
    expect(component.showCurrentPassword).toBe(false);
    component.showCurrentPassword = true;
    expect(component.showCurrentPassword).toBe(true);

    expect(component.showNewPassword).toBe(false);
    component.showNewPassword = true;
    expect(component.showNewPassword).toBe(true);

    expect(component.showConfirmPassword).toBe(false);
    component.showConfirmPassword = true;
    expect(component.showConfirmPassword).toBe(true);
  });

  it('should submit changePassword request and set success message on 200 OK', () => {
    authService.changePassword.mockReturnValue(of({ message: 'Password changed successfully' }));

    component.passwordForm.setValue({
      currentPassword: 'currentPassword123',
      newPassword: 'newPassword123',
      confirmPassword: 'newPassword123'
    });

    component.onSubmit();

    expect(authService.changePassword).toHaveBeenCalledWith({
      currentPassword: 'currentPassword123',
      newPassword: 'newPassword123',
      confirmPassword: 'newPassword123'
    });
    expect(component.isLoading).toBe(false);
    expect(component.successMessage).toBe('Password changed successfully');
    expect(component.errorMessage).toBe('');
  });

  it('should display error message when backend returns 400 Bad Request', () => {
    authService.changePassword.mockReturnValue(throwError(() => ({
      status: 400,
      error: { error: 'Current password is incorrect' }
    })));

    component.passwordForm.setValue({
      currentPassword: 'wrongPassword123',
      newPassword: 'newPassword123',
      confirmPassword: 'newPassword123'
    });

    component.onSubmit();

    expect(authService.changePassword).toHaveBeenCalled();
    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Current password is incorrect');
  });

  it('should emit closed event and reset state when close() is called', () => {
    const closedSpy = vi.spyOn(component.closed, 'emit');
    component.errorMessage = 'Some error';

    component.close();

    expect(closedSpy).toHaveBeenCalled();
    expect(component.errorMessage).toBe('');
    expect(component.passwordForm.value).toEqual({
      currentPassword: null,
      newPassword: null,
      confirmPassword: null
    });
  });

  it('should close on escape key when open', () => {
    const closeSpy = vi.spyOn(component, 'close');
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    component.onEscape();

    expect(closeSpy).toHaveBeenCalled();
  });
});
