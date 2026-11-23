import { ChangeDetectorRef, Component, DestroyRef, HostListener, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ActionButtonDirective } from '../../atoms/action-button/action-button.directive';
import { AuthService } from '../../../core/services/auth.service';

const COMPACT_LOGIN_QUERY = '(max-width: 900px)';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ActionButtonDirective],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly supportEmail = 'jfportilla@unicauca.edu.co';
  readonly supportPhone = '+57 3128883241';
  readonly supportPhoneHref = 'tel:+573128883241';

  loginForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  errorMessage = '';
  sessionExpiredMessage = '';
  isAccountLocked = false;
  lockoutRemainingMinutes = 15;
  isLoading = false;

  readonly isSimplifiedView = signal(false);
  readonly isCompactViewport = signal(false);
  readonly isSupportModalOpen = signal(false);
  readonly showPassword = signal(false);
  readonly copiedField = signal<'email' | 'phone' | null>(null);

  private copyResetTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    const media = typeof window === 'undefined' ? null : window.matchMedia?.(COMPACT_LOGIN_QUERY) ?? null;
    if (media) {
      const sync = () => this.isCompactViewport.set(media.matches);
      sync();
      media.addEventListener('change', sync);
      this.destroyRef.onDestroy(() => media.removeEventListener('change', sync));
    }
    this.destroyRef.onDestroy(() => {
      if (this.copyResetTimer) {
        clearTimeout(this.copyResetTimer);
      }
    });
  }

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('expired') === 'true') {
      this.sessionExpiredMessage = 'Tu sesión ha expirado por inactividad o seguridad. Por favor, inicia sesión nuevamente.';
    }
  }

  toggleSimplifiedView(event?: Event): void {
    (event?.currentTarget as HTMLElement | null)?.blur?.();
    this.isSimplifiedView.update(current => !current);
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(current => !current);
  }

  openSupportModal(): void {
    this.copiedField.set(null);
    this.isSupportModalOpen.set(true);
  }

  closeSupportModal(): void {
    this.isSupportModalOpen.set(false);
  }

  onSupportBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeSupportModal();
    }
  }

  copyContact(value: string, field: 'email' | 'phone'): void {
    if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(value).catch(() => undefined);
    }
    this.copiedField.set(field);
    if (this.copyResetTimer) {
      clearTimeout(this.copyResetTimer);
    }
    this.copyResetTimer = setTimeout(() => {
      this.copiedField.set(null);
    }, 2000);
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isSupportModalOpen()) {
      this.closeSupportModal();
    }
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.sessionExpiredMessage = '';
      this.cdr.detectChanges();

      this.authService.login(this.loginForm.getRawValue()).subscribe({
        next: () => {
          // Si el servidor responde 200 pero no hay token, el guard nos rebotará
          if (this.authService.hasToken()) {
            this.router.navigate(['/']);
          } else {
            this.errorMessage = 'El servidor no devolvió credenciales válidas.';
            this.isLoading = false;
            this.cdr.detectChanges();
          }
        },
        error: (err) => {
          if (err.status === 423) {
            this.isAccountLocked = true;
            this.lockoutRemainingMinutes = err.error?.minutesRemaining ?? 15;
            this.errorMessage = 'Cuenta temporalmente bloqueada por seguridad. Se superó el límite de 5 intentos fallidos consecutivos.';
          } else if (err.status === 0) {
            this.isAccountLocked = false;
            this.errorMessage = 'Error de conexión con el servidor (Posible problema de CORS o servidor caído).';
          } else if (err.status === 401 || err.status === 403) {
            this.isAccountLocked = false;
            this.errorMessage = 'Credenciales inválidas. Advertencia: Tras 5 intentos fallidos consecutivos, tu cuenta será bloqueada temporalmente por 15 minutos.';
          } else {
            this.isAccountLocked = false;
            this.errorMessage = 'Ocurrió un error inesperado (' + err.status + ').';
          }
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      this.loginForm.markAllAsTouched();
    }
  }
}
