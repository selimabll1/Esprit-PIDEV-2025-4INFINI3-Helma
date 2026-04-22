import { Role } from './role.enum';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  isEntrepreneur: boolean;
}

/* Backend returns only accessToken — we enrich it after login */
export interface BackendAuthResponse {
  accessToken: string;
}

/* Full session stored locally (enriched by frontend) */
export interface AuthResponse {
  accessToken: string;
  userId: number;
  email: string;
  role: Role;
}

/* User from GET /api/users */
export interface UserDto {
  id: number;
  fullName: string;
  email: string;
  role: string;
  isEntrepreneur: boolean;
}
