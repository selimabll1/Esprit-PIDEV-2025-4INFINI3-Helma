import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { switchMap, map, tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  BackendAuthResponse,
  LoginRequest,
  RegisterRequest,
  UserDto
} from '../models/auth.models';
import { Role } from '../models/role.enum';
import { AuthStorageService } from './auth-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authStorage = inject(AuthStorageService);

  /**
   * 1. POST /auth/login  → { accessToken }
   * 2. GET  /api/users    → find user by email to get userId + role
   * 3. Store enriched session
   */
  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<BackendAuthResponse>('/auth/login', payload)
      .pipe(
        switchMap((res) => {
          // Temporarily store the token so the interceptor can use it
          const partialSession: AuthResponse = {
            accessToken: res.accessToken,
            userId: 0,
            email: payload.email,
            role: Role.USER
          };
          this.authStorage.save(partialSession);

          // Fetch users to find our userId and role
          return this.http.get<UserDto[]>(`${environment.apiBaseUrl}/users`).pipe(
            map((users) => {
              const me = users.find(
                (u) => u.email.toLowerCase() === payload.email.toLowerCase()
              );
              const session: AuthResponse = {
                accessToken: res.accessToken,
                userId: me?.id ?? 0,
                email: payload.email,
                role: (me?.role as Role) ?? Role.USER
              };
              return session;
            })
          );
        }),
        tap((session) => this.authStorage.save(session))
      );
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<BackendAuthResponse>('/auth/register', payload)
      .pipe(
        switchMap((res) => {
          const partialSession: AuthResponse = {
            accessToken: res.accessToken,
            userId: 0,
            email: payload.email,
            role: Role.USER
          };
          this.authStorage.save(partialSession);

          return this.http.get<UserDto[]>(`${environment.apiBaseUrl}/users`).pipe(
            map((users) => {
              const me = users.find(
                (u) => u.email.toLowerCase() === payload.email.toLowerCase()
              );
              const session: AuthResponse = {
                accessToken: res.accessToken,
                userId: me?.id ?? 0,
                email: payload.email,
                role: (me?.role as Role) ?? Role.USER
              };
              return session;
            })
          );
        }),
        tap((session) => this.authStorage.save(session))
      );
  }

  logout(): void {
    this.authStorage.clear();
    void this.router.navigateByUrl('/auth/login');
  }
}
