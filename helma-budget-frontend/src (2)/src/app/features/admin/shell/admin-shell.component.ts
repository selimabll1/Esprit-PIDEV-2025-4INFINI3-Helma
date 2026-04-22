import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStorageService } from '../../../core/services/auth-storage.service';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div>
          <div class="brand">HELMA</div>
          <p class="tagline">Admin / Compliance Portal</p>
        </div>

        <nav class="nav">
          <a
            routerLink="/admin"
            routerLinkActive="active"
            [routerLinkActiveOptions]="{ exact: true }"
          >
            Applications
          </a>

          <a
            routerLink="/admin/payments"
            routerLinkActive="active"
          >
            Payments
          </a>
        </nav>

        <div class="account" *ngIf="session">
          <p class="label">Signed in as</p>
          <strong>{{ session.email }}</strong>
          <p class="role">{{ session.role }}</p>
          <button type="button" (click)="logout()">Logout</button>
        </div>
      </aside>

      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .shell {
      min-height: 100vh;
      display: grid;
      grid-template-columns: 260px 1fr;
      background: #f7f7f7;
    }

    .sidebar {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding: 24px;
      background: #062a2b;
      color: white;
    }

    .brand {
      font-size: 2rem;
      font-weight: 800;
      letter-spacing: 0.08em;
    }

    .tagline {
      margin: 6px 0 0;
      color: rgba(255,255,255,0.75);
    }

    .nav {
      display: grid;
      gap: 10px;
      margin: 28px 0;
    }

    .nav a {
      color: white;
      text-decoration: none;
      padding: 12px 14px;
      border-radius: 12px;
      background: rgba(255,255,255,0.06);
    }

    .nav a.active {
      background: #2a9d8f;
      color: #062a2b;
      font-weight: 700;
    }

    .account {
      display: grid;
      gap: 10px;
      padding: 16px;
      border-radius: 14px;
      background: rgba(255,255,255,0.08);
    }

    .label,
    .role {
      margin: 0;
      font-size: 0.85rem;
      color: rgba(255,255,255,0.72);
    }

    .account button {
      height: 42px;
      border: 0;
      border-radius: 10px;
      cursor: pointer;
      background: white;
      color: #062a2b;
      font-weight: 700;
    }

    .content {
      padding: 28px;
    }

    @media (max-width: 960px) {
      .shell {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class AdminShellComponent {
  private readonly authService = inject(AuthService);
  private readonly authStorage = inject(AuthStorageService);

  readonly session = this.authStorage.getUser();

  logout(): void {
    this.authService.logout();
  }
}