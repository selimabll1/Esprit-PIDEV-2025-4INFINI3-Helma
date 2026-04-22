import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  PaymentResponse,
  PaymentStatus
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

@Component({
  selector: 'app-investor-my-payments-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>My payments</h1>
          <p>Initiate mock checkout, track payment status, and simulate provider results for your pledges.</p>
        </div>

        <a class="secondary-link" routerLink="/investor/my-pledges">Back to pledges</a>
      </header>

      <p class="success" *ngIf="success()">{{ success() }}</p>
      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="filters">
        <button
          *ngFor="let item of filterOptions"
          type="button"
          [class.active]="selectedFilter() === item.key"
          (click)="selectedFilter.set(item.key)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="state-card" *ngIf="loading()">Loading payments...</div>

      <div class="empty" *ngIf="!loading() && !filteredPayments().length">
        No payments found yet. Create a pledge first, then initiate payment from the campaign page.
      </div>

      <div class="grid" *ngIf="!loading() && filteredPayments().length">
        <article class="card" *ngFor="let payment of filteredPayments()">
          <div class="top">
            <div>
              <div class="pill type">{{ payment.provider }}</div>
              <h2>{{ payment.campaignBusinessName }}</h2>
            </div>

            <div class="pill status" [class]="payment.status.toLowerCase()">
              {{ formatEnumLabel(payment.status) }}
            </div>
          </div>

          <div class="meta">
            <p><strong>Amount:</strong> {{ payment.amount }} {{ payment.currency }}</p>
            <p><strong>Pledge status:</strong> {{ formatEnumLabel(payment.pledgeStatus) }}</p>
            <p><strong>Reference:</strong> {{ payment.providerReference || '—' }}</p>
            <p><strong>Checkout session:</strong> {{ payment.checkoutSessionId || '—' }}</p>
            <p><strong>Created:</strong> {{ payment.createdAt | date:'medium' }}</p>
            <p *ngIf="payment.paidAt"><strong>Paid:</strong> {{ payment.paidAt | date:'medium' }}</p>
            <p *ngIf="payment.refundedAt"><strong>Refunded:</strong> {{ payment.refundedAt | date:'medium' }}</p>
            <p *ngIf="payment.failureReason"><strong>Reason:</strong> {{ payment.failureReason }}</p>
          </div>

          <div class="actions">
            <a [routerLink]="['/investor/campaigns', payment.applicationRaiseId]">Open campaign</a>
            <button
              type="button"
              class="secondary"
              (click)="openMockCheckout(payment)"
              [disabled]="busyPaymentId() === payment.id || !canUseCheckout(payment)"
            >
              {{ busyPaymentId() === payment.id ? 'Working...' : 'Mock checkout' }}
            </button>
          </div>

          <div class="actions" *ngIf="canResolve(payment)">
            <button
              type="button"
              (click)="patchStatus(payment, paymentStatus.SUCCEEDED)"
              [disabled]="busyPaymentId() === payment.id"
            >
              Mark paid
            </button>

            <button
              type="button"
              class="warn"
              (click)="patchStatus(payment, paymentStatus.FAILED, 'Card declined')"
              [disabled]="busyPaymentId() === payment.id"
            >
              Mark failed
            </button>

            <button
              type="button"
              class="secondary"
              (click)="patchStatus(payment, paymentStatus.CANCELED, 'Customer canceled checkout')"
              [disabled]="busyPaymentId() === payment.id"
            >
              Cancel
            </button>
          </div>

          <div class="actions" *ngIf="payment.status === paymentStatus.SUCCEEDED">
            <button
              type="button"
              class="warn"
              (click)="patchStatus(payment, paymentStatus.REFUNDED, 'Refund requested by investor')"
              [disabled]="busyPaymentId() === payment.id"
            >
              Refund
            </button>
          </div>
        </article>
      </div>
    </section>
  `,
  styles: [`
    .page {
      display: grid;
      gap: 20px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
    }

    .page-header h1 {
      margin: 0 0 8px;
      color: #062a2b;
    }

    .page-header p {
      margin: 0;
      color: #5c6b73;
      max-width: 760px;
    }

    .secondary-link {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 42px;
      padding: 0 14px;
      border-radius: 10px;
      text-decoration: none;
      background: #eef3f5;
      color: #062a2b;
      font-weight: 700;
    }

    .filters,
    .actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }

    .filters button,
    .actions a,
    .actions button {
      min-height: 40px;
      border: 0;
      border-radius: 10px;
      padding: 0 14px;
      cursor: pointer;
      background: #062a2b;
      color: white;
      font-weight: 700;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font: inherit;
    }

    .filters button {
      background: #eef3f5;
      color: #062a2b;
    }

    .filters button.active {
      background: #062a2b;
      color: white;
    }

    .actions .secondary {
      background: #eef3f5;
      color: #062a2b;
    }

    .actions .warn {
      background: #b03a2e;
      color: white;
    }

    .actions button:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }

    .grid {
      display: grid;
      gap: 16px;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
    }

    .card,
    .empty,
    .state-card {
      background: white;
      border-radius: 18px;
      padding: 20px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.06);
    }

    .card {
      display: grid;
      gap: 16px;
    }

    .top {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
    }

    .card h2 {
      margin: 10px 0 0;
      color: #062a2b;
      font-size: 1.15rem;
    }

    .meta {
      display: grid;
      gap: 8px;
    }

    .meta p {
      margin: 0;
      color: #33444d;
    }

    .pill {
      display: inline-flex;
      align-items: center;
      padding: 6px 10px;
      border-radius: 999px;
      font-size: 0.78rem;
      font-weight: 700;
    }

    .type {
      background: #eaf7f5;
      color: #0b3b3c;
    }

    .status {
      background: #eef3f5;
      color: #455a64;
    }

    .status.pending_provider,
    .status.created {
      background: #fff5d7;
      color: #8a6d1d;
    }

    .status.succeeded {
      background: #e8f7ef;
      color: #1f7a43;
    }

    .status.failed,
    .status.canceled,
    .status.refunded {
      background: #fdecec;
      color: #b03a2e;
    }

    .success {
      margin: 0;
      color: #1f7a43;
      font-weight: 600;
    }

    .error {
      margin: 0;
      color: #c0392b;
      font-weight: 600;
    }

    @media (max-width: 960px) {
      .page-header {
        flex-direction: column;
      }
    }
  `]
})
export class InvestorMyPaymentsPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  readonly paymentStatus = PaymentStatus;
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly busyPaymentId = signal<number | null>(null);
  readonly payments = signal<PaymentResponse[]>([]);
  readonly selectedFilter = signal<'ALL' | PaymentStatus>('ALL');

  readonly filterOptions = [
    { key: 'ALL' as const, label: 'All' },
    { key: PaymentStatus.PENDING_PROVIDER, label: 'Pending provider' },
    { key: PaymentStatus.SUCCEEDED, label: 'Succeeded' },
    { key: PaymentStatus.FAILED, label: 'Failed' },
    { key: PaymentStatus.CANCELED, label: 'Canceled' },
    { key: PaymentStatus.REFUNDED, label: 'Refunded' }
  ];

  readonly filteredPayments = computed(() => {
    const selected = this.selectedFilter();
    const items = this.payments();
    if (selected === 'ALL') return items;
    return items.filter((payment) => payment.status === selected);
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.service
      .listMyPayments()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (payments) => this.payments.set(payments),
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err))
      });
  }

  openMockCheckout(payment: PaymentResponse): void {
    this.busyPaymentId.set(payment.id);
    this.error.set('');
    this.success.set('');

    this.service
      .getMyPaymentMockCheckout(payment.id)
      .pipe(finalize(() => this.busyPaymentId.set(null)))
      .subscribe({
        next: (response) => this.success.set(response.message),
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err))
      });
  }

  patchStatus(
    payment: PaymentResponse,
    status: PaymentStatus,
    failureReason?: string
  ): void {
    this.busyPaymentId.set(payment.id);
    this.error.set('');
    this.success.set('');

    this.service
      .patchMyPaymentStatus(payment.id, {
        status,
        failureReason: failureReason ?? null
      })
      .pipe(finalize(() => this.busyPaymentId.set(null)))
      .subscribe({
        next: (updated) => {
          this.mergePayment(updated);
          this.success.set(`Payment ${this.formatEnumLabel(updated.status).toLowerCase()} successfully.`);
        },
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err))
      });
  }

  canUseCheckout(payment: PaymentResponse): boolean {
    return (
      payment.status === PaymentStatus.CREATED ||
      payment.status === PaymentStatus.PENDING_PROVIDER
    );
  }

  canResolve(payment: PaymentResponse): boolean {
    return this.canUseCheckout(payment);
  }

  formatEnumLabel(value: string): string {
    return value
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  private mergePayment(updated: PaymentResponse): void {
    this.payments.update((items) =>
      items.map((item) => (item.id === updated.id ? updated : item))
    );
  }

  private extractError(err: HttpErrorResponse): string {
    const fieldErrors = err.error?.fieldErrors;
    if (fieldErrors && typeof fieldErrors === 'object') {
      const joined = Object.values(fieldErrors)
        .filter((value): value is string => typeof value === 'string' && !!value.trim())
        .join(' ');
      if (joined) return joined;
    }

    return (
      err.error?.message ||
      err.error?.error ||
      (typeof err.error === 'string' ? err.error : null) ||
      'Something went wrong.'
    );
  }
}
