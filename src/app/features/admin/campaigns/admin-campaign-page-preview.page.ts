import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  CampaignContentJson,
  CampaignPageResponse,
  CampaignPageStatus,
  CampaignStyleJson,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';
import { CampaignPageRendererComponent } from '../../../shared/components/campaign-page-renderer.component';

const DEFAULT_STYLE: CampaignStyleJson = {
  fontFamily: 'Inter',
  primaryColor: '#111827',
  accentColor: '#C9A227',
  radius: 'large',
  heroLayout: 'centered',
  buttonStyle: 'pill',
};

@Component({
  selector: 'app-admin-campaign-page-preview-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CampaignPageRendererComponent],
  template: `
    <section class="admin-preview-page">
      <header class="topbar">
        <div>
          <a class="back-link" routerLink="/admin/campaigns">← Campaign queue</a>
          <span class="eyebrow">Compliance preview</span>
          <h1>{{ campaign?.title || campaign?.businessName || 'Campaign preview' }}</h1>
          <p>
            This is the authenticated admin preview. It works before publication and
            does not depend on the public <strong>/campaigns/:slug</strong> route.
          </p>
        </div>

        <div class="top-actions" *ngIf="campaign">
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
            [disabled]="working"
            (click)="publish()"
          >
            Publish
          </button>

          <button
            *ngIf="campaign.status === campaignStatus.PENDING_REVIEW || campaign.status === campaignStatus.PUBLISHED"
            class="btn btn-danger"
            type="button"
            [disabled]="working"
            (click)="requestChanges()"
          >
            Request changes
          </button>
        </div>
      </header>

      <p class="error" *ngIf="error">{{ error }}</p>
      <p class="success" *ngIf="success">{{ success }}</p>
      <div class="loading-card" *ngIf="loading">Loading campaign preview...</div>

      <section class="workspace" *ngIf="!loading && campaign">
        <main class="canvas-shell">
          <app-campaign-page-renderer
            [campaign]="campaign"
            [content]="content"
            [style]="style"
            [allowDocumentDownload]="false"
          />
        </main>

        <aside class="review-panel">
          <section class="panel-card status-card">
            <span class="panel-title">Review status</span>
            <strong [ngClass]="statusClass(campaign.status)">
              {{ formatLabel(campaign.status) }}
            </strong>
            <p *ngIf="campaign.status !== campaignStatus.PUBLISHED">
              The public link is locked until this campaign is published.
            </p>
            <p *ngIf="campaign.status === campaignStatus.PUBLISHED">
              This campaign is live for investors and donors.
            </p>
          </section>

          <section class="panel-card">
            <span class="panel-title">Official application</span>
            <dl>
              <div>
                <dt>Application</dt>
                <dd>#{{ campaign.applicationRaiseId }}</dd>
              </div>
              <div>
                <dt>Type</dt>
                <dd>{{ formatLabel(campaign.applicationType) }}</dd>
              </div>
              <div>
                <dt>Business</dt>
                <dd>{{ campaign.businessName || '—' }}</dd>
              </div>
              <div>
                <dt>Location</dt>
                <dd>{{ locationLabel }}</dd>
              </div>
              <div>
                <dt>Goal</dt>
                <dd>{{ formatMoney(campaign.fundingGoal, campaign.currency) }}</dd>
              </div>
            </dl>

            <a
              class="btn btn-ghost full"
              [routerLink]="['/admin/applications', campaign.applicationRaiseId]"
            >
              Open application review
            </a>
          </section>

          <section class="panel-card">
            <span class="panel-title">Selected public documents</span>
            <div class="doc-list" *ngIf="campaign.publicDocuments.length > 0; else noDocs">
              <div class="doc-item" *ngFor="let doc of campaign.publicDocuments">
                <strong>{{ doc.label || formatLabel(doc.docType) }}</strong>
                <span>{{ doc.fileName }}</span>
              </div>
            </div>
            <ng-template #noDocs>
              <p class="muted">No public documents selected.</p>
            </ng-template>
          </section>

          <section class="panel-card">
            <span class="panel-title">Founder note</span>
            <textarea
              [(ngModel)]="reviewNote"
              rows="4"
              placeholder="Explain what must change before publishing"
            ></textarea>

            <button
              *ngIf="campaign.status === campaignStatus.PENDING_REVIEW || campaign.status === campaignStatus.PUBLISHED"
              class="btn btn-danger full"
              type="button"
              [disabled]="working"
              (click)="requestChanges()"
            >
              Send changes request
            </button>
          </section>
        </aside>
      </section>
    </section>
  `,
  styles: [
    `
      .admin-preview-page {
        display: grid;
        gap: 18px;
      }

      .topbar {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 18px;
        flex-wrap: wrap;
        padding: 24px;
        border-radius: 30px;
        background: linear-gradient(135deg, #ffffff, #f8fafc);
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
      }

      .back-link {
        display: inline-flex;
        margin-bottom: 12px;
        color: #475467;
        text-decoration: none;
        font-weight: 900;
      }

      .eyebrow,
      .panel-title {
        display: inline-flex;
        color: #9a6a17;
        font-size: 0.74rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.08em;
      }

      h1 {
        margin: 8px 0;
        color: #111827;
        font-size: clamp(2rem, 4vw, 3.2rem);
        letter-spacing: -0.06em;
      }

      .topbar p {
        max-width: 760px;
        margin: 0;
        color: #667085;
        line-height: 1.7;
        font-weight: 650;
      }

      .top-actions {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
      }

      .workspace {
        display: grid;
        grid-template-columns: minmax(0, 1fr) 360px;
        gap: 18px;
        align-items: start;
      }

      .canvas-shell {
        min-width: 0;
        overflow: hidden;
        border-radius: 34px;
        background: #f3f6f9;
        border: 1px solid rgba(15, 23, 42, 0.08);
      }

      .review-panel {
        position: sticky;
        top: 16px;
        display: grid;
        gap: 14px;
      }

      .panel-card {
        display: grid;
        gap: 14px;
        padding: 18px;
        border-radius: 24px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
      }

      .status-card strong {
        width: fit-content;
        padding: 8px 12px;
        border-radius: 999px;
        font-size: 0.78rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      .status-card p,
      .muted {
        margin: 0;
        color: #667085;
        line-height: 1.55;
        font-weight: 700;
      }

      dl {
        display: grid;
        gap: 8px;
        margin: 0;
      }

      dl div {
        display: grid;
        grid-template-columns: 100px minmax(0, 1fr);
        gap: 10px;
        padding: 10px 12px;
        border-radius: 14px;
        background: #f8fafc;
      }

      dt {
        color: #98a2b3;
        font-weight: 950;
      }

      dd {
        min-width: 0;
        margin: 0;
        color: #344054;
        font-weight: 900;
        overflow: hidden;
        text-overflow: ellipsis;
        text-align: right;
      }

      .doc-list {
        display: grid;
        gap: 8px;
      }

      .doc-item {
        display: grid;
        gap: 4px;
        padding: 12px;
        border-radius: 16px;
        background: #f8fafc;
        border: 1px solid rgba(15, 23, 42, 0.06);
      }

      .doc-item strong {
        color: #1f2937;
        font-weight: 950;
      }

      .doc-item span {
        color: #667085;
        font-size: 0.85rem;
        font-weight: 700;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      textarea {
        width: 100%;
        resize: vertical;
        min-height: 96px;
        border: 1px solid rgba(15, 23, 42, 0.1);
        border-radius: 16px;
        padding: 12px 13px;
        font: inherit;
        color: #1f2937;
        outline: none;
        box-sizing: border-box;
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

      .btn.full {
        width: 100%;
      }

      .btn-primary {
        color: #fff;
        background: linear-gradient(135deg, #059669, #047857);
        box-shadow: 0 14px 30px rgba(4, 120, 87, 0.22);
      }

      .btn-danger {
        color: #991b1b;
        background: #fee2e2;
        border: 1px solid rgba(239, 68, 68, 0.18);
      }

      .btn-ghost {
        color: #344054;
        background: #f2f4f7;
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

      .loading-card,
      .error,
      .success {
        padding: 16px;
        border-radius: 18px;
        font-weight: 900;
      }

      .loading-card {
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        color: #475467;
        text-align: center;
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

      @media (max-width: 1100px) {
        .workspace {
          grid-template-columns: 1fr;
        }
        .review-panel {
          position: static;
        }
      }
    `,
  ],
})
export class AdminCampaignPagePreviewPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly crowdfundingService = inject(CrowdfundingService);

  readonly campaignStatus = CampaignPageStatus;

  campaign: CampaignPageResponse | null = null;
  content: CampaignContentJson = { blocks: [] };
  style: CampaignStyleJson = { ...DEFAULT_STYLE };
  reviewNote = '';
  loading = false;
  working = false;
  error: string | null = null;
  success: string | null = null;

  get locationLabel(): string {
    if (!this.campaign) return '—';
    return [this.campaign.city, this.campaign.governorate].filter(Boolean).join(', ') || '—';
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error = 'Campaign id is missing.';
      return;
    }

    this.load(id);
  }

  load(id: number): void {
    this.loading = true;
    this.error = null;
    this.success = null;

    this.crowdfundingService
      .getCampaignPageBuilder(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (campaign) => {
          this.campaign = campaign;
          this.reviewNote = campaign.reviewNote || '';
          this.content = this.parseContent(campaign.contentJson, campaign);
          this.style = this.parseStyle(campaign.styleJson);
        },
        error: (err) => {
          console.error('[AdminCampaignPreview] load failed:', err);
          this.error = err?.error?.message || 'Unable to load campaign preview.';
        },
      });
  }

  publish(): void {
    this.patchStatus(CampaignPageStatus.PUBLISHED);
  }

  requestChanges(): void {
    this.patchStatus(CampaignPageStatus.CHANGES_REQUESTED);
  }

  patchStatus(status: CampaignPageStatus): void {
    if (!this.campaign) return;

    this.working = true;
    this.error = null;
    this.success = null;

    this.crowdfundingService
      .adminPatchCampaignPageStatus(this.campaign.id, {
        status,
        reviewNote: this.reviewNote || null,
      })
      .pipe(finalize(() => (this.working = false)))
      .subscribe({
        next: (updated) => {
          this.campaign = updated;
          this.reviewNote = updated.reviewNote || '';
          this.content = this.parseContent(updated.contentJson, updated);
          this.style = this.parseStyle(updated.styleJson);
          this.success = `Campaign ${this.formatLabel(status).toLowerCase()}.`;
        },
        error: (err) => {
          console.error('[AdminCampaignPreview] status patch failed:', err);
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

  formatMoney(value: number | null | undefined, currency: string | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: currency || 'TND',
      maximumFractionDigits: 0,
    }).format(value);
  }

  private parseContent(value: string | null, campaign: CampaignPageResponse): CampaignContentJson {
    try {
      const parsed = JSON.parse(value || '') as Partial<CampaignContentJson>;
      if (Array.isArray(parsed.blocks)) return { blocks: parsed.blocks };
    } catch {
      // fallback below
    }

    return {
      blocks: [
        {
          id: 'hero',
          type: 'hero',
          size: 'full',
          title: campaign.title || campaign.businessName || 'Campaign',
          subtitle: campaign.subtitle || campaign.summary || '',
        },
      ],
    };
  }

  private parseStyle(value: string | null): CampaignStyleJson {
    try {
      return { ...DEFAULT_STYLE, ...(JSON.parse(value || '') as Partial<CampaignStyleJson>) };
    } catch {
      return { ...DEFAULT_STYLE };
    }
  }
}