import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guards';
import { PublicShellComponent } from './layout/public-shell/public-shell.component';

export const routes: Routes = [
  {
    path: '',
    component: PublicShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/public/home/home.page').then((m) => m.HomePageComponent)
      },
      {
        path: 'discover',
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Discover',
          title: 'Discover opportunities across Helma',
          description:
            'Explore youth tools, financing products, donation campaigns, and equity opportunities in one place.'
        }
      },
      {
        path: 'youth-tools',
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Youth Tools',
          title: 'Savings, budgeting, and financial guidance',
          description:
            'This section will present savings goals, budgeting dashboards, alerts, and coaching-oriented guidance.'
        }
      },
      {
        path: 'financing',
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Financing',
          title: 'Micro-loans and micro-leasing',
          description:
            'This page will present personal, educational, activity, and progressive micro-loans, plus equipment micro-leasing.'
        }
      },
      {
        path: 'crowdfunding/donation',
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Crowdfunding',
          title: 'Donation crowdfunding',
          description:
            'This page will explain donation campaigns for impact-led, creative, and community projects.'
        }
      },
      {
        path: 'crowdfunding/equity',
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Crowdfunding',
          title: 'Selective equity crowdfunding',
          description:
            'This page will explain proposal review, campaign validation, investor participation, and portfolio tracking.'
        }
      },
      {
        path: 'sponsors',
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Sponsors',
          title: 'Sponsor packages and impact visibility',
          description:
            'This section will present sponsorship packages, visibility options, invoices, and impact reporting.'
        }
      },
      {
        path: 'learning',
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Learning',
          title: 'Learning and guidance resources',
          description:
            'This section will host educational content around financial habits, entrepreneurship, and platform onboarding.'
        }
      },
      {
        path: 'resources',
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Resources',
          title: 'Platform resources',
          description:
            'This page will collect guides, FAQs, documentation, and support-oriented content.'
        }
      },

      {
        path: 'campaigns/:slug',
        loadComponent: () =>
          import('./features/public/campaign-page/public-campaign-page.page').then(
            (m) => m.PublicCampaignPageComponent
          )
      },
      {
        path: 'profile',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Profile',
          title: 'Your profile',
          description:
            'Manage your personal information, role-specific details, and KYC status.'
        }
      },
      {
        path: 'settings',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/public/public-placeholder.page').then(
            (m) => m.PublicPlaceholderPageComponent
          ),
        data: {
          eyebrow: 'Settings',
          title: 'Account settings',
          description:
            'Manage preferences, security, notifications, and account options.'
        }
      }
    ]
  },
  {
    path: 'auth/login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.page').then((m) => m.LoginPageComponent)
  },
  {
    path: 'auth/signup',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/signup/signup.page').then((m) => m.SignupPageComponent)
  },
{
  path: 'youth',
  canActivate: [authGuard, roleGuard],
  data: { roles: ['YOUTH_BENEFICIARY'] },
  loadChildren: () =>
    import('./features/youth-portal/youth.routes').then((m) => m.YOUTH_PORTAL_ROUTES)
},
  {
    path: 'investor',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['INVESTOR'] },
    loadChildren: () =>
      import('./features/investor/investor.routes').then((m) => m.INVESTOR_ROUTES)
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'COMPLIANCE'] },
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES)
  },
{
  path: 'loading',
  loadComponent: () =>
    import('./shared/pages/app-loading.page').then((m) => m.AppLoadingPageComponent)
},
{
  path: '**',
  loadComponent: () =>
    import('./shared/pages/not-found.page').then((m) => m.NotFoundPageComponent)
}
];