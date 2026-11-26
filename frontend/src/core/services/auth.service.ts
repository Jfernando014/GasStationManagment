import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { ChangePasswordRequest, ChangePasswordResponse } from '../../shared/models/dtos/auth-security.dto';
import { getApiBaseUrl } from '../config/api-base';

const TOKEN_KEY = 'token';

interface TokenClaims {
  sub?: string;
  exp?: number;
  role?: unknown;
  roles?: unknown;
  authorities?: unknown;
}

export interface UserProfileSummary {
  username: string;
  displayName: string;
  email: string;
  roleLabel: string;
  initials: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = `${getApiBaseUrl()}/api/v1/auth`;

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

  /** Reads the user's profile info safely from the active JWT claims. */
  getUserProfile(): UserProfileSummary {
    const claims = this.readClaims();
    const rawUser = typeof claims?.sub === 'string' && claims.sub.trim() ? claims.sub.trim() : 'admin';
    const isAdmin = rawUser.toLowerCase() === 'admin';
    const displayName = isAdmin ? 'Administrador' : rawUser;
    const initials = displayName.slice(0, 2).toUpperCase();
    return {
      username: rawUser,
      displayName,
      email: isAdmin ? 'admin@edsbellavista.com' : `${rawUser}@edsbellavista.com`,
      roleLabel: 'Administrador',
      initials
    };
  }

  /**
   * Whether the user may change what only the administrator can. It is the single place that knows the role: when the
   * token names one (`role`, `roles` or `authorities`) it must include ADMIN. Today the backend sends none and
   * ADMIN is its only role, so a token without a role counts as administrator. The backend checks every write.
   */
  isAdmin(): boolean {
    const claims = this.readClaims();
    const named = [claims?.role, claims?.roles, claims?.authorities]
      .flat()
      .filter((role): role is string => typeof role === 'string');
    return named.length === 0 || named.some(role => role.replace(/^ROLE_/, '') === 'ADMIN');
  }

  /** Reads the `exp` claim of the JWT payload without verifying the signature. */
  private isExpired(token: string): boolean {
    const claims = this.decodePayload(token);
    if (!claims || typeof claims.exp !== 'number') return true;
    return claims.exp * 1000 <= Date.now();
  }

  private readClaims(): TokenClaims | null {
    const token = this.getToken();
    return token ? this.decodePayload(token) : null;
  }

  private decodePayload(token: string): TokenClaims | null {
    const payload = token.split('.')[1];
    if (!payload) return null;
    try {
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
      return JSON.parse(atob(padded)) as TokenClaims;
    } catch {
      return null;
    }
  }
}
