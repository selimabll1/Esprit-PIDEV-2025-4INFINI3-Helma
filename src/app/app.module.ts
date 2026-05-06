import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';

import { JwtInterceptor } from './core/interceptors/jwt.interceptor';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';

import { LoginComponent } from './features/auth/components/login/login.component';
import { SignupComponent } from './features/auth/components/signup/signup.component';
import { DashboardComponent } from './features/dashboard/components/dashboard/dashboard.component';
import { LoanListComponent } from './features/loans/components/list/loan-list.component';
import { LoanDetailComponent } from './features/loans/components/detail/loan-detail.component';
import { LoanCreateComponent } from './features/loans/components/form/loan-create.component';
import { LoanSimulationComponent } from './features/loans/components/simulation/loan-simulation.component';
import { PaymentFormComponent } from './features/payments/components/form/payment-form.component';
import { PaymentHistoryComponent } from './features/payments/components/list/payment-history.component';
import { ForbiddenComponent } from './features/forbidden/components/forbidden/forbidden.component';
import { EarlyWarningsComponent } from './features/early-warnings/components/early-warnings/early-warnings.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    SignupComponent,
    DashboardComponent,
    LoanListComponent,
    LoanDetailComponent,
    LoanCreateComponent,
    LoanSimulationComponent,
    PaymentFormComponent,
    PaymentHistoryComponent,
    EarlyWarningsComponent,
    ForbiddenComponent
  ],
  imports: [BrowserModule, HttpClientModule, ReactiveFormsModule, AppRoutingModule],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}

