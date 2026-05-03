import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SessionService } from '../../../core/services/session.service';

type SidebarItem = {
  label: string;
  route: string;
  exact?: boolean;
  icon:
    | 'dashboard'
    | 'applications'
    | 'payments'
    | 'compliance'
    | 'profile'
    | 'settings'
    | 'support';
};

type SidebarSection = {
  title: string;
  items: SidebarItem[];
};

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar" [class.sidebar--collapsed]="collapsed()">
      <div class="sidebar__frame">
        <div class="leaf leaf--top"></div>
        <div class="leaf leaf--bottom"></div>

        <header class="sidebar__header">
          <a routerLink="/admin/dashboard" class="brand">
            <span class="brand__logo">
              <img src="logo/helma-logo.png" alt="Helma logo" />
            </span>

            <span class="brand__copy" *ngIf="!collapsed()">
              <strong>Helma</strong>
              <small>Back Office</small>
            </span>
          </a>

          <button
            type="button"
            class="collapse-btn"
            (click)="toggleCollapsed()"
            [attr.aria-label]="
              collapsed() ? 'Expand sidebar' : 'Collapse sidebar'
            "
          >
            {{ collapsed() ? '›' : '‹' }}
          </button>
        </header>

        <nav class="sidebar__nav">
          <section class="nav-section" *ngFor="let section of sections">
            <p class="nav-section__title" *ngIf="!collapsed()">
              {{ section.title }}
            </p>

            <div class="nav-section__links">
              <a
                *ngFor="let item of section.items"
                class="nav-link"
                [routerLink]="item.route"
                routerLinkActive="active"
                [routerLinkActiveOptions]="{ exact: item.exact === true }"
                [title]="collapsed() ? item.label : ''"
              >
                <span class="nav-link__icon">
                  <svg *ngIf="item.icon === 'dashboard'" viewBox="0 0 24 24">
                    <path
                      d="M4 11.2 12 4.8l8 6.4V20a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1v-8.8Z"
                    />
                  </svg>

                  <svg *ngIf="item.icon === 'applications'" viewBox="0 0 24 24">
                    <path
                      d="M6 3h12a1 1 0 0 1 1 1v16.3a.55.55 0 0 1-.85.46L12 16.8l-6.15 3.96A.55.55 0 0 1 5 20.3V4a1 1 0 0 1 1-1Zm2.2 4.2v2h7.6v-2H8.2Zm0 4.1v2h7.6v-2H8.2Z"
                    />
                  </svg>

                  <svg *ngIf="item.icon === 'payments'" viewBox="0 0 24 24">
                    <path
                      d="M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2H4V6Zm0 5h16v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-7Zm3 4v2h5v-2H7Z"
                    />
                  </svg>

                  <svg *ngIf="item.icon === 'compliance'" viewBox="0 0 24 24">
                    <path
                      d="M12 2.2 5 5.2v5.9c0 4.55 2.85 8.65 7 10 4.15-1.35 7-5.45 7-10V5.2l-7-3Zm3.75 7.75-4.4 4.55-2.15-2.2 1.35-1.35.8.82 3.05-3.15 1.35 1.33Z"
                    />
                  </svg>

                  <svg *ngIf="item.icon === 'profile'" viewBox="0 0 24 24">
                    <path
                      d="M12 12a4.2 4.2 0 1 0 0-8.4 4.2 4.2 0 0 0 0 8.4Zm0 2c-4.2 0-7.4 2.25-7.4 5.05V20a1 1 0 0 0 1 1h12.8a1 1 0 0 0 1-1v-.95C19.4 16.25 16.2 14 12 14Z"
                    />
                  </svg>

                  <svg *ngIf="item.icon === 'settings'" viewBox="0 0 24 24">
                    <path
                      d="M19.1 12.9c.04-.3.06-.6.06-.9s-.02-.6-.06-.9l1.9-1.48a.5.5 0 0 0 .12-.64l-1.8-3.12a.5.5 0 0 0-.6-.22l-2.24.9a7.1 7.1 0 0 0-1.52-.88l-.34-2.38a.5.5 0 0 0-.5-.42h-3.6a.5.5 0 0 0-.5.42l-.34 2.38c-.53.22-1.04.52-1.52.88l-2.24-.9a.5.5 0 0 0-.6.22l-1.8 3.12a.5.5 0 0 0 .12.64l1.9 1.48c-.04.3-.06.6-.06.9s.02.6.06.9l-1.9 1.48a.5.5 0 0 0-.12.64l1.8 3.12a.5.5 0 0 0 .6.22l2.24-.9c.48.36.99.66 1.52.88l.34 2.38a.5.5 0 0 0 .5.42h3.6a.5.5 0 0 0 .5-.42l.34-2.38c.53-.22 1.04-.52 1.52-.88l2.24.9a.5.5 0 0 0 .6-.22l1.8-3.12a.5.5 0 0 0-.12-.64l-1.9-1.48ZM12 15.4A3.4 3.4 0 1 1 12 8.6a3.4 3.4 0 0 1 0 6.8Z"
                    />
                  </svg>

                  <svg *ngIf="item.icon === 'support'" viewBox="0 0 24 24">
                    <path
                      d="M12 3a8.5 8.5 0 0 0-8.5 8.5V17a3 3 0 0 0 3 3H8v-7H5.5v-1.5a6.5 6.5 0 0 1 13 0V13H16v7h1.5a3 3 0 0 0 3-3v-5.5A8.5 8.5 0 0 0 12 3Zm-1 15h2v2h-2v-2Zm1-11.5a3.35 3.35 0 0 0-3.5 3.35h2A1.39 1.39 0 0 1 12 8.5c.95 0 1.5.48 1.5 1.25 0 .7-.32 1.05-1.18 1.6-1.1.7-1.55 1.35-1.55 2.65v.45h1.9v-.35c0-.65.24-.95 1.15-1.55 1.05-.68 1.75-1.42 1.75-2.85C15.57 7.85 14.1 6.5 12 6.5Z"
                    />
                  </svg>
                </span>

                <span class="nav-link__label" *ngIf="!collapsed()">
                  {{ item.label }}
                </span>
              </a>
            </div>
          </section>
        </nav>

        <footer class="account">
          <div class="account__user" *ngIf="!collapsed()">
            <span class="account__avatar">{{ userInitial }}</span>

            <span class="account__copy">
              <strong>{{ roleLabel }}</strong>
              <small>{{ emailLabel }}</small>
            </span>
          </div>

          <div class="account__actions">
            <a
              routerLink="/settings"
              class="account-btn"
              title="Settings"
              aria-label="Settings"
            >
              <svg viewBox="0 0 24 24">
                <path
                  d="M19.1 12.9c.04-.3.06-.6.06-.9s-.02-.6-.06-.9l1.9-1.48a.5.5 0 0 0 .12-.64l-1.8-3.12a.5.5 0 0 0-.6-.22l-2.24.9a7.1 7.1 0 0 0-1.52-.88l-.34-2.38a.5.5 0 0 0-.5-.42h-3.6a.5.5 0 0 0-.5.42l-.34 2.38c-.53.22-1.04.52-1.52.88l-2.24-.9a.5.5 0 0 0-.6.22l-1.8 3.12a.5.5 0 0 0 .12.64l1.9 1.48c-.04.3-.06.6-.06.9s.02.6.06.9l-1.9 1.48a.5.5 0 0 0-.12.64l1.8 3.12a.5.5 0 0 0 .6.22l2.24-.9c.48.36.99.66 1.52.88l.34 2.38a.5.5 0 0 0 .5.42h3.6a.5.5 0 0 0 .5-.42l.34-2.38c.53-.22 1.04-.52 1.52-.88l2.24.9a.5.5 0 0 0 .6-.22l1.8-3.12a.5.5 0 0 0-.12-.64l-1.9-1.48ZM12 15.4A3.4 3.4 0 1 1 12 8.6a3.4 3.4 0 0 1 0 6.8Z"
                />
              </svg>
            </a>

            <button
              type="button"
              class="account-btn account-btn--danger"
              (click)="logout()"
              title="Logout"
              aria-label="Logout"
            >
              <svg viewBox="0 0 24 24">
                <path
                  d="M10 4h9a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-9v-2h8V6h-8V4Zm1.5 4.5L15 12l-3.5 3.5-1.4-1.4 1.1-1.1H4v-2h7.2l-1.1-1.1 1.4-1.4Z"
                />
              </svg>
            </button>
          </div>
        </footer>
      </div>
    </aside>
  `,
  styles: [
    `
      :host {
        display: block;
        min-height: 100vh;
      }

      .sidebar {
        position: sticky;
        top: 0;
        width: 292px;
        height: 100vh;
        min-height: 0;
        overflow: hidden;
        transition: width 0.22s ease;
      }

      .sidebar--collapsed {
        width: 92px;
      }

      .sidebar__frame {
        position: relative;
        height: 100%;
        min-height: 0;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        gap: 14px;
        padding: 14px;
        background:
          radial-gradient(
            circle at top left,
            rgba(243, 223, 152, 0.24),
            transparent 34%
          ),
          radial-gradient(
            circle at right 24%,
            rgba(42, 157, 143, 0.16),
            transparent 32%
          ),
          linear-gradient(
            180deg,
            rgba(246, 253, 248, 0.98),
            rgba(235, 249, 240, 0.98)
          );
        border-right: 1px solid rgba(6, 42, 43, 0.1);
        box-shadow: 12px 0 34px rgba(6, 42, 43, 0.08);
        box-sizing: border-box;
      }

      .leaf {
        position: absolute;
        width: 180px;
        height: 180px;
        pointer-events: none;
        opacity: 0.22;
        background:
          radial-gradient(
            ellipse at center,
            rgba(184, 130, 38, 0.36) 0 24%,
            transparent 25%
          ),
          radial-gradient(
            ellipse at center,
            rgba(218, 165, 62, 0.3) 0 24%,
            transparent 25%
          ),
          radial-gradient(
            ellipse at center,
            rgba(157, 111, 28, 0.2) 0 24%,
            transparent 25%
          );
        background-size:
          42px 72px,
          54px 86px,
          38px 66px;
        background-position:
          0 0,
          38px 42px,
          84px 10px;
        background-repeat: repeat;
        mask-image: radial-gradient(circle, #000 0 46%, transparent 70%);
      }

      .leaf--top {
        top: -80px;
        right: -90px;
        transform: rotate(-18deg);
      }

      .leaf--bottom {
        bottom: -96px;
        left: -96px;
        transform: rotate(24deg);
        opacity: 0.16;
      }

      .sidebar__header,
      .sidebar__nav,
      .account {
        position: relative;
        z-index: 1;
      }

      .sidebar__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 10px;
        padding-bottom: 12px;
      }

      .brand {
        min-width: 0;
        display: flex;
        align-items: center;
        gap: 11px;
        color: #062a2b;
        text-decoration: none;
      }

      .brand__logo {
        width: 50px;
        height: 50px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        border-radius: 18px;
        background: linear-gradient(145deg, #fff8df, #eaf8ef);
        border: 1px solid rgba(212, 166, 42, 0.24);
        box-shadow: 0 12px 28px rgba(6, 42, 43, 0.08);
      }

      .brand__logo img {
        width: 38px;
        height: 38px;
        object-fit: contain;
      }

      .brand__copy {
        display: grid;
        gap: 3px;
        min-width: 0;
      }

      .brand__copy strong {
        color: #062a2b;
        font-size: 1.25rem;
        font-weight: 950;
        letter-spacing: -0.035em;
        line-height: 1;
      }

      .brand__copy small {
        color: #5f746f;
        font-size: 0.78rem;
        font-weight: 850;
      }

      .collapse-btn {
        width: 38px;
        height: 38px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        border: 0;
        border-radius: 14px;
        background: rgba(6, 42, 43, 0.08);
        color: #0b3b3c;
        font-size: 1.45rem;
        font-weight: 950;
        cursor: pointer;
        transition: 0.18s ease;
      }

      .collapse-btn:hover {
        background: rgba(212, 166, 42, 0.18);
        color: #8b6d20;
      }

      .sidebar__nav {
        flex: 1 1 auto;
        min-height: 0;
        overflow-y: auto;
        overflow-x: hidden;
        display: grid;
        align-content: start;
        gap: 14px;
        padding-right: 4px;
        scrollbar-width: thin;
      }
      .sidebar__account {
        flex: 0 0 auto;
      }

      .sidebar__nav::-webkit-scrollbar {
        width: 6px;
      }

      .sidebar__nav::-webkit-scrollbar-thumb {
        background: rgba(6, 42, 43, 0.12);
        border-radius: 999px;
      }

      .nav-section {
        display: grid;
        gap: 8px;
      }

      .nav-section__title {
        margin: 0;
        padding: 0 12px;
        color: #8a9b96;
        font-size: 0.68rem;
        font-weight: 950;
        letter-spacing: 0.12em;
        text-transform: uppercase;
      }

      .nav-section__links {
        display: grid;
        gap: 6px;
      }

      .nav-link {
        min-height: 46px;
        display: flex;
        align-items: center;
        gap: 11px;
        padding: 0 12px;
        border-radius: 16px;
        color: #4e625e;
        text-decoration: none;
        font-size: 0.92rem;
        font-weight: 850;
        transition: 0.18s ease;
      }

      .nav-link:hover {
        background: rgba(255, 255, 255, 0.82);
        color: #062a2b;
        transform: translateX(2px);
      }

      .nav-link.active {
        background: linear-gradient(
          135deg,
          rgba(6, 42, 43, 0.96),
          rgba(11, 66, 64, 0.94)
        );
        color: #f3df98;
        box-shadow: 0 14px 26px rgba(6, 42, 43, 0.16);
      }

      .nav-link__icon {
        width: 24px;
        height: 24px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        color: currentColor;
      }

      .nav-link__icon svg {
        width: 20px;
        height: 20px;
        fill: currentColor;
      }

      .nav-link__label {
        min-width: 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .account {
        display: grid;
        gap: 10px;
        padding-top: 12px;
        border-top: 1px solid rgba(6, 42, 43, 0.08);
      }

      .account__user {
        min-width: 0;
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px;
        border-radius: 18px;
        background: rgba(255, 255, 255, 0.72);
        border: 1px solid rgba(6, 42, 43, 0.06);
      }

      .account__avatar {
        width: 42px;
        height: 42px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        border-radius: 15px;
        background: linear-gradient(135deg, #062a2b, #0b4440);
        color: #f3df98;
        font-weight: 950;
        letter-spacing: 0.04em;
      }

      .account__copy {
        display: grid;
        gap: 2px;
        min-width: 0;
      }

      .account__copy strong {
        color: #062a2b;
        font-size: 0.88rem;
        line-height: 1.15;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .account__copy small {
        color: #6d7d78;
        font-size: 0.74rem;
        font-weight: 750;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .account__actions {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
      }

      .account-btn {
        min-height: 40px;
        display: grid;
        place-items: center;
        border: 0;
        border-radius: 15px;
        background: rgba(6, 42, 43, 0.08);
        color: #0b3b3c;
        cursor: pointer;
        text-decoration: none;
        transition: 0.18s ease;
      }

      .account-btn svg {
        width: 20px;
        height: 20px;
        fill: currentColor;
      }

      .account-btn:hover {
        background: rgba(212, 166, 42, 0.18);
        color: #8b6d20;
      }

      .account-btn--danger:hover {
        background: rgba(164, 58, 58, 0.12);
        color: #a43a3a;
      }

      .sidebar--collapsed .sidebar__frame {
        align-items: center;
        padding-inline: 12px;
      }

      .sidebar--collapsed .sidebar__header {
        flex-direction: column;
        width: 100%;
      }

      .sidebar--collapsed .brand {
        justify-content: center;
      }

      .sidebar--collapsed .nav-section,
      .sidebar--collapsed .nav-section__links,
      .sidebar--collapsed .sidebar__nav {
        width: 100%;
      }

      .sidebar--collapsed .nav-link {
        justify-content: center;
        padding: 0;
      }

      .sidebar--collapsed .nav-link:hover {
        transform: none;
      }

      .sidebar--collapsed .account__actions {
        grid-template-columns: 1fr;
      }

      @media (max-width: 960px) {
        :host {
          min-height: auto;
        }

        .sidebar,
        .sidebar--collapsed {
          width: 100%;
          height: auto;
          min-height: auto;
        }

        .sidebar__frame {
          height: auto;
          min-height: auto;
          border-right: 0;
          border-bottom: 1px solid rgba(6, 42, 43, 0.1);
          box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
          padding: 12px;
        }

        .sidebar__header {
          padding-bottom: 10px;
        }

        .sidebar__nav {
          display: flex;
          gap: 10px;
          overflow-x: auto;
          padding-bottom: 4px;
        }

        .nav-section {
          min-width: max-content;
        }

        .nav-section__links {
          display: flex;
          gap: 8px;
        }

        .nav-link {
          min-width: max-content;
          padding: 0 12px;
        }

        .account {
          display: none;
        }

        .collapse-btn {
          display: none;
        }
      }
    `,
  ],
})
export class AdminSidebarComponent {
  private readonly router = inject(Router);
  private readonly sessionService = inject(SessionService);

  readonly collapsed = signal(false);

  readonly role = this.sessionService.role();
  readonly emailLabel = this.getStoredEmail() || 'admin@helma.tn';
  readonly roleLabel =
    this.role === 'COMPLIANCE' ? 'Compliance Officer' : 'Administrator';
  readonly userInitial = this.roleLabel.charAt(0).toUpperCase();

  readonly sections: SidebarSection[] = this.buildSections();

  toggleCollapsed(): void {
    this.collapsed.update((value) => !value);
  }

  logout(): void {
    this.sessionService.clearSession();
    this.router.navigateByUrl('/auth/login');
  }

  private buildSections(): SidebarSection[] {
    const isAdmin = this.role === 'ADMIN';

    const baseSections: SidebarSection[] = [
      {
        title: 'Overview',
        items: [
          {
            label: 'Dashboard',
            route: '/admin/dashboard',
            exact: true,
            icon: 'dashboard',
          },
        ],
      },
      {
        title: 'Users & Compliance',
        items: [
          {
            label: 'Users',
            route: '/admin/users',
            exact: true,
            icon: 'profile',
          },
          {
            label: 'KYC Reviews',
            route: '/admin/kyc',
            exact: true,
            icon: 'compliance',
          },
          {
            label: 'Compliance Team',
            route: '/admin/compliance',
            exact: true,
            icon: 'compliance',
          },
        ],
      },
      {
        title: 'Youth Finance',
        items: [
          {
            label: 'Supervison finance savings ',
            route: '/admin/dashboard/finance/savings',
            exact: true,
            icon: 'payments',
          },
          {
            label: 'Savings Goals',
            route: '/admin/goals',
            exact: true,
            icon: 'payments',
          },
          {
            label: 'Vouchers',
            route: '/admin/voucher',
            exact: true,
            icon: 'payments',
          },
          {
            label: 'Budgets',
            route: '/admin/budgets',
            exact: true,
            icon: 'dashboard',
          },
          {
            label: 'Guidance Content',
            route: '/admin/guidance',
            exact: true,
            icon: 'support',
          },
        ],
      },
      {
        title: 'Financing',
        items: [
          {
            label: 'Micro-loans',
            route: '/admin/micro-loans',
            exact: true,
            icon: 'payments',
          },
          {
            label: 'Micro-leasing',
            route: '/admin/micro-leasing',
            exact: true,
            icon: 'applications',
          },
          {
            label: 'Installments',
            route: '/admin/installments',
            exact: true,
            icon: 'payments',
          },
        ],
      },
      {
        title: 'Crowdfunding',
        items: [
          {
            label: 'Applications',
            route: '/admin/applications',
            icon: 'applications',
          },
          {
            label: 'Campaigns',
            route: '/admin/campaigns',
            exact: true,
            icon: 'applications',
          },
          {
            label: 'Investments',
            route: '/admin/investments',
            exact: true,
            icon: 'payments',
          },
          {
            label: 'Payments',
            route: '/admin/payments',
            exact: true,
            icon: 'payments',
          },
        ],
      },

      {
        title: 'Platform',
        items: [
          {
            label: 'Audit Trail',
            route: '/admin/audit',
            exact: true,
            icon: 'compliance',
          },
          {
            label: 'Support Tickets',
            route: '/admin/support',
            exact: true,
            icon: 'support',
          },
          {
            label: 'Settings',
            route: '/settings',
            exact: true,
            icon: 'settings',
          },
        ],
      },
    ];

    if (isAdmin) {
      baseSections[1].items.push({
        label: 'Add Compliance',
        route: '/admin/compliance/new',
        exact: true,
        icon: 'compliance',
      });
    }

    return baseSections.filter((section) => section.items.length > 0);
  }

  private getStoredEmail(): string | null {
    const directKeys = ['helma_email', 'email', 'userEmail', 'auth_email'];

    for (const key of directKeys) {
      const value = localStorage.getItem(key);

      if (value) {
        return value;
      }
    }

    const objectKeys = ['helma_user', 'user', 'auth_user', 'session'];

    for (const key of objectKeys) {
      const raw = localStorage.getItem(key);

      if (!raw) {
        continue;
      }

      try {
        const parsed = JSON.parse(raw);
        return parsed?.email || parsed?.user?.email || null;
      } catch {
        continue;
      }
    }

    return null;
  }
}
