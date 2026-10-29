import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { ChangePasswordRequest, ChangePasswordResponse } from '../../shared/models/dtos/auth-security.dto';

const TOKEN_KEY = 'token';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/v1/auth';

  private readonly isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor() {
    // A token stored before an expiry or a database reset must not keep the session alive.
    this.isAuthenticatedSubject.next(this.getToken() !== null);
  }

  login(credentials: { username: string; password: string }): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        if (response && response.token) {
          localStorage.setItem(TOKEN_KEY, response.token);
          this.isAuthenticatedSubject.next(true);
        }
      })
    );
  }

  changePassword(request: ChangePasswordRequest): Observable<ChangePasswordResponse> {
    return this.http.post<ChangePasswordResponse>(`${this.apiUrl}/change-password`, request);
  }

  logout(): void {
    this.clearSession();
  }

  /** True only when the stored token exists and has not expired. */
  hasToken(): boolean {
    return this.getToken() !== null;
  }

  /** Expired or malformed tokens are removed so requests are never sent with them. */
  getToken(): string | null {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return null;
    if (this.isExpired(token)) {
      this.clearSession();
      return null;
    }
    return token;
  }

  private clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.isAuthenticatedSubject.next(false);
  }

  /** Reads the `exp` claim of the JWT payload without verifying the signature. */
  private isExpired(token: string): boolean {
    const payload = token.split('.')[1];
    if (!payload) return true;
    try {
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
      const claims = JSON.parse(atob(padded)) as { exp?: number };
      if (typeof claims.exp !== 'number') return true;
      return claims.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}
