import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { EarlyWarning } from '../../../../core/models';
import { LoanService } from '../../../../core/services/loan.service';

@Component({
  selector: 'app-early-warnings',
  templateUrl: './early-warnings.component.html'
})
export class EarlyWarningsComponent implements OnInit, OnDestroy {
  warnings: EarlyWarning[] = [];
  loading = false;
  error: string | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(private readonly loanService: LoanService) {}

  ngOnInit(): void {
    this.loading = true;
    this.loanService
      .getEarlyWarnings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (warnings) => {
          this.warnings = warnings;
          this.loading = false;
        },
        error: () => {
          this.error = 'Unable to load early warning alerts. Restart loan-service and verify your role is ADMIN or COMPLIANCE.';
          this.loading = false;
        }
      });
  }

  riskLevel(score: number): string {
    if (score >= 0.8) return 'HIGH';
    if (score >= 0.7) return 'MEDIUM';
    return 'LOW';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
