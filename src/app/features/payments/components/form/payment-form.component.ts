import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { CreatePaymentRequest, RepaymentSchedule } from '../../../../core/models';
import { LoanService } from '../../../../core/services/loan.service';
import { PaymentService } from '../../../../core/services/payment.service';

@Component({
  selector: 'app-payment-form',
  templateUrl: './payment-form.component.html'
})
export class PaymentFormComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  loanId!: number;
  schedule: RepaymentSchedule | null = null;
  availableSchedules: RepaymentSchedule[] = [];
  loading = false;
  error: string | null = null;
  submitted = false;
  readonly paymentMethods = ['CARD', 'BANK_TRANSFER', 'MOBILE_MONEY', 'CASH'];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly loanService: LoanService,
    private readonly paymentService: PaymentService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      loanId: [null, [Validators.required]],
      scheduleId: [null, [Validators.required]],
      amount: [null, [Validators.required, Validators.min(0.01)]],
      paymentMethod: ['', [Validators.required]]
    });

    this.route.params.pipe(takeUntil(this.destroy$)).subscribe({
      next: (params) => {
        this.loanId = Number(params['id']);
        this.schedule = (history.state?.schedule ?? null) as RepaymentSchedule | null;

        this.form.patchValue({
          loanId: this.loanId,
          scheduleId: this.schedule?.id ?? null,
          amount: this.schedule?.expectedAmount ?? null
        });

        this.loadSchedules();
      }
    });
  }

  loadSchedules(): void {
    if (!Number.isFinite(this.loanId)) {
      this.error = 'Invalid loan identifier.';
      return;
    }

    this.loanService
      .getLoanSchedule(this.loanId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (schedules) => {
          this.availableSchedules = schedules.filter((item) => item.status !== 'PAID');
          if (!this.schedule && this.availableSchedules.length) {
            this.selectSchedule(this.availableSchedules[0]);
          }
        },
        error: () => (this.error = 'Unable to load repayment schedules.')
      });
  }

  selectSchedule(schedule: RepaymentSchedule): void {
    this.schedule = schedule;
    this.form.patchValue({
      scheduleId: schedule.id,
      amount: schedule.expectedAmount
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = null;

    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.paymentService
      .createPayment(this.form.getRawValue() as CreatePaymentRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          void this.router.navigate(['/loans', this.loanId]);
        },
        error: () => {
          this.error = 'Payment failed.';
          this.loading = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
