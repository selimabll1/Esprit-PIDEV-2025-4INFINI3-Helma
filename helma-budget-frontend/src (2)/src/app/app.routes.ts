import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guards';
import { Role } from './core/models/role.enum';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'auth/login'
  },
  {
    path: 'auth/login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.page').then((m) => m.LoginPageComponent)
  },
  {
    path: 'founder',
    canActivate: [authGuard, roleGuard],
    data: {
      roles: [Role.FOUNDER]
    },
    loadChildren: () =>
      import('./features/founder/founder.routes').then((m) => m.FOUNDER_ROUTES)
  },
  {
    path: 'investor',
    canActivate: [authGuard, roleGuard],
    data: {
      roles: [Role.INVESTOR]
    },
    loadChildren: () =>
      import('./features/investor/investor.routes').then((m) => m.INVESTOR_ROUTES)
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: {
      roles: [Role.ADMIN, Role.COMPLIANCE]
    },
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES)
  },
  {
    path: '**',
    redirectTo: 'auth/login'
  }
];