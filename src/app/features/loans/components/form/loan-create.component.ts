import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { LoanType, SimulationResult } from '../../../../core/models';
import { ApiService } from '../../../../core/services/api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { LoanService } from '../../../../core/services/loan.service';

@Component({
  selector: 'app-loan-create',
  templateUrl: './loan-create.component.html'
})
export class LoanCreateComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  simulationResult: SimulationResult | null = null;
  submitted = false;
  loading = false;
  error: string | null = null;
  readonly loanTypes = Object.values(LoanType);

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly loanService: LoanService,
    private readonly apiService: ApiService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    this.form = this.fb.group({
      userId: [userId, [Validators.required]],
      loanType: ['', [Validators.required]],
      principalAmount: [null, [Validators.required, Validators.min(100)]],
      durationMonths: [null, [Validators.required, Validators.min(1), Validators.max(240)]]
    });
  }

  simulate(): void {
    this.submitted = true;
    this.error = null;
    if (this.form.invalid) {
      return;
    }

    const { principalAmount, durationMonths } = this.form.getRawValue();

    this.loading = true;
    this.apiService
      .simulateLoan({ principalAmount, durationMonths, interestRate: 20 })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.simulationResult = result;
          this.loading = false;
        },
        error: () => {
          this.error = 'Simulation failed.';
          this.loading = false;
        }
      });
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = null;
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.loanService
      .createLoan(this.form.getRawValue())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (loan) => {
          this.loading = false;
          void this.router.navigate(['/loans', loan.id]);
        },
        error: () => {
          this.error = 'Loan creation failed.';
          this.loading = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
