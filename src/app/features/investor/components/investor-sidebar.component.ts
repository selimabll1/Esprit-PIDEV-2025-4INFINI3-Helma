import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SessionService } from '../../../core/services/session.service';

type SidebarItem = {
  label: string;
  route: string;
  exact?: boolean;
  icon: string;
};

type SidebarSection = {
  title: string;
  items: SidebarItem[];
};

@Component({
  selector: 'app-investor-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar" [class.sidebar--collapsed]="collapsed">
      <div class="sidebar__frame">
        <div class="blue-glow blue-glow--top"></div>
        <div class="blue-glow blue-glow--middle"></div>
        <div class="gold-leaf gold-leaf--top"></div>
        <div class="gold-leaf gold-leaf--bottom"></div>

        <div class="sidebar__top">
          <a class="sidebar__brand" routerLink="/investor">
            <span class="sidebar__logo-shell">
              <img
                src="logo/helma-logo.png"
                alt="Helma logo"
                class="sidebar__brand-logo"
              />
            </span>

            <div class="sidebar__brand-copy">
              <span class="sidebar__brand-name">Helma</span>
              <span class="sidebar__brand-role">Investor Portal</span>
            </div>
          </a>

          <button
            type="button"
            class="sidebar__collapse-btn"
            (click)="toggleCollapse()"
            [attr.aria-label]="collapsed ? 'Expand sidebar' : 'Collapse sidebar'"
            [title]="collapsed ? 'Expand sidebar' : 'Collapse sidebar'"
          >
            <span>{{ collapsed ? '›' : '‹' }}</span>
          </button>
        </div>

        <nav class="sidebar__nav" aria-label="Investor portal navigation">
          <section class="sidebar__section" *ngFor="let section of sections">
            <p class="sidebar__section-title">{{ section.title }}</p>

            <div class="sidebar__links">
              <a
                *ngFor="let item of section.items"
                class="sidebar__link"
                [routerLink]="item.route"
                routerLinkActive="active"
                [routerLinkActiveOptions]="{ exact: item.exact ?? false }"
                [title]="collapsed ? item.label : null"
              >
                <span class="sidebar__link-icon">{{ item.icon }}</span>
                <span class="sidebar__link-label">{{ item.label }}</span>
              </a>
            </div>
          </section>
        </nav>

        <div class="sidebar__account">
          <div class="sidebar__account-main">
            <div class="sidebar__avatar">{{ userInitial }}</div>

            <div class="sidebar__account-copy">
              <strong>{{ userNameLabel }}</strong>
              <small>{{ emailLabel }}</small>
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
                  d="M19.14 12.94c.04-.31.06-.62.06-.94s-.02-.63-.06-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.6-.22l-2.39.96a7.16 7.16 0 0 0-1.63-.94L14.5 2.8a.5.5 0 0 0-.5-.42h-4a.5.5 0 0 0-.5.42l-.36 2.52c-.58.23-1.13.55-1.63.94l-2.39-.96a.5.5 0 0 0-.6.22L2.6 8.84a.5.5 0 0 0 .12.64l2.03 1.58c-.04.31-.06.62-.06.94s.02.63.06.94l-2.03 1.58a.5.5 0 0 0-.12.64l1.92 3.32a.5.5 0 0 0 .6.22l2.39-.96c.5.39 1.05.71 1.63.94l.36 2.52a.5.5 0 0 0 .5.42h4a.5.5 0 0 0 .5-.42l.36-2.52c.58-.23 1.13-.55 1.63-.94l2.39.96a.5.5 0 0 0 .6-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58ZM12 15.5A3.5 3.5 0 1 1 12 8a3.5 3.5 0 0 1 0 7.5Z"
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
                  d="M10 4h9a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-9v-2h8V6h-8V4Zm1.5 4.5L15 12l-3.5 3.5-1.4-1.4 1.1-1.1H4v-2h7.2l-1.1-1.1 1.4-1.4Z"
                />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </aside>
  `,
  styles: [
    `
      :host {
        display: block;
        height: 100%;
      }

      .sidebar {
        width: 292px;
        height: 100vh;
        min-height: 100vh;
        transition: width 0.22s ease;
      }

      .sidebar--collapsed {
        width: 92px;
      }

      .sidebar__frame {
        position: relative;
        overflow: hidden;
        height: 100vh;
        min-height: 100vh;
        display: flex;
        flex-direction: column;
        gap: 14px;
        padding: 14px;
        color: #eaf6ff;
        background:
          radial-gradient(circle at 18% 0%, rgba(82, 156, 255, 0.28), transparent 30%),
          radial-gradient(circle at 100% 24%, rgba(42, 157, 143, 0.16), transparent 32%),
          linear-gradient(180deg, #071a3a 0%, #092549 48%, #06152e 100%);
        border-right: 1px solid rgba(123, 185, 255, 0.2);
        box-shadow: 10px 0 32px rgba(7, 26, 58, 0.14);
        box-sizing: border-box;
      }

      .blue-glow {
        position: absolute;
        pointer-events: none;
        border-radius: 999px;
        filter: blur(2px);
      }

      .blue-glow--top {
        width: 190px;
        height: 190px;
        top: -92px;
        left: -72px;
        background: radial-gradient(circle, rgba(109, 183, 255, 0.34), transparent 68%);
      }

      .blue-glow--middle {
        width: 210px;
        height: 210px;
        right: -132px;
        top: 170px;
        background: radial-gradient(circle, rgba(74, 163, 255, 0.18), transparent 70%);
      }

      .gold-leaf {
        position: absolute;
        width: 190px;
        height: 190px;
        pointer-events: none;
        opacity: 0.17;
        background:
          radial-gradient(
            ellipse at center,
            rgba(243, 223, 152, 0.42) 0 24%,
            transparent 25%
          ),
          radial-gradient(
            ellipse at center,
            rgba(218, 165, 62, 0.32) 0 24%,
            transparent 25%
          ),
          radial-gradient(
            ellipse at center,
            rgba(255, 255, 255, 0.14) 0 24%,
            transparent 25%
          );
        background-size: 42px 72px, 54px 86px, 38px 66px;
        background-position: 0 0, 38px 42px, 84px 10px;
        background-repeat: repeat;
        mask-image: radial-gradient(circle, #000 0 46%, transparent 70%);
      }

      .gold-leaf--top {
        top: -78px;
        right: -92px;
        transform: rotate(-18deg);
      }

      .gold-leaf--bottom {
        bottom: -98px;
        left: -96px;
        transform: rotate(24deg);
        opacity: 0.12;
      }

      .sidebar__top,
      .sidebar__nav,
      .sidebar__account {
        position: relative;
        z-index: 1;
      }

      .sidebar__top {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 10px;
        padding-bottom: 12px;
      }

      .sidebar__brand {
        display: flex;
        align-items: center;
        gap: 11px;
        min-width: 0;
        color: #ffffff;
        text-decoration: none;
      }

      .sidebar__logo-shell {
        width: 50px;
        height: 50px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        border-radius: 18px;
        background: linear-gradient(145deg, rgba(255, 250, 230, 0.98), rgba(225, 246, 255, 0.92));
        border: 1px solid rgba(243, 223, 152, 0.34);
        box-shadow: 0 12px 28px rgba(0, 0, 0, 0.18);
      }

      .sidebar__brand-logo {
        width: 38px;
        height: 38px;
        object-fit: contain;
      }

      .sidebar__brand-copy {
        min-width: 0;
        display: grid;
        gap: 3px;
        transition: opacity 0.18s ease, transform 0.18s ease, width 0.18s ease;
      }

      .sidebar__brand-name {
        color: #ffffff;
        font-size: 1.25rem;
        font-weight: 950;
        letter-spacing: -0.035em;
        line-height: 1;
      }

      .sidebar__brand-role {
        color: #9bc4ff;
        font-size: 0.78rem;
        font-weight: 850;
      }

      .sidebar__collapse-btn {
        width: 38px;
        height: 38px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        border: 0;
        border-radius: 14px;
        background: rgba(255, 255, 255, 0.09);
        color: #dceeff;
        font-size: 1.45rem;
        font-weight: 950;
        cursor: pointer;
        transition: 0.18s ease;
      }

      .sidebar__collapse-btn:hover {
        background: rgba(243, 223, 152, 0.18);
        color: #f3df98;
      }

      .sidebar__nav {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        padding-right: 2px;
        display: grid;
        align-content: start;
        gap: 17px;
      }

      .sidebar__nav::-webkit-scrollbar {
        width: 6px;
      }

      .sidebar__nav::-webkit-scrollbar-thumb {
        background: rgba(255, 255, 255, 0.18);
        border-radius: 999px;
      }

      .sidebar__section {
        display: grid;
        gap: 8px;
      }

      .sidebar__section-title {
        margin: 0;
        padding: 0 12px;
        color: rgba(185, 220, 255, 0.66);
        font-size: 0.68rem;
        font-weight: 950;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        transition: opacity 0.18s ease;
      }

      .sidebar__links {
        display: grid;
        gap: 6px;
      }

      .sidebar__link {
        position: relative;
        min-height: 46px;
        display: flex;
        align-items: center;
        gap: 11px;
        padding: 0 12px;
        border-radius: 16px;
        color: rgba(234, 246, 255, 0.76);
        text-decoration: none;
        font-size: 0.92rem;
        font-weight: 850;
        transition: 0.18s ease;
      }

      .sidebar__link::before {
        content: '';
        position: absolute;
        left: 6px;
        top: 11px;
        bottom: 11px;
        width: 3px;
        border-radius: 999px;
        background: transparent;
        transition: 0.18s ease;
      }

      .sidebar__link:hover {
        background: rgba(255, 255, 255, 0.08);
        color: #ffffff;
        transform: translateX(2px);
      }

      .sidebar__link.active {
        background: linear-gradient(135deg, rgba(65, 143, 255, 0.28), rgba(255, 255, 255, 0.1));
        color: #ffffff;
        box-shadow: 0 14px 26px rgba(0, 0, 0, 0.18);
      }

      .sidebar__link.active::before {
        background: #f3df98;
        box-shadow: 0 0 16px rgba(243, 223, 152, 0.45);
      }

      .sidebar__link-icon {
        width: 24px;
        height: 24px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        color: currentColor;
        font-size: 1.08rem;
        font-weight: 900;
      }

      .sidebar__link.active .sidebar__link-icon {
        color: #f3df98;
      }

      .sidebar__link-label {
        min-width: 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        transition: opacity 0.18s ease, transform 0.18s ease, width 0.18s ease;
      }

      .sidebar__account {
        display: grid;
        gap: 10px;
        padding-top: 12px;
        border-top: 1px solid rgba(255, 255, 255, 0.1);
      }

      .sidebar__account-main {
        display: flex;
        align-items: center;
        gap: 10px;
        min-width: 0;
        padding: 10px;
        border-radius: 18px;
        background: rgba(255, 255, 255, 0.09);
        border: 1px solid rgba(255, 255, 255, 0.12);
        box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
      }

      .sidebar__avatar {
        width: 42px;
        height: 42px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        border-radius: 15px;
        background: linear-gradient(135deg, #f3df98, #6db7ff);
        color: #06152e;
        font-weight: 950;
        letter-spacing: 0.04em;
      }

      .sidebar__account-copy {
        min-width: 0;
        display: grid;
        gap: 2px;
        transition: opacity 0.18s ease, transform 0.18s ease, width 0.18s ease;
      }

      .sidebar__account-copy strong {
        color: #ffffff;
        font-size: 0.88rem;
        line-height: 1.15;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .sidebar__account-copy small {
        color: rgba(234, 246, 255, 0.68);
        font-size: 0.74rem;
        font-weight: 750;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .sidebar__account-actions {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
      }

      .sidebar__icon-btn {
        min-height: 40px;
        display: grid;
        place-items: center;
        border: 0;
        border-radius: 15px;
        background: rgba(255, 255, 255, 0.09);
        color: #dceeff;
        cursor: pointer;
        text-decoration: none;
        transition: 0.18s ease;
      }

      .sidebar__icon-btn svg {
        width: 20px;
        height: 20px;
        fill: currentColor;
      }

      .sidebar__icon-btn:hover {
        background: rgba(243, 223, 152, 0.18);
        color: #f3df98;
      }

      .sidebar__icon-btn--danger:hover {
        background: rgba(255, 108, 108, 0.16);
        color: #ffb4b4;
      }

      .sidebar--collapsed .sidebar__frame {
        align-items: center;
        padding-inline: 12px;
      }

      .sidebar--collapsed .sidebar__top {
        flex-direction: column;
        width: 100%;
      }

      .sidebar--collapsed .sidebar__brand {
        justify-content: center;
      }

      .sidebar--collapsed .sidebar__brand-copy,
      .sidebar--collapsed .sidebar__link-label,
      .sidebar--collapsed .sidebar__account-copy,
      .sidebar--collapsed .sidebar__section-title {
        opacity: 0;
        width: 0;
        max-width: 0;
        overflow: hidden;
        transform: translateX(-6px);
        pointer-events: none;
      }

      .sidebar--collapsed .sidebar__section,
      .sidebar--collapsed .sidebar__links,
      .sidebar--collapsed .sidebar__nav {
        width: 100%;
      }

      .sidebar--collapsed .sidebar__link {
        justify-content: center;
        padding: 0;
      }

      .sidebar--collapsed .sidebar__link::before {
        left: 3px;
      }

      .sidebar--collapsed .sidebar__link:hover {
        transform: none;
      }

      .sidebar--collapsed .sidebar__account-main {
        justify-content: center;
        padding: 8px;
      }

      .sidebar--collapsed .sidebar__account-actions {
        grid-template-columns: 1fr;
      }

      @media (max-width: 960px) {
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
          border-bottom: 1px solid rgba(123, 185, 255, 0.22);
          box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
          padding: 12px;
        }

        .sidebar__top {
          padding-bottom: 10px;
        }

        .sidebar__nav {
          display: flex;
          gap: 10px;
          overflow-x: auto;
          padding-bottom: 4px;
        }

        .sidebar__section {
          min-width: max-content;
        }

        .sidebar__section-title {
          display: none;
        }

        .sidebar__links {
          display: flex;
          gap: 8px;
        }

        .sidebar__link {
          min-width: max-content;
          padding: 0 12px;
        }

        .sidebar__link::before {
          display: none;
        }

        .sidebar__account {
          margin-top: 0;
        }

        .sidebar--collapsed .sidebar__frame {
          align-items: stretch;
        }

        .sidebar--collapsed .sidebar__top {
          flex-direction: row;
        }

        .sidebar--collapsed .sidebar__brand-copy,
        .sidebar--collapsed .sidebar__link-label,
        .sidebar--collapsed .sidebar__account-copy {
          opacity: 1;
          width: auto;
          max-width: none;
          transform: none;
          pointer-events: auto;
        }

        .sidebar--collapsed .sidebar__link {
          justify-content: flex-start;
          padding: 0 12px;
        }

        .sidebar--collapsed .sidebar__account-actions {
          grid-template-columns: 1fr 1fr;
        }
      }
    `,
  ],
})
export class InvestorSidebarComponent {
  private readonly sessionService = inject(SessionService);
  private readonly router = inject(Router);

  collapsed = false;

  readonly emailLabel = this.sessionService.email() || 'Signed in investor';
  readonly userNameLabel = this.buildUserName(this.emailLabel);
  readonly userInitial = this.buildInitials(this.userNameLabel);

  readonly sections: SidebarSection[] = [
    {
      title: 'Overview',
      items: [
        {
          label: 'Dashboard',
          route: '/investor',
          exact: true,
          icon: '⌂',
        },
      ],
    },
    {
      title: 'Opportunities',
      items: [
        {
          label: 'Discover Campaigns',
          route: '/investor',
          exact: true,
          icon: '◇',
        },
        {
          label: 'Watchlist',
          route: '/investor/watchlist',
          exact: true,
          icon: '☆',
        },
      ],
    },
    {
      title: 'Investing',
      items: [
        {
          label: 'My Pledges',
          route: '/investor/my-pledges',
          exact: true,
          icon: '◈',
        },
        {
          label: 'Portfolio',
          route: '/investor/portfolio',
          exact: true,
          icon: '▥',
        },
        {
          label: 'Campaign Updates',
          route: '/investor/updates',
          exact: true,
          icon: '✦',
        },
      ],
    },
    {
      title: 'Finance',
      items: [
        {
          label: 'Payments',
          route: '/investor/my-payments',
          exact: true,
          icon: '▣',
        },
        {
          label: 'Documents',
          route: '/investor/documents',
          exact: true,
          icon: '▤',
        },
      ],
    },
    {
      title: 'Account',
      items: [
        {
          label: 'KYC Verification',
          route: '/investor/kyc',
          exact: true,
          icon: '◌',
        },
        {
          label: 'Support',
          route: '/investor/support',
          exact: true,
          icon: '?',
        },
      ],
    },
  ];

  toggleCollapse(): void {
    this.collapsed = !this.collapsed;
  }

  logout(): void {
    this.sessionService.clearSession();
    this.router.navigateByUrl('/auth/login');
  }

  private buildUserName(email: string): string {
    if (!email || email === 'Signed in investor') {
      return 'Helma Investor';
    }

    const local = email.split('@')[0]?.trim() ?? '';

    if (!local) {
      return 'Helma Investor';
    }

    return local
      .split(/[._-]+/)
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
      .join(' ');
  }

  private buildInitials(name: string): string {
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }
}