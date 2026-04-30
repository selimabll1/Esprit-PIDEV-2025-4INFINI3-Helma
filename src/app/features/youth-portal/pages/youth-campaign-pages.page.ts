import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import {
  ApplicationRaiseResponse,
  ApplicationRaiseStatus,
  CampaignPageResponse,
  CampaignPageStatus,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

type CampaignApplicationRow = {
  application: ApplicationRaiseResponse;
  campaign: CampaignPageResponse | null;
};

@Component({
  selector: 'app-youth-campaign-pages-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="campaign-studio-page">
      <header class="hero-card">
        <div>
          <span class="eyebrow">Campaign Studio</span>
          <h1>Turn approved applications into public campaigns.</h1>
          <p>
            Build a polished investor-facing page after compliance approves your application.
            Keep your official data safe, then design the story with blocks, media, documents, and style.
          </p>
        </div>

        <a class="btn btn-ghost" routerLink="/youth/applications">Back to applications</a>
      </header>

      <p class="error" *ngIf="error">{{ error }}</p>

      <div class="loading-card" *ngIf="loading">
        <span class="loader"></span>
        Loading campaign workspace...
      </div>

      <section class="empty-card" *ngIf="!loading && rows.length === 0">
        <div class="empty-icon">✦</div>
        <h2>No approved applications yet</h2>
        <p>
          Campaign Builder unlocks only after an application is approved by compliance.
        </p>
        <a class="btn btn-primary" routerLink="/youth/applications">View my applications</a>
      </section>

      <section class="campaign-grid" *ngIf="!loading && rows.length > 0">
        <article class="campaign-card" *ngFor="let row of rows; trackBy: trackByAppId">
          <div class="card-top">
            <div>
              <span class="type-pill">{{ formatLabel(row.application.type) }}</span>
              <h2>{{ row.application.businessName || 'Untitled application' }}</h2>
              <p>{{ row.application.summary || 'No summary added to the application yet.' }}</p>
            </div>

            <span class="status-badge" [ngClass]="campaignStatusClass(row.campaign)">
              {{ row.campaign ? formatLabel(row.campaign.status) : 'Not created' }}
            </span>
          </div>

          <div class="details-grid">
            <div>
              <span>Funding goal</span>
              <strong>{{ formatMoney(row.application.fundingGoal, row.application.currency) }}</strong>
            </div>
            <div>
              <span>Sector</span>
              <strong>{{ formatLabel(row.application.sector) }}</strong>
            </div>
            <div>
              <span>Location</span>
              <strong>{{ formatLocation(row.application) }}</strong>
            </div>
          </div>

          <div class="link-card" *ngIf="row.campaign">
            <span>Public link</span>
            <strong>/campaigns/{{ row.campaign.slug }}</strong>
          </div>

          <div class="actions">
            <button
              class="btn btn-primary"
              type="button"
              [disabled]="workingApplicationId === row.application.id"
              (click)="openBuilder(row)"
            >
              {{ row.campaign ? 'Edit campaign' : 'Create campaign' }}
            </button>

            <a
              class="btn btn-secondary"
              *ngIf="row.campaign"
              [routerLink]="['/youth/campaigns', row.campaign.id, 'preview']"
            >
              Preview
            </a>
          </div>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      .campaign-studio-page { display: grid; gap: 24px; }
      .hero-card {
        display: flex; justify-content: space-between; align-items: flex-start; gap: 22px; flex-wrap: wrap;
        padding: 34px; border-radius: 34px; border: 1px solid rgba(184,130,38,.16);
        background: radial-gradient(circle at 14% 18%, rgba(245,190,92,.3), transparent 28%), linear-gradient(135deg,#fffaf0,#f7ebc3);
        box-shadow: 0 24px 60px rgba(74,54,18,.11);
      }
      .eyebrow { display:inline-flex; width:max-content; padding:7px 13px; border-radius:999px; background:rgba(255,255,255,.72); color:#9a6a17; font-weight:950; font-size:.74rem; letter-spacing:.08em; text-transform:uppercase; }
      h1 { margin: 12px 0 0; max-width: 780px; font-size: clamp(2rem,4vw,3.15rem); line-height: 1; color:#2f2414; letter-spacing:-.05em; }
      .hero-card p { margin: 14px 0 0; max-width: 720px; color:#6f5a35; line-height:1.75; font-weight:650; }
      .campaign-grid { display:grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap:18px; }
      .campaign-card { display:grid; gap:18px; padding:22px; border-radius:28px; background:#fff; border:1px solid rgba(15,23,42,.08); box-shadow:0 20px 48px rgba(15,23,42,.08); }
      .card-top { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; }
      .campaign-card h2 { margin:8px 0; color:#1f2937; letter-spacing:-.03em; }
      .campaign-card p { margin:0; color:#667085; line-height:1.65; font-weight:600; }
      .type-pill { display:inline-flex; padding:6px 11px; border-radius:999px; background:#ecfdf5; color:#047857; font-weight:950; font-size:.72rem; text-transform:uppercase; letter-spacing:.06em; }
      .status-badge { white-space:nowrap; padding:8px 12px; border-radius:999px; font-size:.76rem; font-weight:950; text-transform:uppercase; }
      .status-none { background:#f2f4f7; color:#475467; }
      .status-draft { background:#fff7ed; color:#b45309; }
      .status-review { background:#eff6ff; color:#1d4ed8; }
      .status-published { background:#ecfdf5; color:#047857; }
      .status-changes { background:#fef2f2; color:#b42318; }
      .details-grid { display:grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap:10px; }
      .details-grid div, .link-card { padding:13px; border-radius:18px; background:#f8fafc; border:1px solid rgba(15,23,42,.06); min-width:0; }
      .details-grid span, .link-card span { display:block; color:#98a2b3; font-size:.72rem; font-weight:950; text-transform:uppercase; letter-spacing:.05em; margin-bottom:6px; }
      .details-grid strong, .link-card strong { display:block; color:#28303f; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
      .actions { display:flex; justify-content:flex-end; gap:10px; flex-wrap:wrap; }
      .btn { min-height:42px; display:inline-flex; align-items:center; justify-content:center; border:0; border-radius:999px; padding:0 16px; font:inherit; font-weight:950; text-decoration:none; cursor:pointer; }
      .btn:disabled { opacity:.62; cursor:not-allowed; }
      .btn-primary { color:#271a08; background:linear-gradient(135deg,#f7d77c,#d6a13d); box-shadow:0 14px 30px rgba(184,130,38,.22); }
      .btn-secondary { color:#271a08; background:#fff0c7; border:1px solid rgba(184,130,38,.18); }
      .btn-ghost { color:#344054; background:#fff; border:1px solid rgba(15,23,42,.1); }
      .loading-card, .empty-card { display:grid; justify-items:center; text-align:center; gap:14px; padding:38px 24px; border-radius:28px; background:#fff; border:1px solid rgba(15,23,42,.08); box-shadow:0 18px 40px rgba(15,23,42,.07); color:#475467; font-weight:800; }
      .empty-icon { width:72px; height:72px; display:grid; place-items:center; border-radius:24px; background:#fff7ed; font-size:2rem; }
      .empty-card h2 { margin:0; color:#1f2937; }
      .empty-card p { margin:0; max-width:520px; line-height:1.7; }
      .loader { width:34px; height:34px; border-radius:50%; border:4px solid rgba(184,130,38,.16); border-top-color:#d6a13d; animation:spin .75s linear infinite; }
      .error { margin:0; padding:14px 16px; border-radius:18px; background:#fef2f2; color:#b42318; border:1px solid rgba(239,68,68,.16); font-weight:900; }
      @keyframes spin { to { transform:rotate(360deg); } }
      @media (max-width: 1100px) { .campaign-grid { grid-template-columns:1fr; } }
      @media (max-width: 760px) { .hero-card { padding:24px; } .details-grid { grid-template-columns:1fr; } .actions .btn { flex:1; } }
    `,
  ],
})
export class YouthCampaignPagesPageComponent implements OnInit {
  private readonly crowdfundingService = inject(CrowdfundingService);
  private readonly router = inject(Router);

  readonly campaignStatus = CampaignPageStatus;

  rows: CampaignApplicationRow[] = [];
  loading = false;
  error: string | null = null;
  workingApplicationId: number | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;

    forkJoin({
      applications: this.crowdfundingService.listMyApplications({
        status: ApplicationRaiseStatus.APPROVED,
        sortBy: 'updatedAt',
        sortDir: 'desc',
      }),
      campaigns: this.crowdfundingService.listMyCampaignPages(),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ applications, campaigns }) => {
          const campaignByApplicationId = new Map<number, CampaignPageResponse>();
          (campaigns ?? []).forEach((campaign) => {
            campaignByApplicationId.set(campaign.applicationRaiseId, campaign);
          });

          this.rows = (applications ?? [])
            .filter((app) => app.status === ApplicationRaiseStatus.APPROVED)
            .map((application) => ({
              application,
              campaign: campaignByApplicationId.get(application.id) ?? null,
            }));
        },
        error: () => {
          this.error = 'Unable to load campaign workspace.';
          this.rows = [];
        },
      });
  }

  openBuilder(row: CampaignApplicationRow): void {
    if (row.campaign) {
      this.router.navigate(['/youth/campaigns', row.campaign.id, 'builder']);
      return;
    }

    this.workingApplicationId = row.application.id;
    this.error = null;

    this.crowdfundingService
      .createCampaignPage(row.application.id)
      .pipe(finalize(() => (this.workingApplicationId = null)))
      .subscribe({
        next: (campaign) => this.router.navigate(['/youth/campaigns', campaign.id, 'builder']),
        error: () => {
          this.error = 'Unable to create a campaign for this application. Make sure it is approved.';
        },
      });
  }

  campaignStatusClass(campaign: CampaignPageResponse | null): string {
    if (!campaign) return 'status-none';

    switch (campaign.status) {
      case CampaignPageStatus.DRAFT:
        return 'status-draft';
      case CampaignPageStatus.PENDING_REVIEW:
        return 'status-review';
      case CampaignPageStatus.PUBLISHED:
        return 'status-published';
      case CampaignPageStatus.CHANGES_REQUESTED:
        return 'status-changes';
      case CampaignPageStatus.ARCHIVED:
        return 'status-none';
      default:
        return 'status-none';
    }
  }

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') return 'Not selected';

    return String(value)
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatLocation(app: ApplicationRaiseResponse): string {
    const parts = [app.governorate, app.city].filter(Boolean);
    return parts.length ? parts.join(', ') : 'Not selected';
  }

  formatMoney(value: number | null | undefined, currency: string | null | undefined): string {
    if (value === null || value === undefined) return '—';

    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: currency || 'TND',
      maximumFractionDigits: 0,
    }).format(value);
  }

  trackByAppId(_index: number, row: CampaignApplicationRow): number {
    return row.application.id;
  }
}
