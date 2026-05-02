import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  PaymentResponse,
  PaymentStatus
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

type PaymentFilter = 'ALL' | 'OPEN' | 'SUCCEEDED' | 'ACTION_NEEDED' | 'REFUNDED';

@Component({
  selector: 'app-investor-my-payments-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <header class="hero">
        <div class="hero__content">
          <span class="eyebrow">Transaction center</span>
          <h1>My payments</h1>
          <p>
            Continue Stripe checkout, verify provider status, and review the payment records
            connected to your investment pledges.
          </p>
        </div>

        <div class="hero__actions">
          <a routerLink="/investor/my-pledges" class="ghost-btn">Open pledges</a>
          <button type="button" class="primary-btn" (click)="load()" [disabled]="loading()">
            {{ loading() ? 'Refreshing...' : 'Refresh' }}
          </button>
        </div>
      </header>

      <p class="success" *ngIf="success()">{{ success() }}</p>
      <p class="error" *ngIf="error()">{{ error() }}</p>

      <section class="summary-grid" *ngIf="!loading() || payments().length">
        <article class="summary-card summary-card--main">
          <small>Paid total</small>
          <strong>{{ formatMoney(totalSucceeded(), displayCurrency()) }}</strong>
          <span>{{ succeededCount() }} confirmed transaction{{ succeededCount() === 1 ? '' : 's' }}</span>
        </article>

        <article class="summary-card">
          <small>Awaiting checkout</small>
          <strong>{{ openCount() }}</strong>
          <span>Created or pending provider</span>
        </article>

        <article class="summary-card">
          <small>Action needed</small>
          <strong>{{ actionNeededCount() }}</strong>
          <span>Failed or canceled payments</span>
        </article>

        <article class="summary-card">
          <small>Latest update</small>
          <strong>{{ latestUpdateLabel() }}</strong>
          <span>Based on your most recent payment</span>
        </article>
      </section>

      <section class="toolbar">
        <div class="search-box">
          <span>⌕</span>
          <input
            type="search"
            placeholder="Search by campaign, provider, reference, session..."
            [value]="searchTerm()"
            (input)="setSearchTerm($event)"
          />
        </div>

        <div class="filters" aria-label="Payment filters">
          <button
            *ngFor="let item of filterOptions"
            type="button"
            [class.active]="selectedFilter() === item.key"
            (click)="selectedFilter.set(item.key)"
          >
            {{ item.label }}
          </button>
        </div>
      </section>

      <div class="state-card" *ngIf="loading()">Loading payments...</div>

      <div class="empty" *ngIf="!loading() && !filteredPayments().length">
        <div class="empty__icon">▣</div>
        <h2>No payments found</h2>
        <p>
          Create a pledge from a campaign page first. Once Stripe Checkout is created,
          the payment will appear here.
        </p>
        <a routerLink="/investor">Discover campaigns</a>
      </div>

      <div class="payment-list" *ngIf="!loading() && filteredPayments().length">
        <article class="payment-card" *ngFor="let payment of filteredPayments()">
          <div class="payment-card__rail" [ngClass]="statusClass(payment.status)"></div>

          <div class="payment-card__main">
            <div class="payment-card__top">
              <div>
                <div class="chip-row">
                  <span class="provider-chip">{{ payment.provider }}</span>
                  <span class="small-chip">Pledge #{{ payment.pledgeId }}</span>
                </div>
                <h2>{{ payment.campaignBusinessName }}</h2>
              </div>

              <span class="status-chip" [ngClass]="statusClass(payment.status)">
                {{ paymentStatusLabel(payment.status) }}
              </span>
            </div>

            <div class="amount-panel">
              <div>
                <small>Amount</small>
                <strong>{{ formatMoney(payment.amount, payment.currency) }}</strong>
              </div>

              <div>
                <small>Pledge status</small>
                <strong>{{ formatEnumLabel(payment.pledgeStatus) }}</strong>
              </div>

              <div>
                <small>Created</small>
                <strong>{{ payment.createdAt | date:'mediumDate' }}</strong>
              </div>
            </div>

            <div class="timeline">
              <div class="timeline__item" [class.done]="true">
                <span></span>
                <div>
                  <strong>Payment record created</strong>
                  <small>{{ payment.createdAt | date:'short' }}</small>
                </div>
              </div>

              <div class="timeline__item" [class.done]="payment.checkoutSessionId">
                <span></span>
                <div>
                  <strong>Stripe Checkout session</strong>
                  <small>{{ payment.checkoutSessionId ? 'Created' : 'Waiting for checkout session' }}</small>
                </div>
              </div>

              <div class="timeline__item" [class.done]="payment.status === paymentStatus.SUCCEEDED">
                <span></span>
                <div>
                  <strong>Provider confirmation</strong>
                  <small>
                    {{ payment.paidAt ? (payment.paidAt | date:'short') : paymentStatusLabel(payment.status) }}
                  </small>
                </div>
              </div>
            </div>

            <details class="technical-details">
              <summary>Transaction details</summary>

              <div class="details-grid">
                <p>
                  <span>Payment ID</span>
                  <strong>#{{ payment.id }}</strong>
                </p>
                <p>
                  <span>Provider reference</span>
                  <strong>{{ payment.providerReference || '—' }}</strong>
                </p>
                <p>
                  <span>Checkout session</span>
                  <strong>{{ payment.checkoutSessionId || '—' }}</strong>
                </p>
                <p>
                  <span>Paid at</span>
                  <strong>{{ payment.paidAt ? (payment.paidAt | date:'medium') : '—' }}</strong>
                </p>
                <p *ngIf="payment.refundedAt">
                  <span>Refunded at</span>
                  <strong>{{ payment.refundedAt | date:'medium' }}</strong>
                </p>
                <p *ngIf="payment.failureReason" class="detail-error">
                  <span>Failure reason</span>
                  <strong>{{ payment.failureReason }}</strong>
                </p>
              </div>
            </details>

            <div class="actions">
              <a class="primary-action" [routerLink]="['/investor/campaigns', payment.applicationRaiseId]">
                Open campaign
              </a>

              <button
                type="button"
                class="secondary-action"
                (click)="openStripeCheckout(payment)"
                [disabled]="busyPaymentId() === payment.id || !canUseCheckout(payment)"
              >
                {{ busyPaymentId() === payment.id ? 'Opening...' : 'Continue checkout' }}
              </button>

              <button
                type="button"
                class="sync-action"
                (click)="syncStripePayment(payment.id)"
                [disabled]="busyPaymentId() === payment.id || payment.status === paymentStatus.SUCCEEDED"
              >
                {{ busyPaymentId() === payment.id ? 'Syncing...' : 'Sync Stripe status' }}
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>
  `,
  styles: [`
    .page {
      display: grid;
      gap: 22px;
      color: #06152e;
    }

    .hero {
      position: relative;
      overflow: hidden;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 22px;
      padding: 26px;
      border-radius: 30px;
      background:
        radial-gradient(circle at 92% 8%, rgba(109, 183, 255, 0.32), transparent 31%),
        radial-gradient(circle at 0% 100%, rgba(243, 223, 152, 0.24), transparent 34%),
        linear-gradient(135deg, #071a3a 0%, #0a2d57 52%, #06152e 100%);
      color: #ffffff;
      box-shadow: 0 24px 56px rgba(7, 26, 58, 0.18);
    }

    .hero::after {
      content: '';
      position: absolute;
      width: 220px;
      height: 220px;
      right: -88px;
      bottom: -104px;
      border-radius: 999px;
      background: rgba(243, 223, 152, 0.12);
      pointer-events: none;
    }

    .hero__content,
    .hero__actions {
      position: relative;
      z-index: 1;
    }

    .eyebrow {
      display: inline-flex;
      align-items: center;
      min-height: 28px;
      padding: 0 10px;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.12);
      color: #f3df98;
      font-size: 0.72rem;
      font-weight: 950;
      letter-spacing: 0.13em;
      text-transform: uppercase;
    }

    h1 {
      margin: 12px 0 8px;
      font-size: clamp(2rem, 4vw, 3.4rem);
      line-height: 0.95;
      letter-spacing: -0.06em;
    }

    .hero p {
      margin: 0;
      max-width: 720px;
      color: rgba(234, 246, 255, 0.78);
      line-height: 1.65;
      font-weight: 650;
    }

    .hero__actions {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      justify-content: flex-end;
    }

    .primary-btn,
    .ghost-btn,
    .primary-action,
    .secondary-action,
    .sync-action,
    .empty a {
      min-height: 44px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 0 16px;
      border: 0;
      border-radius: 15px;
      cursor: pointer;
      text-decoration: none;
      font: inherit;
      font-size: 0.88rem;
      font-weight: 900;
      transition: 0.18s ease;
    }

    .primary-btn,
    .primary-action,
    .empty a {
      background: #f3df98;
      color: #06152e;
      box-shadow: 0 14px 28px rgba(0, 0, 0, 0.16);
    }

    .ghost-btn {
      background: rgba(255, 255, 255, 0.1);
      color: #ffffff;
      border: 1px solid rgba(255, 255, 255, 0.14);
    }

    .secondary-action {
      background: #eef4ff;
      color: #071a3a;
      border: 1px solid rgba(7, 26, 58, 0.08);
    }

    .sync-action {
      background: #fff5d7;
      color: #73580f;
      border: 1px solid rgba(243, 223, 152, 0.45);
    }

    .primary-btn:hover,
    .ghost-btn:hover,
    .primary-action:hover,
    .secondary-action:hover,
    .sync-action:hover,
    .empty a:hover {
      transform: translateY(-1px);
    }

    button:disabled {
      opacity: 0.65;
      cursor: not-allowed;
      transform: none !important;
    }

    .success,
    .error {
      margin: 0;
      padding: 14px 16px;
      border-radius: 18px;
      font-weight: 850;
      border: 1px solid transparent;
    }

    .success {
      background: #e8f7ef;
      color: #1f7a43;
      border-color: rgba(31, 122, 67, 0.12);
    }

    .error {
      background: #fdecec;
      color: #b03a2e;
      border-color: rgba(176, 58, 46, 0.12);
    }

    .summary-grid {
      display: grid;
      gap: 14px;
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }

    .summary-card,
    .state-card,
    .empty,
    .toolbar,
    .payment-card {
      background: rgba(255, 255, 255, 0.95);
      border: 1px solid rgba(7, 26, 58, 0.08);
      box-shadow: 0 18px 42px rgba(7, 26, 58, 0.08);
    }

    .summary-card {
      min-height: 116px;
      display: grid;
      align-content: center;
      gap: 6px;
      padding: 18px;
      border-radius: 24px;
    }

    .summary-card--main {
      background:
        radial-gradient(circle at 100% 0%, rgba(109, 183, 255, 0.22), transparent 40%),
        #ffffff;
    }

    .summary-card small,
    .amount-panel small,
    .details-grid span {
      color: #6c7a89;
      font-size: 0.72rem;
      font-weight: 900;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .summary-card strong {
      color: #071a3a;
      font-size: clamp(1.35rem, 2.2vw, 2rem);
      letter-spacing: -0.04em;
    }

    .summary-card span {
      color: #64748b;
      font-size: 0.84rem;
      font-weight: 700;
    }

    .toolbar {
      display: grid;
      gap: 14px;
      padding: 14px;
      border-radius: 24px;
    }

    .search-box {
      min-height: 48px;
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 0 14px;
      border-radius: 16px;
      background: #f7fbff;
      border: 1px solid rgba(7, 26, 58, 0.08);
    }

    .search-box span {
      color: #3b82f6;
      font-weight: 950;
    }

    .search-box input {
      width: 100%;
      border: 0;
      outline: 0;
      background: transparent;
      color: #06152e;
      font: inherit;
      font-weight: 750;
    }

    .filters {
      display: flex;
      gap: 9px;
      flex-wrap: wrap;
    }

    .filters button {
      min-height: 38px;
      padding: 0 13px;
      border: 1px solid rgba(7, 26, 58, 0.08);
      border-radius: 999px;
      background: #eef4ff;
      color: #071a3a;
      cursor: pointer;
      font: inherit;
      font-size: 0.82rem;
      font-weight: 900;
    }

    .filters button.active {
      background: #071a3a;
      color: #ffffff;
      border-color: #071a3a;
    }

    .state-card,
    .empty {
      border-radius: 26px;
      padding: 22px;
      color: #334155;
      font-weight: 800;
    }

    .empty {
      display: grid;
      justify-items: center;
      gap: 12px;
      text-align: center;
      padding: 42px 22px;
    }

    .empty__icon {
      width: 64px;
      height: 64px;
      display: grid;
      place-items: center;
      border-radius: 22px;
      background: linear-gradient(135deg, #eaf4ff, #fff5d7);
      color: #071a3a;
      font-size: 2rem;
      font-weight: 950;
    }

    .empty h2,
    .empty p {
      margin: 0;
    }

    .empty p {
      max-width: 560px;
      color: #64748b;
      line-height: 1.6;
    }

    .payment-list {
      display: grid;
      gap: 16px;
    }

    .payment-card {
      position: relative;
      overflow: hidden;
      display: grid;
      grid-template-columns: 7px 1fr;
      border-radius: 26px;
    }

    .payment-card__rail {
      width: 7px;
      background: #94a3b8;
    }

    .payment-card__rail.created,
    .payment-card__rail.pending_provider { background: #f3df98; }
    .payment-card__rail.succeeded { background: #22c55e; }
    .payment-card__rail.failed,
    .payment-card__rail.canceled { background: #ef4444; }
    .payment-card__rail.refunded { background: #8b5cf6; }

    .payment-card__main {
      display: grid;
      gap: 16px;
      padding: 20px;
    }

    .payment-card__top {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 18px;
    }

    .chip-row {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .payment-card h2 {
      margin: 10px 0 0;
      color: #06152e;
      font-size: 1.25rem;
      letter-spacing: -0.035em;
    }

    .provider-chip,
    .small-chip,
    .status-chip {
      display: inline-flex;
      align-items: center;
      min-height: 30px;
      padding: 0 10px;
      border-radius: 999px;
      font-size: 0.74rem;
      font-weight: 950;
      letter-spacing: 0.02em;
    }

    .provider-chip {
      background: #eaf4ff;
      color: #071a3a;
    }

    .small-chip {
      background: #f8fafc;
      color: #64748b;
      border: 1px solid rgba(7, 26, 58, 0.08);
    }

    .status-chip {
      white-space: nowrap;
      background: #eef3f5;
      color: #455a64;
    }

    .status-chip.created,
    .status-chip.pending_provider { background: #fff5d7; color: #73580f; }
    .status-chip.succeeded { background: #e8f7ef; color: #1f7a43; }
    .status-chip.failed,
    .status-chip.canceled { background: #fdecec; color: #b03a2e; }
    .status-chip.refunded { background: #f1edff; color: #5b35b1; }

    .amount-panel {
      display: grid;
      gap: 12px;
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .amount-panel div,
    .details-grid p {
      display: grid;
      gap: 5px;
      margin: 0;
      padding: 14px;
      border-radius: 18px;
      background: #f8fbff;
      border: 1px solid rgba(7, 26, 58, 0.06);
    }

    .amount-panel strong,
    .details-grid strong {
      min-width: 0;
      overflow-wrap: anywhere;
      color: #071a3a;
      font-size: 1rem;
    }

    .timeline {
      display: grid;
      gap: 10px;
      padding: 14px;
      border-radius: 20px;
      background: linear-gradient(135deg, #f7fbff, #fffaf0);
      border: 1px solid rgba(7, 26, 58, 0.06);
    }

    .timeline__item {
      display: grid;
      grid-template-columns: 28px 1fr;
      gap: 10px;
      align-items: start;
      color: #64748b;
    }

    .timeline__item > span {
      width: 18px;
      height: 18px;
      margin-top: 2px;
      border-radius: 999px;
      border: 3px solid #cbd5e1;
      background: #ffffff;
    }

    .timeline__item.done > span {
      border-color: #0a84ff;
      background: #0a84ff;
      box-shadow: 0 0 0 5px rgba(10, 132, 255, 0.1);
    }

    .timeline__item strong {
      display: block;
      color: #071a3a;
      font-size: 0.9rem;
    }

    .timeline__item small {
      color: #64748b;
      font-weight: 750;
    }

    .technical-details {
      border-radius: 18px;
      background: #f8fafc;
      border: 1px solid rgba(7, 26, 58, 0.08);
    }

    .technical-details summary {
      min-height: 44px;
      display: flex;
      align-items: center;
      padding: 0 14px;
      cursor: pointer;
      color: #071a3a;
      font-weight: 900;
    }

    .details-grid {
      display: grid;
      gap: 10px;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      padding: 0 14px 14px;
    }

    .details-grid .detail-error {
      background: #fff5f5;
    }

    .details-grid .detail-error strong {
      color: #b03a2e;
    }

    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
    }

    @media (max-width: 1100px) {
      .summary-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (max-width: 760px) {
      .hero,
      .payment-card__top {
        flex-direction: column;
      }

      .hero__actions {
        justify-content: flex-start;
      }

      .summary-grid,
      .amount-panel,
      .details-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class InvestorMyPaymentsPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);
  private readonly route = inject(ActivatedRoute);

  readonly paymentStatus = PaymentStatus;
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly searchTerm = signal('');
  readonly busyPaymentId = signal<number | null>(null);
  readonly payments = signal<PaymentResponse[]>([]);
  readonly selectedFilter = signal<PaymentFilter>('ALL');

  readonly filterOptions: Array<{ key: PaymentFilter; label: string }> = [
    { key: 'ALL', label: 'All' },
    { key: 'OPEN', label: 'Open checkout' },
    { key: 'SUCCEEDED', label: 'Paid' },
    { key: 'ACTION_NEEDED', label: 'Action needed' },
    { key: 'REFUNDED', label: 'Refunded' }
  ];

  readonly filteredPayments = computed(() => {
    const selected = this.selectedFilter();
    const search = this.searchTerm().trim().toLowerCase();

    return this.payments().filter((payment) => {
      const matchesFilter =
        selected === 'ALL' ||
        (selected === 'OPEN' &&
          [PaymentStatus.CREATED, PaymentStatus.PENDING_PROVIDER].includes(payment.status)) ||
        (selected === 'SUCCEEDED' && payment.status === PaymentStatus.SUCCEEDED) ||
        (selected === 'ACTION_NEEDED' &&
          [PaymentStatus.FAILED, PaymentStatus.CANCELED].includes(payment.status)) ||
        (selected === 'REFUNDED' && payment.status === PaymentStatus.REFUNDED);

      if (!matchesFilter) {
        return false;
      }

      if (!search) {
        return true;
      }

      return [
        payment.campaignBusinessName,
        payment.provider,
        payment.status,
        payment.pledgeStatus,
        payment.currency,
        payment.providerReference ?? '',
        payment.checkoutSessionId ?? '',
        payment.failureReason ?? ''
      ]
        .join(' ')
        .toLowerCase()
        .includes(search);
    });
  });

  readonly totalSucceeded = computed(() =>
    this.payments()
      .filter((payment) => payment.status === PaymentStatus.SUCCEEDED)
      .reduce((sum, payment) => sum + (Number(payment.amount) || 0), 0)
  );

  readonly succeededCount = computed(() =>
    this.payments().filter((payment) => payment.status === PaymentStatus.SUCCEEDED).length
  );

  readonly openCount = computed(() =>
    this.payments().filter((payment) =>
      [PaymentStatus.CREATED, PaymentStatus.PENDING_PROVIDER].includes(payment.status)
    ).length
  );

  readonly actionNeededCount = computed(() =>
    this.payments().filter((payment) =>
      [PaymentStatus.FAILED, PaymentStatus.CANCELED].includes(payment.status)
    ).length
  );

  readonly displayCurrency = computed(() => this.payments()[0]?.currency ?? 'TND');

  readonly latestUpdateLabel = computed(() => {
    const latest = [...this.payments()].sort((a, b) =>
      new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime()
    )[0];

    return latest ? this.paymentStatusLabel(latest.status) : '—';
  });

  ngOnInit(): void {
    const stripe = this.route.snapshot.queryParamMap.get('stripe');
    const paymentIdRaw = this.route.snapshot.queryParamMap.get('paymentId');
    const paymentId = Number(paymentIdRaw);

    if (stripe === 'success' && Number.isFinite(paymentId) && paymentId > 0) {
      this.syncStripePayment(paymentId);
      return;
    }

    if (stripe === 'cancel') {
      this.error.set('Stripe checkout was canceled. You can continue checkout again.');
    }

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

  openStripeCheckout(payment: PaymentResponse): void {
    if (!payment.checkoutUrl) {
      this.error.set('No Stripe checkout URL is available for this payment. Try creating a new payment from the pledge.');
      return;
    }

    this.busyPaymentId.set(payment.id);
    window.location.href = payment.checkoutUrl;
  }

  syncStripePayment(paymentId: number): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.busyPaymentId.set(paymentId);

    this.service
      .syncMyStripePayment(paymentId)
      .pipe(
        finalize(() => {
          this.loading.set(false);
          this.busyPaymentId.set(null);
        })
      )
      .subscribe({
        next: (payment) => {
          this.mergePayment(payment);
          this.success.set(
            payment.status === PaymentStatus.SUCCEEDED
              ? 'Stripe payment confirmed. Your payment is now marked as paid.'
              : `Stripe payment synced. Current status: ${this.paymentStatusLabel(payment.status)}.`
          );
          this.load();
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
          this.load();
        }
      });
  }

  canUseCheckout(payment: PaymentResponse): boolean {
    return (
      [PaymentStatus.CREATED, PaymentStatus.PENDING_PROVIDER, PaymentStatus.FAILED, PaymentStatus.CANCELED].includes(payment.status) &&
      !!payment.checkoutUrl
    );
  }

  setSearchTerm(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    this.searchTerm.set(input?.value ?? '');
  }

  statusClass(status: PaymentStatus): string {
    return status.toLowerCase();
  }

  paymentStatusLabel(status: PaymentStatus): string {
    switch (status) {
      case PaymentStatus.CREATED:
        return 'Created';
      case PaymentStatus.PENDING_PROVIDER:
        return 'Awaiting Stripe';
      case PaymentStatus.SUCCEEDED:
        return 'Paid';
      case PaymentStatus.FAILED:
        return 'Failed';
      case PaymentStatus.CANCELED:
        return 'Canceled';
      case PaymentStatus.REFUNDED:
        return 'Refunded';
      default:
        return this.formatEnumLabel(status);
    }
  }

  formatEnumLabel(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }

    return value
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  formatMoney(amount: number | null | undefined, currency: string | null | undefined): string {
    if (amount === null || amount === undefined || Number.isNaN(Number(amount))) {
      return '—';
    }

    return `${Number(amount).toLocaleString('en-US', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    })} ${currency || 'TND'}`;
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
