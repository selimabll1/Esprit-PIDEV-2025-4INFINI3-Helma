import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SessionService } from '../../core/services/session.service';

@Component({
  selector: 'app-top-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './top-navbar.component.html',
  styleUrl: './top-navbar.component.scss'
})
export class TopNavbarComponent {
  private readonly sessionService = inject(SessionService);
  private readonly router = inject(Router);

  readonly portalRoute = computed(() => this.sessionService.getPortalRoute());

  isAuthenticated(): boolean {
    return this.sessionService.isAuthenticated();
  }

  email(): string | null {
    return this.sessionService.email();
  }

  role(): string | null {
    return this.sessionService.role();
  }

  logout(): void {
    this.sessionService.clearSession();
    void this.router.navigateByUrl('/auth/login');
  }

  roleLabel(): string {
    const role = this.role();

    if (!role) {
      return 'Account';
    }

    switch (role) {
      case 'YOUTH_BENEFICIARY':
        return 'Youth Beneficiary';
      case 'INVESTOR':
        return 'Investor';
      case 'ADMIN':
        return 'Admin';
      case 'COMPLIANCE':
        return 'Compliance';
      default:
        return role;
    }
  }
}