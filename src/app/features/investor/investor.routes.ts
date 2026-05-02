import { Routes } from '@angular/router';
import { InvestorShellComponent } from './shell/investor-shell.component';

export const INVESTOR_ROUTES: Routes = [
  {
    path: '',
    component: InvestorShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./campaigns/investor-campaigns.page').then(
            (m) => m.InvestorCampaignsPageComponent,
          ),
      },
      {
        path: 'campaigns/:slug',
        loadComponent: () =>
          import('./campaigns/investor-campaign-detail.page').then(
            (m) => m.InvestorCampaignDetailPageComponent,
          ),
      },
      {
        path: 'my-pledges',
        loadComponent: () =>
          import('./pledges/investor-my-pledges.page').then(
            (m) => m.InvestorMyPledgesPageComponent,
          ),
      },
      {
        path: 'my-payments',
        loadComponent: () =>
          import('./payments/investor-my-payments.page').then(
            (m) => m.InvestorMyPaymentsPageComponent,
          ),
      },
      {
        path: 'portfolio',
        loadComponent: () =>
          import('./portfolio/investor-portfolio.page').then(
            (m) => m.InvestorPortfolioPageComponent,
          ),
      },
    ],
  },
];
