import { Injectable } from '@angular/core';

const TOKEN_KEY = 'helma_token';
const ROLE_KEY = 'helma_role';
const EMAIL_KEY = 'helma_email';
const USER_ID_KEY = 'helma_user_id';
const LEGACY_AUTH_KEY = 'helma.auth';

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
  providedIn: 'root',
})
export class SessionService {
  saveSession(
    tokenOrPayload: string | SessionPayload,
    roleOrRememberMe?: string | boolean,
    email?: string | null,
    rememberMeOrUserId?: boolean | number | null,
    userId?: number | null,
  ): void {
    const normalized = this.normalizeSessionArgs(
      tokenOrPayload,
      roleOrRememberMe,
      email,
      rememberMeOrUserId,
      userId,
    );

    this.clearSession();

    const storage = normalized.rememberMe ? localStorage : sessionStorage;

    storage.setItem(TOKEN_KEY, normalized.token);
    storage.setItem(ROLE_KEY, normalized.role);

    if (normalized.email && normalized.email.trim()) {
      storage.setItem(EMAIL_KEY, normalized.email.trim());
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
    userId?: number | null,
  ): void {
    this.saveSession(
      tokenOrPayload,
      roleOrRememberMe,
      email,
      rememberMeOrUserId,
      userId,
    );
  }

  getToken(): string | null {
    return this.readValue(TOKEN_KEY) ?? this.getLegacyToken();
  }

  token(): string | null {
    return this.getToken();
  }

  getRole(): string | null {
    const role = this.readValue(ROLE_KEY) ?? this.getLegacyRole();
    return this.normalizeRole(role);
  }

  role(): string | null {
    return this.getRole();
  }

  getEmail(): string | null {
    return this.readValue(EMAIL_KEY) ?? this.getLegacyEmail();
  }

  email(): string | null {
    return this.getEmail();
  }

  userId(): number | null {
    const raw = this.readValue(USER_ID_KEY) ?? this.getLegacyUserId();

    if (!raw) {
      return null;
    }

    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getPortalRoute(): string {
    switch (this.getRole()) {
      case 'YOUTH_BENEFICIARY':
        return '/youth';

      case 'INVESTOR':
        return '/investor';

      case 'ADMIN':
      case 'COMPLIANCE':
        return '/admin/dashboard';

      default:
        return '/';
    }
  }

  clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(EMAIL_KEY);
    localStorage.removeItem(USER_ID_KEY);
    localStorage.removeItem(LEGACY_AUTH_KEY);

    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(ROLE_KEY);
    sessionStorage.removeItem(EMAIL_KEY);
    sessionStorage.removeItem(USER_ID_KEY);
    sessionStorage.removeItem(LEGACY_AUTH_KEY);
  }

  private readValue(key: string): string | null {
    return localStorage.getItem(key) ?? sessionStorage.getItem(key);
  }

  private normalizeRole(role: string | null | undefined): string | null {
    if (!role) {
      return null;
    }

    return role.replace(/^ROLE_/, '').trim().toUpperCase();
  }

  private normalizeSessionArgs(
    tokenOrPayload: string | SessionPayload,
    roleOrRememberMe?: string | boolean,
    email?: string | null,
    rememberMeOrUserId?: boolean | number | null,
    userId?: number | null,
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
        role: this.normalizeRole(role) ?? role,
        email: email ?? null,
        userId: resolvedUserId,
        rememberMe,
      };
    }

    const rememberMe =
      typeof roleOrRememberMe === 'boolean' ? roleOrRememberMe : true;

    return {
      token: tokenOrPayload.token,
      role: this.normalizeRole(tokenOrPayload.role) ?? tokenOrPayload.role,
      email: tokenOrPayload.email ?? null,
      userId: tokenOrPayload.userId ?? null,
      rememberMe,
    };
  }

  private getLegacyAuth(): any | null {
    const raw =
      localStorage.getItem(LEGACY_AUTH_KEY) ??
      sessionStorage.getItem(LEGACY_AUTH_KEY);

    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  private getLegacyToken(): string | null {
    const legacy = this.getLegacyAuth();

    return (
      legacy?.token ??
      legacy?.accessToken ??
      legacy?.jwt ??
      legacy?.data?.token ??
      legacy?.data?.accessToken ??
      null
    );
  }

  private getLegacyRole(): string | null {
    const legacy = this.getLegacyAuth();

    return (
      legacy?.role ??
      legacy?.user?.role ??
      legacy?.userRole ??
      legacy?.data?.role ??
      legacy?.data?.user?.role ??
      null
    );
  }

  private getLegacyEmail(): string | null {
    const legacy = this.getLegacyAuth();

    return (
      legacy?.email ??
      legacy?.user?.email ??
      legacy?.data?.email ??
      legacy?.data?.user?.email ??
      null
    );
  }

  private getLegacyUserId(): string | null {
    const legacy = this.getLegacyAuth();

    const value =
      legacy?.userId ??
      legacy?.id ??
      legacy?.user?.id ??
      legacy?.data?.userId ??
      legacy?.data?.id ??
      legacy?.data?.user?.id ??
      null;

    return value === null || value === undefined ? null : String(value);
  }
}