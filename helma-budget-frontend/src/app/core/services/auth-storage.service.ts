import { computed, Injectable, signal } from '@angular/core';
import { AuthResponse } from '../models/auth.models';
import { Role } from '../models/role.enum';

@Injectable({
  providedIn: 'root'
})
export class AuthStorageService {
  private readonly storageKey = 'helma.auth';
  private readonly sessionState = signal<AuthResponse | null>(this.readFromStorage());

  readonly session = this.sessionState.asReadonly();
  readonly isAuthenticated = computed(() => !!this.sessionState()?.accessToken);

  save(session: AuthResponse): void {
    localStorage.setItem(this.storageKey, JSON.stringify(session));
    this.sessionState.set(session);
  }

  clear(): void {
    localStorage.removeItem(this.storageKey);
    this.sessionState.set(null);
  }

  getToken(): string | null {
    return this.sessionState()?.accessToken ?? null;
  }

  getRole(): Role | null {
    return this.sessionState()?.role ?? null;
  }

  getUser(): AuthResponse | null {
    return this.sessionState();
  }

  private readFromStorage(): AuthResponse | null {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) return null;

    try {
      return JSON.parse(raw) as AuthResponse;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }
}