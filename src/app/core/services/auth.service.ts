import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { AuthResponse, LoginRequest, RegisterRequest, UserRole } from '../models/auth.model';
import { AuthApiService } from './auth-api.service';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class AuthService {

  constructor(
    private readonly authApi: AuthApiService,
    private readonly sessionService: SessionService,
    private readonly router: Router
  ) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.authApi.login({ email: credentials.email.trim(), password: credentials.password }).pipe(
      map(res => this.normalizeResponse(res)),
      tap(session => this.sessionService.saveSession(
        { token: session.accessToken, role: session.role, email: session.email, userId: session.userId },
        /* rememberMe */ true
      ))
    );
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.authApi.register(payload).pipe(
      map(res => this.normalizeResponse(res)),
      tap(session => this.sessionService.saveSession(
        { token: session.accessToken, role: session.role, email: session.email, userId: session.userId },
        true
      ))
    );
  }

  logout(): void {
    this.sessionService.clearSession();
    void this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return this.sessionService.getToken();
  }

  getUser(): AuthResponse | null {
    const token = this.sessionService.getToken();
    const role = this.sessionService.role() as UserRole | null;
    const email = this.sessionService.email();
    const userId = this.sessionService.userId();
    if (!token || !role || !userId) return null;
    return { accessToken: token, userId, email: email ?? '', role };
  }

  getUserId(): number | null {
    return this.sessionService.userId();
  }

  isAuthenticated(): boolean {
    return this.sessionService.isAuthenticated();
  }

  hasRole(role: string): boolean {
    return this.sessionService.role() === role;
  }

  hasAnyRole(roles: string[]): boolean {
    const current = this.sessionService.role();
    return !!current && roles.includes(current);
  }

  getPortalRoute(): string {
    return this.sessionService.getPortalRoute();
  }

  private normalizeResponse(res: AuthResponse): AuthResponse {
    const token = res.accessToken ?? (res as any).token ?? '';
    const rawRole = res.role ?? (res as any).roles?.[0] ?? '';
    const role = this.normalizeRole(String(rawRole)) as UserRole;
    return { ...res, accessToken: token, role };
  }

  private normalizeRole(raw: string): string {
    if (!raw) return '';
    const n = raw.replace(/^ROLE_/, '').trim().toUpperCase();
    return n === 'YOUTH' ? 'YOUTH_BENEFICIARY' : n;
  }
}
