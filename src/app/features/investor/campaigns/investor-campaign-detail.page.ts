import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import {
  CampaignPageResponse,
  CampaignStyleJson,
  CrowdfundingType,
  PaymentResponse,
  PaymentStatus,
  PledgeResponse,
  PledgeStatus,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';
import { CampaignPageRendererComponent } from '../../../shared/components/campaign-page-renderer.component';

interface StudioContentJson {
  version?: number;
  editor?: string;
  sections?: unknown[];
  blocks?: unknown[];
}

const DEFAULT_STYLE: CampaignStyleJson = {
  fontFamily: 'Inter',
  primaryColor: '#111827',
  accentColor: '#C9A227',
  radius: 'large',
  heroLayout: 'split',
  buttonStyle: 'pill',
};

@Component({
  selector: 'app-investor-campaign-detail-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CampaignPageRendererComponent],
  template: `
    <section class="investor-preview-page">
      <header class="topbar">
        <div>
          <a class="back-link" routerLink="/investor">← Discover campaigns</a>
          <span class="eyebrow">Investor preview</span>
          <h1>{{ campaign()?.title || campaign()?.businessName || 'Campaign preview' }}</h1>
          <p>
            This page uses the same campaign renderer as the public marketplace, with
            investor actions added on the right side.
          </p>
        </div>

        <div class="top-actions" *ngIf="campaign() as campaignData">
          <a class="btn btn-ghost" [routerLink]="['/campaigns', campaignData.slug]" target="_blank">
            Open public page
          </a>
          <a class="btn btn-ghost" routerLink="/investor/my-pledges">My pledges</a>
          <a class="btn btn-ghost" routerLink="/investor/my-payments">My payments</a>
        </div>
      </header>

      <p class="success" *ngIf="success()">{{ success() }}</p>
      <p class="error" *ngIf="error()">{{ error() }}</p>
      <div class="loading-card" *ngIf="loading()">Loading campaign preview...</div>

      <section class="workspace" *ngIf="!loading() && campaign() as campaignData">
        <main class="canvas-shell">
          <app-campaign-page-renderer
            [campaign]="campaignData"
            [content]="content()"
            [style]="style()"
            [allowDocumentDownload]="false"
            [showTrustHeader]="true"
            [showSectionLabels]="false"
          />
        </main>

        <aside class="investor-panel">
          <section class="panel-card summary-card">
            <span class="panel-title">Investment snapshot</span>
            <h2>{{ campaignData.businessName || campaignData.title || 'Campaign' }}</h2>
            <p>{{ campaignData.subtitle || campaignData.summary || 'Review this campaign before pledging.' }}</p>

            <div class="snapshot-grid">
              <div>
                <span>Type</span>
                <strong>{{ formatLabel(campaignData.applicationType) }}</strong>
              </div>
              <div>
                <span>Location</span>
                <strong>{{ locationLabel(campaignData) }}</strong>
              </div>
              <div>
                <span>Goal</span>
                <strong>{{ formatMoney(campaignData.fundingGoal, campaignData.currency) }}</strong>
              </div>
              <div>
                <span>Raised</span>
                <strong>{{ formatMoney(campaignData.investorsPledgedAmount, campaignData.currency) }}</strong>
              </div>
            </div>

            <div class="progress-box">
              <div>
                <span>Funding progress</span>
                <strong>{{ progressPercent(campaignData) }}%</strong>
              </div>
              <div class="progress-track">
                <span [style.width.%]="progressPercent(campaignData)"></span>
              </div>
            </div>

            <div class="chips">
              <span>{{ formatLabel(campaignData.sector) }}</span>
              <span>{{ formatLabel(campaignData.subSector) }}</span>
              <span>{{ campaignData.publicDocuments.length }} public docs</span>
            </div>
          </section>

          <section class="panel-card" *ngIf="campaignData.applicationType === crowdfundingType.EQUITY">
            <span class="panel-title">Equity terms</span>
            <dl>
              <div>
                <dt>Minimum investment</dt>
                <dd>{{ formatMoney(campaignData.equityDetail?.minInvestment, campaignData.currency) }}</dd>
              </div>
              <div>
                <dt>Equity offered</dt>
                <dd>{{ campaignData.equityDetail?.equityOfferedPercent ?? '—' }}%</dd>
              </div>
              <div>
                <dt>Pre-money valuation</dt>
                <dd>{{ formatMoney(campaignData.equityDetail?.preMoneyValuation, campaignData.currency) }}</dd>
              </div>
            </dl>
          </section>

          <section class="panel-card pledge-card">
            <div class="pledge-header">
              <div>
                <span class="panel-title">Your pledge</span>
                <h3>{{ existingPledge() ? 'Update your commitment' : 'Create a commitment' }}</h3>
              </div>

              <span class="status-badge" *ngIf="existingPledge()">
                {{ formatLabel(existingPledge()!.status) }}
              </span>
            </div>

            <p class="hint" *ngIf="campaignData.applicationType === crowdfundingType.EQUITY && campaignData.equityDetail?.minInvestment">
              Minimum investment is
              {{ formatMoney(campaignData.equityDetail?.minInvestment, campaignData.currency) }}.
            </p>

            <p class="hint" *ngIf="isPaidPledge()">
              This pledge is already paid and cannot be edited.
            </p>

            <form [formGroup]="form" (ngSubmit)="savePledge(campaignData)" class="pledge-form">
              <label class="field">
                <span>Amount *</span>
                <input type="number" min="1" step="0.001" formControlName="amount" />
                <small *ngIf="showError('amount')">Enter a valid pledge amount.</small>
              </label>

              <label class="field">
                <span>Message</span>
                <textarea rows="4" maxlength="500" formControlName="message" placeholder="Optional note to the founder"></textarea>
                <small *ngIf="showError('message')">Message must be at most 500 characters.</small>
              </label>

              <button type="submit" class="btn btn-primary full" [disabled]="saving() || isPaidPledge()">
                {{ saving() ? 'Saving...' : existingPledge() ? 'Update pledge' : 'Create pledge' }}
              </button>
            </form>
          </section>

          <section class="panel-card" *ngIf="existingPledge() as pledge">
            <div class="pledge-header">
              <div>
                <span class="panel-title">Payment</span>
                <h3>Complete your pledge</h3>
              </div>

              <span
                class="status-badge payment"
                *ngIf="latestPayment()"
                [ngClass]="latestPayment()!.status.toLowerCase()"
              >
                {{ formatLabel(latestPayment()!.status) }}
              </span>
            </div>

            <dl>
              <div>
                <dt>Pledge amount</dt>
                <dd>{{ formatMoney(pledge.amount, pledge.currency) }}</dd>
              </div>
              <div>
                <dt>Pledge status</dt>
                <dd>{{ formatLabel(pledge.status) }}</dd>
              </div>
              <ng-container *ngIf="latestPayment() as payment">
                <div>
                  <dt>Payment provider</dt>
                  <dd>{{ formatLabel(payment.provider) }}</dd>
                </div>
                <div>
                  <dt>Reference</dt>
                  <dd>{{ payment.providerReference || '—' }}</dd>
                </div>
                <div *ngIf="payment.failureReason">
                  <dt>Reason</dt>
                  <dd>{{ payment.failureReason }}</dd>
                </div>
              </ng-container>
            </dl>

            <div class="payment-actions">
              <button
                type="button"
                class="btn btn-primary"
                (click)="initiatePayment()"
                [disabled]="processingPayment() || isPaidPledge()"
              >
                {{ processingPayment() ? 'Redirecting...' : latestPayment() ? 'Continue Stripe checkout' : 'Pay with Stripe' }}
              </button>

              <button
                type="button"
                class="btn btn-ghost"
                *ngIf="latestPayment()"
                (click)="openStripeCheckout()"
                [disabled]="processingPayment() || !canUseCheckout(latestPayment())"
              >
                Open checkout
              </button>
            </div>

          </section>

          <section class="panel-card">
            <span class="panel-title">Public documents</span>
            <div class="doc-list" *ngIf="campaignData.publicDocuments.length > 0; else noDocs">
              <div class="doc-item" *ngFor="let doc of campaignData.publicDocuments">
                <span>PDF</span>
                <div>
                  <strong>{{ doc.label || formatLabel(doc.docType) }}</strong>
                  <small>{{ doc.fileName }}</small>
                </div>
              </div>
            </div>
            <ng-template #noDocs>
              <p class="hint">No public documents are attached to this campaign.</p>
            </ng-template>
          </section>
        </aside>
      </section>
    </section>
  `,
  styles: [
    `
      .investor-preview-page {
        display: grid;
        gap: 18px;
        padding-bottom: 34px;
      }

      .topbar {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 18px;
        flex-wrap: wrap;
        padding: 24px;
        border-radius: 32px;
        color: #ffffff;
        background:
          radial-gradient(circle at 16% 16%, rgba(109, 183, 255, 0.32), transparent 34%),
          radial-gradient(circle at 100% 18%, rgba(243, 223, 152, 0.15), transparent 34%),
          linear-gradient(135deg, #071a3a, #092549 58%, #06152e);
        border: 1px solid rgba(123, 185, 255, 0.18);
        box-shadow: 0 24px 60px rgba(7, 26, 58, 0.16);
      }

      .back-link {
        display: inline-flex;
        margin-bottom: 12px;
        color: rgba(234, 246, 255, 0.8);
        text-decoration: none;
        font-weight: 950;
      }

      .eyebrow,
      .panel-title {
        display: inline-flex;
        color: #f3df98;
        font-size: 0.72rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.09em;
      }

      h1 {
        margin: 8px 0;
        max-width: 850px;
        color: #ffffff;
        font-size: clamp(2rem, 4vw, 3.3rem);
        line-height: 0.95;
        letter-spacing: -0.07em;
      }

      .topbar p {
        max-width: 760px;
        margin: 0;
        color: rgba(234, 246, 255, 0.74);
        line-height: 1.7;
        font-weight: 650;
      }

      .top-actions,
      .payment-actions {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
      }

      .workspace {
        display: grid;
        grid-template-columns: minmax(0, 1fr) 390px;
        gap: 18px;
        align-items: start;
      }

      .canvas-shell {
        min-width: 0;
        overflow: hidden;
        border-radius: 34px;
        background: #f3f6f9;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 20px 54px rgba(7, 26, 58, 0.08);
      }

      .investor-panel {
        position: sticky;
        top: 16px;
        display: grid;
        gap: 14px;
      }

      .panel-card,
      .loading-card {
        display: grid;
        gap: 14px;
        padding: 18px;
        border-radius: 26px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 44px rgba(7, 26, 58, 0.08);
      }

      .summary-card h2,
      .pledge-header h3 {
        margin: 0;
        color: #071a3a;
        letter-spacing: -0.045em;
      }

      .summary-card h2 {
        font-size: 1.35rem;
      }

      .summary-card p,
      .hint {
        margin: 0;
        color: #667085;
        line-height: 1.55;
        font-weight: 700;
      }

      .snapshot-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
      }

      .snapshot-grid div {
        display: grid;
        gap: 4px;
        padding: 12px;
        border-radius: 18px;
        background: #f8fbff;
        border: 1px solid rgba(71, 103, 136, 0.12);
      }

      .snapshot-grid span,
      dt {
        color: #667085;
        font-size: 0.72rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .snapshot-grid strong,
      dd {
        min-width: 0;
        margin: 0;
        color: #071a3a;
        font-weight: 950;
        overflow-wrap: anywhere;
      }

      .progress-box {
        display: grid;
        gap: 8px;
        padding: 12px;
        border-radius: 20px;
        background: linear-gradient(135deg, #f8fbff, #edf5ff);
        border: 1px solid rgba(71, 103, 136, 0.12);
      }

      .progress-box > div:first-child {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        color: #667085;
        font-size: 0.84rem;
        font-weight: 850;
      }

      .progress-box strong {
        color: #071a3a;
        font-weight: 950;
      }

      .progress-track {
        height: 10px;
        overflow: hidden;
        border-radius: 999px;
        background: #dbeafe;
      }

      .progress-track span {
        display: block;
        height: 100%;
        border-radius: inherit;
        background: linear-gradient(90deg, #0b4f8a, #6db7ff);
      }

      .chips {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .chips span,
      .status-badge {
        display: inline-flex;
        align-items: center;
        width: max-content;
        max-width: 100%;
        padding: 7px 10px;
        border-radius: 999px;
        color: #354b67;
        background: #f3f7fb;
        font-size: 0.74rem;
        font-weight: 950;
      }

      .status-badge {
        color: #0b4f8a;
        background: #e6f2ff;
      }

      .status-badge.payment.created,
      .status-badge.payment.pending_provider {
        color: #8a6d1d;
        background: #fff5d7;
      }

      .status-badge.payment.succeeded {
        color: #1f7a43;
        background: #e8f7ef;
      }

      .status-badge.payment.failed,
      .status-badge.payment.canceled,
      .status-badge.payment.refunded {
        color: #b03a2e;
        background: #fdecec;
      }

      dl {
        display: grid;
        gap: 10px;
        margin: 0;
      }

      dl div {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        align-items: baseline;
        padding-bottom: 10px;
        border-bottom: 1px solid rgba(15, 23, 42, 0.07);
      }

      dl div:last-child {
        padding-bottom: 0;
        border-bottom: 0;
      }

      .pledge-header {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        align-items: flex-start;
      }

      .pledge-form {
        display: grid;
        gap: 12px;
      }

      .field {
        display: grid;
        gap: 8px;
      }

      .field span {
        color: #071a3a;
        font-size: 0.78rem;
        font-weight: 950;
      }

      input,
      textarea {
        width: 100%;
        border: 1px solid rgba(71, 103, 136, 0.18);
        border-radius: 15px;
        padding: 12px;
        color: #071a3a;
        background: #ffffff;
        font: inherit;
        font-weight: 750;
        box-sizing: border-box;
        outline: none;
      }

      input:focus,
      textarea:focus {
        border-color: rgba(74, 163, 255, 0.58);
        box-shadow: 0 0 0 4px rgba(74, 163, 255, 0.12);
      }

      textarea {
        resize: vertical;
      }

      small {
        color: #b42318;
        font-weight: 800;
      }

      .btn {
        min-height: 42px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        border: 0;
        border-radius: 15px;
        padding: 0 14px;
        font: inherit;
        font-weight: 950;
        cursor: pointer;
        text-decoration: none;
        transition: 0.18s ease;
      }

      .btn:hover:not(:disabled) {
        transform: translateY(-1px);
      }

      .btn:disabled {
        opacity: 0.62;
        cursor: not-allowed;
      }

      .btn.full {
        width: 100%;
      }

      .btn-primary {
        color: #ffffff;
        background: linear-gradient(135deg, #071a3a, #0b4f8a);
      }

      .btn-ghost {
        color: #071a3a;
        background: #edf5ff;
      }

      .topbar .btn-ghost {
        color: #ffffff;
        background: rgba(255, 255, 255, 0.11);
      }

      .btn-success {
        color: #ffffff;
        background: #17804a;
      }

      .btn-danger {
        color: #ffffff;
        background: #b42318;
      }

      .doc-list {
        display: grid;
        gap: 10px;
      }

      .doc-item {
        display: flex;
        gap: 10px;
        min-width: 0;
        padding: 11px;
        border-radius: 18px;
        background: #f8fbff;
        border: 1px solid rgba(71, 103, 136, 0.12);
      }

      .doc-item > span {
        width: 42px;
        height: 42px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        border-radius: 14px;
        color: #071a3a;
        background: #f3df98;
        font-size: 0.72rem;
        font-weight: 950;
      }

      .doc-item div {
        min-width: 0;
        display: grid;
        gap: 2px;
      }

      .doc-item strong,
      .doc-item small {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .doc-item strong {
        color: #071a3a;
        font-size: 0.9rem;
        font-weight: 950;
      }

      .doc-item small {
        color: #667085;
      }

      .success,
      .error,
      .loading-card {
        margin: 0;
        font-weight: 850;
      }

      .success,
      .error {
        padding: 14px 16px;
        border-radius: 18px;
      }

      .success {
        color: #1f7a43;
        background: #e8f7ef;
        border: 1px solid #b7e4ca;
      }

      .error {
        color: #b42318;
        background: #fff1f0;
        border: 1px solid #ffd5d2;
      }

      @media (max-width: 1240px) {
        .workspace {
          grid-template-columns: 1fr;
        }

        .investor-panel {
          position: static;
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }

      @media (max-width: 820px) {
        .topbar,
        .snapshot-grid,
        .investor-panel {
          grid-template-columns: 1fr;
        }

        .topbar {
          display: grid;
        }
      }

      @media (max-width: 560px) {
        .topbar,
        .panel-card {
          padding: 16px;
          border-radius: 24px;
        }

        dl div,
        .pledge-header {
          display: grid;
        }
      }
    `,
  ],
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

  readonly campaign = signal<CampaignPageResponse | null>(null);
  readonly content = signal<StudioContentJson>({ sections: [] });
  readonly style = signal<CampaignStyleJson>({ ...DEFAULT_STYLE });
  readonly existingPledge = signal<PledgeResponse | null>(null);
  readonly latestPayment = signal<PaymentResponse | null>(null);

  private campaignSlug: string | null = null;

  readonly form = this.fb.group({
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    message: ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const routeKey = params.get('slug');
        if (!routeKey) {
          this.error.set('Campaign link is missing.');
          return;
        }

        this.resolveAndLoad(routeKey);
      });
  }


  private resolveAndLoad(routeKey: string): void {
    const numericKey = Number(routeKey);
    const isNumericRoute = Number.isFinite(numericKey) && /^\d+$/.test(routeKey.trim());

    if (!isNumericRoute) {
      this.campaignSlug = routeKey;
      this.load(routeKey);
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    this.service.listPublicCampaignPages().subscribe({
      next: (campaigns) => {
        const match = (campaigns ?? []).find(
          (item) => item.applicationRaiseId === numericKey || item.id === numericKey,
        );

        if (!match?.slug) {
          this.loading.set(false);
          this.error.set('This pledged application does not have a published campaign page yet.');
          return;
        }

        this.campaignSlug = match.slug;
        this.load(match.slug);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(this.extractError(err));
      },
    });
  }

  load(slug: string): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    forkJoin({
      campaign: this.service.getPublicCampaignBySlug(slug),
      pledges: this.service.listMyPledges(),
      payments: this.service.listMyPayments(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ campaign, pledges, payments }) => {
          this.campaign.set(campaign);
          this.content.set(this.parseContent(campaign.contentJson));
          this.style.set(this.parseStyle(campaign.styleJson));
          this.applyAmountValidator(campaign.equityDetail?.minInvestment ?? null);

          const pledge =
            pledges.find((item) => item.applicationRaiseId === campaign.applicationRaiseId) ?? null;

          this.existingPledge.set(pledge);

          if (pledge) {
            this.form.patchValue({
              amount: pledge.amount,
              message: pledge.message ?? '',
            });

            const latest =
              [...payments]
                .filter((item) => item.pledgeId === pledge.id)
                .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())[0] ?? null;

            this.latestPayment.set(latest);
          } else {
            this.form.patchValue({ amount: null, message: '' });
            this.latestPayment.set(null);
          }
        },
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err)),
      });
  }

  savePledge(campaign: CampaignPageResponse): void {
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
      .createOrUpdateMyPledge(campaign.applicationRaiseId, {
        amount: raw.amount,
        message: raw.message?.trim() ? raw.message.trim() : null,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (pledge) => {
          this.existingPledge.set(pledge);
          this.latestPayment.set(null);
          this.success.set('Pledge saved successfully. You can now initiate payment.');
          if (this.campaignSlug) this.refreshCampaignOnly(this.campaignSlug);
        },
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err)),
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

          if (payment.checkoutUrl) {
            this.success.set('Stripe checkout is ready. Redirecting you now...');
            window.location.href = payment.checkoutUrl;
            return;
          }

          this.error.set('Stripe checkout was created, but no checkout URL was returned.');
        },
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err)),
      });
  }

  openStripeCheckout(): void {
    const payment = this.latestPayment();
    if (!payment) return;

    if (!payment.checkoutUrl) {
      this.error.set('No Stripe checkout URL is available for this payment.');
      return;
    }

    window.location.href = payment.checkoutUrl;
  }

  patchLatestPayment(
    status: PaymentStatus,
    campaign: CampaignPageResponse,
    failureReason?: string,
  ): void {
    const payment = this.latestPayment();
    if (!payment) return;

    this.processingPayment.set(true);
    this.error.set('');
    this.success.set('');

    this.service
      .patchMyPaymentStatus(payment.id, {
        status,
        failureReason: failureReason ?? null,
      })
      .pipe(finalize(() => this.processingPayment.set(false)))
      .subscribe({
        next: (updatedPayment) => {
          this.latestPayment.set(updatedPayment);

          const pledge = this.existingPledge();
          if (pledge) {
            this.existingPledge.set({
              ...pledge,
              status: updatedPayment.pledgeStatus,
            });
          }

          this.success.set(`Payment ${this.formatLabel(updatedPayment.status).toLowerCase()} successfully.`);
          this.refreshCampaignOnly(campaign.slug);
        },
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err)),
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

  progressPercent(campaign: CampaignPageResponse): number {
    const goal = Number(campaign.fundingGoal ?? 0);
    const raised = Number(campaign.investorsPledgedAmount ?? 0);
    if (!Number.isFinite(goal) || goal <= 0) return 0;
    return Math.min(100, Math.max(0, Math.round((raised / goal) * 100)));
  }

  locationLabel(campaign: CampaignPageResponse): string {
    const parts = [campaign.city, campaign.governorate].filter((value) => !!value?.trim());
    return parts.length ? parts.join(', ') : 'Tunisia';
  }

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }

    return String(value)
      .replace(/-/g, '_')
      .toLowerCase()
      .split('_')
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatMoney(value: number | null | undefined, currency: string | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
      return `— ${currency || 'TND'}`;
    }

    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: currency || 'TND',
      maximumFractionDigits: 0,
    }).format(Number(value));
  }

  private applyAmountValidator(minInvestment: number | null): void {
    const minAmount = Math.max(1, Number(minInvestment ?? 1));
    const control = this.form.get('amount');
    control?.setValidators([Validators.required, Validators.min(minAmount)]);
    control?.updateValueAndValidity({ emitEvent: false });
  }

  private refreshCampaignOnly(slug: string): void {
    this.service.getPublicCampaignBySlug(slug).subscribe({
      next: (campaign) => {
        this.campaign.set(campaign);
        this.content.set(this.parseContent(campaign.contentJson));
        this.style.set(this.parseStyle(campaign.styleJson));
      },
    });
  }

  private parseContent(value: string | null): StudioContentJson {
    try {
      const parsed = JSON.parse(value || '{}') as StudioContentJson;
      if (Array.isArray(parsed.sections) || Array.isArray(parsed.blocks)) {
        return parsed;
      }
    } catch {
      // fallback below
    }

    return { sections: [] };
  }

  private parseStyle(value: string | null): CampaignStyleJson {
    try {
      return { ...DEFAULT_STYLE, ...(JSON.parse(value || '{}') as Partial<CampaignStyleJson>) };
    } catch {
      return { ...DEFAULT_STYLE };
    }
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
      'Unable to load this campaign preview.'
    );
  }
}
