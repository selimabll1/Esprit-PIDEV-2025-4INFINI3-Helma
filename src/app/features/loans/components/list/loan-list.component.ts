import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { Loan, PagedLoansResponse } from '../../../../core/models';
import { AuthService } from '../../../../core/services/auth.service';
import { LoanService } from '../../../../core/services/loan.service';

@Component({
  selector: 'app-loan-list',
  templateUrl: './loan-list.component.html'
})
export class LoanListComponent implements OnInit, OnDestroy {
  loans: Loan[] = [];
  page = 0;
  pageSize = 8;
  totalElements = 0;
  loading = false;
  error: string | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly loanService: LoanService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadLoans();
  }

  loadLoans(): void {
    this.loading = true;
    this.error = null;

    if (this.hasAnyRole(['ADMIN', 'COMPLIANCE'])) {
      this.loanService
        .getAllLoans(this.page, this.pageSize)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response: PagedLoansResponse) => {
            this.loans = response.content ?? [];
            this.totalElements = response.totalElements ?? this.loans.length;
            this.loading = false;
          },
          error: () => {
            this.error = 'Unable to load the loan catalog.';
            this.loading = false;
          }
        });
      return;
    }

    const userId = this.authService.getUserId();
    if (!userId) {
      this.error = 'Unable to resolve the current user.';
      this.loading = false;
      return;
    }

    this.loanService
      .getLoansByUser(userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (loans) => {
          this.loans = loans;
          this.totalElements = loans.length;
          this.loading = false;
        },
        error: () => {
          this.error = 'Unable to load your loan records.';
          this.loading = false;
        }
      });
  }

  approveLoan(id: number): void {
    this.loading = true;
    this.loanService
      .approveLoan(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadLoans(),
        error: () => {
          this.error = 'Approval failed.';
          this.loading = false;
        }
      });
  }

  rejectLoan(id: number): void {
    this.loading = true;
    this.loanService
      .rejectLoan(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadLoans(),
        error: () => {
          this.error = 'Rejection failed.';
          this.loading = false;
        }
      });
  }

  prevPage(): void {
    if (this.page === 0) {
      return;
    }
    this.page -= 1;
    this.loadLoans();
  }

  nextPage(): void {
    if ((this.page + 1) * this.pageSize >= this.totalElements) {
      return;
    }
    this.page += 1;
    this.loadLoans();
  }

  hasRole(role: string): boolean {
    return this.authService.hasRole(role);
  }

  hasAnyRole(roles: string[]): boolean {
    return this.authService.hasAnyRole(roles);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
