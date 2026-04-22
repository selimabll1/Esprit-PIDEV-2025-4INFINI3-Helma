import { Role } from '../models/role.enum';

export function getHomeRouteByRole(role: Role | null | undefined): string {
  switch (role) {
    case Role.USER:
      return '/budget';
    case Role.ADMIN:
      return '/admin';
    case Role.FOUNDER:
      return '/founder';
    case Role.INVESTOR:
      return '/investor';
    default:
      return '/budget';
  }
}
