import { Component } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './core/services/auth.service';
import { AuthResponse } from './core/models';

interface NavItem {
  label: string;
  path: string;
  roles?: string[];
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  readonly navItems: NavItem[] = [
    { label: 'Youth portal', path: '/youth', roles: ['YOUTH_BENEFICIARY'] },
    { label: 'Admin portal', path: '/admin/dashboard', roles: ['ADMIN', 'COMPLIANCE'] },
    { label: 'Loans', path: '/loans', roles: ['YOUTH_BENEFICIARY', 'ADMIN', 'COMPLIANCE'] },
    { label: 'Early Warnings', path: '/early-warnings', roles: ['ADMIN', 'COMPLIANCE'] },
    { label: 'New Request', path: '/loans/create', roles: ['YOUTH_BENEFICIARY'] },
    { label: 'Simulation', path: '/simulate' }
  ];

  currentYear = new Date().getFullYear();
  private currentUrl = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.currentUrl = event.urlAfterRedirects;
      });

    this.currentUrl = this.router.url;
  }

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  getUser(): AuthResponse | null {
    return this.authService.getUser();
  }

  canShowItem(item: NavItem): boolean {
    if (!item.roles?.length) {
      return true;
    }
    return this.authService.hasAnyRole(item.roles);
  }

  isActive(path: string): boolean {
    return this.currentUrl === path || this.currentUrl.startsWith(`${path}/`);
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/auth/login']);
  }

  portalRoute(): string {
    return this.authService.getPortalRoute();
  }

  roleLabel(): string {
    const role = this.authService.getUser()?.role;
    switch (role) {
      case 'YOUTH_BENEFICIARY': return 'Youth Beneficiary';
      case 'INVESTOR':          return 'Investor';
      case 'ADMIN':             return 'Admin';
      case 'COMPLIANCE':        return 'Compliance';
      default:                  return 'Account';
    }
  }
}
