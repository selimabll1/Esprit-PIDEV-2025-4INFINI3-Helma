import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, Subject, takeUntil } from 'rxjs';
import {
  Loan,
  LoanPayment,
  LoanStatus,
  LoanSummary,
  MultiAgentDecision,
  RepaymentSchedule
} from '../../../../core/models';
import { ApiService } from '../../../../core/services/api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { LoanService } from '../../../../core/services/loan.service';
import { PaymentService } from '../../../../core/services/payment.service';

@Component({
  selector: 'app-loan-detail',
  templateUrl: './loan-detail.component.html'
})
export class LoanDetailComponent implements OnInit, OnDestroy {
  loan: Loan | null = null;
  summary: LoanSummary | null = null;
  schedule: RepaymentSchedule[] = [];
  payments: LoanPayment[] = [];
  aiResult: Record<string, unknown> | null = null;
  multiAgentDecision: MultiAgentDecision | null = null;
  multiAgentLoading = false;
  mlResult: Record<string, unknown> | null = null;
  markovResult: Record<string, unknown> | null = null;
  loading = false;
  error: string | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly loanService: LoanService,
    private readonly paymentService: PaymentService,
    private readonly apiService: ApiService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe({
      next: (params) => {
        const id = Number(params['id']);
        if (!Number.isFinite(id)) {
          this.error = 'Invalid loan identifier.';
          return;
        }
        this.loadAll(id);
      }
    });
  }

  loadAll(id: number): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      loan: this.loanService.getLoanById(id),
      summary: this.loanService.getLoanSummary(id),
      schedule: this.loanService.getLoanSchedule(id),
      payments: this.paymentService.getPaymentsByLoan(id)
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ loan, summary, schedule, payments }) => {
          this.loan = loan;
          this.summary = summary;
          this.schedule = schedule;
          this.payments = payments;
          this.loading = false;
        },
        error: () => {
          this.error = 'Unable to load the loan details.';
          this.loading = false;
        }
      });
  }

  progressPercent(): number {
    if (!this.loan || !this.summary || !this.loan.principalAmount) {
      return 0;
    }
    return Math.min(100, (this.summary.totalPaid / this.loan.principalAmount) * 100);
  }

  pendingSchedule(): RepaymentSchedule[] {
    return this.schedule.filter((item) => item.status !== 'PAID');
  }

  goToPayment(schedule: RepaymentSchedule): void {
    if (!this.loan) {
      return;
    }
    void this.router.navigate([`/loans/${this.loan.id}/pay`], { state: { schedule } });
  }

  hasRole(role: string): boolean {
    return this.authService.hasRole(role);
  }

  getAIDecision(): void {
    if (!this.loan) {
      return;
    }
    this.apiService
      .getAIDecision(this.loan.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => (this.aiResult = result as Record<string, unknown>),
        error: () => (this.error = 'AI decision request failed.')
      });
  }

  resultText(result: Record<string, unknown> | null, path: string, fallback = 'Not available'): string {
    const value = this.resultValue(result, path);
    return value === null || value === undefined || value === '' ? fallback : String(value);
  }

  resultPercent(result: Record<string, unknown> | null, path: string): string {
    const value = this.resultValue(result, path);
    if (typeof value === 'number') {
      return `${(value * 100).toFixed(1)}%`;
    }
    return value ? String(value) : 'Not available';
  }

  riskBadgeValue(value: string): string {
    const normalized = value.toUpperCase();
    if (normalized.includes('HIGH') || normalized.includes('REJECT') || normalized.includes('DEFAULT')) {
      return 'HIGH';
    }
    if (normalized.includes('MEDIUM') || normalized.includes('MANUAL')) {
      return 'MEDIUM';
    }
    return 'LOW';
  }

  getMultiAgentDecision(): void {
    if (!this.loan) {
      return;
    }
    this.error = null;
    this.multiAgentLoading = true;
    this.loanService
      .getMultiAgentDecision(this.loan.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.multiAgentDecision = result;
          this.multiAgentLoading = false;
        },
        error: (err: unknown) => {
          this.error = `La decision multi-agent a echoue. ${this.httpErrorHint(err)}`;
          this.multiAgentLoading = false;
        }
      });
  }

  downloadContract(): void {
    if (!this.loan) {
      return;
    }
    this.loanService
      .downloadContract(this.loan.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `contrat_${this.loan?.id}.pdf`;
          link.click();
          URL.revokeObjectURL(url);
        },
        error: () => (this.error = 'Le telechargement du contrat PDF a echoue.')
      });
  }

  canDownloadContract(): boolean {
    return this.loan?.status === LoanStatus.ACTIVE || this.loan?.status === LoanStatus.CLOSED;
  }

  getMLPrediction(): void {
    if (!this.loan) {
      return;
    }
    this.apiService
      .getMLPrediction(this.loan.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => (this.mlResult = result as Record<string, unknown>),
        error: () => (this.error = 'ML prediction request failed.')
      });
  }

  getMarkovPrediction(): void {
    if (!this.loan) {
      return;
    }
    this.apiService
      .getMarkovPrediction(this.loan.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => (this.markovResult = result as Record<string, unknown>),
        error: () => (this.error = 'Markov prediction request failed.')
      });
  }

  private httpErrorHint(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 404) {
        return 'Endpoint introuvable: redemarre le backend loan-service apres les changements.';
      }
      if (err.status === 403) {
        return 'Acces refuse: connecte-toi avec le role Admin.';
      }
      if (err.status === 0) {
        return 'Backend inaccessible sur http://localhost:8081/h.';
      }
      return `Statut HTTP ${err.status}.`;
    }
    return '';
  }

  private resultValue(result: Record<string, unknown> | null, path: string): unknown {
    if (!result) {
      return null;
    }
    return path.split('.').reduce<unknown>((current, key) => {
      if (current && typeof current === 'object' && key in current) {
        return (current as Record<string, unknown>)[key];
      }
      return null;
    }, result);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
