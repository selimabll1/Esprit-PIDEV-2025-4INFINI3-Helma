import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from '../services/session.service';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const sessionService = inject(SessionService);

  if (!sessionService.isAuthenticated()) {
    return router.createUrlTree(['/auth/login']);
  }

  return true;
};

export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const sessionService = inject(SessionService);

  if (sessionService.isAuthenticated()) {
    return router.createUrlTree([sessionService.getPortalRoute()]);
  }

  return true;
};

export const roleGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const sessionService = inject(SessionService);

  const allowedRoles = route.data?.['roles'] as string[] | undefined;
  const currentRole = sessionService.role();

  if (!currentRole) {
    return router.createUrlTree(['/auth/login']);
  }

  if (!allowedRoles?.includes(currentRole)) {
    return router.createUrlTree([sessionService.getPortalRoute()]);
  }

  return true;
};