import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import {
  ApplicationRaiseResponse,
  ApplicationRaiseStatus,
  CampaignPageResponse,
  CampaignPageStatus,
  CrowdfundingType,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

type CampaignApplicationRow = {
  application: ApplicationRaiseResponse;
  campaign: CampaignPageResponse | null;
};

type StudioContent = {
  sections?: Array<{
    elements?: Array<{
      type?: string | null;
      url?: string | null;
      text?: string | null;
    }>;
  }>;
};

@Component({
  selector: 'app-youth-campaign-pages-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="campaign-list-page">
      <header class="page-hero">
        <div class="hero-copy">
          <span class="eyebrow">Campaign Studio</span>
          <h1>Your campaign pages</h1>
          <p>
            Turn approved applications into investor-facing campaign pages. Start from a professional structure,
            then customize the story like a presentation.
          </p>
        </div>

        <a class="btn btn-light" routerLink="/youth/applications">
          <span>←</span>
          Back to applications
        </a>
      </header>

      <section class="overview-strip" *ngIf="!loading && rows.length > 0">
        <article>
          <span>Approved</span>
          <strong>{{ rows.length }}</strong>
        </article>
        <article>
          <span>Created</span>
          <strong>{{ createdCount }}</strong>
        </article>
        <article>
          <span>Published</span>
          <strong>{{ publishedCount }}</strong>
        </article>
        <article>
          <span>Need action</span>
          <strong>{{ needsActionCount }}</strong>
        </article>
      </section>

      <section class="list-toolbar" *ngIf="!loading && rows.length > 0">
        <div>
          <strong>{{ rows.length }}</strong>
          <span>approved applications ready for campaign work</span>
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

      <div class="loading-card" *ngIf="loading">
        <span class="loader"></span>
        Loading campaign workspace...
      </div>

      <section class="empty-card" *ngIf="!loading && rows.length === 0">
        <div class="empty-visual leaf-fallback"></div>
        <div>
          <span class="eyebrow muted">Locked for now</span>
          <h2>No approved applications yet</h2>
          <p>
            Campaign Studio unlocks when compliance approves an application.
            Once approved, you can design the public campaign page from here.
          </p>
        </div>
        <a class="btn btn-primary" routerLink="/youth/applications">View my applications</a>
      </section>

      <section class="campaign-grid" *ngIf="!loading && rows.length > 0">
        <article
          class="campaign-card"
          *ngFor="let row of pagedRows; trackBy: trackByAppId"
        >
          <div class="visual-panel" [class.has-image]="!!campaignImage(row)">
            <img *ngIf="campaignImage(row) as imageUrl" [src]="imageUrl" alt="Campaign visual" />
            <div class="leaf-fallback" *ngIf="!campaignImage(row)">
              <span>Helma</span>
            </div>

            <div class="floating-badges">
              <span>{{ formatLabel(row.application.type) }}</span>
              <span [ngClass]="campaignStatusClass(row.campaign)">
                {{ row.campaign ? formatLabel(row.campaign.status) : 'Not created' }}
              </span>
            </div>
          </div>

          <div class="card-content">
            <div class="title-group">
              <h2>{{ row.campaign?.title || row.application.businessName || 'Untitled campaign' }}</h2>
              <p>{{ row.application.businessName || 'Helma founder' }}</p>
            </div>

            <p class="summary">
              {{ row.campaign?.subtitle || row.application.summary || 'No summary added yet. Add a short investor-facing introduction in the studio.' }}
            </p>

            <div class="meta-tags">
              <span>{{ formatLocation(row.application) }}</span>
              <span>{{ formatLabel(row.application.sector) }}</span>
              <span>{{ row.campaign?.publicDocuments?.length || 0 }} docs</span>
            </div>

            <div class="progress-shell">
              <div>
                <span>{{ formatMoney(row.application.investorsPledgedAmount, row.application.currency) }} raised</span>
                <strong>{{ progressPercent(row.application) }}%</strong>
              </div>
              <div class="progress-track">
                <span [style.width.%]="progressPercent(row.application)"></span>
              </div>
              <div>
                <span>Goal</span>
                <strong>{{ formatMoney(row.application.fundingGoal, row.application.currency) }}</strong>
              </div>
            </div>

            <div class="mini-info">
              <span>Updated {{ formatDate(row.campaign?.updatedAt || row.application.updatedAt) }}</span>
              <span>{{ row.campaign ? '/campaigns/' + row.campaign.slug : 'No public link yet' }}</span>
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
                class="btn btn-soft"
                *ngIf="row.campaign"
                [routerLink]="['/youth/campaigns', row.campaign.id, 'preview']"
              >
                Preview
              </a>

              <a
                class="btn btn-ghost"
                *ngIf="row.campaign?.status === campaignStatus.PUBLISHED"
                [href]="'/campaigns/' + row.campaign!.slug"
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
      .campaign-list-page {
        display: grid;
        gap: 18px;
      }

      .page-hero {
        position: relative;
        overflow: hidden;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 18px;
        flex-wrap: wrap;
        padding: 24px;
        border-radius: 30px;
        color: #2f2414;
        border: 1px solid rgba(184, 130, 38, 0.16);
        background:
          radial-gradient(circle at 12% 18%, rgba(247, 215, 124, 0.5), transparent 30%),
          radial-gradient(circle at 92% 20%, rgba(4, 120, 87, 0.11), transparent 30%),
          linear-gradient(135deg, #fffaf0, #f7ebc3);
        box-shadow: 0 18px 46px rgba(74, 54, 18, 0.1);
      }

      .hero-copy {
        max-width: 820px;
      }

      .eyebrow {
        display: inline-flex;
        width: max-content;
        padding: 6px 11px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.74);
        color: #9a6a17;
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
        font-size: clamp(2.2rem, 4vw, 3.8rem);
        line-height: 0.92;
        letter-spacing: -0.075em;
      }

      .page-hero p,
      .empty-card p {
        margin: 12px 0 0;
        color: #6f5a35;
        line-height: 1.65;
        font-weight: 650;
      }

      .overview-strip {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 10px;
      }

      .overview-strip article,
      .list-toolbar {
        border-radius: 22px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 12px 30px rgba(15, 23, 42, 0.055);
      }

      .overview-strip article {
        display: grid;
        gap: 4px;
        padding: 16px;
      }

      .overview-strip span,
      .list-toolbar span,
      .progress-shell span,
      .mini-info span {
        color: #98a2b3;
        font-size: 0.7rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      .overview-strip strong {
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

      select {
        border: 1px solid rgba(15, 23, 42, 0.1);
        border-radius: 14px;
        padding: 9px 12px;
        background: #f8fafc;
        color: #111827;
        font: inherit;
        font-weight: 850;
        outline: none;
      }

      .campaign-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 18px;
      }

      .campaign-card {
        overflow: hidden;
        min-width: 0;
        border-radius: 28px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 16px 40px rgba(15, 23, 42, 0.075);
        transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
      }

      .campaign-card:hover {
        transform: translateY(-4px);
        box-shadow: 0 24px 54px rgba(15, 23, 42, 0.12);
        border-color: rgba(184, 130, 38, 0.24);
      }

      .visual-panel {
        position: relative;
        height: 178px;
        overflow: hidden;
        background: #fff7ed;
      }

      .visual-panel::after {
        content: '';
        position: absolute;
        inset: 0;
        background: linear-gradient(180deg, rgba(17, 24, 39, 0.08), transparent 42%, rgba(17, 24, 39, 0.24));
        pointer-events: none;
      }

      .visual-panel img {
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

      .floating-badges {
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

      .floating-badges span {
        max-width: 100%;
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

      .floating-badges .status-none { color: #b45309; }
      .floating-badges .status-draft { color: #b45309; }
      .floating-badges .status-review { color: #1d4ed8; }
      .floating-badges .status-published { color: #047857; }
      .floating-badges .status-changes { color: #b42318; }

      .card-content {
        display: grid;
        gap: 13px;
        padding: 16px;
      }

      .title-group h2 {
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

      .title-group p {
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
        margin: 0;
        color: #667085;
        line-height: 1.5;
        font-size: 0.92rem;
        font-weight: 650;
      }

      .meta-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
      }

      .meta-tags span {
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

      .actions {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
      }

      .actions .btn-primary {
        grid-column: 1 / -1;
      }

      .btn {
        min-height: 38px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        border-radius: 999px;
        padding: 0 14px;
        border: 0;
        font: inherit;
        font-size: 0.84rem;
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

      .btn-light {
        color: #2f2414;
        background: rgba(255, 255, 255, 0.72);
        border: 1px solid rgba(184, 130, 38, 0.15);
      }

      .btn-soft {
        color: #047857;
        background: #ecfdf5;
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
        grid-template-columns: 180px minmax(0, 1fr) auto;
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

      .error {
        margin: 0;
        padding: 12px 14px;
        border-radius: 16px;
        background: #fef2f2;
        color: #b42318;
        border: 1px solid rgba(239, 68, 68, 0.16);
        font-weight: 900;
      }

      @keyframes spin { to { transform: rotate(360deg); } }

      @media (min-width: 1480px) {
        .campaign-grid {
          grid-template-columns: repeat(4, minmax(0, 1fr));
        }
      }

      @media (max-width: 900px) {
        .overview-strip {
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
        .overview-strip {
          grid-template-columns: 1fr;
        }
        .visual-panel {
          height: 190px;
        }
        .actions {
          grid-template-columns: 1fr;
        }
        .actions .btn-primary {
          grid-column: auto;
        }
      }
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
  currentPage = 1;
  pageSize = 8;

  get createdCount(): number {
    return this.rows.filter((row) => !!row.campaign).length;
  }

  get publishedCount(): number {
    return this.rows.filter((row) => row.campaign?.status === CampaignPageStatus.PUBLISHED).length;
  }

  get needsActionCount(): number {
    return this.rows.filter(
      (row) => !row.campaign || row.campaign.status === CampaignPageStatus.CHANGES_REQUESTED,
    ).length;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.rows.length / this.pageSize));
  }

  get pagedRows(): CampaignApplicationRow[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.rows.slice(start, start + this.pageSize);
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

          this.setPage(1);
        },
        error: (err) => {
          console.error('[YouthCampaignPages] load failed:', err);
          this.error = err?.error?.message || 'Unable to load campaign workspace.';
          this.rows = [];
          this.setPage(1);
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
        next: (campaign) => {
          this.router.navigate(['/youth/campaigns', campaign.id, 'builder']);
        },
        error: (err) => {
          console.error('[YouthCampaignPages] create failed:', err);
          this.error = err?.error?.message || 'Unable to create campaign page.';
        },
      });
  }

  setPage(page: number): void {
    this.currentPage = Math.max(1, Math.min(this.totalPages, page));
  }

  campaignImage(row: CampaignApplicationRow): string | null {
    return row.campaign ? this.extractCampaignImage(row.campaign) : null;
  }

  extractCampaignImage(campaign: CampaignPageResponse): string | null {
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

  progressPercent(application: ApplicationRaiseResponse): number {
    const goal = Number(application.fundingGoal || 0);
    const raised = Number(application.investorsPledgedAmount || application.raisedAmount || 0);
    if (!goal || goal <= 0) return 0;
    return Math.max(0, Math.min(100, Math.round((raised / goal) * 100)));
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
        return 'status-draft';
      default:
        return 'status-none';
    }
  }

  formatLocation(application: ApplicationRaiseResponse): string {
    return [application.city, application.governorate].filter(Boolean).join(', ') || 'Tunisia';
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

  trackByAppId(_index: number, row: CampaignApplicationRow): number {
    return row.application.id;
  }
}
