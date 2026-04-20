import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';
import { AuthStorageService } from './auth-storage.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authStorage = inject(AuthStorageService);

  login(payload: LoginRequest) {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/auth/login`, payload)
      .pipe(tap((session) => this.authStorage.save(session)));
  }

  register(payload: RegisterRequest) {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/auth/register`, payload)
      .pipe(tap((session) => this.authStorage.save(session)));
  }

  logout(): void {
    this.authStorage.clear();
    void this.router.navigateByUrl('/auth/login');
  }
}