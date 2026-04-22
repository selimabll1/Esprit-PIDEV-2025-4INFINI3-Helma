import { Role } from '../models/role.enum';

export function getHomeRouteByRole(role: Role | null | undefined): string {
  switch (role) {
    case Role.FOUNDER:
      return '/founder';
    case Role.INVESTOR:
      return '/investor';
    case Role.ADMIN:
    case Role.COMPLIANCE:
      return '/admin';
    default:
      return '/auth/login';
  }
}