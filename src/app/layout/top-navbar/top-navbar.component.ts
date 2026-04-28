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

  readonly isAuthenticated = this.sessionService.isAuthenticated;
  readonly email = this.sessionService.email;
  readonly role = this.sessionService.role;
  readonly portalRoute = computed(() => this.sessionService.getPortalRoute());

  logout(): void {
    this.sessionService.clearSession();
    void this.router.navigateByUrl('/');
  }

  roleLabel(): string {
    const role = this.role();
    if (!role) return 'Account';

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