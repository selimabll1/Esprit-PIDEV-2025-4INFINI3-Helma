import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  AppTag,
  SECTOR_OPTIONS,
  SUB_SECTOR_OPTIONS_BY_SECTOR,
  Sector,
  SubSector,
  TAG_OPTIONS,
} from '../../../core/models/application-taxonomy';
import {
  CampaignPageResponse,
  CrowdfundingType,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

type InvestorTypeFilter = 'ALL' | CrowdfundingType.DONATION | CrowdfundingType.EQUITY;
type InvestorSort = 'featured' | 'progress' | 'goal' | 'recent' | 'name';

type StudioContent = {
  sections?: Array<{
    background?: string | null;
    elements?: Array<{
      type?: string | null;
      url?: string | null;
      src?: string | null;
      imageUrl?: string | null;
    }>;
  }>;
};

@Component({
  selector: 'app-investor-campaigns-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="investor-discover-page">
      <header class="hero-panel">
        <div class="hero-copy">
          <span class="eyebrow">Investor marketplace</span>
          <h1>Discover vetted Helma campaigns.</h1>
          <p>
            Review published founder campaign pages, compare funding progress, and
            open a full investor preview before creating your pledge.
          </p>

          <div class="hero-metrics">
            <div>
              <strong>{{ campaigns().length }}</strong>
              <span>published campaigns</span>
            </div>
            <div>
              <strong>{{ equityCount() }}</strong>
              <span>equity opportunities</span>
            </div>
            <div>
              <strong>{{ formatMoney(totalGoal(), 'TND') }}</strong>
              <span>total funding goals</span>
            </div>
          </div>
        </div>

        <div class="hero-visual">
          <div class="orb orb-one"></div>
          <div class="orb orb-two"></div>
          <span>HELMA</span>
          <strong>Investor workspace</strong>
          <small>Verified campaigns only</small>
        </div>
      </header>

      <section class="toolbar-card">
        <label class="field field-wide">
          <span>Search opportunities</span>
          <input
            type="search"
            [ngModel]="search()"
            (ngModelChange)="onSearchChange($event)"
            placeholder="Search by campaign, business, sector, location, tag..."
          />
        </label>

        <label class="field">
          <span>Type</span>
          <select [ngModel]="typeFilter()" (ngModelChange)="onTypeChange($event)">
            <option value="ALL">All types</option>
            <option [value]="crowdfundingType.DONATION">Donation</option>
            <option [value]="crowdfundingType.EQUITY">Equity</option>
          </select>
        </label>

        <label class="field">
          <span>Sector</span>
          <select [ngModel]="sectorFilter()" (ngModelChange)="onSectorChange($event)">
            <option value="ALL">All sectors</option>
            <option *ngFor="let sector of sectorOptions" [value]="sector">
              {{ formatLabel(sector) }}
            </option>
          </select>
        </label>

        <label class="field">
          <span>Sub-sector</span>
          <select [ngModel]="subSectorFilter()" (ngModelChange)="onSubSectorChange($event)">
            <option value="ALL">All sub-sectors</option>
            <option *ngFor="let subSector of availableSubSectors()" [value]="subSector">
              {{ formatLabel(subSector) }}
            </option>
          </select>
        </label>

        <label class="field">
          <span>Tag</span>
          <select [ngModel]="tagFilter()" (ngModelChange)="onTagChange($event)">
            <option value="ALL">All tags</option>
            <option *ngFor="let tag of tagOptions" [value]="tag">{{ formatLabel(tag) }}</option>
          </select>
        </label>

        <label class="field">
          <span>Sort</span>
          <select [ngModel]="sortBy()" (ngModelChange)="onSortChange($event)">
            <option value="featured">Featured</option>
            <option value="progress">Funding progress</option>
            <option value="goal">Funding goal</option>
            <option value="recent">Recently updated</option>
            <option value="name">Business name</option>
          </select>
        </label>

        <label class="field compact-field">
          <span>Per page</span>
          <select [ngModel]="pageSize()" (ngModelChange)="onPageSizeChange($event)">
            <option [ngValue]="6">6</option>
            <option [ngValue]="8">8</option>
            <option [ngValue]="12">12</option>
            <option [ngValue]="16">16</option>
          </select>
        </label>

        <div class="toolbar-actions">
          <button type="button" class="reset-btn" (click)="resetFilters()">Reset</button>
          <button type="button" class="refresh-btn" (click)="load()">Refresh</button>
        </div>
      </section>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <section class="loading-card" *ngIf="loading()">
        <span class="loader"></span>
        Loading investor opportunities...
      </section>

      <section class="empty-card" *ngIf="!loading() && filteredCampaigns().length === 0">
        <div class="empty-icon">◇</div>
        <div>
          <span class="eyebrow muted">No opportunities</span>
          <h2>No campaigns match your filters</h2>
          <p>Try another search term, sector, or investment type.</p>
        </div>
      </section>

      <section class="result-bar" *ngIf="!loading() && filteredCampaigns().length > 0">
        <div>
          <strong>{{ filteredCampaigns().length }}</strong>
          <span>matching campaigns</span>
        </div>

        <span>Page {{ currentPage() }} of {{ totalPages() }}</span>
      </section>

      <section class="campaign-grid" *ngIf="!loading() && filteredCampaigns().length > 0">
        <article
          class="campaign-card"
          *ngFor="let campaign of pagedCampaigns(); trackBy: trackByCampaignId"
        >
          <div class="visual-strip" [class.has-image]="!!campaignImage(campaign)">
            <img
              *ngIf="campaignImage(campaign) as imageUrl"
              [src]="imageUrl"
              alt="Campaign visual"
            />

            <div class="visual-fallback" *ngIf="!campaignImage(campaign)">
              <span>Helma</span>
            </div>

            <div class="visual-badges">
              <span>{{ formatLabel(campaign.applicationType) }}</span>
              <span>{{ campaign.publicDocuments.length }} docs</span>
            </div>
          </div>

          <div class="card-body">
            <div class="title-block">
              <span class="status-pill">Published</span>
              <h2>{{ campaign.title || campaign.businessName || 'Untitled campaign' }}</h2>
              <p>{{ campaign.businessName || 'Helma founder' }}</p>
            </div>

            <p class="summary">
              {{ campaign.subtitle || campaign.summary || 'Open the campaign preview to review the full founder story.' }}
            </p>

            <div class="info-tags">
              <span>{{ formatLocation(campaign) }}</span>
              <span>{{ formatLabel(campaign.sector) }}</span>
              <span *ngIf="campaign.equityDetail?.minInvestment">
                Min {{ formatMoney(campaign.equityDetail?.minInvestment, campaign.currency) }}
              </span>
            </div>

            <div class="progress-box">
              <div class="progress-head">
                <span>{{ formatMoney(campaign.investorsPledgedAmount, campaign.currency) }} raised</span>
                <strong>{{ progressPercent(campaign) }}%</strong>
              </div>

              <div class="progress-track">
                <span [style.width.%]="progressPercent(campaign)"></span>
              </div>

              <div class="progress-foot">
                <span>Goal</span>
                <strong>{{ formatMoney(campaign.fundingGoal, campaign.currency) }}</strong>
              </div>
            </div>

            <div class="card-actions">
              <a class="btn btn-primary" [routerLink]="['/investor/campaigns', campaign.slug]">
                Investor preview
                <span>→</span>
              </a>

              <a class="btn btn-ghost" [routerLink]="['/campaigns', campaign.slug]" target="_blank">
                Public page
              </a>
            </div>
          </div>
        </article>
      </section>

      <nav class="pagination" *ngIf="!loading() && totalPages() > 1" aria-label="Campaign pagination">
        <button
          type="button"
          class="page-btn"
          [disabled]="currentPage() === 1"
          (click)="setPage(currentPage() - 1)"
        >
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

        <button
          type="button"
          class="page-btn"
          [disabled]="currentPage() === totalPages()"
          (click)="setPage(currentPage() + 1)"
        >
          Next →
        </button>
      </nav>
    </section>
  `,
  styles: [
    `
      .investor-discover-page {
        display: grid;
        gap: 18px;
        padding-bottom: 34px;
      }

      .hero-panel {
        position: relative;
        overflow: hidden;
        display: grid;
        grid-template-columns: minmax(0, 1fr) 250px;
        gap: 18px;
        align-items: stretch;
        padding: 26px;
        border-radius: 32px;
        color: #ffffff;
        background:
          radial-gradient(circle at 14% 12%, rgba(109, 183, 255, 0.34), transparent 34%),
          radial-gradient(circle at 100% 10%, rgba(243, 223, 152, 0.16), transparent 34%),
          linear-gradient(135deg, #071a3a, #092549 56%, #06152e);
        border: 1px solid rgba(123, 185, 255, 0.18);
        box-shadow: 0 24px 60px rgba(7, 26, 58, 0.18);
      }

      .hero-copy {
        max-width: 900px;
      }

      .eyebrow {
        display: inline-flex;
        width: max-content;
        padding: 7px 12px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.1);
        color: #f3df98;
        font-size: 0.72rem;
        font-weight: 950;
        letter-spacing: 0.09em;
        text-transform: uppercase;
      }

      .eyebrow.muted {
        color: #46627f;
        background: #edf5ff;
      }

      h1 {
        margin: 10px 0 0;
        max-width: 820px;
        color: #ffffff;
        font-size: clamp(2.2rem, 5vw, 4rem);
        line-height: 0.92;
        letter-spacing: -0.075em;
      }

      .hero-copy p {
        max-width: 780px;
        margin: 14px 0 0;
        color: rgba(234, 246, 255, 0.76);
        font-size: 1rem;
        font-weight: 650;
        line-height: 1.7;
      }

      .hero-metrics {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
        margin-top: 22px;
      }

      .hero-metrics div {
        min-width: 150px;
        display: grid;
        gap: 4px;
        padding: 14px;
        border-radius: 20px;
        background: rgba(255, 255, 255, 0.09);
        border: 1px solid rgba(255, 255, 255, 0.1);
      }

      .hero-metrics strong {
        color: #ffffff;
        font-size: 1.35rem;
        font-weight: 950;
        letter-spacing: -0.04em;
      }

      .hero-metrics span {
        color: rgba(185, 220, 255, 0.74);
        font-size: 0.78rem;
        font-weight: 850;
      }

      .hero-visual {
        position: relative;
        overflow: hidden;
        min-height: 230px;
        display: grid;
        align-content: end;
        gap: 8px;
        padding: 20px;
        border-radius: 28px;
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.12);
      }

      .hero-visual span,
      .hero-visual strong,
      .hero-visual small {
        position: relative;
        z-index: 1;
      }

      .hero-visual span {
        width: max-content;
        padding: 7px 11px;
        border-radius: 999px;
        color: #071a3a;
        background: #f3df98;
        font-size: 0.72rem;
        font-weight: 950;
        letter-spacing: 0.12em;
      }

      .hero-visual strong {
        max-width: 180px;
        color: #ffffff;
        font-size: 1.55rem;
        line-height: 1;
        letter-spacing: -0.06em;
      }

      .hero-visual small {
        color: rgba(234, 246, 255, 0.74);
        font-weight: 800;
      }

      .orb {
        position: absolute;
        border-radius: 999px;
        filter: blur(1px);
      }

      .orb-one {
        width: 132px;
        height: 132px;
        right: -34px;
        top: -24px;
        background: radial-gradient(circle, rgba(109, 183, 255, 0.38), transparent 68%);
      }

      .orb-two {
        width: 140px;
        height: 140px;
        left: -54px;
        bottom: -40px;
        background: radial-gradient(circle, rgba(243, 223, 152, 0.2), transparent 70%);
      }

      .toolbar-card {
        display: grid;
        grid-template-columns: minmax(240px, 1.6fr) repeat(6, minmax(145px, 1fr)) auto;
        gap: 12px;
        align-items: end;
        padding: 16px;
        border-radius: 28px;
        background: rgba(255, 255, 255, 0.86);
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 44px rgba(7, 26, 58, 0.08);
        backdrop-filter: blur(16px);
      }

      .field {
        min-width: 0;
        display: grid;
        gap: 7px;
      }

      .field span {
        color: #354b67;
        font-size: 0.74rem;
        font-weight: 950;
        letter-spacing: 0.04em;
        text-transform: uppercase;
      }

      input,
      select {
        width: 100%;
        min-height: 44px;
        border: 1px solid rgba(71, 103, 136, 0.18);
        border-radius: 15px;
        padding: 0 12px;
        color: #071a3a;
        background: #ffffff;
        font: inherit;
        font-weight: 750;
        box-sizing: border-box;
        outline: none;
      }

      input:focus,
      select:focus {
        border-color: rgba(74, 163, 255, 0.58);
        box-shadow: 0 0 0 4px rgba(74, 163, 255, 0.12);
      }

      .toolbar-actions {
        display: flex;
        gap: 8px;
      }

      .reset-btn,
      .refresh-btn,
      .btn,
      .page-btn,
      .number-btn {
        border: 0;
        font: inherit;
        cursor: pointer;
        text-decoration: none;
        transition: 0.18s ease;
      }

      .reset-btn,
      .refresh-btn {
        min-height: 44px;
        padding: 0 14px;
        border-radius: 15px;
        font-weight: 950;
      }

      .reset-btn {
        color: #071a3a;
        background: #edf5ff;
      }

      .refresh-btn {
        color: #ffffff;
        background: #092549;
      }

      .error {
        margin: 0;
        padding: 14px 16px;
        border-radius: 18px;
        color: #b42318;
        background: #fff1f0;
        border: 1px solid #ffd5d2;
        font-weight: 850;
      }

      .loading-card,
      .empty-card,
      .result-bar {
        border-radius: 26px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 44px rgba(7, 26, 58, 0.07);
      }

      .loading-card {
        min-height: 120px;
        display: grid;
        place-items: center;
        gap: 12px;
        color: #46627f;
        font-weight: 900;
      }

      .loader {
        width: 30px;
        height: 30px;
        border-radius: 999px;
        border: 3px solid #dbeafe;
        border-top-color: #0b4f8a;
        animation: spin 0.75s linear infinite;
      }

      @keyframes spin {
        to { transform: rotate(360deg); }
      }

      .empty-card {
        display: flex;
        gap: 16px;
        align-items: center;
        padding: 22px;
      }

      .empty-icon {
        width: 70px;
        height: 70px;
        flex: 0 0 auto;
        display: grid;
        place-items: center;
        border-radius: 24px;
        color: #f3df98;
        background: #071a3a;
        font-size: 2rem;
        font-weight: 950;
      }

      .empty-card h2 {
        margin: 8px 0 0;
        color: #071a3a;
        letter-spacing: -0.04em;
      }

      .empty-card p {
        margin: 6px 0 0;
        color: #667085;
        font-weight: 650;
      }

      .result-bar {
        display: flex;
        justify-content: space-between;
        gap: 14px;
        align-items: center;
        padding: 14px 16px;
        color: #46627f;
        font-weight: 850;
      }

      .result-bar div {
        display: flex;
        gap: 7px;
        align-items: baseline;
      }

      .result-bar strong {
        color: #071a3a;
        font-size: 1.1rem;
        font-weight: 950;
      }

      .campaign-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 18px;
      }

      .campaign-card {
        overflow: hidden;
        display: grid;
        grid-template-rows: 190px 1fr;
        border-radius: 30px;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 20px 54px rgba(7, 26, 58, 0.09);
      }

      .visual-strip {
        position: relative;
        overflow: hidden;
        background:
          radial-gradient(circle at 20% 10%, rgba(109, 183, 255, 0.32), transparent 32%),
          radial-gradient(circle at 94% 24%, rgba(243, 223, 152, 0.25), transparent 30%),
          linear-gradient(135deg, #071a3a, #0b345f);
      }

      .visual-strip img {
        width: 100%;
        height: 100%;
        display: block;
        object-fit: cover;
      }

      .visual-strip.has-image::after {
        content: '';
        position: absolute;
        inset: 0;
        background: linear-gradient(180deg, rgba(7, 26, 58, 0.06), rgba(7, 26, 58, 0.38));
      }

      .visual-fallback {
        height: 100%;
        display: grid;
        place-items: center;
      }

      .visual-fallback span {
        width: 86px;
        height: 86px;
        display: grid;
        place-items: center;
        border-radius: 999px;
        color: #071a3a;
        background: linear-gradient(135deg, #fffaf0, #f3df98);
        font-size: 0.78rem;
        font-weight: 950;
        letter-spacing: 0.12em;
        text-transform: uppercase;
      }

      .visual-badges {
        position: absolute;
        z-index: 1;
        left: 14px;
        right: 14px;
        top: 14px;
        display: flex;
        justify-content: space-between;
        gap: 8px;
      }

      .visual-badges span,
      .status-pill,
      .info-tags span {
        display: inline-flex;
        align-items: center;
        width: max-content;
        max-width: 100%;
        border-radius: 999px;
        font-size: 0.72rem;
        font-weight: 950;
      }

      .visual-badges span {
        padding: 6px 10px;
        color: #ffffff;
        background: rgba(7, 26, 58, 0.68);
        backdrop-filter: blur(12px);
      }

      .card-body {
        display: grid;
        gap: 14px;
        padding: 17px;
      }

      .status-pill {
        padding: 6px 10px;
        color: #0b4f8a;
        background: #e6f2ff;
      }

      .title-block h2 {
        margin: 9px 0 0;
        color: #071a3a;
        font-size: 1.22rem;
        line-height: 1.05;
        letter-spacing: -0.045em;
      }

      .title-block p,
      .summary {
        margin: 6px 0 0;
        color: #667085;
        font-weight: 750;
      }

      .summary {
        display: -webkit-box;
        min-height: 3.9em;
        overflow: hidden;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        line-height: 1.3;
      }

      .info-tags {
        display: flex;
        gap: 7px;
        flex-wrap: wrap;
      }

      .info-tags span {
        padding: 7px 9px;
        color: #354b67;
        background: #f3f7fb;
      }

      .progress-box {
        display: grid;
        gap: 8px;
        padding: 12px;
        border-radius: 20px;
        background: #f8fbff;
        border: 1px solid rgba(71, 103, 136, 0.12);
      }

      .progress-head,
      .progress-foot {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        color: #667085;
        font-size: 0.82rem;
        font-weight: 850;
      }

      .progress-head strong,
      .progress-foot strong {
        color: #071a3a;
        font-weight: 950;
      }

      .progress-track {
        height: 9px;
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

      .card-actions {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 9px;
        align-items: center;
      }

      .btn {
        min-height: 44px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        padding: 0 14px;
        border-radius: 15px;
        font-weight: 950;
      }

      .btn-primary {
        color: #ffffff;
        background: linear-gradient(135deg, #071a3a, #0b4f8a);
      }

      .btn-ghost {
        color: #071a3a;
        background: #edf5ff;
      }

      .btn:hover,
      .reset-btn:hover,
      .refresh-btn:hover,
      .page-btn:hover:not(:disabled),
      .number-btn:hover {
        transform: translateY(-1px);
      }

      .pagination {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        flex-wrap: wrap;
      }

      .page-numbers {
        display: flex;
        gap: 6px;
        flex-wrap: wrap;
      }

      .page-btn,
      .number-btn {
        min-height: 40px;
        border-radius: 14px;
        color: #071a3a;
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.1);
        font-weight: 950;
      }

      .page-btn {
        padding: 0 14px;
      }

      .number-btn {
        width: 40px;
      }

      .number-btn.active {
        color: #ffffff;
        background: #092549;
      }

      .page-btn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      @media (max-width: 1320px) {
        .toolbar-card {
          grid-template-columns: repeat(3, minmax(0, 1fr));
        }

        .field-wide {
          grid-column: 1 / -1;
        }

        .toolbar-actions {
          grid-column: 1 / -1;
        }

        .campaign-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }

      @media (max-width: 900px) {
        .hero-panel {
          grid-template-columns: 1fr;
        }

        .hero-visual {
          min-height: 170px;
        }

        .toolbar-card,
        .campaign-grid {
          grid-template-columns: 1fr;
        }

        .toolbar-actions,
        .field-wide {
          grid-column: auto;
        }
      }

      @media (max-width: 620px) {
        .hero-panel {
          padding: 20px;
          border-radius: 26px;
        }

        .hero-metrics,
        .result-bar,
        .empty-card {
          display: grid;
        }

        .card-actions {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class InvestorCampaignsPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly campaigns = signal<CampaignPageResponse[]>([]);

  readonly search = signal('');
  readonly typeFilter = signal<InvestorTypeFilter>('ALL');
  readonly sectorFilter = signal<Sector | 'ALL'>('ALL');
  readonly subSectorFilter = signal<SubSector | 'ALL'>('ALL');
  readonly tagFilter = signal<AppTag | 'ALL'>('ALL');
  readonly sortBy = signal<InvestorSort>('featured');
  readonly currentPage = signal(1);
  readonly pageSize = signal(6);

  readonly crowdfundingType = CrowdfundingType;
  readonly sectorOptions = SECTOR_OPTIONS;
  readonly tagOptions = TAG_OPTIONS;
  readonly allSubSectorOptions = Object.values(SubSector);

  readonly availableSubSectors = computed(() => {
    const sector = this.sectorFilter();
    if (sector === 'ALL') return this.allSubSectorOptions;
    return SUB_SECTOR_OPTIONS_BY_SECTOR[sector] ?? this.allSubSectorOptions;
  });

  readonly filteredCampaigns = computed(() => {
    const searchTerm = this.normalize(this.search());
    const type = this.typeFilter();
    const sector = this.sectorFilter();
    const subSector = this.subSectorFilter();
    const tag = this.tagFilter();
    const sortBy = this.sortBy();

    const filtered = this.campaigns().filter((campaign) => {
      if (type !== 'ALL' && campaign.applicationType !== type) return false;
      if (sector !== 'ALL' && campaign.sector !== sector) return false;
      if (subSector !== 'ALL' && campaign.subSector !== subSector) return false;
      if (tag !== 'ALL' && !campaign.tags?.includes(tag)) return false;

      if (!searchTerm) return true;

      const haystack = this.normalize([
        campaign.title,
        campaign.subtitle,
        campaign.businessName,
        campaign.summary,
        campaign.sector,
        campaign.subSector,
        campaign.governorate,
        campaign.city,
        ...(campaign.tags ?? []),
      ].join(' '));

      return haystack.includes(searchTerm);
    });

    return [...filtered].sort((a, b) => this.compareCampaigns(a, b, sortBy));
  });

  readonly totalPages = computed(() => {
    const total = Math.ceil(this.filteredCampaigns().length / this.pageSize());
    return Math.max(1, total || 1);
  });

  readonly pagedCampaigns = computed(() => {
    const safePage = Math.min(this.currentPage(), this.totalPages());
    const start = (safePage - 1) * this.pageSize();
    return this.filteredCampaigns().slice(start, start + this.pageSize());
  });

  readonly pageNumbers = computed(() => {
    const total = this.totalPages();
    const current = Math.min(this.currentPage(), total);
    const start = Math.max(1, current - 2);
    const end = Math.min(total, start + 4);
    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  });

  readonly equityCount = computed(
    () => this.campaigns().filter((campaign) => campaign.applicationType === CrowdfundingType.EQUITY).length,
  );

  readonly totalGoal = computed(() =>
    this.campaigns().reduce((sum, campaign) => sum + Number(campaign.fundingGoal ?? 0), 0),
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.service
      .listPublicCampaignPages()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (campaigns) => {
          this.campaigns.set(campaigns ?? []);
          this.setPage(1);
        },
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err)),
      });
  }

  onSearchChange(value: string): void {
    this.search.set(value ?? '');
    this.setPage(1);
  }

  onTypeChange(value: InvestorTypeFilter): void {
    this.typeFilter.set(value || 'ALL');
    this.setPage(1);
  }

  onSectorChange(value: Sector | 'ALL'): void {
    this.sectorFilter.set(value || 'ALL');

    const selectedSubSector = this.subSectorFilter();
    if (selectedSubSector !== 'ALL' && !this.availableSubSectors().includes(selectedSubSector)) {
      this.subSectorFilter.set('ALL');
    }

    this.setPage(1);
  }

  onSubSectorChange(value: SubSector | 'ALL'): void {
    this.subSectorFilter.set(value || 'ALL');
    this.setPage(1);
  }

  onTagChange(value: AppTag | 'ALL'): void {
    this.tagFilter.set(value || 'ALL');
    this.setPage(1);
  }

  onSortChange(value: InvestorSort): void {
    this.sortBy.set(value || 'featured');
    this.setPage(1);
  }

  onPageSizeChange(value: number): void {
    const parsed = Number(value);
    this.pageSize.set(Number.isFinite(parsed) && parsed > 0 ? parsed : 6);
    this.setPage(1);
  }

  resetFilters(): void {
    this.search.set('');
    this.typeFilter.set('ALL');
    this.sectorFilter.set('ALL');
    this.subSectorFilter.set('ALL');
    this.tagFilter.set('ALL');
    this.sortBy.set('featured');
    this.pageSize.set(6);
    this.setPage(1);
  }

  setPage(page: number): void {
    const next = Math.min(Math.max(1, page), this.totalPages());
    this.currentPage.set(next);
  }

  trackByCampaignId(_index: number, campaign: CampaignPageResponse): number {
    return campaign.id;
  }

  campaignImage(campaign: CampaignPageResponse): string | null {
    if (campaign.coverMediaUrl?.trim()) {
      return campaign.coverMediaUrl.trim();
    }

    const content = this.parseContent(campaign.contentJson);
    const elements = content.sections?.flatMap((section) => section.elements ?? []) ?? [];

    const imageElement = elements.find((element) => {
      const type = String(element.type ?? '').toLowerCase();
      return type === 'image' || type === 'image_box';
    });

    return (
      imageElement?.url?.trim() ||
      imageElement?.src?.trim() ||
      imageElement?.imageUrl?.trim() ||
      null
    );
  }

  progressPercent(campaign: CampaignPageResponse): number {
    const goal = Number(campaign.fundingGoal ?? 0);
    const raised = Number(campaign.investorsPledgedAmount ?? 0);
    if (!Number.isFinite(goal) || goal <= 0) return 0;
    return Math.min(100, Math.max(0, Math.round((raised / goal) * 100)));
  }

  formatLocation(campaign: CampaignPageResponse): string {
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

  private compareCampaigns(a: CampaignPageResponse, b: CampaignPageResponse, sortBy: InvestorSort): number {
    if (sortBy === 'progress') return this.progressPercent(b) - this.progressPercent(a);
    if (sortBy === 'goal') return Number(b.fundingGoal ?? 0) - Number(a.fundingGoal ?? 0);
    if (sortBy === 'recent') return new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime();
    if (sortBy === 'name') return String(a.businessName || a.title || '').localeCompare(String(b.businessName || b.title || ''));

    const bScore = this.progressPercent(b) + Number(b.publicDocuments?.length ?? 0) * 8 + Number(b.fundingGoal ?? 0) / 10000;
    const aScore = this.progressPercent(a) + Number(a.publicDocuments?.length ?? 0) * 8 + Number(a.fundingGoal ?? 0) / 10000;
    return bScore - aScore;
  }

  private parseContent(value: string | null): StudioContent {
    try {
      const parsed = JSON.parse(value || '{}') as StudioContent;
      return parsed && typeof parsed === 'object' ? parsed : {};
    } catch {
      return {};
    }
  }

  private normalize(value: string): string {
    return value.trim().toLowerCase();
  }

  private extractError(err: HttpErrorResponse): string {
    return (
      err.error?.message ||
      err.error?.error ||
      (typeof err.error === 'string' ? err.error : null) ||
      'Unable to load investor campaigns.'
    );
  }
}
