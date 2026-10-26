import { Component, inject, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginPageComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  loginForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  errorMessage = '';
  sessionExpiredMessage = '';
  isAccountLocked = false;
  lockoutRemainingMinutes = 15;
  isLoading = false;

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('expired') === 'true') {
      this.sessionExpiredMessage = 'Tu sesión ha expirado por inactividad o seguridad. Por favor, inicia sesión nuevamente.';
    }
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.sessionExpiredMessage = '';
      this.cdr.detectChanges();
      
      this.authService.login(this.loginForm.getRawValue()).subscribe({
        next: (res) => {
          console.log('Login response:', res);
          // Si el servidor responde 200 pero no hay token, el guard nos rebotará
          if (this.authService.hasToken()) {
            this.router.navigate(['/']); 
          } else {
            console.warn('Login responded with success but no token was saved.');
            this.errorMessage = 'El servidor no devolvió credenciales válidas.';
            this.isLoading = false;
            this.cdr.detectChanges();
          }
        },
        error: (err) => {
          console.error('Login error:', err);
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
