import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  PaymentResponse,
  PledgeResponse,
  PledgeStatus
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

type PledgeFilter = 'ALL' | 'OPEN' | 'PAID' | 'ACTION_NEEDED' | 'REFUNDED';

@Component({
  selector: 'app-investor-my-pledges-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <header class="hero">
        <div class="hero__content">
          <span class="eyebrow">Investor commitments</span>
          <h1>My pledges</h1>
          <p>
            Follow every campaign you committed to, complete pending Stripe payments,
            and separate confirmed investments from open commitments.
          </p>
        </div>

        <div class="hero__actions">
          <a routerLink="/investor" class="ghost-btn">Discover campaigns</a>
          <a routerLink="/investor/my-payments" class="primary-btn">Open payments</a>
        </div>
      </header>

      <p class="success" *ngIf="success()">{{ success() }}</p>
      <p class="error" *ngIf="error()">{{ error() }}</p>

      <section class="summary-grid" *ngIf="!loading() || pledges().length">
        <article class="summary-card summary-card--main">
          <small>Total pledged</small>
          <strong>{{ formatMoney(totalPledged(), displayCurrency()) }}</strong>
          <span>{{ pledges().length }} commitment{{ pledges().length === 1 ? '' : 's' }}</span>
        </article>

        <article class="summary-card">
          <small>Confirmed</small>
          <strong>{{ formatMoney(totalPaid(), displayCurrency()) }}</strong>
          <span>{{ paidCount() }} paid pledge{{ paidCount() === 1 ? '' : 's' }}</span>
        </article>

        <article class="summary-card">
          <small>Pending</small>
          <strong>{{ pendingCount() }}</strong>
          <span>Need checkout or provider confirmation</span>
        </article>

        <article class="summary-card">
          <small>Action needed</small>
          <strong>{{ actionNeededCount() }}</strong>
          <span>Failed or canceled commitments</span>
        </article>
      </section>

      <section class="toolbar">
        <div class="search-box">
          <span>⌕</span>
          <input
            type="search"
            placeholder="Search by campaign, type, or message..."
            [value]="searchTerm()"
            (input)="setSearchTerm($event)"
          />
        </div>

        <div class="filters" aria-label="Pledge filters">
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

      <div class="state-card" *ngIf="loading()">Loading your pledges...</div>

      <div class="empty" *ngIf="!loading() && !filteredPledges().length">
        <div class="empty__icon">◇</div>
        <h2>No pledges found</h2>
        <p>
          You do not have pledges matching this view yet. Discover approved opportunities
          and create your first investment commitment.
        </p>
        <a routerLink="/investor">Browse campaigns</a>
      </div>

      <div class="pledge-list" *ngIf="!loading() && filteredPledges().length">
        <article class="pledge-card" *ngFor="let pledge of filteredPledges()">
          <div class="pledge-card__rail" [ngClass]="statusClass(pledge.status)"></div>

          <div class="pledge-card__main">
            <div class="pledge-card__top">
              <div class="campaign-title">
                <span class="type-chip">{{ formatEnumLabel(pledge.campaignType) }}</span>
                <h2>{{ pledge.campaignBusinessName }}</h2>
              </div>

              <span class="status-chip" [ngClass]="statusClass(pledge.status)">
                {{ pledgeStatusLabel(pledge.status) }}
              </span>
            </div>

            <div class="money-row">
              <div>
                <small>Your pledge</small>
                <strong>{{ formatMoney(pledge.amount, pledge.currency) }}</strong>
              </div>

              <div>
                <small>Campaign goal</small>
                <strong>{{ formatMoney(pledge.campaignFundingGoal, pledge.currency) }}</strong>
              </div>

              <div>
                <small>Raised</small>
                <strong>{{ formatMoney(pledge.campaignInvestorsPledgedAmount, pledge.currency) }}</strong>
              </div>
            </div>

            <div class="progress-block" *ngIf="pledge.campaignFundingGoal">
              <div class="progress-block__labels">
                <span>Funding progress</span>
                <strong>{{ campaignProgressPct(pledge) | number:'1.0-0' }}%</strong>
              </div>
              <div class="progress-bar">
                <span [style.width.%]="campaignProgressPct(pledge)"></span>
              </div>
            </div>

            <div class="info-grid">
              <p *ngIf="pledge.campaignType === 'EQUITY'">
                <span>Minimum investment</span>
                <strong>{{ formatMoney(pledge.campaignMinInvestment, pledge.currency) }}</strong>
              </p>
              <p>
                <span>Created</span>
                <strong>{{ pledge.createdAt | date:'mediumDate' }}</strong>
              </p>
              <p>
                <span>Last update</span>
                <strong>{{ pledge.updatedAt | date:'short' }}</strong>
              </p>
            </div>

            <blockquote *ngIf="pledge.message">
              “{{ pledge.message }}”
            </blockquote>

            <div class="actions">
              <a class="primary-action" [routerLink]="['/investor/campaigns', pledge.applicationRaiseId]">
                Open campaign
              </a>

              <button
                type="button"
                class="secondary-action"
                *ngIf="canStartPayment(pledge)"
                (click)="startPayment(pledge)"
                [disabled]="busyPledgeId() === pledge.id"
              >
                {{ busyPledgeId() === pledge.id ? 'Opening Stripe...' : 'Complete payment' }}
              </button>

              <a class="secondary-action" routerLink="/investor/my-payments">
                View payments
              </a>
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

    .primary-btn:hover,
    .ghost-btn:hover,
    .primary-action:hover,
    .secondary-action:hover,
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
    .pledge-card {
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
    .money-row small,
    .info-grid span {
      color: #6c7a89;
      font-size: 0.72rem;
      font-weight: 900;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .summary-card strong {
      color: #071a3a;
      font-size: clamp(1.4rem, 2.4vw, 2rem);
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

    .pledge-list {
      display: grid;
      gap: 16px;
    }

    .pledge-card {
      position: relative;
      overflow: hidden;
      display: grid;
      grid-template-columns: 7px 1fr;
      border-radius: 26px;
    }

    .pledge-card__rail {
      width: 7px;
      background: #94a3b8;
    }

    .pledge-card__rail.pending { background: #f3df98; }
    .pledge-card__rail.paid { background: #22c55e; }
    .pledge-card__rail.failed,
    .pledge-card__rail.canceled { background: #ef4444; }
    .pledge-card__rail.refunded { background: #8b5cf6; }

    .pledge-card__main {
      display: grid;
      gap: 16px;
      padding: 20px;
    }

    .pledge-card__top {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 18px;
    }

    .campaign-title h2 {
      margin: 10px 0 0;
      color: #06152e;
      font-size: 1.25rem;
      letter-spacing: -0.035em;
    }

    .type-chip,
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

    .type-chip {
      background: #eaf4ff;
      color: #071a3a;
    }

    .status-chip {
      white-space: nowrap;
      background: #eef3f5;
      color: #455a64;
    }

    .status-chip.pending { background: #fff5d7; color: #73580f; }
    .status-chip.paid { background: #e8f7ef; color: #1f7a43; }
    .status-chip.failed,
    .status-chip.canceled { background: #fdecec; color: #b03a2e; }
    .status-chip.refunded { background: #f1edff; color: #5b35b1; }

    .money-row {
      display: grid;
      gap: 12px;
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .money-row div,
    .info-grid p {
      display: grid;
      gap: 5px;
      margin: 0;
      padding: 14px;
      border-radius: 18px;
      background: #f8fbff;
      border: 1px solid rgba(7, 26, 58, 0.06);
    }

    .money-row strong,
    .info-grid strong {
      color: #071a3a;
      font-size: 1rem;
    }

    .progress-block {
      display: grid;
      gap: 8px;
    }

    .progress-block__labels {
      display: flex;
      align-items: center;
      justify-content: space-between;
      color: #64748b;
      font-size: 0.83rem;
      font-weight: 850;
    }

    .progress-block__labels strong {
      color: #071a3a;
    }

    .progress-bar {
      height: 11px;
      overflow: hidden;
      border-radius: 999px;
      background: #eaf1fb;
    }

    .progress-bar span {
      display: block;
      height: 100%;
      border-radius: inherit;
      background: linear-gradient(90deg, #0a84ff, #f3df98);
    }

    .info-grid {
      display: grid;
      gap: 10px;
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    blockquote {
      margin: 0;
      padding: 14px 16px;
      border-left: 4px solid #f3df98;
      border-radius: 16px;
      background: #fffaf0;
      color: #5c4a19;
      font-weight: 750;
      line-height: 1.6;
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
      .page-header,
      .pledge-card__top {
        flex-direction: column;
      }

      .hero__actions {
        justify-content: flex-start;
      }

      .summary-grid,
      .money-row,
      .info-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class InvestorMyPledgesPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly searchTerm = signal('');
  readonly selectedFilter = signal<PledgeFilter>('ALL');
  readonly busyPledgeId = signal<number | null>(null);
  readonly pledges = signal<PledgeResponse[]>([]);

  readonly filterOptions: Array<{ key: PledgeFilter; label: string }> = [
    { key: 'ALL', label: 'All' },
    { key: 'OPEN', label: 'Open' },
    { key: 'PAID', label: 'Paid' },
    { key: 'ACTION_NEEDED', label: 'Action needed' },
    { key: 'REFUNDED', label: 'Refunded' }
  ];

  readonly filteredPledges = computed(() => {
    const filter = this.selectedFilter();
    const search = this.searchTerm().trim().toLowerCase();

    return this.pledges().filter((pledge) => {
      const matchesFilter =
        filter === 'ALL' ||
        (filter === 'OPEN' && pledge.status === PledgeStatus.PENDING) ||
        (filter === 'PAID' && pledge.status === PledgeStatus.PAID) ||
        (filter === 'ACTION_NEEDED' &&
          [PledgeStatus.FAILED, PledgeStatus.CANCELED].includes(pledge.status)) ||
        (filter === 'REFUNDED' && pledge.status === PledgeStatus.REFUNDED);

      if (!matchesFilter) {
        return false;
      }

      if (!search) {
        return true;
      }

      return [
        pledge.campaignBusinessName,
        pledge.campaignType,
        pledge.status,
        pledge.currency,
        pledge.message ?? ''
      ]
        .join(' ')
        .toLowerCase()
        .includes(search);
    });
  });

  readonly totalPledged = computed(() =>
    this.pledges().reduce((sum, pledge) => sum + (Number(pledge.amount) || 0), 0)
  );

  readonly totalPaid = computed(() =>
    this.pledges()
      .filter((pledge) => pledge.status === PledgeStatus.PAID)
      .reduce((sum, pledge) => sum + (Number(pledge.amount) || 0), 0)
  );

  readonly paidCount = computed(() =>
    this.pledges().filter((pledge) => pledge.status === PledgeStatus.PAID).length
  );

  readonly pendingCount = computed(() =>
    this.pledges().filter((pledge) => pledge.status === PledgeStatus.PENDING).length
  );

  readonly actionNeededCount = computed(() =>
    this.pledges().filter((pledge) =>
      [PledgeStatus.FAILED, PledgeStatus.CANCELED].includes(pledge.status)
    ).length
  );

  readonly displayCurrency = computed(() => this.pledges()[0]?.currency ?? 'TND');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.service
      .listMyPledges()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (pledges) => this.pledges.set(pledges),
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err))
      });
  }

  startPayment(pledge: PledgeResponse): void {
    this.error.set('');
    this.success.set('');
    this.busyPledgeId.set(pledge.id);

    this.service
      .initiateMyPayment(pledge.id)
      .pipe(finalize(() => this.busyPledgeId.set(null)))
      .subscribe({
        next: (payment: PaymentResponse) => {
          if (payment.checkoutUrl) {
            window.location.href = payment.checkoutUrl;
            return;
          }

          this.success.set('Payment was created. Open payments to continue checkout.');
        },
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err))
      });
  }

  canStartPayment(pledge: PledgeResponse): boolean {
    return pledge.status === PledgeStatus.PENDING || pledge.status === PledgeStatus.FAILED;
  }

  setSearchTerm(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    this.searchTerm.set(input?.value ?? '');
  }

  campaignProgressPct(pledge: PledgeResponse): number {
    const goal = Number(pledge.campaignFundingGoal) || 0;
    const raised = Number(pledge.campaignInvestorsPledgedAmount) || 0;

    if (goal <= 0) {
      return 0;
    }

    return Math.max(0, Math.min(100, (raised / goal) * 100));
  }

  statusClass(status: PledgeStatus): string {
    return status.toLowerCase();
  }

  pledgeStatusLabel(status: PledgeStatus): string {
    switch (status) {
      case PledgeStatus.PENDING:
        return 'Awaiting payment';
      case PledgeStatus.PAID:
        return 'Confirmed';
      case PledgeStatus.FAILED:
        return 'Failed';
      case PledgeStatus.CANCELED:
        return 'Canceled';
      case PledgeStatus.REFUNDED:
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
