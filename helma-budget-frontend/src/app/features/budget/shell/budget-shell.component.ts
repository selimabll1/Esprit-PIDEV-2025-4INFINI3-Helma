import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthStorageService } from '../../../core/services/auth-storage.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-budget-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <a class="sidebar-brand" routerLink="/">
          <img src="logo/helma-logo.png" alt="Helma" class="sidebar-logo" />
          <div>
            <span class="brand-name">Helma</span>
            <span class="brand-sub">Budget Manager</span>
          </div>
        </a>

        <nav class="sidebar-nav">
          <a routerLink="/budget/dashboard" routerLinkActive="active">
            <span class="nav-icon">◎</span> Dashboard
          </a>
          <a routerLink="/budget/transactions" routerLinkActive="active">
            <span class="nav-icon">↕</span> Transactions
          </a>
          <a routerLink="/budget/budgets" routerLinkActive="active">
            <span class="nav-icon">◈</span> Budgets
          </a>
          <a routerLink="/budget/savings" routerLinkActive="active">
            <span class="nav-icon">◔</span> Savings Goals
          </a>
          <a routerLink="/budget/coach" routerLinkActive="active">
            <span class="nav-icon">✦</span> AI Coach
          </a>
          <a routerLink="/budget/reports" routerLinkActive="active">
            <span class="nav-icon">📄</span> Reports
          </a>
        </nav>

        <div class="sidebar-footer">
          <div class="user-pill" *ngIf="user">
            <span class="user-avatar">{{ user.email.charAt(0).toUpperCase() }}</span>
            <span class="user-email">{{ user.email }}</span>
          </div>
          <button class="btn-logout" (click)="logout()">Sign out</button>
        </div>
      </aside>

      <main class="main-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .shell {
      display: grid;
      grid-template-columns: 260px 1fr;
      min-height: 100vh;
    }

    .sidebar {
      background: #fff;
      border-right: 1px solid var(--color-border);
      display: flex;
      flex-direction: column;
      padding: 20px 16px;
      position: sticky;
      top: 0;
      height: 100vh;
      overflow-y: auto;
    }

    .sidebar-brand {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
      padding: 8px 12px;
      margin-bottom: 28px;
    }

    .sidebar-logo {
      width: 44px;
      height: 44px;
      object-fit: contain;
    }

    .brand-name {
      display: block;
      font-size: 20px;
      font-weight: 800;
      color: var(--helma-teal);
      line-height: 1;
    }

    .brand-sub {
      font-size: 11px;
      color: var(--color-text-muted);
      margin-top: 2px;
      display: block;
    }

    .sidebar-nav {
      display: flex;
      flex-direction: column;
      gap: 4px;
      flex: 1;
    }

    .sidebar-nav a {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 14px;
      border-radius: 12px;
      color: var(--color-text);
      text-decoration: none;
      font-weight: 600;
      font-size: 14px;
      transition: 0.15s ease;
    }

    .sidebar-nav a:hover {
      background: var(--helma-teal-soft);
      color: var(--helma-teal);
    }

    .sidebar-nav a.active {
      background: var(--helma-teal);
      color: #fff;
    }

    .nav-icon {
      width: 28px;
      height: 28px;
      display: grid;
      place-items: center;
      border-radius: 8px;
      font-size: 14px;
    }

    .sidebar-footer {
      padding-top: 16px;
      border-top: 1px solid var(--color-border);
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .user-pill {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 10px;
      border-radius: 10px;
      background: var(--color-surface-soft);
    }

    .user-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--helma-teal);
      color: #fff;
      display: grid;
      place-items: center;
      font-weight: 800;
      font-size: 14px;
      flex: 0 0 auto;
    }

    .user-email {
      font-size: 12px;
      color: var(--color-text-muted);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .btn-logout {
      background: none;
      border: 1px solid var(--color-border);
      border-radius: 10px;
      padding: 8px;
      font: inherit;
      font-size: 13px;
      font-weight: 600;
      color: var(--color-text-muted);
      cursor: pointer;
      transition: 0.15s;
    }

    .btn-logout:hover {
      border-color: var(--color-danger);
      color: var(--color-danger);
    }

    .main-content {
      background: var(--color-bg);
      padding: 28px 32px;
      overflow-y: auto;
    }

    @media (max-width: 900px) {
      .shell {
        grid-template-columns: 1fr;
      }
      .sidebar {
        position: fixed;
        left: -280px;
        z-index: 100;
      }
    }
  `]
})
export class BudgetShellComponent {
  private readonly authStorage = inject(AuthStorageService);
  private readonly authService = inject(AuthService);

  user = this.authStorage.getUser();

  logout(): void {
    this.authService.logout();
  }
}
