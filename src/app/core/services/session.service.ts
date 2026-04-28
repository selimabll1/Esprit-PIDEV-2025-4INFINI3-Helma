import { Injectable } from '@angular/core';

const TOKEN_KEY = 'helma_token';
const ROLE_KEY = 'helma_role';
const EMAIL_KEY = 'helma_email';
const USER_ID_KEY = 'helma_user_id';

type SessionPayload = {
  userId?: number | null;
  email?: string | null;
  role: string;
  token: string;
};

type NormalizedSession = {
  token: string;
  role: string;
  email: string | null;
  userId: number | null;
  rememberMe: boolean;
};

@Injectable({
  providedIn: 'root'
})
export class SessionService {
  saveSession(
    tokenOrPayload: string | SessionPayload,
    roleOrRememberMe?: string | boolean,
    email?: string | null,
    rememberMeOrUserId?: boolean | number | null,
    userId?: number | null
  ): void {
    const normalized = this.normalizeSessionArgs(
      tokenOrPayload,
      roleOrRememberMe,
      email,
      rememberMeOrUserId,
      userId
    );

    this.clearSession();

    const storage = normalized.rememberMe ? localStorage : sessionStorage;

    storage.setItem(TOKEN_KEY, normalized.token);
    storage.setItem(ROLE_KEY, normalized.role);

    if (normalized.email && normalized.email.trim()) {
      storage.setItem(EMAIL_KEY, normalized.email);
    }

    if (normalized.userId !== null && normalized.userId !== undefined) {
      storage.setItem(USER_ID_KEY, String(normalized.userId));
    }
  }

  setSession(
    tokenOrPayload: string | SessionPayload,
    roleOrRememberMe?: string | boolean,
    email?: string | null,
    rememberMeOrUserId?: boolean | number | null,
    userId?: number | null
  ): void {
    this.saveSession(
      tokenOrPayload,
      roleOrRememberMe,
      email,
      rememberMeOrUserId,
      userId
    );
  }

  token(): string | null {
    return this.readValue(TOKEN_KEY);
  }

  role(): string | null {
    return this.readValue(ROLE_KEY);
  }

  email(): string | null {
    return this.readValue(EMAIL_KEY);
  }

  userId(): number | null {
    const raw = this.readValue(USER_ID_KEY);
    if (!raw) {
      return null;
    }

    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  }

  isAuthenticated(): boolean {
    return !!this.token();
  }

  getPortalRoute(): string {
    switch (this.role()) {
      case 'YOUTH_BENEFICIARY':
        return '/youth';
      case 'INVESTOR':
        return '/investor';
      case 'ADMIN':
      case 'COMPLIANCE':
        return '/admin';
      default:
        return '/';
    }
  }

  clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(EMAIL_KEY);
    localStorage.removeItem(USER_ID_KEY);

    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(ROLE_KEY);
    sessionStorage.removeItem(EMAIL_KEY);
    sessionStorage.removeItem(USER_ID_KEY);
  }

  private readValue(key: string): string | null {
    return localStorage.getItem(key) ?? sessionStorage.getItem(key);
  }

  private normalizeSessionArgs(
    tokenOrPayload: string | SessionPayload,
    roleOrRememberMe?: string | boolean,
    email?: string | null,
    rememberMeOrUserId?: boolean | number | null,
    userId?: number | null
  ): NormalizedSession {
    if (typeof tokenOrPayload === 'string') {
      const role = typeof roleOrRememberMe === 'string' ? roleOrRememberMe : '';
      const rememberMe =
        typeof roleOrRememberMe === 'boolean'
          ? roleOrRememberMe
          : typeof rememberMeOrUserId === 'boolean'
            ? rememberMeOrUserId
            : true;

      const resolvedUserId =
        typeof rememberMeOrUserId === 'number'
          ? rememberMeOrUserId
          : typeof userId === 'number'
            ? userId
            : null;

      return {
        token: tokenOrPayload,
        role,
        email: email ?? null,
        userId: resolvedUserId,
        rememberMe
      };
    }

    const rememberMe =
      typeof roleOrRememberMe === 'boolean' ? roleOrRememberMe : true;

    return {
      token: tokenOrPayload.token,
      role: tokenOrPayload.role,
      email: tokenOrPayload.email ?? null,
      userId: tokenOrPayload.userId ?? null,
      rememberMe
    };
  }
}