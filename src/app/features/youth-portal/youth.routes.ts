import { Routes } from '@angular/router';
import { YouthPortalShellComponent } from './shell/youth-portal-shell.component';

const loadYouthFeaturePlaceholder = () =>
  import('./pages/youth-feature-placeholder.page').then(
    (m) => m.YouthFeaturePlaceholderPageComponent
  );

export const YOUTH_PORTAL_ROUTES: Routes = [
  {
    path: '',
    component: YouthPortalShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/youth-home.page').then((m) => m.YouthHomePageComponent)
      },
      {
        path: 'savings',
        loadComponent: loadYouthFeaturePlaceholder,
        data: {
          eyebrow: 'Financial Tools',
          title: 'Savings Goals',
          description:
            'Create, organize, and track your savings goals in one structured workspace.'
        }
      },
      {
        path: 'budgeting',
        loadComponent: loadYouthFeaturePlaceholder,
        data: {
          eyebrow: 'Financial Tools',
          title: 'Budgeting',
          description:
            'Track spending, monitor categories, and view budget alerts designed for youth users.'
        }
      },
      {
        path: 'guidance',
        loadComponent: loadYouthFeaturePlaceholder,
        data: {
          eyebrow: 'Financial Tools',
          title: 'Guidance',
          description:
            'Access guidance and coaching content that helps you build better financial habits.'
        }
      },
      {
        path: 'micro-loans',
        loadComponent: loadYouthFeaturePlaceholder,
        data: {
          eyebrow: 'Financing',
          title: 'Micro-Loans',
          description:
            'Explore responsible youth financing and manage your loan requests from one place.'
        }
      },
      {
        path: 'micro-leasing',
        loadComponent: loadYouthFeaturePlaceholder,
        data: {
          eyebrow: 'Financing',
          title: 'Micro-Leasing',
          description:
            'Request equipment access through installment-based micro-leasing flows.'
        }
      },
      {
        path: 'crowdfunding',
        loadComponent: () =>
          import('./pages/youth-crowdfunding-hub.page').then(
            (m) => m.YouthCrowdfundingHubPageComponent
          )
      },
      {
        path: 'applications',
        loadComponent: () =>
          import('./pages/youth-applications.page').then(
            (m) => m.YouthApplicationsPageComponent
          )
      },
      {
        path: 'applications/new',
        loadComponent: () =>
          import('./pages/youth-application-raise-form.page').then(
            (m) => m.YouthApplicationRaiseFormPageComponent
          )
      },
      {
        path: 'applications/:id/edit',
        loadComponent: () =>
          import('./pages/youth-application-raise-form.page').then(
            (m) => m.YouthApplicationRaiseFormPageComponent
          )
      },

      {
        path: 'campaigns',
        loadComponent: () =>
          import('./pages/youth-campaign-pages.page').then(
            (m) => m.YouthCampaignPagesPageComponent
          )
      },
      {
        path: 'campaigns/create/:applicationId',
        loadComponent: () =>
          import('./pages/youth-campaign-builder.page').then(
            (m) => m.YouthCampaignBuilderPageComponent
          )
      },
      {
        path: 'campaigns/:id/builder',
        loadComponent: () =>
          import('./pages/youth-campaign-builder.page').then(
            (m) => m.YouthCampaignBuilderPageComponent
          )
      },
      {
        path: 'campaigns/:id/preview',
        loadComponent: () =>
          import('./pages/youth-campaign-preview.page').then(
            (m) => m.YouthCampaignPreviewPageComponent
          )
      },
      {
        path: 'kyc',
        loadComponent: loadYouthFeaturePlaceholder,
        data: {
          eyebrow: 'Account',
          title: 'KYC & Verification',
          description:
            'Submit identity information, upload required documents, and follow verification status.'
        }
      },
      {
        path: 'support',
        loadComponent: loadYouthFeaturePlaceholder,
        data: {
          eyebrow: 'Account',
          title: 'Support',
          description:
            'Open support requests and follow help tickets for platform or account issues.'
        }
      }
    ]
  }
];