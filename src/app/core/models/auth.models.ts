import { Role } from './role.enum';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  userId: number;
  email: string;
  role: Role;
}