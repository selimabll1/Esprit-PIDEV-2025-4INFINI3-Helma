export type UserRole = 'YOUTH_BENEFICIARY' | 'INVESTOR' | 'ADMIN' | 'COMPLIANCE';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  userId: number;
  profileId?: number | null;
  email: string;
  role: UserRole;
  firstName?: string | null;
  lastName?: string | null;
}
