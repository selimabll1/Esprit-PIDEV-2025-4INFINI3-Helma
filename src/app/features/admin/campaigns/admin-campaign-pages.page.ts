import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  CampaignPageResponse,
  CampaignPageStatus,
  CrowdfundingType,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

type StudioContent = {
  sections?: Array<{
    elements?: Array<{
      type?: string | null;
      url?: string | null;
    }>;
  }>;
};

@Component({
  selector: 'app-admin-campaign-pages-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="admin-campaigns-page">
      <header class="page-hero">
        <div class="hero-copy">
          <span class="eyebrow">Compliance</span>
          <h1>Campaign review queue</h1>
          <p>
            Review campaign cards from an investor point of view. Consult the live preview,
            verify documents and funding clarity, then publish or request changes.
          </p>
        </div>

        <label class="filter-card">
          <span>Status filter</span>
          <select [(ngModel)]="statusFilter" (ngModelChange)="load()">
            <option [ngValue]="null">All campaigns</option>
            <option [ngValue]="campaignStatus.PENDING_REVIEW">Pending review</option>
            <option [ngValue]="campaignStatus.PUBLISHED">Published</option>
            <option [ngValue]="campaignStatus.CHANGES_REQUESTED">Changes requested</option>
            <option [ngValue]="campaignStatus.DRAFT">Draft</option>
            <option [ngValue]="campaignStatus.ARCHIVED">Archived</option>
          </select>
        </label>
      </header>

      <section class="review-stats" *ngIf="!loading && campaigns.length > 0">
        <article>
          <span>Total</span>
          <strong>{{ campaigns.length }}</strong>
        </article>
        <article>
          <span>Pending</span>
          <strong>{{ countByStatus(campaignStatus.PENDING_REVIEW) }}</strong>
        </article>
        <article>
          <span>Published</span>
          <strong>{{ countByStatus(campaignStatus.PUBLISHED) }}</strong>
        </article>
        <article>
          <span>Changes</span>
          <strong>{{ countByStatus(campaignStatus.CHANGES_REQUESTED) }}</strong>
        </article>
      </section>

      <section class="list-toolbar" *ngIf="!loading && campaigns.length > 0">
        <div>
          <strong>{{ campaigns.length }}</strong>
          <span>campaigns in this filter</span>
        </div>

        <label>
          <span>Cards per page</span>
          <select [(ngModel)]="pageSize" (ngModelChange)="setPage(1)">
            <option [ngValue]="6">6</option>
            <option [ngValue]="8">8</option>
            <option [ngValue]="12">12</option>
            <option [ngValue]="16">16</option>
          </select>
        </label>
      </section>

      <p class="error" *ngIf="error">{{ error }}</p>
      <p class="success" *ngIf="success">{{ success }}</p>

      <div class="loading-card" *ngIf="loading">
        <span class="loader"></span>
        Loading campaign pages...
      </div>

      <section class="empty-card" *ngIf="!loading && campaigns.length === 0">
        <div class="empty-visual leaf-fallback"></div>
        <div>
          <span class="eyebrow muted">Clear queue</span>
          <h2>No campaign pages found</h2>
          <p>There are no campaigns matching this filter right now.</p>
        </div>
      </section>

      <section class="campaign-grid" *ngIf="!loading && campaigns.length > 0">
        <article
          class="review-card"
          *ngFor="let campaign of pagedCampaigns; trackBy: trackByCampaignId"
        >
          <div class="visual-strip" [class.has-image]="!!campaignImage(campaign)">
            <img *ngIf="campaignImage(campaign) as imageUrl" [src]="imageUrl" alt="Campaign visual" />
            <div class="leaf-fallback" *ngIf="!campaignImage(campaign)">
              <span>Helma</span>
            </div>

            <div class="visual-badges">
              <span>{{ formatLabel(campaign.applicationType) }}</span>
              <span [ngClass]="statusClass(campaign.status)">{{ formatLabel(campaign.status) }}</span>
            </div>
          </div>

          <div class="card-content">
            <div class="campaign-copy">
              <h2>{{ campaign.title || campaign.businessName || 'Untitled campaign' }}</h2>
              <p class="business">{{ campaign.businessName || 'Helma founder' }}</p>
              <p class="summary">
                {{ campaign.subtitle || campaign.summary || 'No campaign description yet.' }}
              </p>
            </div>

            <div class="compliance-flags">
              <span>App #{{ campaign.applicationRaiseId }}</span>
              <span>Founder #{{ campaign.ownerUserId }}</span>
              <span>{{ formatLocation(campaign) }}</span>
              <span>{{ campaign.publicDocuments.length }} docs</span>
            </div>

            <div class="progress-shell">
              <div>
                <span>{{ formatMoney(campaign.investorsPledgedAmount, campaign.currency) }} pledged</span>
                <strong>{{ progressPercent(campaign) }}%</strong>
              </div>
              <div class="progress-track">
                <span [style.width.%]="progressPercent(campaign)"></span>
              </div>
              <div>
                <span>Goal</span>
                <strong>{{ formatMoney(campaign.fundingGoal, campaign.currency) }}</strong>
              </div>
            </div>

            <div class="mini-info">
              <span>Updated {{ formatDate(campaign.updatedAt) }}</span>
              <span>/campaigns/{{ campaign.slug }}</span>
            </div>

            <label class="note-field">
              <span>Review note</span>
              <textarea
                [(ngModel)]="reviewNotes[campaign.id]"
                rows="2"
                placeholder="Optional note"
              ></textarea>
            </label>

            <div class="actions">
              <a class="btn btn-dark" [routerLink]="['/admin/campaigns', campaign.id, 'preview']">
                Consult
              </a>

              <button
                *ngIf="campaign.status === campaignStatus.PENDING_REVIEW"
                class="btn btn-primary"
                type="button"
                [disabled]="workingId === campaign.id"
                (click)="publish(campaign)"
              >
                Publish
              </button>

              <button
                *ngIf="campaign.status === campaignStatus.PENDING_REVIEW || campaign.status === campaignStatus.PUBLISHED"
                class="btn btn-danger"
                type="button"
                [disabled]="workingId === campaign.id"
                (click)="requestChanges(campaign)"
              >
                Changes
              </button>

              <a
                *ngIf="campaign.status === campaignStatus.PUBLISHED"
                class="btn btn-ghost"
                [href]="'/campaigns/' + campaign.slug"
                target="_blank"
                rel="noopener"
              >
                Public
              </a>
            </div>
          </div>
        </article>
      </section>

      <nav class="pagination" *ngIf="!loading && totalPages > 1" aria-label="Campaign pagination">
        <button type="button" class="page-btn" [disabled]="currentPage === 1" (click)="setPage(currentPage - 1)">
          ← Previous
        </button>

        <div class="page-numbers">
          <button
            type="button"
            class="number-btn"
            *ngFor="let page of pageNumbers"
            [class.active]="page === currentPage"
            (click)="setPage(page)"
          >
            {{ page }}
          </button>
        </div>

        <button type="button" class="page-btn" [disabled]="currentPage === totalPages" (click)="setPage(currentPage + 1)">
          Next →
        </button>
      </nav>
    </section>
  `,
  styles: [
    `
      .admin-campaigns-page {
        display: grid;
        gap: 18px;
      }

      .page-hero {
        position: relative;
        overflow: hidden;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 18px;
        flex-wrap: wrap;
        padding: 24px;
        border-radius: 30px;
        background:
          radial-gradient(circle at 14% 18%, rgba(16, 185, 129, 0.15), transparent 30%),
          radial-gradient(circle at 85% 12%, rgba(247, 215, 124, 0.38), transparent 28%),
          linear-gradient(135deg, #f6fdf8, #fff7ed);
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 46px rgba(15, 23, 42, 0.07);
      }

      .hero-copy {
        max-width: 820px;
      }

      .eyebrow {
        display: inline-flex;
        width: max-content;
        padding: 6px 11px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.78);
        color: #047857;
        font-size: 0.7rem;
        font-weight: 950;
        letter-spacing: 0.08em;
        text-transform: uppercase;
      }

      .eyebrow.muted {
        background: #f2f4f7;
        color: #667085;
      }

      h1 {
        margin: 8px 0 0;
        color: #111827;
        font-size: clamp(2.1rem, 4vw, 3.7rem);
        line-height: 0.92;
        letter-spacing: -0.075em;
      }

      .page-hero p,
      .empty-card p {
        margin: 12px 0 0;
        max-width: 760px;
        color: #667085;
        line-height: 1.65;
        font-weight: 650;
      }

      .filter-card {
        display: grid;
        gap: 8px;
        min-width: 230px;
        padding: 14px;
        border-radius: 20px;
        background: rgba(255, 255, 255, 0.76);
        border: 1px solid rgba(15, 23, 42, 0.08);
      }

      .filter-card span,
      .review-stats span,
      .list-toolbar span,
      .progress-shell span,
      .note-field span,
      .mini-info span {
        color: #98a2b3;
        font-size: 0.7rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      select,
      textarea {
        width: 100%;
        border: 1px solid rgba(15, 23, 42, 0.1);
        border-radius: 14px;
        padding: 9px 11px;
        background: #f8fafc;
        color: #111827;
        font: inherit;
        font-weight: 800;
        outline: none;
        box-sizing: border-box;
      }

      textarea {
        min-height: 64px;
        resize: vertical;
      }

      .review-stats {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 10px;
      }

      .review-stats article,
      .list-toolbar {
        border-radius: 22px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 12px 30px rgba(15, 23, 42, 0.055);
      }

      .review-stats article {
        display: grid;
        gap: 4px;
        padding: 16px;
      }

      .review-stats strong {
        color: #111827;
        font-size: 2rem;
        line-height: 1;
        letter-spacing: -0.05em;
      }

      .list-toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
        padding: 12px 14px;
      }

      .list-toolbar > div {
        display: flex;
        align-items: baseline;
        gap: 8px;
      }

      .list-toolbar strong {
        color: #111827;
        font-size: 1.15rem;
        font-weight: 950;
      }

      .list-toolbar label {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .campaign-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(285px, 1fr));
        gap: 18px;
      }

      .review-card {
        overflow: hidden;
        min-width: 0;
        border-radius: 28px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 16px 40px rgba(15, 23, 42, 0.075);
        transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
      }

      .review-card:hover {
        transform: translateY(-4px);
        box-shadow: 0 24px 54px rgba(15, 23, 42, 0.12);
        border-color: rgba(4, 120, 87, 0.18);
      }

      .visual-strip {
        position: relative;
        height: 176px;
        overflow: hidden;
        background: #fff7ed;
      }

      .visual-strip::after {
        content: '';
        position: absolute;
        inset: 0;
        background: linear-gradient(180deg, rgba(17, 24, 39, 0.08), transparent 42%, rgba(17, 24, 39, 0.24));
        pointer-events: none;
      }

      .visual-strip img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }

      .leaf-fallback {
        position: absolute;
        inset: 0;
        overflow: hidden;
        display: grid;
        place-items: center;
        background:
          radial-gradient(ellipse at 20% 20%, rgba(247, 215, 124, 0.82) 0 18%, transparent 19%),
          radial-gradient(ellipse at 80% 26%, rgba(214, 161, 61, 0.62) 0 17%, transparent 18%),
          radial-gradient(ellipse at 32% 78%, rgba(4, 120, 87, 0.2) 0 16%, transparent 17%),
          radial-gradient(ellipse at 78% 76%, rgba(247, 215, 124, 0.56) 0 20%, transparent 21%),
          linear-gradient(145deg, #fffaf0, #f7ebc3 54%, #ecfdf5);
      }

      .leaf-fallback::before,
      .leaf-fallback::after {
        content: '';
        position: absolute;
        width: 150px;
        height: 150px;
        border-radius: 42% 58% 45% 55%;
        border: 1px solid rgba(154, 106, 23, 0.14);
        transform: rotate(-28deg);
      }

      .leaf-fallback::before { left: -50px; top: -44px; }
      .leaf-fallback::after { right: -54px; bottom: -42px; transform: rotate(24deg); }

      .leaf-fallback span {
        position: relative;
        z-index: 1;
        color: rgba(47, 36, 20, 0.38);
        font-weight: 950;
        letter-spacing: 0.2em;
        text-transform: uppercase;
      }

      .visual-badges {
        position: absolute;
        z-index: 3;
        left: 12px;
        right: 12px;
        top: 12px;
        display: flex;
        justify-content: space-between;
        gap: 8px;
        flex-wrap: wrap;
      }

      .visual-badges span {
        padding: 6px 9px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.9);
        color: #2f2414;
        font-size: 0.65rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
      }

      .visual-badges .status-draft { color: #b45309; }
      .visual-badges .status-review { color: #1d4ed8; }
      .visual-badges .status-published { color: #047857; }
      .visual-badges .status-changes { color: #b42318; }

      .card-content {
        display: grid;
        gap: 12px;
        padding: 16px;
      }

      .campaign-copy h2 {
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        margin: 0;
        color: #111827;
        font-size: 1.25rem;
        line-height: 1.08;
        letter-spacing: -0.045em;
      }

      .business {
        margin: 5px 0 0;
        color: #98a2b3;
        font-size: 0.82rem;
        font-weight: 850;
      }

      .summary {
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        min-height: 42px;
        margin: 10px 0 0;
        color: #667085;
        line-height: 1.5;
        font-size: 0.92rem;
        font-weight: 650;
      }

      .compliance-flags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
      }

      .compliance-flags span {
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        padding: 6px 9px;
        border-radius: 999px;
        background: #f8fafc;
        color: #475467;
        font-size: 0.72rem;
        font-weight: 850;
      }

      .progress-shell {
        display: grid;
        gap: 7px;
        padding: 12px;
        border-radius: 18px;
        background: linear-gradient(180deg, #ffffff, #f8fafc);
        border: 1px solid rgba(15, 23, 42, 0.07);
      }

      .progress-shell > div:first-child,
      .progress-shell > div:last-child {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 10px;
      }

      .progress-shell strong {
        color: #111827;
        font-size: 0.86rem;
        font-weight: 950;
      }

      .progress-shell > div:first-child strong { color: #047857; }

      .progress-track {
        height: 8px;
        border-radius: 999px;
        background: #eef2f6;
        overflow: hidden;
      }

      .progress-track span {
        display: block;
        height: 100%;
        border-radius: inherit;
        background: linear-gradient(135deg, #d6a13d, #047857);
      }

      .mini-info {
        display: grid;
        gap: 4px;
      }

      .mini-info span:last-child {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #667085;
      }

      .note-field {
        display: grid;
        gap: 7px;
      }

      .actions {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 8px;
      }

      .btn {
        min-height: 38px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        border-radius: 999px;
        padding: 0 12px;
        border: 0;
        font: inherit;
        font-size: 0.82rem;
        font-weight: 950;
        text-decoration: none;
        cursor: pointer;
        white-space: nowrap;
      }

      .btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .btn-primary {
        color: #271a08;
        background: linear-gradient(135deg, #f7d77c, #d6a13d);
        box-shadow: 0 10px 22px rgba(184, 130, 38, 0.18);
      }

      .btn-dark {
        color: #ffffff;
        background: #111827;
      }

      .btn-danger {
        color: #991b1b;
        background: #fee2e2;
      }

      .btn-ghost {
        color: #344054;
        background: #f2f4f7;
      }

      .pagination {
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;
        padding: 10px;
      }

      .page-numbers {
        display: flex;
        gap: 6px;
        flex-wrap: wrap;
        justify-content: center;
      }

      .page-btn,
      .number-btn {
        min-height: 38px;
        border-radius: 999px;
        border: 1px solid rgba(15, 23, 42, 0.09);
        background: #ffffff;
        color: #344054;
        font: inherit;
        font-size: 0.84rem;
        font-weight: 950;
        padding: 0 13px;
        cursor: pointer;
      }

      .number-btn {
        width: 38px;
        padding: 0;
      }

      .number-btn.active {
        color: #271a08;
        background: linear-gradient(135deg, #f7d77c, #d6a13d);
        border-color: transparent;
      }

      .page-btn:disabled,
      .number-btn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .loading-card,
      .empty-card {
        display: grid;
        justify-items: center;
        text-align: center;
        gap: 14px;
        padding: 30px 22px;
        border-radius: 26px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
        color: #475467;
        font-weight: 800;
      }

      .empty-card {
        grid-template-columns: 180px minmax(0, 1fr);
        justify-items: start;
        text-align: left;
        align-items: center;
      }

      .empty-visual {
        position: relative;
        width: 160px;
        height: 116px;
        border-radius: 24px;
      }

      .empty-card h2 {
        margin: 8px 0 0;
        color: #111827;
        font-size: 1.55rem;
        letter-spacing: -0.04em;
      }

      .loader {
        width: 30px;
        height: 30px;
        border-radius: 50%;
        border: 4px solid rgba(184, 130, 38, 0.16);
        border-top-color: #d6a13d;
        animation: spin 0.75s linear infinite;
      }

      .error,
      .success {
        margin: 0;
        padding: 12px 14px;
        border-radius: 16px;
        font-weight: 900;
      }

      .error {
        background: #fef2f2;
        color: #b42318;
        border: 1px solid rgba(239, 68, 68, 0.16);
      }

      .success {
        background: #ecfdf5;
        color: #047857;
        border: 1px solid rgba(16, 185, 129, 0.16);
      }

      @keyframes spin { to { transform: rotate(360deg); } }

      @media (min-width: 1480px) {
        .campaign-grid {
          grid-template-columns: repeat(4, minmax(0, 1fr));
        }
      }

      @media (max-width: 900px) {
        .review-stats {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
        .empty-card {
          grid-template-columns: 1fr;
          text-align: center;
          justify-items: center;
        }
      }

      @media (max-width: 640px) {
        .page-hero,
        .list-toolbar {
          align-items: stretch;
        }
        .review-stats {
          grid-template-columns: 1fr;
        }
        .visual-strip {
          height: 190px;
        }
        .actions {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class AdminCampaignPagesPageComponent implements OnInit {
  private readonly crowdfundingService = inject(CrowdfundingService);

  readonly campaignStatus = CampaignPageStatus;

  campaigns: CampaignPageResponse[] = [];
  statusFilter: CampaignPageStatus | null = CampaignPageStatus.PENDING_REVIEW;
  reviewNotes: Record<number, string> = {};
  loading = false;
  workingId: number | null = null;
  error: string | null = null;
  success: string | null = null;
  currentPage = 1;
  pageSize = 8;

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.campaigns.length / this.pageSize));
  }

  get pagedCampaigns(): CampaignPageResponse[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.campaigns.slice(start, start + this.pageSize);
  }

  get pageNumbers(): number[] {
    const total = this.totalPages;
    const windowSize = 5;
    let start = Math.max(1, this.currentPage - 2);
    const end = Math.min(total, start + windowSize - 1);
    start = Math.max(1, end - windowSize + 1);

    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.success = null;

    this.crowdfundingService
      .adminListCampaignPages(this.statusFilter)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (items) => {
          this.campaigns = items ?? [];
          this.campaigns.forEach((campaign) => {
            this.reviewNotes[campaign.id] = campaign.reviewNote || '';
          });
          this.setPage(1);
        },
        error: (err) => {
          console.error('[AdminCampaignPages] load failed:', err);
          this.error = err?.error?.message || 'Unable to load campaign pages.';
          this.campaigns = [];
          this.setPage(1);
        },
      });
  }

  publish(campaign: CampaignPageResponse): void {
    this.patchStatus(campaign, CampaignPageStatus.PUBLISHED);
  }

  requestChanges(campaign: CampaignPageResponse): void {
    this.patchStatus(campaign, CampaignPageStatus.CHANGES_REQUESTED);
  }

  patchStatus(campaign: CampaignPageResponse, status: CampaignPageStatus): void {
    this.workingId = campaign.id;
    this.error = null;
    this.success = null;

    this.crowdfundingService
      .adminPatchCampaignPageStatus(campaign.id, {
        status,
        reviewNote: this.reviewNotes[campaign.id] || null,
      })
      .pipe(finalize(() => (this.workingId = null)))
      .subscribe({
        next: () => {
          this.success = `Campaign ${this.formatLabel(status).toLowerCase()}.`;
          this.load();
        },
        error: (err) => {
          console.error('[AdminCampaignPages] status patch failed:', err);
          this.error = err?.error?.message || 'Unable to update campaign status.';
        },
      });
  }

  setPage(page: number): void {
    this.currentPage = Math.max(1, Math.min(this.totalPages, page));
  }

  countByStatus(status: CampaignPageStatus): number {
    return this.campaigns.filter((campaign) => campaign.status === status).length;
  }

  campaignImage(campaign: CampaignPageResponse): string | null {
    if (campaign.coverMediaUrl?.trim()) {
      return campaign.coverMediaUrl.trim();
    }

    try {
      const parsed = JSON.parse(campaign.contentJson || '{}') as StudioContent;
      const image = parsed.sections
        ?.flatMap((section) => section.elements || [])
        .find((element) => element.type === 'image' && !!element.url?.trim());

      return image?.url?.trim() || null;
    } catch {
      return null;
    }
  }

  progressPercent(campaign: CampaignPageResponse): number {
    const goal = Number(campaign.fundingGoal || 0);
    const raised = Number(campaign.investorsPledgedAmount || 0);
    if (!goal || goal <= 0) return 0;
    return Math.max(0, Math.min(100, Math.round((raised / goal) * 100)));
  }

  statusClass(status: CampaignPageStatus): string {
    switch (status) {
      case CampaignPageStatus.DRAFT:
        return 'status-draft';
      case CampaignPageStatus.PENDING_REVIEW:
        return 'status-review';
      case CampaignPageStatus.PUBLISHED:
        return 'status-published';
      case CampaignPageStatus.CHANGES_REQUESTED:
        return 'status-changes';
      case CampaignPageStatus.ARCHIVED:
        return 'status-draft';
      default:
        return 'status-draft';
    }
  }

  formatLocation(campaign: CampaignPageResponse): string {
    return [campaign.city, campaign.governorate].filter(Boolean).join(', ') || 'Tunisia';
  }

  formatMoney(value: number | null | undefined, currency: string | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: currency || 'TND',
      maximumFractionDigits: 0,
    }).format(value);
  }

  formatLabel(value: string | number | CrowdfundingType | null | undefined): string {
    if (value === null || value === undefined || value === '') return '—';
    return String(value)
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatDate(value: string | null | undefined): string {
    if (!value) return '—';
    try {
      return new Intl.DateTimeFormat('en-GB', {
        day: '2-digit',
        month: 'short',
      }).format(new Date(value));
    } catch {
      return '—';
    }
  }

  trackByCampaignId(_index: number, campaign: CampaignPageResponse): number {
    return campaign.id;
  }
}
