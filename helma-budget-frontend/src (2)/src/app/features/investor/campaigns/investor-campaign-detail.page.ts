import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import {
  CampaignResponse,
  CrowdfundingType,
  PaymentResponse,
  PaymentStatus,
  PledgeResponse,
  PledgeStatus
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

@Component({
  selector: 'app-investor-campaign-detail-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page">
      <div class="header">
        <div>
          <a class="back" routerLink="/investor">← Back to campaigns</a>
          <h1>Campaign details</h1>
          <p *ngIf="campaign()">{{ campaign()!.businessName }} · {{ campaign()!.type }}</p>
        </div>

        <div class="header-links">
          <a class="secondary-link" routerLink="/investor/my-pledges">My pledges</a>
          <a class="secondary-link" routerLink="/investor/my-payments">My payments</a>
        </div>
      </div>

      <p class="success" *ngIf="success()">{{ success() }}</p>
      <p class="error" *ngIf="error()">{{ error() }}</p>
      <div class="state-card" *ngIf="loading()">Loading campaign...</div>

      <div class="layout" *ngIf="!loading() && campaign() as campaign">
        <section class="left">
          <article class="card">
            <div class="top">
              <div>
                <div class="pill type">{{ campaign.type }}</div>
                <h2>{{ campaign.businessName }}</h2>
              </div>

              <div class="pill progress">{{ fundingPercent(campaign) }}%</div>
            </div>

            <div class="details">
              <p><strong>Sector:</strong> {{ campaign.sector ? formatEnumLabel(campaign.sector) : '—' }}</p>
              <p><strong>Sub-sector:</strong> {{ campaign.subSector ? formatEnumLabel(campaign.subSector) : '—' }}</p>
              <p><strong>Tags:</strong> {{ formatTagList(campaign.tags) }}</p>
              <p><strong>Website:</strong> {{ campaign.website || '—' }}</p>
              <p><strong>Funding goal:</strong> {{ formatMoney(campaign.fundingGoal, campaign.currency) }}</p>
              <p><strong>Raised:</strong> {{ formatMoney(campaign.investorsPledgedAmount, campaign.currency) }}</p>
              <p *ngIf="campaign.type === crowdfundingType.EQUITY">
                <strong>Minimum investment:</strong> {{ formatMoney(campaign.minInvestment, campaign.currency) }}
              </p>
              <p *ngIf="campaign.type === crowdfundingType.EQUITY">
                <strong>Equity offered:</strong> {{ campaign.equityOfferedPercent ?? '—' }}
              </p>
              <p *ngIf="campaign.type === crowdfundingType.EQUITY">
                <strong>Pre-money valuation:</strong> {{ campaign.preMoneyValuation ?? '—' }}
              </p>
            </div>

            <div class="summary">
              <h3>Summary</h3>
              <p>{{ campaign.summary || 'No summary provided.' }}</p>
            </div>
          </article>
        </section>

        <section class="right">
          <article class="card">
            <div class="pledge-header">
              <h3>Your pledge</h3>
              <span class="status-badge" *ngIf="existingPledge()">
                {{ existingPledge()!.status }}
              </span>
            </div>

            <p class="hint" *ngIf="campaign.type === crowdfundingType.EQUITY && campaign.minInvestment">
              Minimum investment for this equity campaign is
              {{ formatMoney(campaign.minInvestment, campaign.currency) }}.
            </p>

            <p class="hint" *ngIf="existingPledge() && existingPledge()!.status === pledgeStatus.PAID">
              This pledge is already PAID and cannot be edited anymore.
            </p>

            <form [formGroup]="form" (ngSubmit)="savePledge()" class="pledge-form">
              <div class="field">
                <label>Amount *</label>
                <input type="number" step="0.001" formControlName="amount" [disabled]="isPaidPledge()" />
                <small *ngIf="showError('amount')">
                  Enter a valid amount.
                </small>
              </div>

              <div class="field">
                <label>Message</label>
                <textarea rows="4" formControlName="message" [disabled]="isPaidPledge()"></textarea>
                <small *ngIf="showError('message')">
                  Message must be at most 500 characters.
                </small>
              </div>

              <button type="submit" [disabled]="saving() || isPaidPledge()">
                {{ saving() ? 'Saving...' : existingPledge() ? 'Update pledge' : 'Create pledge' }}
              </button>
            </form>
          </article>

          <article class="card" *ngIf="existingPledge() as pledge">
            <div class="pledge-header">
              <h3>Payment</h3>
              <span class="status-badge payment" *ngIf="latestPayment()" [class]="latestPayment()!.status.toLowerCase()">
                {{ formatEnumLabel(latestPayment()!.status) }}
              </span>
            </div>

            <div class="meta">
              <p><strong>Pledge amount:</strong> {{ pledge.amount }} {{ pledge.currency }}</p>
              <p><strong>Pledge status:</strong> {{ formatEnumLabel(pledge.status) }}</p>
              <ng-container *ngIf="latestPayment() as payment">
                <p><strong>Provider:</strong> {{ payment.provider }}</p>
                <p><strong>Reference:</strong> {{ payment.providerReference || '—' }}</p>
                <p><strong>Checkout session:</strong> {{ payment.checkoutSessionId || '—' }}</p>
                <p><strong>Created:</strong> {{ payment.createdAt | date:'medium' }}</p>
                <p *ngIf="payment.paidAt"><strong>Paid:</strong> {{ payment.paidAt | date:'medium' }}</p>
                <p *ngIf="payment.failureReason"><strong>Reason:</strong> {{ payment.failureReason }}</p>
              </ng-container>
            </div>

            <p class="hint" *ngIf="!latestPayment()">
              No payment session exists yet. Save your pledge, then initiate the mock payment flow.
            </p>

            <div class="actions">
              <button
                type="button"
                (click)="initiatePayment()"
                [disabled]="processingPayment() || isPaidPledge() || !existingPledge()"
              >
                {{ processingPayment() ? 'Working...' : latestPayment() ? 'Re-open payment' : 'Initiate payment' }}
              </button>

              <button
                type="button"
                class="secondary"
                *ngIf="latestPayment()"
                (click)="openMockCheckout()"
                [disabled]="processingPayment() || !canUseCheckout(latestPayment())"
              >
                Mock checkout
              </button>

              <a class="secondary-link inline" routerLink="/investor/my-payments">Open payments board</a>
            </div>

            <div class="actions" *ngIf="latestPayment() && canResolve(latestPayment()!)">
              <button
                type="button"
                (click)="patchLatestPayment(paymentStatus.SUCCEEDED)"
                [disabled]="processingPayment()"
              >
                Mark paid
              </button>

              <button
                type="button"
                class="warn"
                (click)="patchLatestPayment(paymentStatus.FAILED, 'Card declined')"
                [disabled]="processingPayment()"
              >
                Mark failed
              </button>

              <button
                type="button"
                class="secondary"
                (click)="patchLatestPayment(paymentStatus.CANCELED, 'Customer canceled checkout')"
                [disabled]="processingPayment()"
              >
                Cancel
              </button>
            </div>

            <div class="actions" *ngIf="latestPayment()?.status === paymentStatus.SUCCEEDED">
              <button
                type="button"
                class="warn"
                (click)="patchLatestPayment(paymentStatus.REFUNDED, 'Refund requested by investor')"
                [disabled]="processingPayment()"
              >
                Refund
              </button>
            </div>
          </article>
        </section>
      </div>
    </section>
  `,
  styles: [`
    .page {
      display: grid;
      gap: 20px;
    }

    .header {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
    }

    .header-links,
    .actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }

    .back,
    .secondary-link {
      color: #0b3b3c;
      text-decoration: none;
      font-weight: 600;
    }

    .secondary-link {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 40px;
      padding: 0 14px;
      border-radius: 10px;
      background: #eef3f5;
      color: #062a2b;
    }

    .secondary-link.inline {
      min-height: 42px;
    }

    .header h1 {
      margin: 8px 0 0;
      color: #062a2b;
    }

    .header p {
      margin: 8px 0 0;
      color: #5c6b73;
    }

    .layout {
      display: grid;
      grid-template-columns: 1.15fr 420px;
      gap: 20px;
      align-items: start;
    }

    .card,
    .state-card {
      background: white;
      border-radius: 18px;
      padding: 20px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.06);
    }

    .right {
      display: grid;
      gap: 20px;
    }

    .top {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
      margin-bottom: 16px;
    }

    .card h2,
    .card h3 {
      margin: 0;
      color: #062a2b;
    }

    .details,
    .summary,
    .pledge-form,
    .meta {
      display: grid;
      gap: 12px;
    }

    .details p,
    .summary p,
    .hint,
    .meta p {
      margin: 0;
      color: #33444d;
    }

    .field {
      display: grid;
      gap: 8px;
    }

    label {
      font-weight: 600;
      color: #062a2b;
    }

    input,
    textarea {
      width: 100%;
      border: 1px solid #d8dfe3;
      border-radius: 10px;
      padding: 12px;
      font: inherit;
      background: white;
    }

    textarea {
      resize: vertical;
    }

    .pledge-form button,
    .actions button {
      min-height: 44px;
      border: 0;
      border-radius: 10px;
      background: #062a2b;
      color: white;
      font-weight: 700;
      cursor: pointer;
      font: inherit;
      padding: 0 14px;
    }

    .actions .secondary {
      background: #eef3f5;
      color: #062a2b;
    }

    .actions .warn {
      background: #b03a2e;
      color: white;
    }

    .pledge-form button:disabled,
    .actions button:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }

    .pill,
    .status-badge {
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

    .progress,
    .status-badge {
      background: #eef3f5;
      color: #455a64;
    }

    .status-badge.payment.pending_provider,
    .status-badge.payment.created {
      background: #fff5d7;
      color: #8a6d1d;
    }

    .status-badge.payment.succeeded {
      background: #e8f7ef;
      color: #1f7a43;
    }

    .status-badge.payment.failed,
    .status-badge.payment.canceled,
    .status-badge.payment.refunded {
      background: #fdecec;
      color: #b03a2e;
    }

    .pledge-header {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: center;
      margin-bottom: 12px;
    }

    .success {
      color: #1f7a43;
      margin: 0;
      font-weight: 600;
    }

    .error,
    small {
      color: #c0392b;
      margin: 0;
    }

    @media (max-width: 1100px) {
      .layout {
        grid-template-columns: 1fr;
      }

      .header {
        flex-direction: column;
      }
    }
  `]
})
export class InvestorCampaignDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CrowdfundingService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly crowdfundingType = CrowdfundingType;
  readonly pledgeStatus = PledgeStatus;
  readonly paymentStatus = PaymentStatus;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly processingPayment = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly campaign = signal<CampaignResponse | null>(null);
  readonly existingPledge = signal<PledgeResponse | null>(null);
  readonly latestPayment = signal<PaymentResponse | null>(null);

  private campaignId: number | null = null;

  readonly form = this.fb.group({
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    message: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const id = Number(params.get('id'));
        if (Number.isFinite(id)) {
          this.campaignId = id;
          this.load(id);
        }
      });
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    forkJoin({
      campaign: this.service.getApprovedCampaign(id),
      pledges: this.service.listMyPledges(),
      payments: this.service.listMyPayments()
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ campaign, pledges, payments }) => {
          this.campaign.set(campaign);
          this.applyAmountValidator(campaign.minInvestment);

          const pledge =
            pledges.find((item) => item.applicationRaiseId === id) ?? null;
          this.existingPledge.set(pledge);

          if (pledge) {
            this.form.patchValue({
              amount: pledge.amount,
              message: pledge.message ?? ''
            });

            const latest =
              payments.find((item) => item.pledgeId === pledge.id) ?? null;
            this.latestPayment.set(latest);
          } else {
            this.form.patchValue({
              amount: null,
              message: ''
            });
            this.latestPayment.set(null);
          }
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  savePledge(): void {
    if (!this.campaignId) return;

    if (this.isPaidPledge()) {
      this.error.set('Paid pledges cannot be edited.');
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    const raw = this.form.getRawValue();

    this.service
      .createOrUpdateMyPledge(this.campaignId, {
        amount: raw.amount,
        message: raw.message?.trim() ? raw.message.trim() : null
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (pledge) => {
          this.existingPledge.set(pledge);
          this.latestPayment.set(null);
          this.success.set('Pledge saved successfully. You can now initiate payment.');
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  initiatePayment(): void {
    const pledge = this.existingPledge();
    if (!pledge) {
      this.error.set('Create your pledge first.');
      return;
    }

    if (this.isPaidPledge()) {
      this.error.set('This pledge is already paid.');
      return;
    }

    this.processingPayment.set(true);
    this.error.set('');
    this.success.set('');

    this.service
      .initiateMyPayment(pledge.id)
      .pipe(finalize(() => this.processingPayment.set(false)))
      .subscribe({
        next: (payment) => {
          this.latestPayment.set(payment);
          this.success.set('Payment session ready. Open mock checkout or simulate a result below.');
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  openMockCheckout(): void {
    const payment = this.latestPayment();
    if (!payment) return;

    this.processingPayment.set(true);
    this.error.set('');
    this.success.set('');

    this.service
      .getMyPaymentMockCheckout(payment.id)
      .pipe(finalize(() => this.processingPayment.set(false)))
      .subscribe({
        next: (response) => this.success.set(response.message),
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err))
      });
  }

  patchLatestPayment(status: PaymentStatus, failureReason?: string): void {
    const payment = this.latestPayment();
    if (!payment) return;

    this.processingPayment.set(true);
    this.error.set('');
    this.success.set('');

    this.service
      .patchMyPaymentStatus(payment.id, {
        status,
        failureReason: failureReason ?? null
      })
      .pipe(finalize(() => this.processingPayment.set(false)))
      .subscribe({
        next: (updatedPayment) => {
          this.latestPayment.set(updatedPayment);

          const pledge = this.existingPledge();
          if (pledge) {
            this.existingPledge.set({
              ...pledge,
              status: updatedPayment.pledgeStatus
            });
          }

          this.success.set(`Payment ${this.formatEnumLabel(updatedPayment.status).toLowerCase()} successfully.`);
          if (this.campaignId) {
            this.service.getApprovedCampaign(this.campaignId).subscribe({
              next: (campaign) => this.campaign.set(campaign)
            });
          }
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  canUseCheckout(payment: PaymentResponse | null): boolean {
    return (
      !!payment &&
      (payment.status === PaymentStatus.CREATED ||
        payment.status === PaymentStatus.PENDING_PROVIDER)
    );
  }

  canResolve(payment: PaymentResponse): boolean {
    return this.canUseCheckout(payment);
  }

  isPaidPledge(): boolean {
    return this.existingPledge()?.status === PledgeStatus.PAID;
  }

  showError(controlName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  applyAmountValidator(minInvestment: number | null): void {
    const minAmount = Math.max(1, Number(minInvestment ?? 1));
    const control = this.form.get('amount');
    control?.setValidators([Validators.required, Validators.min(minAmount)]);
    control?.updateValueAndValidity({ emitEvent: false });
  }

  fundingPercent(campaign: CampaignResponse): number {
    const goal = Number(campaign.fundingGoal ?? 0);
    const raised = Number(campaign.investorsPledgedAmount ?? 0);
    if (!goal || goal <= 0) return 0;
    return Math.min(100, Math.round((raised / goal) * 100));
  }

  formatMoney(value: number | null | undefined, currency: string): string {
    if (value === null || value === undefined) return `— ${currency}`;
    return `${value} ${currency}`;
  }

  formatEnumLabel(value: string): string {
    return value
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  formatTagList(tags: string[] | null | undefined): string {
    if (!tags?.length) return '—';
    return tags.map((tag) => this.formatEnumLabel(tag)).join(', ');
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
