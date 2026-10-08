import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginPageComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  loginForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  errorMessage = '';
  isLoading = false;

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
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
          if (err.status === 0) {
            this.errorMessage = 'Error de conexión con el servidor (Posible problema de CORS o servidor caído).';
          } else if (err.status === 401 || err.status === 403) {
            this.errorMessage = 'Credenciales inválidas. Por favor, intenta de nuevo.';
          } else {
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
