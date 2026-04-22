import { Routes } from '@angular/router';
import { AdminShellComponent } from './shell/admin-shell.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      {
        path: '',
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
      }
    ]
  }
];