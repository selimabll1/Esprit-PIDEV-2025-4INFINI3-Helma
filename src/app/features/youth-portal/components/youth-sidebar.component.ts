import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SessionService } from '../../../core/services/session.service';

type SidebarItem = {
  label: string;
  route: string;
  exact?: boolean;
};

type SidebarSection = {
  title: string;
  items: SidebarItem[];
};

@Component({
  selector: 'app-youth-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <div class="sidebar__frame">
        <a class="sidebar__brand" routerLink="/">
          <img src="logo/helma-logo.png" alt="Helma logo" class="sidebar__brand-logo" />

          <div class="sidebar__brand-copy">
            <span class="sidebar__brand-name">Helma</span>
            <span class="sidebar__brand-role">Youth Portal</span>
          </div>
        </a>

        <nav class="sidebar__nav" aria-label="Youth portal navigation">
          <section class="sidebar__section" *ngFor="let section of sections">
            <p class="sidebar__section-title">{{ section.title }}</p>

            <div class="sidebar__links">
              <a
                *ngFor="let item of section.items"
                class="sidebar__link"
                [routerLink]="item.route"
                routerLinkActive="active"
                [routerLinkActiveOptions]="{ exact: item.exact ?? false }"
              >
                {{ item.label }}
              </a>
            </div>
          </section>
        </nav>

        <div class="sidebar__account">
          <div class="sidebar__account-main">
            <div class="sidebar__avatar">{{ initials() }}</div>

            <div class="sidebar__account-copy">
              <strong>{{ userName() }}</strong>
              <small>{{ email() || 'Signed in user' }}</small>
            </div>
          </div>

          <div class="sidebar__account-actions">
            <a
              class="sidebar__icon-btn"
              routerLink="/settings"
              aria-label="Settings"
              title="Settings"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M19.14 12.94c.04-.31.06-.62.06-.94s-.02-.63-.06-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.6-.22l-2.39.96a7.2 7.2 0 0 0-1.63-.94l-.36-2.54a.5.5 0 0 0-.49-.42h-3.84a.5.5 0 0 0-.49.42l-.36 2.54c-.57.23-1.12.55-1.63.94l-2.39-.96a.5.5 0 0 0-.6.22L2.7 8.84a.5.5 0 0 0 .12.64l2.03 1.58c-.04.31-.06.62-.06.94s.02.63.06.94L2.82 14.52a.5.5 0 0 0-.12.64l1.92 3.32a.5.5 0 0 0 .6.22l2.39-.96c.51.39 1.06.71 1.63.94l.36 2.54a.5.5 0 0 0 .49.42h3.84a.5.5 0 0 0 .49-.42l.36-2.54c.57-.23 1.12-.55 1.63-.94l2.39.96a.5.5 0 0 0 .6-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58ZM12 15.5A3.5 3.5 0 1 1 12 8.5a3.5 3.5 0 0 1 0 7Z"
                />
              </svg>
            </a>

            <button
              type="button"
              class="sidebar__icon-btn sidebar__icon-btn--danger"
              (click)="logout()"
              aria-label="Logout"
              title="Logout"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M10 17v-2h5V9h-5V7h5a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2h-5Zm-1.41-1.59L5 11.83l3.59-3.58L10 9.66 8.83 10.83H14v2H8.83L10 14l-1.41 1.41ZM19 19V5h2v14h-2Z"
                />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </aside>
  `,
  styles: [`
    :host {
      display: block;
      height: 100%;
    }

    .sidebar {
      height: 100vh;
      min-height: 100vh;
    }

    .sidebar__frame {
      height: 100vh;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      gap: 14px;
      padding: 10px 14px 14px;
      background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(250, 248, 244, 0.98));
      border-right: 1px solid rgba(229, 231, 235, 0.9);
      box-shadow: 8px 0 24px rgba(15, 23, 42, 0.03);
      box-sizing: border-box;
    }

    .sidebar__brand {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 4px 6px 10px;
      text-decoration: none;
      border-bottom: 1px solid rgba(229, 231, 235, 0.78);
    }

    .sidebar__brand-logo {
      width: 46px;
      height: 46px;
      object-fit: contain;
      flex-shrink: 0;
    }

    .sidebar__brand-copy {
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .sidebar__brand-name {
      font-size: 1.25rem;
      font-weight: 800;
      line-height: 1;
      letter-spacing: -0.03em;
      color: var(--helma-teal);
    }

    .sidebar__brand-role {
      margin-top: 4px;
      font-size: 11px;
      font-weight: 800;
      color: var(--color-text-muted);
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .sidebar__nav {
      display: grid;
      gap: 12px;
      align-content: start;
      flex: 1;
      min-height: 0;
    }

    .sidebar__section {
      display: grid;
      gap: 7px;
    }

    .sidebar__section-title {
      margin: 0;
      padding: 0 6px;
      font-size: 10px;
      font-weight: 800;
      letter-spacing: 0.09em;
      text-transform: uppercase;
      color: var(--color-text-muted);
    }

    .sidebar__links {
      display: grid;
      gap: 7px;
    }

    .sidebar__link {
      min-height: 42px;
      display: flex;
      align-items: center;
      padding: 0 14px;
      border-radius: 15px;
      border: 1px solid transparent;
      background: rgba(255, 255, 255, 0.88);
      color: var(--color-text);
      text-decoration: none;
      font-weight: 700;
      transition:
        transform 0.18s ease,
        background 0.18s ease,
        border-color 0.18s ease,
        box-shadow 0.18s ease;
    }

    .sidebar__link:hover {
      background: #ffffff;
      border-color: rgba(15, 107, 104, 0.12);
      box-shadow: var(--shadow-sm);
      transform: translateY(-1px);
    }

    .sidebar__link.active {
      background: linear-gradient(90deg, rgba(217, 239, 238, 0.94), rgba(255, 255, 255, 0.98));
      border-color: rgba(15, 107, 104, 0.16);
      color: var(--helma-teal);
      box-shadow: var(--shadow-sm);
    }

    .sidebar__account {
      margin-top: auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      padding: 12px;
      border-radius: 22px;
      border: 1px solid rgba(229, 231, 235, 0.95);
      background: linear-gradient(180deg, #ffffff, rgba(247, 248, 250, 0.98));
      box-shadow: var(--shadow-sm);
    }

    .sidebar__account-main {
      min-width: 0;
      display: flex;
      align-items: center;
      gap: 10px;
      flex: 1;
    }

    .sidebar__avatar {
      width: 52px;
      height: 52px;
      display: grid;
      place-items: center;
      border-radius: 18px;
      flex-shrink: 0;
      background: linear-gradient(180deg, var(--helma-teal), var(--helma-teal-dark));
      color: #ffffff;
      font-size: 14px;
      font-weight: 800;
      letter-spacing: 0.04em;
    }

    .sidebar__account-copy {
      min-width: 0;
      display: grid;
      gap: 3px;
    }

    .sidebar__account-copy strong {
      font-size: 14px;
      font-weight: 800;
      color: var(--color-text);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .sidebar__account-copy small {
      font-size: 12px;
      color: var(--color-text-muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .sidebar__account-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
    }

    .sidebar__icon-btn {
      width: 40px;
      height: 40px;
      display: inline-grid;
      place-items: center;
      padding: 0;
      border: 1px solid rgba(229, 231, 235, 0.95);
      border-radius: 15px;
      background: #ffffff;
      color: var(--helma-teal);
      cursor: pointer;
      text-decoration: none;
      transition:
        transform 0.18s ease,
        background 0.18s ease,
        border-color 0.18s ease,
        box-shadow 0.18s ease;
    }

    .sidebar__icon-btn svg {
      width: 18px;
      height: 18px;
      fill: currentColor;
    }

    .sidebar__icon-btn:hover {
      transform: translateY(-1px);
      background: var(--helma-teal-soft);
      border-color: rgba(15, 107, 104, 0.16);
      box-shadow: var(--shadow-sm);
    }

    .sidebar__icon-btn--danger {
      color: #cb5b5b;
      background: #fff8f8;
    }

    .sidebar__icon-btn--danger:hover {
      background: #ffecec;
      border-color: rgba(203, 91, 91, 0.15);
    }

    @media (max-width: 960px) {
      .sidebar,
      .sidebar__frame {
        height: auto;
        min-height: auto;
      }

      .sidebar__frame {
        border-right: 0;
        border-bottom: 1px solid rgba(229, 231, 235, 0.9);
        box-shadow: none;
        padding: 14px;
      }

      .sidebar__account {
        margin-top: 0;
      }
    }
  `]
})
export class YouthSidebarComponent {
  private readonly sessionService = inject(SessionService);
  private readonly router = inject(Router);

  readonly sections: SidebarSection[] = [
    {
      title: 'Overview',
      items: [
        { label: 'Dashboard', route: '/youth', exact: true }
      ]
    },
    {
      title: 'Financial Tools',
      items: [
        { label: 'Savings', route: '/youth/savings', exact: true },
        { label: 'Budgeting', route: '/youth/budgeting', exact: true },
        { label: 'Guidance', route: '/youth/guidance', exact: true }
      ]
    },
    {
      title: 'Financing',
      items: [
        { label: 'Micro-Loans', route: '/youth/micro-loans', exact: true },
        { label: 'Micro-Leasing', route: '/youth/micro-leasing', exact: true }
      ]
    },
    {
      title: 'Fundraising',
      items: [
        { label: 'Crowdfunding Hub', route: '/youth/crowdfunding', exact: true },
        { label: 'My Applications', route: '/youth/applications', exact: true },
        { label: 'New Raise', route: '/youth/applications/new', exact: true }
      ]
    },
    {
      title: 'Account',
      items: [
        { label: 'Profile', route: '/profile', exact: true },
        { label: 'Support', route: '/youth/support', exact: true }
      ]
    }
  ];

  email(): string | null {
    return this.sessionService.email();
  }

  userName(): string {
    const email = this.email();

    if (!email) {
      return 'Helma User';
    }

    const local = email.split('@')[0]?.trim() ?? '';
    const cleaned = local.replace(/[._-]+/g, ' ').trim();

    if (!cleaned) {
      return 'Helma User';
    }

    return cleaned
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
      .join(' ');
  }

  initials(): string {
    const parts = this.userName().split(' ').filter(Boolean).slice(0, 2);

    if (!parts.length) {
      return 'HM';
    }

    return parts.map((part) => part[0]!.toUpperCase()).join('');
  }

  logout(): void {
    this.sessionService.clearSession();
    this.router.navigateByUrl('/auth/login');
  }
}