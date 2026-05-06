import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  Router,
  RouterStateSnapshot
} from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate, CanActivateChild {
  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    return this.checkAccess(route, state);
  }

  canActivateChild(childRoute: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    return this.checkAccess(childRoute, state);
  }

  private checkAccess(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return false;
    }

    const roles = this.resolveRoles(route);
    if (roles && !this.authService.hasAnyRole(roles)) {
      this.router.navigate(['/forbidden']);
      return false;
    }

    return true;
  }

  private resolveRoles(route: ActivatedRouteSnapshot): string[] | undefined {
    const childRoles = route.data['roles'] as string[] | undefined;
    const parentRoles = route.parent?.data['roles'] as string[] | undefined;
    return childRoles ?? parentRoles;
  }
}

