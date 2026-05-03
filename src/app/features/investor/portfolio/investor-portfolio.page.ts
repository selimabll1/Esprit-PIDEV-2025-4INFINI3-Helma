import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { PortfolioImportResultResponse, PortfolioOverviewResponse, PortfolioPositionResponse } from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

@Component({
  selector: 'app-investor-portfolio-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="portfolio-page">
      <header class="portfolio-hero">
        <div class="hero-copy">
          <span class="eyebrow">Investment portfolio</span>
          <h1>Equity portfolio optimisation</h1>
          <p>
            Track tracked equity investments, deployment, concentration, diversification,
            ownership estimates, funding progress, and region/sector exposure. Pending checkouts
            stay separate until payment succeeds.
          </p>
        </div>

        <div class="hero-actions">
          <input
            #portfolioImportInput
            type="file"
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            hidden
            (change)="onImportFileSelected($event)"
          />
          <button type="button" class="ghost-btn" (click)="load()" [disabled]="loading() || importBusy()">
            {{ loading() ? 'Refreshing...' : 'Refresh' }}
          </button>
          <button type="button" class="ghost-btn" (click)="exportPositionsXlsx()" [disabled]="loading() || importBusy()">
            Export XLSX
          </button>
          <button type="button" class="ghost-btn" (click)="downloadImportTemplate()" [disabled]="importBusy()">
            Template
          </button>
          <button type="button" class="solid-btn" (click)="portfolioImportInput.click()" [disabled]="importBusy()">
            {{ importBusy() ? 'Importing...' : 'Import XLSX' }}
          </button>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>
      <p class="import-message" *ngIf="importMessage()">{{ importMessage() }}</p>

      <article class="panel import-result" *ngIf="importResult() as result">
        <div class="panel-head compact">
          <div>
            <h2>Last XLSX import</h2>
            <p>{{ result.message || 'Import finished.' }}</p>
          </div>
          <span class="hhi-pill">{{ result.importedRows }} imported</span>
        </div>
        <div class="mini-grid four">
          <div class="mini-metric">
            <span>Total rows</span>
            <strong>{{ result.totalRows }}</strong>
          </div>
          <div class="mini-metric">
            <span>Valid rows</span>
            <strong>{{ result.validRows }}</strong>
          </div>
          <div class="mini-metric" [class.problem]="result.invalidRows > 0">
            <span>Invalid rows</span>
            <strong>{{ result.invalidRows }}</strong>
          </div>
          <div class="mini-metric">
            <span>Mode</span>
            <strong>{{ result.replaceExisting ? 'Replace' : 'Append' }}</strong>
          </div>
        </div>
        <div class="import-errors" *ngIf="result.invalidRows > 0">
          <div *ngFor="let row of result.rows">
            <p *ngIf="!row.valid">
              <strong>Row {{ row.rowNumber }}:</strong> {{ row.errors.join(' ') }}
            </p>
          </div>
        </div>
      </article>

      <div class="state-card" *ngIf="loading()">Loading portfolio optimisation metrics...</div>

      <ng-container *ngIf="!loading() && overview() as portfolio">
        <section class="metric-grid">
          <article class="metric-card primary">
            <span>Total tracked equity</span>
            <strong>{{ formatMoney(portfolio.summary.totalInvested, portfolio.summary.currency) }}</strong>
            <small>Helma paid positions plus imported XLSX tracked positions.</small>
          </article>

          <article class="metric-card">
            <span>Committed capital</span>
            <strong>{{ formatMoney(portfolio.summary.committedCapital, portfolio.summary.currency) }}</strong>
            <small>{{ formatPct(portfolio.summary.deploymentRatePct) }} deployed into paid positions.</small>
          </article>

          <article class="metric-card warning" [class.has-value]="safeNumber(portfolio.summary.pendingCommitments) > 0">
            <span>Pending commitments</span>
            <strong>{{ formatMoney(portfolio.summary.pendingCommitments, portfolio.summary.currency) }}</strong>
            <small>{{ formatPct(portfolio.summary.pendingCommitmentWeightPct) }} of committed capital.</small>
          </article>

          <article class="metric-card">
            <span>Portfolio health</span>
            <strong>{{ formatRiskLabel(portfolio.summary.portfolioHealth) }}</strong>
            <small>Concentration risk: {{ formatRiskLabel(portfolio.summary.concentrationRisk) }}</small>
          </article>
        </section>

        <section class="analysis-grid">
          <article class="panel score-panel">
            <div class="panel-head">
              <div>
                <span class="panel-kicker">Optimisation score</span>
                <h2>Diversification engine</h2>
                <p>Uses position HHI, sector HHI, region HHI, breadth, and top-position concentration.</p>
              </div>
            </div>

            <div class="score-row">
              <div class="score-ring" [style.--score]="safePercent(portfolio.summary.diversificationScore)">
                <strong>{{ portfolio.summary.diversificationScore ?? 0 }}</strong>
                <span>/100</span>
              </div>

              <div class="score-details">
                <div>
                  <span>Effective positions</span>
                  <strong>{{ formatNumber(portfolio.optimization.effectiveNumberOfPositions) }}</strong>
                </div>
                <div>
                  <span>Position HHI</span>
                  <strong>{{ formatNumber(portfolio.summary.concentrationIndexHhi) }}</strong>
                </div>
                <div>
                  <span>Top 3 weight</span>
                  <strong>{{ formatPct(portfolio.summary.top3PositionsWeightPct) }}</strong>
                </div>
              </div>
            </div>
          </article>

          <article class="panel health-panel">
            <div class="panel-head">
              <div>
                <span class="panel-kicker">Risk profile</span>
                <h2>Core optimisation metrics</h2>
                <p>These are the practical private-portfolio metrics that do not require public market pricing.</p>
              </div>
            </div>

            <div class="mini-grid">
              <div class="mini-metric">
                <span>Suggested max position</span>
                <strong>{{ formatPct(portfolio.optimization.suggestedMaxPositionWeightPct) }}</strong>
              </div>
              <div class="mini-metric">
                <span>Overweight positions</span>
                <strong>{{ portfolio.optimization.overweightPositions ?? 0 }}</strong>
              </div>
              <div class="mini-metric">
                <span>Largest position</span>
                <strong>{{ formatPct(portfolio.summary.largestPositionWeightPct) }}</strong>
              </div>
              <div class="mini-metric">
                <span>Average ticket</span>
                <strong>{{ formatMoney(portfolio.summary.averageTicket, portfolio.summary.currency) }}</strong>
              </div>
            </div>
          </article>
        </section>

        <section class="analysis-grid reverse">
          <article class="panel">
            <div class="panel-head">
              <div>
                <span class="panel-kicker">Capital deployment</span>
                <h2>Deployment and campaign progress</h2>
                <p>Shows whether committed money is actually deployed and whether invested campaigns are progressing.</p>
              </div>
            </div>

            <div class="mini-grid four">
              <div class="mini-metric">
                <span>Deployment rate</span>
                <strong>{{ formatPct(portfolio.optimization.deploymentRatePct) }}</strong>
              </div>
              <div class="mini-metric">
                <span>Pending weight</span>
                <strong>{{ formatPct(portfolio.optimization.pendingCommitmentWeightPct) }}</strong>
              </div>
              <div class="mini-metric">
                <span>Weighted progress</span>
                <strong>{{ formatPct(portfolio.optimization.weightedAverageFundingProgressPct) }}</strong>
              </div>
              <div class="mini-metric">
                <span>Failed / refunded</span>
                <strong>{{ formatMoney(portfolio.summary.failedOrCanceledAmount, portfolio.summary.currency) }}</strong>
              </div>
            </div>
          </article>

          <article class="panel ownership-panel">
            <div class="panel-head">
              <div>
                <span class="panel-kicker">Equity terms</span>
                <h2>Ownership snapshot</h2>
                <p>Ownership is estimated from investment amount, target raise, and offered equity.</p>
              </div>
            </div>

            <div class="mini-grid">
              <div class="mini-metric">
                <span>Max ownership</span>
                <strong>{{ formatPct(portfolio.summary.maxOwnershipPercent) }}</strong>
              </div>
              <div class="mini-metric">
                <span>Average ownership</span>
                <strong>{{ formatPct(portfolio.summary.averageOwnershipPercent) }}</strong>
              </div>
              <div class="mini-metric">
                <span>Active positions</span>
                <strong>{{ portfolio.summary.activePositions }}</strong>
              </div>
            </div>

            <p class="fine-print">
              Ownership remains indicative until final legal validation and campaign closing.
            </p>
          </article>
        </section>

        <section class="content-grid">
          <article class="panel insights-panel">
            <div class="panel-head">
              <div>
                <span class="panel-kicker">Smart summary</span>
                <h2>Portfolio insights</h2>
                <p>Human-readable signals generated from confirmed equity positions.</p>
              </div>
            </div>

            <div class="insight-list" *ngIf="portfolio.insights?.length; else noInsights">
              <div class="insight" *ngFor="let insight of portfolio.insights" [class]="insightClass(insight.type)">
                <span class="insight-dot"></span>
                <div>
                  <strong>{{ insight.title }}</strong>
                  <p>{{ insight.message }}</p>
                </div>
              </div>
            </div>

            <ng-template #noInsights>
              <p class="empty-copy">No insights available yet.</p>
            </ng-template>
          </article>

          <article class="panel action-panel">
            <div class="panel-head">
              <div>
                <span class="panel-kicker">Needs attention</span>
                <h2>Action center</h2>
                <p>Pending and unsuccessful commitments stay outside tracked analytics.</p>
              </div>
            </div>

            <div class="action-box" *ngIf="safeNumber(portfolio.summary.pendingCommitments) > 0; else noActions">
              <strong>{{ portfolio.summary.pendingCommitmentsCount }} pending checkout{{ portfolio.summary.pendingCommitmentsCount === 1 ? '' : 's' }}</strong>
              <p>
                {{ formatMoney(portfolio.summary.pendingCommitments, portfolio.summary.currency) }} is waiting for payment completion.
                It will not affect optimisation metrics until the payment succeeds.
              </p>
            </div>

            <ng-template #noActions>
              <div class="action-box positive">
                <strong>No pending checkout</strong>
                <p>Your tracked equity portfolio is clean. No unpaid commitment is being mixed into analytics.</p>
              </div>
            </ng-template>
          </article>
        </section>

        <section class="allocation-grid">
          <article class="panel">
            <div class="panel-head compact">
              <div>
                <h2>Sector allocation</h2>
                <p>Capital exposure by business sector.</p>
              </div>
              <span class="hhi-pill">HHI {{ formatNumber(portfolio.summary.sectorConcentrationHhi) }}</span>
            </div>
            <ng-container *ngTemplateOutlet="allocationList; context: { items: portfolio.sectorAllocation, empty: 'No sector allocation yet.', currency: portfolio.summary.currency }"></ng-container>
          </article>

          <article class="panel">
            <div class="panel-head compact">
              <div>
                <h2>Region allocation</h2>
                <p>Governorate exposure across tracked equity investments.</p>
              </div>
              <span class="hhi-pill">HHI {{ formatNumber(portfolio.summary.regionConcentrationHhi) }}</span>
            </div>
            <ng-container *ngTemplateOutlet="allocationList; context: { items: portfolio.regionAllocation, empty: 'No region allocation yet.', currency: portfolio.summary.currency }"></ng-container>
          </article>

          <article class="panel">
            <div class="panel-head compact">
              <div>
                <h2>Tag exposure</h2>
                <p>Strategic tags linked to invested campaigns.</p>
              </div>
            </div>
            <ng-container *ngTemplateOutlet="allocationList; context: { items: portfolio.tagExposure, empty: 'No tag exposure yet.', currency: portfolio.summary.currency }"></ng-container>
          </article>
        </section>

        <ng-template #allocationList let-items="items" let-empty="empty" let-currency="currency">
          <div class="allocation-list" *ngIf="items?.length; else emptyAllocation">
            <div class="allocation-item" *ngFor="let item of items">
              <div class="allocation-row">
                <strong>{{ prettifyKey(item.key) }}</strong>
                <span>{{ formatPct(item.weightPct) }}</span>
              </div>
              <div class="bar-track">
                <div class="bar-fill" [style.width.%]="safePercent(item.weightPct)"></div>
              </div>
              <small>{{ formatMoney(item.amount, currency) }}</small>
            </div>
          </div>
          <ng-template #emptyAllocation>
            <p class="empty-copy">{{ empty }}</p>
          </ng-template>
        </ng-template>

        <article class="panel positions-panel">
          <div class="panel-head">
            <div>
              <span class="panel-kicker">Tracked positions</span>
              <h2>My equity positions</h2>
              <p>Cards include Helma paid equity pledges plus imported XLSX tracked positions.</p>
            </div>
          </div>

          <div class="positions-grid" *ngIf="sortedPositions().length; else emptyPositions">
            <article class="position-card" *ngFor="let position of sortedPositions()">
              <div class="position-top">
                <div>
                  <span class="type-pill">{{ formatRiskLabel(position.concentrationRisk) }} weight</span>
                  <span class="source-pill" [class.imported]="position.imported">{{ formatPositionSource(position.positionSource) }}</span>
                  <h3>{{ position.campaignBusinessName }}</h3>
                  <p>{{ prettifyKey(position.sector) }} · {{ displayLocation(position) }}</p>
                </div>
                <strong>{{ formatMoney(position.investedAmount, position.currency) }}</strong>
              </div>

              <div class="progress-block">
                <div class="allocation-row">
                  <span>Campaign progress</span>
                  <strong>{{ formatPct(position.campaignFundingProgressPct) }}</strong>
                </div>
                <div class="bar-track tall">
                  <div class="bar-fill" [style.width.%]="safePercent(position.campaignFundingProgressPct)"></div>
                </div>
                <small>
                  {{ formatMoney(position.campaignRaisedAmount, position.currency) }} raised of
                  {{ formatMoney(position.campaignFundingGoal, position.currency) }} ·
                  {{ formatPct(position.campaignFundingGapPct) }} gap remaining
                </small>
              </div>

              <div class="position-metrics">
                <div>
                  <span>Portfolio weight</span>
                  <strong>{{ formatPct(position.positionWeightPct) }}</strong>
                </div>
                <div>
                  <span>Ownership</span>
                  <strong>{{ formatPct(position.ownershipPercent) }}</strong>
                </div>
                <div>
                  <span>Offered equity</span>
                  <strong>{{ formatPct(position.equityOfferedPercent) }}</strong>
                </div>
                <div>
                  <span>{{ position.imported ? 'Imported investment' : 'Pledged' }}</span>
                  <strong>{{ position.pledgedAt | date:'mediumDate' }}</strong>
                </div>
              </div>
            </article>
          </div>

          <ng-template #emptyPositions>
            <div class="empty-state">
              <strong>No equity positions yet.</strong>
              <p>Paid equity pledges and imported XLSX tracked positions will appear here. Pending checkouts stay outside tracked analytics.</p>
            </div>
          </ng-template>
        </article>
      </ng-container>
    </section>
  `,
  styles: [`
    :host { display: block; }

    .portfolio-page { display: grid; gap: 22px; }

    .portfolio-hero {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      align-items: flex-start;
      padding: 28px;
      border-radius: 26px;
      background:
        radial-gradient(circle at top right, rgba(212, 175, 55, 0.22), transparent 34%),
        linear-gradient(135deg, #061d38 0%, #0a315d 58%, #08213d 100%);
      color: white;
      box-shadow: 0 24px 50px rgba(6, 29, 56, 0.22);
      overflow: hidden;
      position: relative;
    }

    .portfolio-hero::after {
      content: '';
      position: absolute;
      width: 180px;
      height: 180px;
      right: -70px;
      bottom: -90px;
      border-radius: 999px;
      border: 34px solid rgba(255, 255, 255, 0.06);
    }

    .hero-copy { max-width: 840px; position: relative; z-index: 1; }

    .eyebrow,
    .panel-kicker,
    .metric-card span,
    .mini-metric span,
    .position-metrics span {
      font-size: 0.78rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .eyebrow { color: #f2d375; }

    .portfolio-hero h1 {
      margin: 8px 0 10px;
      font-size: clamp(2rem, 3vw, 3.15rem);
      line-height: 1;
    }

    .portfolio-hero p { margin: 0; color: rgba(255, 255, 255, 0.78); line-height: 1.7; }

    .hero-actions { position: relative; z-index: 1; display: flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }

    .ghost-btn,
    .solid-btn {
      min-width: 112px;
      height: 44px;
      padding: 0 18px;
      border: 1px solid rgba(255, 255, 255, 0.24);
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.1);
      color: white;
      font-weight: 800;
      cursor: pointer;
      backdrop-filter: blur(14px);
    }

    .solid-btn {
      border-color: rgba(212, 175, 55, 0.68);
      background: linear-gradient(135deg, #d4af37, #b8860b);
      box-shadow: 0 12px 24px rgba(0, 0, 0, 0.16);
    }

    .ghost-btn:disabled,
    .solid-btn:disabled { opacity: 0.65; cursor: default; }

    .metric-grid,
    .analysis-grid,
    .content-grid,
    .allocation-grid,
    .positions-grid { display: grid; gap: 16px; }

    .metric-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
    .analysis-grid { grid-template-columns: minmax(0, 1.18fr) minmax(340px, 0.82fr); }
    .analysis-grid.reverse { grid-template-columns: minmax(0, 1fr) minmax(340px, 0.7fr); }
    .content-grid { grid-template-columns: minmax(0, 1.2fr) minmax(340px, 0.8fr); }
    .allocation-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .positions-grid { grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); }

    .metric-card,
    .panel,
    .state-card,
    .position-card,
    .empty-state {
      border: 1px solid rgba(8, 33, 61, 0.08);
      border-radius: 22px;
      background: rgba(255, 255, 255, 0.94);
      box-shadow: 0 18px 38px rgba(6, 29, 56, 0.08);
    }

    .metric-card { display: grid; gap: 10px; padding: 20px; min-height: 148px; }
    .metric-card.primary { background: linear-gradient(145deg, #f8fbff, #eef5ff); border-color: rgba(10, 49, 93, 0.13); }
    .metric-card.warning.has-value { background: linear-gradient(145deg, #fffaf0, #fff4d7); border-color: rgba(212, 175, 55, 0.26); }

    .metric-card span,
    .panel-kicker,
    .mini-metric span,
    .position-metrics span { color: #64748b; }

    .metric-card strong { color: #08213d; font-size: 1.5rem; line-height: 1.1; }

    .metric-card small,
    .panel-head p,
    .fine-print,
    .empty-copy,
    .position-top p,
    .progress-block small,
    .action-box p,
    .insight p { color: #64748b; line-height: 1.55; }

    .panel,
    .state-card,
    .empty-state { padding: 22px; }
    .panel { display: grid; gap: 18px; }

    .panel-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
    .panel-head.compact { margin-bottom: -4px; }
    .panel-head h2 { margin: 5px 0 6px; color: #08213d; font-size: 1.18rem; }
    .panel-head p,
    .fine-print,
    .empty-copy { margin: 0; }

    .score-row { display: grid; grid-template-columns: 168px 1fr; gap: 20px; align-items: center; }

    .score-ring {
      width: 150px;
      height: 150px;
      border-radius: 50%;
      display: grid;
      place-content: center;
      background:
        radial-gradient(circle, white 55%, transparent 56%),
        conic-gradient(#d4af37 calc(var(--score) * 1%), #e8eef5 0);
    }

    .score-ring strong { color: #08213d; font-size: 2.25rem; line-height: 1; text-align: center; }
    .score-ring span { color: #64748b; text-align: center; font-weight: 800; }

    .score-details,
    .mini-grid,
    .position-metrics { display: grid; gap: 12px; }
    .score-details { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .mini-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .mini-grid.four { grid-template-columns: repeat(4, minmax(0, 1fr)); }

    .score-details div,
    .mini-metric,
    .position-metrics div {
      display: grid;
      gap: 7px;
      padding: 14px;
      border-radius: 16px;
      background: #f8fafc;
    }

    .score-details span { color: #64748b; font-size: 0.82rem; font-weight: 700; }
    .score-details strong,
    .mini-metric strong,
    .position-metrics strong { color: #08213d; }

    .insight-list,
    .allocation-list { display: grid; gap: 13px; }

    .insight {
      display: grid;
      grid-template-columns: 10px 1fr;
      gap: 12px;
      padding: 14px;
      border-radius: 16px;
      background: #f8fafc;
      border: 1px solid #eef2f7;
    }

    .insight-dot { width: 10px; height: 10px; margin-top: 6px; border-radius: 999px; background: #64748b; }
    .insight strong { color: #08213d; }
    .insight p { margin: 4px 0 0; }
    .insight.positive .insight-dot { background: #16a34a; }
    .insight.warning .insight-dot { background: #d97706; }
    .insight.action .insight-dot { background: #2563eb; }

    .action-box {
      display: grid;
      gap: 8px;
      padding: 18px;
      border-radius: 18px;
      background: #fff7ed;
      border: 1px solid #fed7aa;
    }

    .action-box.positive { background: #f0fdf4; border-color: #bbf7d0; }
    .action-box strong { color: #08213d; }
    .action-box p { margin: 0; }

    .hhi-pill {
      white-space: nowrap;
      padding: 7px 10px;
      border-radius: 999px;
      background: #eef5ff;
      color: #0a315d;
      font-size: 0.76rem;
      font-weight: 850;
    }

    .allocation-item { display: grid; gap: 8px; }
    .allocation-row { display: flex; justify-content: space-between; gap: 12px; align-items: baseline; }
    .allocation-row strong,
    .allocation-row span { color: #08213d; }
    .allocation-item small { color: #64748b; }

    .bar-track { height: 9px; border-radius: 999px; background: #e8eef5; overflow: hidden; }
    .bar-track.tall { height: 11px; }
    .bar-fill { height: 100%; border-radius: inherit; background: linear-gradient(90deg, #d4af37, #0a315d); }

    .position-card { display: grid; gap: 18px; padding: 20px; }
    .position-top { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; }
    .position-top h3 { margin: 10px 0 6px; color: #08213d; font-size: 1.12rem; }
    .position-top p { margin: 0; }
    .position-top > strong { white-space: nowrap; color: #08213d; font-size: 1.05rem; }

    .type-pill,
    .source-pill {
      display: inline-flex;
      align-items: center;
      min-height: 26px;
      padding: 0 10px;
      border-radius: 999px;
      font-size: 0.76rem;
      font-weight: 800;
    }

    .type-pill { background: #eef5ff; color: #0a315d; }
    .source-pill { margin-left: 6px; background: #f8fafc; color: #475569; }
    .source-pill.imported { background: #fff7ed; color: #9a3412; }

    .progress-block { display: grid; gap: 8px; }
    .position-metrics { grid-template-columns: repeat(4, minmax(0, 1fr)); }

    .empty-state { text-align: center; color: #64748b; }
    .empty-state strong { color: #08213d; }
    .error { margin: 0; color: #b42318; font-weight: 700; }
    .import-message { margin: 0; color: #0a315d; font-weight: 800; }
    .import-result { border-color: rgba(212, 175, 55, 0.2); }
    .import-errors { display: grid; gap: 6px; padding: 12px; border-radius: 14px; background: #fff7ed; border: 1px solid #fed7aa; }
    .import-errors p { margin: 0; color: #9a3412; line-height: 1.5; }
    .mini-metric.problem { background: #fff7ed; border: 1px solid #fed7aa; }

    @media (max-width: 1180px) {
      .metric-grid,
      .allocation-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .analysis-grid,
      .analysis-grid.reverse,
      .content-grid { grid-template-columns: 1fr; }
      .mini-grid.four { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }

    @media (max-width: 760px) {
      .portfolio-hero,
      .position-top { flex-direction: column; }
      .hero-actions,
      .ghost-btn,
      .solid-btn { width: 100%; }
      .metric-grid,
      .allocation-grid,
      .score-details,
      .mini-grid,
      .mini-grid.four,
      .position-metrics { grid-template-columns: 1fr; }
      .score-row { grid-template-columns: 1fr; }
    }
  `]
})
export class InvestorPortfolioPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  @ViewChild('portfolioImportInput') portfolioImportInput?: ElementRef<HTMLInputElement>;

  readonly loading = signal(false);
  readonly importBusy = signal(false);
  readonly error = signal('');
  readonly importMessage = signal('');
  readonly importResult = signal<PortfolioImportResultResponse | null>(null);
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

  exportPositionsXlsx(): void {
    this.error.set('');
    this.importMessage.set('');

    this.service.exportMyPortfolioPositionsXlsx().subscribe({
      next: (blob) => this.downloadBlob(blob, `helma-portfolio-positions-${this.today()}.xlsx`),
      error: (err: HttpErrorResponse) => this.error.set(this.extractError(err))
    });
  }

  downloadImportTemplate(): void {
    this.error.set('');
    this.importMessage.set('');

    this.service.downloadPortfolioImportTemplate().subscribe({
      next: (blob) => this.downloadBlob(blob, 'helma-portfolio-import-template.xlsx'),
      error: (err: HttpErrorResponse) => this.error.set(this.extractError(err))
    });
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    this.importBusy.set(true);
    this.error.set('');
    this.importMessage.set('');
    this.importResult.set(null);

    this.service
      .importMyPortfolioPositionsXlsx(file, false)
      .pipe(finalize(() => {
        this.importBusy.set(false);
        if (this.portfolioImportInput?.nativeElement) {
          this.portfolioImportInput.nativeElement.value = '';
        }
      }))
      .subscribe({
        next: (result) => {
          this.importResult.set(result);
          this.importMessage.set(result.message || 'Portfolio positions import finished.');
          if ((result.importedRows ?? 0) > 0) {
            this.load();
          }
        },
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

  safeNumber(value: number | null | undefined): number {
    const parsed = Number(value ?? 0);
    return Number.isFinite(parsed) ? parsed : 0;
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

  displayLocation(position: PortfolioPositionResponse): string {
    const city = position.city?.trim();
    const governorate = position.governorate?.trim();

    if (city && governorate) {
      return `${city}, ${governorate}`;
    }

    return city || governorate || 'Location not specified';
  }

  insightClass(type: string | null | undefined): string {
    const normalized = (type ?? 'INFO').toLowerCase();
    return `insight ${normalized}`;
  }

  formatRiskLabel(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }

    return value
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatPositionSource(value: string | null | undefined): string {
    if (value === 'IMPORTED_XLSX') {
      return 'Imported XLSX';
    }

    return 'Helma paid';
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private extractError(err: HttpErrorResponse): string {
    return (
      err.error?.message ||
      err.error?.error ||
      (typeof err.error === 'string' ? err.error : null) ||
      'Could not load portfolio optimisation analytics.'
    );
  }
}
