import { Routes } from '@angular/router';
import { FounderShellComponent } from './shell/founder-shell.component';

export const FOUNDER_ROUTES: Routes = [
  {
    path: '',
    component: FounderShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./applications/founder-applications.page').then(
            (m) => m.FounderApplicationsPageComponent
          )
      },
      {
        path: 'applications/new/:type',
        loadComponent: () =>
          import('./applications/founder-application-form.page').then(
            (m) => m.FounderApplicationFormPageComponent
          )
      },
      {
        path: 'applications/:id/edit',
        loadComponent: () =>
          import('./applications/founder-application-form.page').then(
            (m) => m.FounderApplicationFormPageComponent
          )
      }
    ]
  }
];