import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  CampaignPageResponse,
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

type CampaignFilter = 'ALL' | CrowdfundingType.DONATION | CrowdfundingType.EQUITY;
type CampaignSort = 'featured' | 'progress' | 'goal' | 'recent';

@Component({
  selector: 'app-public-campaigns-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="marketplace-page">
      <header class="market-hero">
        <div class="hero-copy">
          <span class="eyebrow">Helma marketplace</span>
          <h1>Discover youth-led campaigns.</h1>
          <p>
            Browse approved donation and equity campaigns, review their public story,
            check progress, and support the next generation of founders.
          </p>
        </div>

        <div class="hero-card leaf-fallback">
          <span>Verified</span>
          <strong>{{ campaigns().length }}</strong>
          <small>published campaigns</small>
        </div>
      </header>

      <section class="toolbar">
        <label class="search-box">
          <span>Search</span>
          <input
            type="search"
            [ngModel]="search()"
            (ngModelChange)="onSearchChange($event)"
            placeholder="Search by title, business, sector, city..."
          />
        </label>

        <label>
          <span>Type</span>
          <select [ngModel]="typeFilter()" (ngModelChange)="onTypeChange($event)">
            <option value="ALL">All campaigns</option>
            <option [value]="crowdfundingType.DONATION">Donation</option>
            <option [value]="crowdfundingType.EQUITY">Equity</option>
          </select>
        </label>

        <label>
          <span>Sort</span>
          <select [ngModel]="sortBy()" (ngModelChange)="onSortChange($event)">
            <option value="featured">Featured</option>
            <option value="progress">Funding progress</option>
            <option value="goal">Funding goal</option>
            <option value="recent">Recently updated</option>
          </select>
        </label>

        <label>
          <span>Per page</span>
          <select [ngModel]="pageSize()" (ngModelChange)="onPageSizeChange($event)">
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
        Loading published campaigns...
      </div>

      <section class="empty-card" *ngIf="!loading && filteredCampaigns().length === 0">
        <div class="empty-visual leaf-fallback"></div>
        <div>
          <span class="eyebrow muted">No results</span>
          <h2>No campaigns match your filters</h2>
          <p>Try another search term or switch the campaign type filter.</p>
        </div>
      </section>

      <section class="result-bar" *ngIf="!loading && filteredCampaigns().length > 0">
        <div>
          <strong>{{ filteredCampaigns().length }}</strong>
          <span>campaigns found</span>
        </div>
        <span>Page {{ currentPage() }} of {{ totalPages() }}</span>
      </section>

      <section class="campaign-grid" *ngIf="!loading && filteredCampaigns().length > 0">
        <article
          class="market-card"
          *ngFor="let campaign of pagedCampaigns(); trackBy: trackByCampaignId"
        >
          <div class="visual-strip" [class.has-image]="!!campaignImage(campaign)">
            <img *ngIf="campaignImage(campaign) as imageUrl" [src]="imageUrl" alt="Campaign visual" />
            <div class="leaf-fallback" *ngIf="!campaignImage(campaign)">
              <span>Helma</span>
            </div>
            <div class="visual-badges">
              <span>{{ formatLabel(campaign.applicationType) }}</span>
              <span>Verified</span>
            </div>
          </div>

          <div class="card-body">
            <div class="title-group">
              <h2>{{ campaign.title || campaign.businessName || 'Untitled campaign' }}</h2>
              <p>{{ campaign.businessName || 'Helma founder' }}</p>
            </div>

            <p class="summary">
              {{ campaign.subtitle || campaign.summary || 'Explore this verified campaign on Helma.' }}
            </p>

            <div class="info-tags">
              <span>{{ formatLocation(campaign) }}</span>
              <span>{{ formatLabel(campaign.sector) }}</span>
              <span *ngIf="campaign.equityDetail?.minInvestment">
                Min {{ formatMoney(campaign.equityDetail?.minInvestment, campaign.currency) }}
              </span>
              <span>{{ campaign.publicDocuments.length }} docs</span>
            </div>

            <div class="progress-shell">
              <div>
                <span>{{ formatMoney(campaign.investorsPledgedAmount, campaign.currency) }} raised</span>
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

            <a class="btn btn-primary" [routerLink]="['/campaigns', campaign.slug]">
              View campaign
              <span>→</span>
            </a>
          </div>
        </article>
      </section>

      <nav class="pagination" *ngIf="!loading && totalPages() > 1" aria-label="Campaign pagination">
        <button type="button" class="page-btn" [disabled]="currentPage() === 1" (click)="setPage(currentPage() - 1)">
          ← Previous
        </button>

        <div class="page-numbers">
          <button
            type="button"
            class="number-btn"
            *ngFor="let page of pageNumbers()"
            [class.active]="page === currentPage()"
            (click)="setPage(page)"
          >
            {{ page }}
          </button>
        </div>

        <button type="button" class="page-btn" [disabled]="currentPage() === totalPages()" (click)="setPage(currentPage() + 1)">
          Next →
        </button>
      </nav>
    </section>
  `,
  styles: [
    `
      .marketplace-page {
        display: grid;
        gap: 18px;
        padding-bottom: 28px;
      }

      .market-hero {
        position: relative;
        overflow: hidden;
        display: grid;
        grid-template-columns: minmax(0, 1fr) 190px;
        gap: 18px;
        align-items: stretch;
        padding: 24px;
        border-radius: 30px;
        color: #2f2414;
        background:
          radial-gradient(circle at 16% 18%, rgba(247, 215, 124, 0.56), transparent 30%),
          radial-gradient(circle at 92% 28%, rgba(4, 120, 87, 0.12), transparent 30%),
          linear-gradient(135deg, #fffaf0, #f7ebc3);
        border: 1px solid rgba(184, 130, 38, 0.16);
        box-shadow: 0 18px 44px rgba(74, 54, 18, 0.11);
      }

      .hero-copy { max-width: 860px; }

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

      .eyebrow.muted { background: #f2f4f7; color: #667085; }

      h1 {
        margin: 8px 0 0;
        max-width: 860px;
        font-size: clamp(2rem, 4.5vw, 3.8rem);
        line-height: 0.9;
        letter-spacing: -0.075em;
      }

      .market-hero p,
      .empty-card p {
        margin: 12px 0 0;
        max-width: 720px;
        color: #6f5a35;
        line-height: 1.62;
        font-size: 0.98rem;
        font-weight: 650;
      }

      .hero-card {
        position: relative;
        overflow: hidden;
        display: grid;
        align-content: end;
        min-height: 150px;
        padding: 18px;
        border-radius: 24px;
        border: 1px solid rgba(184, 130, 38, 0.16);
      }

      .hero-card span,
      .hero-card small {
        position: relative;
        z-index: 1;
        color: rgba(47, 36, 20, 0.62);
        font-weight: 900;
        text-transform: uppercase;
        letter-spacing: 0.06em;
        font-size: 0.7rem;
      }

      .hero-card strong {
        position: relative;
        z-index: 1;
        color: #2f2414;
        font-size: 3.2rem;
        line-height: 0.88;
        letter-spacing: -0.08em;
      }

      .toolbar {
        display: grid;
        grid-template-columns: minmax(0, 1fr) 160px 170px 130px;
        gap: 10px;
        padding: 12px;
        border-radius: 22px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 12px 30px rgba(15, 23, 42, 0.055);
      }

      .toolbar label {
        display: grid;
        gap: 6px;
      }

      .toolbar span,
      .result-bar span,
      .progress-shell span {
        display: block;
        color: #98a2b3;
        font-size: 0.68rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      input,
      select {
        width: 100%;
        border: 1px solid rgba(15, 23, 42, 0.09);
        border-radius: 14px;
        padding: 10px 12px;
        background: #f8fafc;
        color: #1f2937;
        font: inherit;
        font-size: 0.92rem;
        font-weight: 750;
        outline: none;
        box-sizing: border-box;
      }

      .result-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
        padding: 12px 14px;
        border-radius: 20px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
      }

      .result-bar > div {
        display: flex;
        align-items: baseline;
        gap: 8px;
      }

      .result-bar strong {
        color: #111827;
        font-size: 1.15rem;
        font-weight: 950;
      }

      .campaign-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 18px;
      }

      .market-card {
        overflow: hidden;
        min-width: 0;
        border-radius: 28px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 16px 40px rgba(15, 23, 42, 0.075);
        transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
      }

      .market-card:hover {
        transform: translateY(-4px);
        box-shadow: 0 24px 54px rgba(15, 23, 42, 0.12);
        border-color: rgba(184, 130, 38, 0.24);
      }

      .visual-strip {
        position: relative;
        height: 180px;
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

      .visual-badges {
        position: absolute;
        z-index: 2;
        left: 12px;
        right: 12px;
        top: 12px;
        display: flex;
        justify-content: space-between;
        gap: 7px;
        flex-wrap: wrap;
      }

      .visual-badges span {
        padding: 6px 8px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.88);
        color: #2f2414;
        font-size: 0.64rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
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
        width: 160px;
        height: 160px;
        border-radius: 42% 58% 45% 55%;
        border: 1px solid rgba(154, 106, 23, 0.14);
        transform: rotate(-28deg);
      }

      .leaf-fallback::before { left: -54px; top: -44px; }
      .leaf-fallback::after { right: -58px; bottom: -42px; transform: rotate(24deg); }

      .leaf-fallback span {
        position: relative;
        z-index: 1;
        color: rgba(47, 36, 20, 0.4);
        font-weight: 950;
        letter-spacing: 0.18em;
        text-transform: uppercase;
      }

      .card-body {
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

      .info-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
      }

      .info-tags span {
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

      .progress-shell > div:first-child strong {
        color: #047857;
      }

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

      .btn {
        min-height: 40px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        border-radius: 999px;
        padding: 0 14px;
        border: 0;
        font: inherit;
        font-size: 0.86rem;
        font-weight: 950;
        text-decoration: none;
        cursor: pointer;
      }

      .btn-primary {
        color: #271a08;
        background: linear-gradient(135deg, #f7d77c, #d6a13d);
        box-shadow: 0 10px 22px rgba(184, 130, 38, 0.18);
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

      @media (max-width: 980px) {
        .market-hero,
        .toolbar,
        .empty-card {
          grid-template-columns: 1fr;
        }
      }

      @media (max-width: 640px) {
        .visual-strip {
          height: 190px;
        }
        .empty-card {
          text-align: center;
          justify-items: center;
        }
      }
    `,
  ],
})
export class PublicCampaignsPageComponent implements OnInit {
  private readonly crowdfundingService = inject(CrowdfundingService);

  readonly crowdfundingType = CrowdfundingType;

  campaigns = signal<CampaignPageResponse[]>([]);
  search = signal('');
  typeFilter = signal<CampaignFilter>('ALL');
  sortBy = signal<CampaignSort>('featured');
  currentPage = signal(1);
  pageSize = signal(8);
  loading = false;
  error: string | null = null;

  filteredCampaigns = computed(() => {
    const term = this.search().trim().toLowerCase();
    const type = this.typeFilter();
    const sort = this.sortBy();

    let items = this.campaigns().filter((campaign) => {
      if (type !== 'ALL' && campaign.applicationType !== type) return false;
      if (!term) return true;

      const haystack = [
        campaign.title,
        campaign.subtitle,
        campaign.businessName,
        campaign.summary,
        campaign.sector,
        campaign.subSector,
        campaign.city,
        campaign.governorate,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return haystack.includes(term);
    });

    items = [...items].sort((a, b) => {
      if (sort === 'progress') return this.progressPercent(b) - this.progressPercent(a);
      if (sort === 'goal') return Number(b.fundingGoal || 0) - Number(a.fundingGoal || 0);
      if (sort === 'recent') return new Date(b.updatedAt || 0).getTime() - new Date(a.updatedAt || 0).getTime();
      return this.featureScore(b) - this.featureScore(a);
    });

    return items;
  });

  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredCampaigns().length / this.pageSize())));

  pagedCampaigns = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.filteredCampaigns().slice(start, start + this.pageSize());
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;

    this.crowdfundingService
      .listPublicCampaignPages()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (items) => {
          this.campaigns.set(items ?? []);
          this.setPage(1);
        },
        error: (err) => {
          console.error('[PublicCampaigns] load failed:', err);
          this.error = err?.error?.message || 'Unable to load campaigns.';
          this.campaigns.set([]);
          this.setPage(1);
        },
      });
  }

  onSearchChange(value: string): void {
    this.search.set(value || '');
    this.setPage(1);
  }

  onTypeChange(value: string): void {
    this.typeFilter.set(value as CampaignFilter);
    this.setPage(1);
  }

  onSortChange(value: string): void {
    this.sortBy.set(value as CampaignSort);
    this.setPage(1);
  }

  onPageSizeChange(value: number | string): void {
    this.pageSize.set(Number(value) || 8);
    this.setPage(1);
  }

  setPage(page: number): void {
    this.currentPage.set(Math.max(1, Math.min(this.totalPages(), page)));
  }

  pageNumbers(): number[] {
    const total = this.totalPages();
    const windowSize = 5;
    let start = Math.max(1, this.currentPage() - 2);
    const end = Math.min(total, start + windowSize - 1);
    start = Math.max(1, end - windowSize + 1);

    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
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

  featureScore(campaign: CampaignPageResponse): number {
    return (
      this.progressPercent(campaign) * 3 +
      Number(campaign.publicDocuments?.length || 0) * 6 +
      (this.campaignImage(campaign) ? 20 : 0) +
      new Date(campaign.updatedAt || 0).getTime() / 100000000000
    );
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
