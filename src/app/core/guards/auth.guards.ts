import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/role.enum';
import { AuthStorageService } from '../services/auth-storage.service';
import { getHomeRouteByRole } from '../utils/role-home.util';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);

  return authStorage.isAuthenticated()
    ? true
    : router.createUrlTree(['/auth/login']);
};

export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);

  return !authStorage.isAuthenticated()
    ? true
    : router.createUrlTree([getHomeRouteByRole(authStorage.getRole())]);
};

export const roleGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);

  if (!authStorage.isAuthenticated()) {
    return router.createUrlTree(['/auth/login']);
  }

  const allowedRoles = (route.data['roles'] as Role[] | undefined) ?? [];
  const currentRole = authStorage.getRole();

  return currentRole && allowedRoles.includes(currentRole)
    ? true
    : router.createUrlTree([getHomeRouteByRole(currentRole)]);
};