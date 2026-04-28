export type UserRole =
  | 'YOUTH_BENEFICIARY'
  | 'INVESTOR'
  | 'ADMIN'
  | 'COMPLIANCE';

export interface UserSession {
  userId: number;
  email: string;
  role: UserRole;
  token: string;
}