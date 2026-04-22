import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  AppTag,
  SECTOR_OPTIONS,
  SUB_SECTOR_OPTIONS_BY_SECTOR,
  Sector,
  SubSector,
  TAG_OPTIONS
} from '../../../core/models/application-taxonomy';
import {
  CampaignResponse,
  CampaignSearchCriteria,
  CampaignSortKey,
  CrowdfundingType,
  SortDirection
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

@Component({
  selector: 'app-investor-campaigns-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Approved campaigns</h1>
          <p>Browse campaigns that are available for investors.</p>
        </div>
      </header>

      <div class="toolbar-card">
        <div class="toolbar-grid search-grid">
          <label class="field field-wide">
            <span>Search</span>
            <input
              type="text"
              placeholder="Business name, summary, sector, sub-sector, or tag"
              [(ngModel)]="search"
              (keyup.enter)="load()"
            />
          </label>

          <label class="field">
            <span>Type</span>
            <select [(ngModel)]="typeFilter">
              <option value="">All types</option>
              <option *ngFor="let type of crowdfundingTypes" [value]="type">{{ formatEnumLabel(type) }}</option>
            </select>
          </label>

          <label class="field">
            <span>Sector</span>
            <select [(ngModel)]="sectorFilter" (ngModelChange)="onSectorChange($event)">
              <option value="">All sectors</option>
              <option *ngFor="let sector of sectorOptions" [value]="sector">{{ formatEnumLabel(sector) }}</option>
            </select>
          </label>

          <label class="field">
            <span>Sub-sector</span>
            <select [(ngModel)]="subSectorFilter">
              <option value="">All sub-sectors</option>
              <option *ngFor="let subSector of availableSubSectors" [value]="subSector">
                {{ formatEnumLabel(subSector) }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>Tag</span>
            <select [(ngModel)]="tagFilter">
              <option value="">All tags</option>
              <option *ngFor="let tag of tagOptions" [value]="tag">{{ formatEnumLabel(tag) }}</option>
            </select>
          </label>

          <label class="field">
            <span>Funding goal min</span>
            <input type="number" min="0" step="0.001" [(ngModel)]="fundingGoalMin" />
          </label>

          <label class="field">
            <span>Funding goal max</span>
            <input type="number" min="0" step="0.001" [(ngModel)]="fundingGoalMax" />
          </label>

          <label class="field">
            <span>Sort by</span>
            <select [(ngModel)]="sort">
              <option *ngFor="let option of sortOptions" [value]="option.value">{{ option.label }}</option>
            </select>
          </label>

          <label class="field">
            <span>Direction</span>
            <select [(ngModel)]="sortDir">
              <option value="desc">Descending</option>
              <option value="asc">Ascending</option>
            </select>
          </label>
        </div>

        <div class="toolbar-actions">
          <button type="button" (click)="load()">Apply</button>
          <button type="button" class="secondary" (click)="resetFilters()">Reset</button>
        </div>
      </div>

      <p class="error" *ngIf="error()">{{ error() }}</p>
      <div class="state-card" *ngIf="loading()">Loading campaigns...</div>

      <div class="empty" *ngIf="!loading() && !campaigns().length">
        No campaigns found.
      </div>

      <div class="grid" *ngIf="!loading() && campaigns().length">
        <article class="card" *ngFor="let campaign of campaigns()">
          <div class="card-top">
            <div>
              <div class="pill type">{{ campaign.type }}</div>
              <h2>{{ campaign.businessName }}</h2>
            </div>

            <div class="pill progress">
              {{ fundingPercent(campaign) }}%
            </div>
          </div>

          <div class="trend-strip" *ngIf="hasTrendData(campaign)">
            <span class="pill trend" *ngIf="campaign.trendLabel">{{ campaign.trendLabel }}</span>
            <span *ngIf="campaign.trendScore !== null && campaign.trendScore !== undefined">
              <strong>Trend:</strong> {{ formatMetric(campaign.trendScore) }}
            </span>
            <span *ngIf="campaign.nearTermGrowth !== null && campaign.nearTermGrowth !== undefined">
              <strong>Expected growth:</strong> {{ formatMetric(campaign.nearTermGrowth) }}
            </span>
          </div>

          <div class="meta">
            <p><strong>Sector:</strong> {{ campaign.sector ? formatEnumLabel(campaign.sector) : '—' }}</p>
            <p><strong>Sub-sector:</strong> {{ campaign.subSector ? formatEnumLabel(campaign.subSector) : '—' }}</p>
            <p><strong>Tags:</strong> {{ formatTagList(campaign.tags) }}</p>
            <p><strong>Funding goal:</strong> {{ formatMoney(campaign.fundingGoal, campaign.currency) }}</p>
            <p><strong>Raised:</strong> {{ formatMoney(campaign.investorsPledgedAmount, campaign.currency) }}</p>
            <p *ngIf="campaign.type === crowdfundingType.EQUITY">
              <strong>Min investment:</strong>
              {{ formatMoney(campaign.minInvestment, campaign.currency) }}
            </p>
            <p *ngIf="campaign.quarterGrowth !== null && campaign.quarterGrowth !== undefined">
              <strong>Quarter growth:</strong> {{ formatMetric(campaign.quarterGrowth) }}
            </p>
          </div>

          <p class="summary">{{ campaign.summary || 'No summary provided.' }}</p>

          <div class="actions">
            <a [routerLink]="['/investor/campaigns', campaign.id]">View details</a>
          </div>
        </article>
      </div>
    </section>
  `,
  styles: [`
    .page {
      display: grid;
      gap: 20px;
    }

    .page-header h1 {
      margin: 0 0 8px;
      color: #062a2b;
    }

    .page-header p {
      margin: 0;
      color: #5c6b73;
    }

    .toolbar-card,
    .card,
    .empty,
    .state-card {
      background: white;
      border-radius: 18px;
      padding: 20px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.06);
    }

    .toolbar-grid {
      display: grid;
      gap: 14px;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    }

    .search-grid .field-wide {
      grid-column: 1 / -1;
    }

    .field {
      display: grid;
      gap: 8px;
    }

    .field span {
      font-size: 0.86rem;
      font-weight: 700;
      color: #33444d;
    }

    .field input,
    .field select {
      width: 100%;
      min-height: 44px;
      border: 1px solid #d8dfe3;
      border-radius: 10px;
      padding: 0 12px;
      font: inherit;
      background: white;
    }

    .toolbar-actions,
    .actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      margin-top: 16px;
    }

    .toolbar-actions button,
    .actions a {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 42px;
      padding: 0 14px;
      border-radius: 10px;
      border: 0;
      text-decoration: none;
      background: #062a2b;
      color: white;
      font-weight: 700;
      cursor: pointer;
    }

    .toolbar-actions .secondary {
      background: #eef3f5;
      color: #062a2b;
    }

    .grid {
      display: grid;
      gap: 16px;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    }

    .card {
      display: grid;
      gap: 16px;
    }

    .card-top {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
    }

    .card h2 {
      margin: 10px 0 0;
      color: #062a2b;
      font-size: 1.15rem;
    }

    .trend-strip {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      align-items: center;
      color: #33444d;
      font-size: 0.9rem;
    }

    .meta {
      display: grid;
      gap: 8px;
    }

    .meta p,
    .summary {
      margin: 0;
      color: #33444d;
    }

    .summary {
      color: #5c6b73;
    }

    .pill {
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

    .progress {
      background: #eef3f5;
      color: #455a64;
    }

    .trend {
      background: #fff5d7;
      color: #8a6d1d;
    }

    .error {
      margin: 0;
      color: #c0392b;
      font-weight: 600;
    }
  `]
})
export class InvestorCampaignsPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly campaigns = signal<CampaignResponse[]>([]);

  readonly crowdfundingType = CrowdfundingType;
  readonly crowdfundingTypes = Object.values(CrowdfundingType);
  readonly sectorOptions = SECTOR_OPTIONS;
  readonly tagOptions = TAG_OPTIONS;
  readonly allSubSectorOptions = Object.values(SubSector);
  readonly sortOptions: ReadonlyArray<{ value: CampaignSortKey; label: string }> = [
    { value: 'trending', label: 'Trending' },
    { value: 'createdAt', label: 'Newest first' },
    { value: 'businessName', label: 'Business name' },
    { value: 'fundingGoal', label: 'Funding goal' },
    { value: 'investorsPledgedAmount', label: 'Raised amount' }
  ];

  search = '';
  typeFilter: CrowdfundingType | '' = '';
  sectorFilter: Sector | '' = '';
  subSectorFilter: SubSector | '' = '';
  tagFilter: AppTag | '' = '';
  fundingGoalMin: number | null = null;
  fundingGoalMax: number | null = null;
  sort: CampaignSortKey = 'trending';
  sortDir: SortDirection = 'desc';

  get availableSubSectors(): readonly SubSector[] {
    if (!this.sectorFilter) return this.allSubSectorOptions;
    return SUB_SECTOR_OPTIONS_BY_SECTOR[this.sectorFilter] ?? this.allSubSectorOptions;
  }

  ngOnInit(): void {
    this.load();
  }

  onSectorChange(value: string): void {
    this.sectorFilter = (value as Sector | '') || '';
    if (this.subSectorFilter && !this.availableSubSectors.includes(this.subSectorFilter as SubSector)) {
      this.subSectorFilter = '';
    }
  }

  resetFilters(): void {
    this.search = '';
    this.typeFilter = '';
    this.sectorFilter = '';
    this.subSectorFilter = '';
    this.tagFilter = '';
    this.fundingGoalMin = null;
    this.fundingGoalMax = null;
    this.sort = 'trending';
    this.sortDir = 'desc';
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.service
      .listApprovedCampaigns(this.buildCriteria())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (campaigns) => this.campaigns.set(campaigns),
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  fundingPercent(campaign: CampaignResponse): number {
    const goal = Number(campaign.fundingGoal ?? 0);
    const raised = Number(campaign.investorsPledgedAmount ?? 0);
    if (!goal || goal <= 0) return 0;
    return Math.min(100, Math.round((raised / goal) * 100));
  }

  hasTrendData(campaign: CampaignResponse): boolean {
    return (
      (campaign.trendScore !== null && campaign.trendScore !== undefined) ||
      (campaign.trendLabel !== null && campaign.trendLabel !== undefined) ||
      (campaign.nearTermGrowth !== null && campaign.nearTermGrowth !== undefined)
    );
  }

  formatMoney(value: number | null | undefined, currency: string): string {
    if (value === null || value === undefined) return `— ${currency}`;
    return `${value} ${currency}`;
  }

  formatMetric(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return Number(value).toFixed(2).replace(/\.00$/, '');
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

  private buildCriteria(): CampaignSearchCriteria {
    return {
      search: this.search,
      type: this.typeFilter || null,
      sector: this.sectorFilter || null,
      subSector: this.subSectorFilter || null,
      tag: this.tagFilter || null,
      fundingGoalMin: this.fundingGoalMin,
      fundingGoalMax: this.fundingGoalMax,
      sort: this.sort,
      sortDir: this.sortDir
    };
  }

  private extractError(err: HttpErrorResponse): string {
    return (
      err.error?.message ||
      err.error?.error ||
      (typeof err.error === 'string' ? err.error : null) ||
      'Something went wrong.'
    );
  }
}
