import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BudgetApiService } from '../../../core/services/budget-api.service';
import { AuthStorageService } from '../../../core/services/auth-storage.service';
import { Dashboard } from '../../../core/models/budget.models';
import { forkJoin } from 'rxjs';

declare var Chart: any;

@Component({
  selector: 'app-budget-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="page-header">
      <div>
        <span class="eyebrow">Dashboard</span>
        <h1>Financial Overview</h1>
      </div>
      <div class="header-actions">
        <button class="btn btn-secondary" (click)="downloadReport()">Download PDF</button>
        <button class="btn btn-primary" (click)="load()">Refresh</button>
      </div>
    </header>

    <div class="loading" *ngIf="loading()">Loading dashboard…</div>
    <div class="error-box" *ngIf="error()">{{ error() }}</div>

    <ng-container *ngIf="data() as d">
      <!-- ── Daily allowance banner ────────────── -->
      <div class="allowance-banner" *ngIf="dailyAllowance() as da">
        <div class="allowance-main">
          <span class="allowance-label">You can spend today</span>
          <span class="allowance-value">{{ da.dailyAllowance | number:'1.2-2' }} TND</span>
        </div>
        <div class="allowance-meta">
          <span>{{ da.remaining | number:'1.0-0' }} TND left</span>
          <span>{{ da.daysLeft }} days remaining</span>
        </div>
      </div>

      <!-- ── KPI row ─────────────────────────────── -->
      <div class="kpi-grid">
        <div class="card kpi" [class.kpi-green]="(d.healthScore?.score ?? 0) >= 60"
             [class.kpi-amber]="(d.healthScore?.score ?? 0) >= 30 && (d.healthScore?.score ?? 0) < 60"
             [class.kpi-red]="(d.healthScore?.score ?? 0) < 30">
          <span class="kpi-label">Health Score</span>
          <span class="kpi-value">{{ d.healthScore?.score | number:'1.0-0' }}<small>/100</small></span>
          <span class="kpi-sub">{{ d.healthScore?.label ?? '—' }}</span>
        </div>
        <div class="card kpi">
          <span class="kpi-label">Balance</span>
          <span class="kpi-value">{{ d.currentCashFlow?.cumulativeBalance | number:'1.0-0' }}</span>
          <span class="kpi-sub">TND cumulative</span>
        </div>
        <div class="card kpi" [class.kpi-red]="d.burnRate?.status === 'CRITICAL'" [class.kpi-amber]="d.burnRate?.status === 'WARNING'">
          <span class="kpi-label">Burn Rate</span>
          <span class="kpi-value">{{ d.burnRate?.burnRate | number:'1.0-0' }}</span>
          <span class="kpi-sub">TND/month</span>
        </div>
        <div class="card kpi">
          <span class="kpi-label">Runway</span>
          <span class="kpi-value">{{ d.burnRate?.runwayMonths | number:'1.1-1' }}</span>
          <span class="kpi-sub">months left</span>
        </div>
        <div class="card kpi">
          <span class="kpi-label">Net Flow</span>
          <span class="kpi-value" [class.positive]="(d.currentCashFlow?.netFlow ?? 0) >= 0" [class.negative]="(d.currentCashFlow?.netFlow ?? 0) < 0">
            {{ d.currentCashFlow?.netFlow | number:'1.0-0' }}
          </span>
          <span class="kpi-sub">this month</span>
        </div>
        <div class="card kpi">
          <span class="kpi-label">Trust Badge</span>
          <span class="kpi-value badge-text">{{ d.trustBadge?.level ?? '—' }}</span>
          <span class="kpi-sub">{{ d.trustBadge?.creditCapacity | number:'1.0-0' }} TND</span>
        </div>
      </div>

      <!-- ── Charts row 1: Cash Flow + Spending Donut ── -->
      <div class="grid-2">
        <div class="card chart-card">
          <h2>Income vs Expenses</h2>
          <canvas #cashFlowChart></canvas>
        </div>
        <div class="card chart-card">
          <h2>Spending by Category</h2>
          <canvas #donutChart></canvas>
        </div>
      </div>

      <!-- ── Charts row 2: Forecast + Health Radar ────── -->
      <div class="grid-2">
        <div class="card chart-card">
          <h2>3-Month Forecast</h2>
          <canvas #forecastChart></canvas>
        </div>
        <div class="card chart-card">
          <h2>Health Score Breakdown</h2>
          <canvas #radarChart></canvas>
        </div>
      </div>

      <!-- ── Budget vs Actual ────────────────────────── -->
      <div class="card chart-card" *ngIf="budgetVsActual().length">
        <h2>Budget vs Actual</h2>
        <canvas #budgetChart></canvas>
      </div>

      <!-- ── Alerts & Insights ───────────────────────── -->
      <div class="grid-2" *ngIf="d.mainAlert || d.mainPositive">
        <div class="card insight-card alert-card" *ngIf="d.mainAlert">
          <span class="insight-icon warning-icon">!</span>
          <div>
            <strong>Alert</strong>
            <p>{{ d.mainAlert }}</p>
          </div>
        </div>
        <div class="card insight-card positive-card" *ngIf="d.mainPositive">
          <span class="insight-icon positive-icon">✓</span>
          <div>
            <strong>Positive</strong>
            <p>{{ d.mainPositive }}</p>
          </div>
        </div>
      </div>

      <!-- ── Quick links ─────────────────────────────── -->
      <div class="quick-links">
        <a class="card quick-link" routerLink="/budget/transactions">
          <span class="ql-icon">↕</span>
          <span>Transactions</span>
        </a>
        <a class="card quick-link" routerLink="/budget/budgets">
          <span class="ql-icon">◈</span>
          <span>Budgets</span>
        </a>
        <a class="card quick-link" routerLink="/budget/savings">
          <span class="ql-icon">◔</span>
          <span>Savings</span>
        </a>
        <a class="card quick-link" routerLink="/budget/coach">
          <span class="ql-icon">✦</span>
          <span>AI Coach</span>
        </a>
      </div>
    </ng-container>
  `,
  styles: [`
    :host { display: block; }

    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
    .page-header h1 { margin-bottom: 0; }
    .header-actions { display: flex; gap: 8px; }
    .eyebrow { display: inline-flex; padding: 6px 10px; border-radius: 999px; background: var(--helma-mint); color: var(--helma-teal); font-size: 12px; font-weight: 800; margin-bottom: 8px; }
    .loading { padding: 40px; text-align: center; color: var(--color-text-muted); }
    .error-box { padding: 14px 18px; background: #fff0f0; border: 1px solid var(--color-danger); border-radius: 12px; color: var(--color-danger); margin-bottom: 20px; }

    .allowance-banner {
      background: linear-gradient(135deg, rgba(15,107,104,0.08), rgba(212,166,42,0.12));
      border: 1px solid var(--color-border); border-radius: 18px;
      padding: 20px 24px; margin-bottom: 20px;
      display: flex; justify-content: space-between; align-items: center;
    }
    .allowance-label { display: block; font-size: 13px; color: var(--color-text-muted); font-weight: 600; }
    .allowance-value { font-size: 32px; font-weight: 800; color: var(--helma-teal); }
    .allowance-meta { display: flex; flex-direction: column; gap: 4px; text-align: right; font-size: 13px; color: var(--color-text-muted); }

    .kpi-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; margin-bottom: 20px; }
    .kpi { display: flex; flex-direction: column; gap: 4px; }
    .kpi-label { font-size: 11px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
    .kpi-value { font-size: 26px; font-weight: 800; line-height: 1; }
    .kpi-value small { font-size: 14px; font-weight: 600; color: var(--color-text-muted); }
    .kpi-sub { font-size: 12px; color: var(--color-text-muted); }
    .kpi-green { border-left: 3px solid var(--color-success); }
    .kpi-amber { border-left: 3px solid var(--color-warning); }
    .kpi-red { border-left: 3px solid var(--color-danger); }
    .positive { color: var(--color-success); }
    .negative { color: var(--color-danger); }
    .badge-text { font-size: 18px; color: var(--helma-gold-dark); }

    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }

    h2 { font-size: 13px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 14px; }

    .chart-card { min-height: 280px; margin-bottom: 20px; }
    .chart-card canvas { max-height: 260px; }

    .insight-card { display: flex; gap: 14px; align-items: flex-start; }
    .insight-card p { margin: 4px 0 0; font-size: 13px; color: var(--color-text-muted); }
    .insight-icon { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 10px; font-weight: 800; font-size: 16px; flex: 0 0 auto; }
    .warning-icon { background: #fff7e0; color: var(--color-warning); }
    .positive-icon { background: #eaf8f1; color: var(--color-success); }

    .quick-links { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
    .quick-link { display: flex; align-items: center; gap: 10px; text-decoration: none; color: var(--color-text); font-weight: 600; font-size: 14px; transition: 0.15s; cursor: pointer; }
    .quick-link:hover { border-color: var(--helma-teal); color: var(--helma-teal); }
    .ql-icon { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 10px; background: var(--helma-teal-soft); color: var(--helma-teal); font-size: 16px; }

    @media (max-width: 1100px) { .kpi-grid { grid-template-columns: repeat(3, 1fr); } }
    @media (max-width: 900px) { .grid-2 { grid-template-columns: 1fr; } .kpi-grid { grid-template-columns: repeat(2, 1fr); } .quick-links { grid-template-columns: repeat(2, 1fr); } }
  `]
})
export class BudgetDashboardPage implements OnInit {
  private readonly api = inject(BudgetApiService);
  private readonly auth = inject(AuthStorageService);

  data = signal<Dashboard | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  dailyAllowance = signal<any>(null);
  budgetVsActual = signal<any[]>([]);

  @ViewChild('cashFlowChart') cashFlowCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('donutChart') donutCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('forecastChart') forecastCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('radarChart') radarCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('budgetChart') budgetCanvas!: ElementRef<HTMLCanvasElement>;

  private charts: any[] = [];

  ngOnInit(): void {
    this.loadChartJs().then(() => this.load());
  }

  loadChartJs(): Promise<void> {
    return new Promise((resolve) => {
      if (typeof Chart !== 'undefined') { resolve(); return; }
      const s = document.createElement('script');
      s.src = 'https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.js';
      s.onload = () => resolve();
      document.head.appendChild(s);
    });
  }

  load(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) { this.error.set('No user logged in'); return; }

    this.loading.set(true);
    this.error.set(null);
    const now = new Date();
    const monthStr = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-01';

    this.api.getDashboard(userId).subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
        // Load chart data in parallel
        setTimeout(() => this.buildCharts(userId, monthStr, d), 100);
      },
      error: (e) => { this.error.set(e.message || 'Failed to load'); this.loading.set(false); }
    });

    this.api.getDailyAllowance(userId).subscribe({ next: (da) => this.dailyAllowance.set(da) });
  }

  buildCharts(userId: number, monthStr: string, d: Dashboard): void {
    this.charts.forEach(c => c.destroy());
    this.charts = [];

    // 1. Cash Flow history (income vs expense bars)
    if (this.cashFlowCanvas && d.forecast) {
      const history = d.forecast.months || [];
      // Use cashflow history from dashboard
      this.api.getCashFlowHistory(userId).subscribe({
        next: (cfHistory) => {
          const last6 = cfHistory.slice(-6);
          const labels = last6.map(cf => cf.monthStart?.substring(0, 7) || '');
          this.charts.push(new Chart(this.cashFlowCanvas.nativeElement, {
            type: 'bar',
            data: {
              labels,
              datasets: [
                { label: 'Income', data: last6.map(cf => cf.totalIncome || 0), backgroundColor: 'rgba(15,107,104,0.7)', borderRadius: 6 },
                { label: 'Expense', data: last6.map(cf => cf.totalExpense || 0), backgroundColor: 'rgba(212,166,42,0.7)', borderRadius: 6 }
              ]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, scales: { y: { beginAtZero: true } } }
          }));
        }
      });
    }

    // 2. Spending donut by category
    if (this.donutCanvas) {
      this.api.getSpendingByCategory(userId, monthStr).subscribe({
        next: (cats) => {
          if (!cats.length) return;
          const colors = ['#0F6B68', '#D4A62A', '#534AB7', '#D85A30', '#1D9E75', '#D4537E', '#378ADD', '#639922'];
          this.charts.push(new Chart(this.donutCanvas.nativeElement, {
            type: 'doughnut',
            data: {
              labels: cats.map(c => c.category),
              datasets: [{ data: cats.map(c => c.amount), backgroundColor: colors.slice(0, cats.length), borderWidth: 0 }]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } }, cutout: '60%' }
          }));
        }
      });
    }

    // 3. Forecast chart
    if (this.forecastCanvas && d.forecast?.months?.length) {
      this.api.getCashFlowHistory(userId).subscribe({
        next: (cfHistory) => {
          const last3 = cfHistory.slice(-3);
          const histLabels = last3.map(cf => cf.monthStart?.substring(0, 7) || '');
          const histBalance = last3.map(cf => cf.cumulativeBalance || 0);

          const foreLabels = d.forecast.months.map((m: any) => m.monthStart?.substring(0, 7) || '');
          const foreBalance = d.forecast.months.map((m: any) => m.predictedBalance || 0);

          this.charts.push(new Chart(this.forecastCanvas.nativeElement, {
            type: 'line',
            data: {
              labels: [...histLabels, ...foreLabels],
              datasets: [
                { label: 'Actual', data: [...histBalance, ...Array(foreLabels.length).fill(null)], borderColor: '#0F6B68', borderWidth: 2.5, pointRadius: 4, tension: 0.3 },
                { label: 'Forecast', data: [...Array(histLabels.length - 1).fill(null), histBalance[histBalance.length - 1], ...foreBalance],
                  borderColor: '#D4A62A', borderWidth: 2.5, borderDash: [6, 4], pointRadius: 4, tension: 0.3 }
              ]
            },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
          }));
        }
      });
    }

    // 4. Health score radar
    if (this.radarCanvas && d.healthScore) {
      const hs = d.healthScore;
      this.charts.push(new Chart(this.radarCanvas.nativeElement, {
        type: 'radar',
        data: {
          labels: ['Runway', 'Budget', 'Savings', 'Stability', 'Risk'],
          datasets: [{
            label: 'Score',
            data: [hs.runwayScore, hs.budgetScore, hs.savingsScore, hs.stabilityScore, hs.riskScore],
            backgroundColor: 'rgba(15,107,104,0.15)',
            borderColor: '#0F6B68',
            borderWidth: 2,
            pointBackgroundColor: '#0F6B68'
          }]
        },
        options: {
          responsive: true, maintainAspectRatio: false,
          scales: { r: { min: 0, max: 100, ticks: { stepSize: 25 } } },
          plugins: { legend: { display: false } }
        }
      }));
    }

    // 5. Budget vs Actual
    this.api.getBudgetVsActual(userId, monthStr).subscribe({
      next: (bva) => {
        this.budgetVsActual.set(bva);
        if (bva.length && this.budgetCanvas) {
          setTimeout(() => {
            this.charts.push(new Chart(this.budgetCanvas.nativeElement, {
              type: 'bar',
              data: {
                labels: bva.map(b => b.category),
                datasets: [
                  { label: 'Budget', data: bva.map(b => b.budgetLimit), backgroundColor: 'rgba(15,107,104,0.3)', borderColor: '#0F6B68', borderWidth: 1, borderRadius: 6 },
                  { label: 'Spent', data: bva.map(b => b.actualSpent), backgroundColor: bva.map(b => b.usagePercent > 100 ? 'rgba(214,69,69,0.7)' : 'rgba(212,166,42,0.7)'), borderRadius: 6 }
                ]
              },
              options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, scales: { y: { beginAtZero: true } } }
            }));
          }, 200);
        }
      }
    });
  }

  downloadReport(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    this.api.downloadCashFlowReport(userId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'helma-report.pdf';
        a.click();
        URL.revokeObjectURL(url);
      }
    });
  }
}
