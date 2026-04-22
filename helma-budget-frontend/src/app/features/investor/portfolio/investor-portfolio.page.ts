import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { PortfolioOverviewResponse } from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

@Component({
  selector: 'app-investor-portfolio-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Portfolio analytics</h1>
          <p>Overview, allocation, diversification, and position-level metrics for your paid pledges.</p>
        </div>

        <button type="button" class="refresh-btn" (click)="load()" [disabled]="loading()">
          Refresh
        </button>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>
      <div class="state-card" *ngIf="loading()">Loading portfolio metrics...</div>

      <ng-container *ngIf="!loading() && overview() as portfolio">
        <section class="hero-grid">
          <article class="metric-card">
            <span class="metric-label">Total invested</span>
            <strong>{{ formatMoney(portfolio.summary.totalInvested, portfolio.summary.currency) }}</strong>
            <small>{{ portfolio.summary.activePositions }} active positions</small>
          </article>

          <article class="metric-card">
            <span class="metric-label">Average ticket</span>
            <strong>{{ formatMoney(portfolio.summary.averageTicket, portfolio.summary.currency) }}</strong>
            <small>Largest position {{ formatPct(portfolio.summary.largestPositionWeightPct) }}</small>
          </article>

          <article class="metric-card">
            <span class="metric-label">Diversification score</span>
            <strong>{{ portfolio.summary.diversificationScore ?? 0 }}/100</strong>
            <small>HHI {{ formatNumber(portfolio.summary.concentrationIndexHhi) }}</small>
          </article>

          <article class="metric-card">
            <span class="metric-label">Distinct coverage</span>
            <strong>{{ portfolio.summary.distinctSectors }} sectors</strong>
            <small>{{ portfolio.summary.distinctSubSectors }} sub-sectors · {{ portfolio.summary.distinctTags }} tags</small>
          </article>
        </section>

        <section class="split-grid">
          <article class="panel">
            <div class="panel-head">
              <div>
                <h2>Allocation mix</h2>
                <p>How your invested capital is split across donation and equity.</p>
              </div>
            </div>

            <div class="dual-metrics">
              <div class="mini-card">
                <span>Equity invested</span>
                <strong>{{ formatMoney(portfolio.summary.equityInvested, portfolio.summary.currency) }}</strong>
                <small>{{ formatPct(portfolio.summary.equityAllocationPct) }}</small>
              </div>

              <div class="mini-card">
                <span>Donation invested</span>
                <strong>{{ formatMoney(portfolio.summary.donationInvested, portfolio.summary.currency) }}</strong>
                <small>{{ formatPct(portfolio.summary.donationAllocationPct) }}</small>
              </div>
            </div>
          </article>

          <article class="panel">
            <div class="panel-head">
              <div>
                <h2>Diversification</h2>
                <p>Concentration and scoring inputs returned by the backend.</p>
              </div>
            </div>

            <div class="stats-grid">
              <div class="stat">
                <span>Top 3 concentration</span>
                <strong>{{ formatPct(portfolio.diversification.top3PositionsWeightPct) }}</strong>
              </div>
              <div class="stat">
                <span>Largest sector</span>
                <strong>{{ formatPct(portfolio.diversification.largestSectorWeightPct) }}</strong>
              </div>
              <div class="stat">
                <span>Breadth penalty</span>
                <strong>{{ portfolio.diversification.breadthPenalty ?? 0 }}</strong>
              </div>
              <div class="stat">
                <span>Position penalty</span>
                <strong>{{ portfolio.diversification.positionConcentrationPenalty ?? 0 }}</strong>
              </div>
              <div class="stat">
                <span>Sector penalty</span>
                <strong>{{ portfolio.diversification.sectorConcentrationPenalty ?? 0 }}</strong>
              </div>
              <div class="stat">
                <span>HHI</span>
                <strong>{{ formatNumber(portfolio.diversification.concentrationIndexHhi) }}</strong>
              </div>
            </div>
          </article>
        </section>

        <section class="triple-grid">
          <article class="panel">
            <div class="panel-head">
              <div>
                <h2>Sector allocation</h2>
                <p>Weighted by invested amount.</p>
              </div>
            </div>

            <div class="allocation-list" *ngIf="portfolio.sectorAllocation.length; else emptySector">
              <div class="allocation-item" *ngFor="let item of portfolio.sectorAllocation">
                <div class="allocation-row">
                  <strong>{{ prettifyKey(item.key) }}</strong>
                  <span>{{ formatPct(item.weightPct) }}</span>
                </div>
                <div class="bar-track">
                  <div class="bar-fill" [style.width.%]="safePercent(item.weightPct)"></div>
                </div>
                <small>{{ formatMoney(item.amount, portfolio.summary.currency) }}</small>
              </div>
            </div>
            <ng-template #emptySector>
              <p class="empty-copy">No sector allocation available yet.</p>
            </ng-template>
          </article>

          <article class="panel">
            <div class="panel-head">
              <div>
                <h2>Sub-sector allocation</h2>
                <p>Weighted by invested amount.</p>
              </div>
            </div>

            <div class="allocation-list" *ngIf="portfolio.subSectorAllocation.length; else emptySubSector">
              <div class="allocation-item" *ngFor="let item of portfolio.subSectorAllocation">
                <div class="allocation-row">
                  <strong>{{ prettifyKey(item.key) }}</strong>
                  <span>{{ formatPct(item.weightPct) }}</span>
                </div>
                <div class="bar-track">
                  <div class="bar-fill" [style.width.%]="safePercent(item.weightPct)"></div>
                </div>
                <small>{{ formatMoney(item.amount, portfolio.summary.currency) }}</small>
              </div>
            </div>
            <ng-template #emptySubSector>
              <p class="empty-copy">No sub-sector allocation available yet.</p>
            </ng-template>
          </article>

          <article class="panel">
            <div class="panel-head">
              <div>
                <h2>Tag exposure</h2>
                <p>Exposure by app tag.</p>
              </div>
            </div>

            <div class="allocation-list" *ngIf="portfolio.tagExposure.length; else emptyTags">
              <div class="allocation-item" *ngFor="let item of portfolio.tagExposure">
                <div class="allocation-row">
                  <strong>{{ prettifyKey(item.key) }}</strong>
                  <span>{{ formatPct(item.weightPct) }}</span>
                </div>
                <div class="bar-track">
                  <div class="bar-fill" [style.width.%]="safePercent(item.weightPct)"></div>
                </div>
                <small>{{ formatMoney(item.amount, portfolio.summary.currency) }}</small>
              </div>
            </div>
            <ng-template #emptyTags>
              <p class="empty-copy">No tag exposure available yet.</p>
            </ng-template>
          </article>
        </section>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h2>Positions</h2>
              <p>Campaign-level position metrics and equity ownership math.</p>
            </div>
          </div>

          <div class="table-wrap" *ngIf="portfolio.positions.length; else emptyPositions">
            <table>
              <thead>
                <tr>
                  <th>Campaign</th>
                  <th>Type</th>
                  <th>Sector</th>
                  <th>Invested</th>
                  <th>Weight</th>
                  <th>Funding progress</th>
                  <th>Ownership</th>
                  <th>Pledged</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let position of sortedPositions()">
                  <td>
                    <div class="campaign-cell">
                      <strong>{{ position.campaignBusinessName }}</strong>
                      <small *ngIf="position.subSector">{{ prettifyKey(position.subSector) }}</small>
                    </div>
                  </td>
                  <td>{{ position.campaignType }}</td>
                  <td>{{ prettifyKey(position.sector) }}</td>
                  <td>{{ formatMoney(position.investedAmount, position.currency) }}</td>
                  <td>{{ formatPct(position.positionWeightPct) }}</td>
                  <td>{{ formatPct(position.campaignFundingProgressPct) }}</td>
                  <td>{{ position.campaignType === 'EQUITY' ? formatPct(position.ownershipPercent) : '—' }}</td>
                  <td>{{ position.pledgedAt | date:'mediumDate' }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <ng-template #emptyPositions>
            <p class="empty-copy">No portfolio positions found yet. Paid pledges should appear here.</p>
          </ng-template>
        </article>
      </ng-container>
    </section>
  `,
  styles: [`
    .page {
      display: grid;
      gap: 20px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
    }

    .page-header h1 {
      margin: 0 0 8px;
      color: #062a2b;
    }

    .page-header p {
      margin: 0;
      color: #5c6b73;
    }

    .refresh-btn {
      min-width: 110px;
      height: 44px;
      border: 0;
      border-radius: 10px;
      cursor: pointer;
      background: #062a2b;
      color: white;
      font-weight: 700;
    }

    .refresh-btn:disabled {
      opacity: 0.7;
      cursor: default;
    }

    .hero-grid,
    .split-grid,
    .triple-grid {
      display: grid;
      gap: 16px;
    }

    .hero-grid {
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    }

    .split-grid {
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
    }

    .triple-grid {
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    }

    .metric-card,
    .panel,
    .state-card {
      background: white;
      border-radius: 18px;
      padding: 20px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.06);
    }

    .metric-card {
      display: grid;
      gap: 10px;
    }

    .metric-card strong {
      color: #062a2b;
      font-size: 1.5rem;
    }

    .metric-card small,
    .panel-head p,
    .empty-copy {
      color: #5c6b73;
    }

    .metric-label,
    .mini-card span,
    .stat span {
      color: #63747d;
      font-size: 0.85rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .panel {
      display: grid;
      gap: 16px;
    }

    .panel-head {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
    }

    .panel-head h2 {
      margin: 0 0 6px;
      color: #062a2b;
      font-size: 1.1rem;
    }

    .panel-head p,
    .empty-copy {
      margin: 0;
    }

    .dual-metrics,
    .stats-grid {
      display: grid;
      gap: 14px;
    }

    .dual-metrics {
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    }

    .stats-grid {
      grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    }

    .mini-card,
    .stat {
      display: grid;
      gap: 8px;
      padding: 14px;
      border-radius: 14px;
      background: #f7faf9;
    }

    .mini-card strong,
    .stat strong {
      color: #062a2b;
      font-size: 1.1rem;
    }

    .allocation-list {
      display: grid;
      gap: 14px;
    }

    .allocation-item {
      display: grid;
      gap: 8px;
    }

    .allocation-row {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: baseline;
    }

    .allocation-row strong {
      color: #062a2b;
      font-size: 0.95rem;
    }

    .allocation-row span,
    .allocation-item small {
      color: #5c6b73;
    }

    .bar-track {
      height: 10px;
      border-radius: 999px;
      background: #ebf1f2;
      overflow: hidden;
    }

    .bar-fill {
      height: 100%;
      border-radius: inherit;
      background: linear-gradient(90deg, #2a9d8f, #0b3b3c);
    }

    .table-wrap {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      min-width: 840px;
    }

    th,
    td {
      text-align: left;
      padding: 12px 10px;
      border-bottom: 1px solid #eef3f5;
      vertical-align: top;
    }

    th {
      color: #63747d;
      font-size: 0.85rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    td {
      color: #33444d;
    }

    .campaign-cell {
      display: grid;
      gap: 4px;
    }

    .campaign-cell strong {
      color: #062a2b;
    }

    .campaign-cell small {
      color: #5c6b73;
    }

    .error {
      margin: 0;
      color: #c0392b;
      font-weight: 600;
    }

    @media (max-width: 720px) {
      .page-header {
        flex-direction: column;
      }

      .refresh-btn {
        width: 100%;
      }
    }
  `]
})
export class InvestorPortfolioPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly overview = signal<PortfolioOverviewResponse | null>(null);

  readonly sortedPositions = computed(() =>
    [...(this.overview()?.positions ?? [])].sort(
      (a, b) => this.safeNumber(b.positionWeightPct) - this.safeNumber(a.positionWeightPct)
    )
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.service
      .getMyPortfolioOverview()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (overview) => this.overview.set(overview),
        error: (err: HttpErrorResponse) => this.error.set(this.extractError(err))
      });
  }

  formatMoney(value: number | null | undefined, currency: string | null | undefined): string {
    if (value === null || value === undefined) {
      return `—${currency ? ` ${currency}` : ''}`;
    }

    const normalizedCurrency = currency ?? '';
    return `${this.formatNumber(value)}${normalizedCurrency ? ` ${normalizedCurrency}` : ''}`;
  }

  formatPct(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }

    return `${this.formatNumber(value)}%`;
  }

  formatNumber(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }

    return new Intl.NumberFormat('en-US', {
      maximumFractionDigits: 2
    }).format(Number(value));
  }

  safePercent(value: number | null | undefined): number {
    const parsed = this.safeNumber(value);
    return Math.max(0, Math.min(100, parsed));
  }

  prettifyKey(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }

    return value
      .toString()
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private safeNumber(value: number | null | undefined): number {
    const parsed = Number(value ?? 0);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private extractError(err: HttpErrorResponse): string {
    return (
      err.error?.message ||
      err.error?.error ||
      (typeof err.error === 'string' ? err.error : null) ||
      'Could not load portfolio analytics.'
    );
  }
}
