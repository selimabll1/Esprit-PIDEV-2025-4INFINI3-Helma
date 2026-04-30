import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/auth.guards';
import { AdminShellComponent } from './shell/admin-shell.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./dashboard/admin-dashboard.page').then(
            (m) => m.AdminDashboardPageComponent
          )
      },
      {
        path: 'applications',
        loadComponent: () =>
          import('./applications/admin-applications.page').then(
            (m) => m.AdminApplicationsPageComponent
          )
      },
      {
        path: 'applications/:id',
        loadComponent: () =>
          import('./applications/admin-application-review.page').then(
            (m) => m.AdminApplicationReviewPageComponent
          )
      },
      {
        path: 'payments',
        loadComponent: () =>
          import('./payments/admin-payments.page').then(
            (m) => m.AdminPaymentsPageComponent
          )
      },
      {
        path: 'compliance/new',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./compliance/admin-create-compliance.page').then(
            (m) => m.AdminCreateCompliancePageComponent
          )
      }
    ]
  }
];