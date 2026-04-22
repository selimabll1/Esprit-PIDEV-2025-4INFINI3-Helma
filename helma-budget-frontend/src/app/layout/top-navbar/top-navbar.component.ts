import { CommonModule } from '@angular/common';
import { Component, inject, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthStorageService } from '../../core/services/auth-storage.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-top-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './top-navbar.component.html',
  styleUrl: './top-navbar.component.scss'
})
export class TopNavbarComponent {
  private readonly authStorage = inject(AuthStorageService);
  private readonly authService = inject(AuthService);

  isLoggedIn = this.authStorage.isAuthenticated;
  userEmail = computed(() => this.authStorage.getUser()?.email ?? '');

  logout(): void {
    this.authService.logout();
  }
}
