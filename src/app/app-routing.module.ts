import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/components/login/login.component';
import { SignupComponent } from './features/auth/components/signup/signup.component';
import { DashboardComponent } from './features/dashboard/components/dashboard/dashboard.component';
import { LoanListComponent } from './features/loans/components/list/loan-list.component';
import { LoanCreateComponent } from './features/loans/components/form/loan-create.component';
import { LoanDetailComponent } from './features/loans/components/detail/loan-detail.component';
import { LoanSimulationComponent } from './features/loans/components/simulation/loan-simulation.component';
import { PaymentFormComponent } from './features/payments/components/form/payment-form.component';
import { ForbiddenComponent } from './features/forbidden/components/forbidden/forbidden.component';
import { EarlyWarningsComponent } from './features/early-warnings/components/early-warnings/early-warnings.component';

const routes: Routes = [
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  { path: 'auth/login', component: LoginComponent },
  { path: 'auth/signup', component: SignupComponent },
  { path: 'simulate', component: LoanSimulationComponent },
  {
    path: 'youth',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    data: { roles: ['YOUTH_BENEFICIARY'] }
  },
  {
    path: 'investor',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    data: { roles: ['INVESTOR'] }
  },
  {
    path: 'admin/dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN', 'COMPLIANCE'] }
  },
  {
    path: 'early-warnings',
    component: EarlyWarningsComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN', 'COMPLIANCE'] }
  },
  {
    path: 'loans',
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    data: { roles: ['YOUTH_BENEFICIARY', 'ADMIN', 'COMPLIANCE'] },
    children: [
      { path: '', component: LoanListComponent },
      {
        path: 'create',
        component: LoanCreateComponent,
        data: { roles: ['YOUTH_BENEFICIARY'] }
      },
      { path: ':id', component: LoanDetailComponent },
      {
        path: ':id/pay',
        component: PaymentFormComponent,
        data: { roles: ['YOUTH_BENEFICIARY'] }
      }
    ]
  },
  { path: 'forbidden', component: ForbiddenComponent },
  { path: '**', redirectTo: '/simulate' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}

