import { Component, ElementRef, EventEmitter, Output, ViewChild, effect, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

export function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const newPassword = control.get('newPassword')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  if (!newPassword || !confirmPassword) return null;
  return newPassword === confirmPassword ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-change-password-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password-modal.component.html',
  styleUrl: './change-password-modal.component.css',
  host: { '(document:keydown.escape)': 'onEscape()' }
})
export class ChangePasswordModalComponent {
  readonly open = input(false);
  @Output() readonly closed = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  @ViewChild('currentPasswordInput') private readonly currentPasswordInput?: ElementRef<HTMLInputElement>;
  private lastFocused: HTMLElement | null = null;

  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  isLoading = false;
  errorMessage = '';
  successMessage = '';

  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordsMatchValidator });

  constructor() {
    effect(() => {
      if (this.open()) {
        this.lastFocused = document.activeElement as HTMLElement;
        this.resetState();
        queueMicrotask(() => this.currentPasswordInput?.nativeElement.focus());
      } else {
        this.lastFocused?.focus();
        this.lastFocused = null;
      }
    });
  }

  onEscape(): void {
    if (this.open()) {
      this.close();
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close();
    }
  }

  close(): void {
    this.resetState();
    this.closed.emit();
  }

  onSubmit(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword, confirmPassword } = this.passwordForm.getRawValue();
    if (!currentPassword || !newPassword || !confirmPassword) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.changePassword({ currentPassword, newPassword, confirmPassword }).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successMessage = response.message || 'Contraseña cambiada exitosamente.';
        this.passwordForm.reset();
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 400 && err.error?.error) {
          this.errorMessage = err.error.error;
        } else if (err.status === 401 || err.status === 403) {
          this.errorMessage = 'No tienes autorización para realizar esta acción o tu sesión ha expirado.';
        } else {
          this.errorMessage = 'Ocurrió un error al intentar cambiar la contraseña.';
        }
      }
    });
  }

  private resetState(): void {
    this.passwordForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = false;
    this.showCurrentPassword = false;
    this.showNewPassword = false;
    this.showConfirmPassword = false;
  }
}
