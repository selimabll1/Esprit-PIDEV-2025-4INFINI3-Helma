import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  CampaignPageResponse,
  CampaignPageStatus,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

@Component({
  selector: 'app-admin-campaign-pages-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="admin-campaigns-page">
      <header class="hero-card">
        <div>
          <span class="eyebrow">Compliance</span>
          <h1>Campaign review queue</h1>
          <p>
            Consult public campaign pages before they become visible to investors
            and donors.
          </p>
        </div>

        <label class="filter">
          <span>Status</span>
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

      <p class="error" *ngIf="error">{{ error }}</p>
      <p class="success" *ngIf="success">{{ success }}</p>

      <div class="loading-card" *ngIf="loading">Loading campaign pages...</div>

      <section class="empty-card" *ngIf="!loading && campaigns.length === 0">
        <h2>No campaign pages found</h2>
        <p>There are no campaigns matching this filter.</p>
      </section>

      <section class="campaign-list" *ngIf="!loading && campaigns.length > 0">
        <article
          class="campaign-card"
          *ngFor="let campaign of campaigns; trackBy: trackByCampaignId"
        >
          <div class="main">
            <div>
              <span class="status" [ngClass]="statusClass(campaign.status)">
                {{ formatLabel(campaign.status) }}
              </span>
              <h2>{{ campaign.title || campaign.businessName || 'Untitled campaign' }}</h2>
              <p>{{ campaign.subtitle || campaign.summary || 'No campaign description.' }}</p>
            </div>

            <dl>
              <div>
                <dt>Application</dt>
                <dd>#{{ campaign.applicationRaiseId }}</dd>
              </div>
              <div>
                <dt>Founder</dt>
                <dd>#{{ campaign.ownerUserId }}</dd>
              </div>
              <div>
                <dt>Public link</dt>
                <dd>/campaigns/{{ campaign.slug }}</dd>
              </div>
              <div>
                <dt>Documents</dt>
                <dd>{{ campaign.publicDocuments.length }}</dd>
              </div>
            </dl>
          </div>

          <label class="note-field">
            <span>Review note</span>
            <textarea
              [(ngModel)]="reviewNotes[campaign.id]"
              rows="3"
              placeholder="Optional note for founder"
            ></textarea>
          </label>

          <div class="actions">
            <a
              class="btn btn-dark"
              [routerLink]="['/admin/campaigns', campaign.id, 'preview']"
            >
              Consult page
            </a>

            <a
              *ngIf="campaign.status === campaignStatus.PUBLISHED"
              class="btn btn-ghost"
              [href]="'/campaigns/' + campaign.slug"
              target="_blank"
              rel="noopener"
            >
              Open public page
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
              class="btn btn-secondary"
              type="button"
              [disabled]="workingId === campaign.id"
              (click)="requestChanges(campaign)"
            >
              Request changes
            </button>
          </div>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      .admin-campaigns-page {
        display: grid;
        gap: 22px;
      }
      .hero-card {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 20px;
        flex-wrap: wrap;
        padding: 30px;
        border-radius: 32px;
        background: linear-gradient(135deg, #f6fdf8, #fff7ed);
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 24px 58px rgba(15, 23, 42, 0.08);
      }
      .eyebrow {
        display: inline-flex;
        padding: 7px 12px;
        border-radius: 999px;
        background: #ecfdf5;
        color: #047857;
        font-size: 0.74rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.08em;
      }
      h1 {
        margin: 10px 0 0;
        color: #1f2937;
        font-size: clamp(2rem, 4vw, 3rem);
        letter-spacing: -0.05em;
      }
      .hero-card p {
        margin: 10px 0 0;
        color: #667085;
        line-height: 1.7;
        font-weight: 650;
      }
      .filter {
        display: grid;
        gap: 8px;
        min-width: 220px;
      }
      .filter span,
      .note-field span {
        color: #667085;
        font-size: 0.76rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
      select,
      textarea {
        width: 100%;
        border: 1px solid rgba(15, 23, 42, 0.1);
        border-radius: 16px;
        padding: 12px 13px;
        background: #fff;
        color: #1f2937;
        font: inherit;
        font-weight: 750;
        outline: none;
        box-sizing: border-box;
      }
      .campaign-list {
        display: grid;
        gap: 16px;
      }
      .campaign-card {
        display: grid;
        gap: 16px;
        padding: 22px;
        border-radius: 28px;
        background: #fff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 44px rgba(15, 23, 42, 0.07);
      }
      .main {
        display: grid;
        grid-template-columns: minmax(0, 1fr) 360px;
        gap: 18px;
        align-items: start;
      }
      .campaign-card h2 {
        margin: 10px 0 8px;
        color: #1f2937;
        letter-spacing: -0.035em;
      }
      .campaign-card p {
        margin: 0;
        color: #667085;
        line-height: 1.65;
        font-weight: 600;
      }
      .status {
        display: inline-flex;
        padding: 7px 11px;
        border-radius: 999px;
        font-size: 0.74rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }
      .status-draft {
        background: #fff7ed;
        color: #b45309;
      }
      .status-review {
        background: #eff6ff;
        color: #1d4ed8;
      }
      .status-published {
        background: #ecfdf5;
        color: #047857;
      }
      .status-changes {
        background: #fef2f2;
        color: #b42318;
      }
      dl {
        display: grid;
        gap: 8px;
        margin: 0;
      }
      dl div {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        padding: 10px 12px;
        border-radius: 14px;
        background: #f8fafc;
      }
      dt {
        color: #98a2b3;
        font-weight: 900;
      }
      dd {
        margin: 0;
        color: #344054;
        font-weight: 900;
        text-align: right;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .note-field {
        display: grid;
        gap: 8px;
      }
      .actions {
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        flex-wrap: wrap;
      }
      .btn {
        min-height: 42px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border: 0;
        border-radius: 999px;
        padding: 0 16px;
        font: inherit;
        font-weight: 950;
        text-decoration: none;
        cursor: pointer;
      }
      .btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
      .btn-primary {
        color: #fff;
        background: linear-gradient(135deg, #059669, #047857);
        box-shadow: 0 14px 30px rgba(4, 120, 87, 0.22);
      }
      .btn-secondary {
        color: #991b1b;
        background: #fee2e2;
        border: 1px solid rgba(239, 68, 68, 0.18);
      }
      .btn-ghost {
        color: #344054;
        background: #f2f4f7;
      }
      .btn-dark {
        color: #fff;
        background: #111827;
        box-shadow: 0 14px 30px rgba(17, 24, 39, 0.2);
      }
      .loading-card,
      .empty-card {
        padding: 30px;
        border-radius: 26px;
        background: #fff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        text-align: center;
        color: #475467;
        font-weight: 850;
      }
      .empty-card h2 {
        margin: 0 0 8px;
        color: #1f2937;
      }
      .error,
      .success {
        margin: 0;
        padding: 14px 16px;
        border-radius: 18px;
        font-weight: 900;
        border: 1px solid transparent;
      }
      .error {
        background: #fef2f2;
        color: #b42318;
        border-color: rgba(239, 68, 68, 0.16);
      }
      .success {
        background: #ecfdf5;
        color: #047857;
        border-color: rgba(16, 185, 129, 0.16);
      }
      @media (max-width: 950px) {
        .main {
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
        },
        error: (err) => {
          console.error('[AdminCampaignPages] load failed:', err);
          this.error = err?.error?.message || 'Unable to load campaign pages.';
          this.campaigns = [];
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

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') return '—';
    return String(value)
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  trackByCampaignId(_index: number, campaign: CampaignPageResponse): number {
    return campaign.id;
  }
}