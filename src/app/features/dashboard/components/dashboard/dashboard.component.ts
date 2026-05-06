import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { FinancialHealth, Loan, LoanStatistics, LoanStatus } from '../../../../core/models';
import { AuthService } from '../../../../core/services/auth.service';
import { DashboardService } from '../../../../core/services/dashboard.service';
import { LoanService } from '../../../../core/services/loan.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit, OnDestroy {
  readonly loanStatus = LoanStatus;
  loading = false;
  error: string | null = null;
  statistics: LoanStatistics | null = null;
  userLoans: Loan[] = [];
  financialHealth: FinancialHealth | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly authService: AuthService,
    private readonly dashboardService: DashboardService,
    private readonly loanService: LoanService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.error = null;

    if (this.authService.hasAnyRole(['ADMIN', 'COMPLIANCE', 'INVESTOR'])) {
      this.dashboardService
        .getStatistics()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (stats) => {
            this.statistics = stats;
            this.loading = false;
          },
          error: () => {
            this.error = 'Unable to load portfolio-wide loan statistics.';
            this.loading = false;
          }
        });
    }

    const userId = this.authService.getUserId();
    if (!userId) {
      if (!this.statistics) {
        this.loading = false;
      }
      return;
    }

    // Youth beneficiaries should see their own loans; other roles can rely on portfolio views.
    if (this.authService.hasRole('YOUTH_BENEFICIARY')) {
      this.dashboardService
        .getUserLoans(userId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (loans) => {
            this.userLoans = loans;
            this.loading = false;
          },
          error: () => {
            if (!this.statistics) {
              this.error = 'Unable to load your loan portfolio.';
              this.loading = false;
            }
          }
        });
    } else if (!this.statistics) {
      this.loading = false;
    }

    if (!this.authService.hasAnyRole(['YOUTH_BENEFICIARY', 'ADMIN', 'COMPLIANCE'])) {
      return;
    }

    this.loanService
      .getFinancialHealth(userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (health) => (this.financialHealth = health),
        error: () => {
          if (!this.error) {
            this.error = 'Unable to load your financial health score.';
          }
        }
      });
  }

  hasAnyRole(roles: string[]): boolean {
    return this.authService.hasAnyRole(roles);
  }

  hasRole(role: string): boolean {
    return this.authService.hasRole(role);
  }

  pendingLoans(): Loan[] {
    return this.userLoans.filter((loan) => loan.status === this.loanStatus.PENDING);
  }

  activeLoans(): Loan[] {
    return this.userLoans.filter((loan) => loan.status === this.loanStatus.ACTIVE);
  }

  defaultedLoans(): Loan[] {
    return this.userLoans.filter((loan) => loan.status === this.loanStatus.DEFAULTED);
  }

  pendingReviewCount(): number {
    return this.statistics ? Math.max(this.statistics.totalLoans - this.statistics.activeLoans - this.statistics.defaultedLoans, 0) : 0;
  }

  scorePercent(): number {
    return this.financialHealth ? Math.min(100, this.financialHealth.helmaScore / 10) : 0;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
