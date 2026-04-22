import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BudgetApiService } from '../../../core/services/budget-api.service';
import { AuthStorageService } from '../../../core/services/auth-storage.service';
import { Budget, BudgetCreate, TrustBudgetResponse } from '../../../core/models/budget.models';

declare var Chart: any;

@Component({
  selector: 'app-budgets-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="page-header">
      <div>
        <span class="eyebrow">Budgets</span>
        <h1>Monthly Spending Limits</h1>
      </div>
      <button class="btn btn-primary" (click)="showForm = !showForm">
        {{ showForm ? 'Cancel' : '+ Add Budget' }}
      </button>
    </header>

    <!-- ── Trust Budget banner ─────────────────── -->
    <div class="trust-banner" *ngIf="trust() as t">
      <div class="trust-row">
        <div><span class="trust-label">Trust Budget</span><span class="trust-value">{{ t.trustBudget | number:'1.0-0' }} TND</span></div>
        <div><span class="trust-label">Badge</span><span class="trust-badge">{{ t.badgeLevel }}</span></div>
        <div><span class="trust-label">Risk</span><span class="trust-value">{{ t.riskScore | number:'1.0-0' }}</span></div>
        <div><span class="trust-label">Usage</span><span class="trust-value">{{ t.budgetUsage | number:'1.1-1' }}%</span></div>
      </div>
    </div>

    <!-- ── Add form ────────────────────────────── -->
    <div class="card form-card" *ngIf="showForm">
      <h2>{{ editingId ? 'Edit Budget' : 'New Budget' }}</h2>
      <div class="form-grid">
        <div class="field"><label>Category</label><input class="input" type="text" [(ngModel)]="form.category" placeholder="food, transport…" /></div>
        <div class="field"><label>Limit (TND)</label><input class="input" type="number" min="0.01" step="0.01" [(ngModel)]="form.limitAmount" /></div>
        <div class="field"><label>Month</label><input class="input" type="date" [(ngModel)]="form.monthStart" /></div>
      </div>
      <div class="form-actions">
        <button class="btn btn-primary" (click)="submit()">{{ editingId ? 'Update' : 'Add' }}</button>
        <button class="btn btn-secondary" (click)="resetForm()">Reset</button>
      </div>
    </div>

    <!-- ── 3-tab view (Buddy style) ────────────── -->
    <div class="tabs-bar">
      <button class="tab" [class.active]="activeTab === 'plan'" (click)="activeTab = 'plan'">Plan</button>
      <button class="tab" [class.active]="activeTab === 'remaining'" (click)="activeTab = 'remaining'">Remaining</button>
      <button class="tab" [class.active]="activeTab === 'insights'" (click)="activeTab = 'insights'; loadInsights()">Insights</button>
    </div>

    <div class="loading" *ngIf="loading()">Loading…</div>

    <!-- ── TAB 1: Plan ─────────────────────────── -->
    <div *ngIf="activeTab === 'plan' && !loading()">
      <div class="empty card" *ngIf="!budgets().length">No budgets configured. Add one to start tracking.</div>
      <div class="budget-grid" *ngIf="budgets().length">
        <div class="card budget-card" *ngFor="let b of budgets()">
          <div class="budget-top">
            <span class="budget-cat">{{ b.category }}</span>
            <span class="budget-month">{{ b.monthStart }}</span>
          </div>
          <div class="budget-amount">{{ b.limitAmount | number:'1.0-0' }} TND</div>
          <div class="budget-actions">
            <button class="btn-sm" (click)="edit(b)">Edit</button>
            <button class="btn-sm btn-danger" (click)="remove(b.id)">Delete</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ── TAB 2: Remaining (circular rings) ──── -->
    <div *ngIf="activeTab === 'remaining' && !loading()">
      <div class="empty card" *ngIf="!bva().length">No budget data available for this month.</div>
      <div class="rings-grid" *ngIf="bva().length">
        <div class="card ring-card" *ngFor="let item of bva()">
          <div class="ring-visual">
            <svg viewBox="0 0 80 80" class="ring-svg">
              <circle cx="40" cy="40" r="34" fill="none" stroke="#E5E7EB" stroke-width="6"/>
              <circle cx="40" cy="40" r="34" fill="none"
                      [attr.stroke]="item.usagePercent > 100 ? '#D64545' : item.usagePercent > 80 ? '#E6A700' : '#0F6B68'"
                      stroke-width="6" stroke-linecap="round"
                      [attr.stroke-dasharray]="ringDash(item.usagePercent)"
                      transform="rotate(-90 40 40)"/>
            </svg>
            <span class="ring-pct" [class.over]="item.usagePercent > 100">{{ item.usagePercent | number:'1.0-0' }}%</span>
          </div>
          <div class="ring-info">
            <span class="ring-cat">{{ item.category }}</span>
            <span class="ring-detail">{{ item.actualSpent | number:'1.0-0' }} / {{ item.budgetLimit | number:'1.0-0' }} TND</span>
            <span class="ring-remaining" [class.over]="item.remaining < 0">
              {{ item.remaining >= 0 ? (item.remaining | number:'1.0-0') + ' left' : ((-item.remaining) | number:'1.0-0') + ' over' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ── TAB 3: Insights (chart) ───────────── -->
    <div *ngIf="activeTab === 'insights' && !loading()">
      <div class="card chart-card">
        <h2>Budget vs Actual</h2>
        <canvas #insightsChart></canvas>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
    .page-header h1 { margin-bottom: 0; }
    .eyebrow { display: inline-flex; padding: 6px 10px; border-radius: 999px; background: var(--helma-mint); color: var(--helma-teal); font-size: 12px; font-weight: 800; margin-bottom: 8px; }
    h2 { font-size: 13px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 14px; }

    .trust-banner { background: linear-gradient(135deg, rgba(15,107,104,0.06), rgba(212,166,42,0.08)); border: 1px solid var(--color-border); border-radius: 18px; padding: 18px 22px; margin-bottom: 20px; }
    .trust-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
    .trust-label { display: block; font-size: 11px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; margin-bottom: 4px; }
    .trust-value { font-size: 20px; font-weight: 800; display: block; }
    .trust-badge { padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 800; background: var(--helma-gold-soft); color: #6b5900; }

    .form-card { margin-bottom: 20px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 14px; }
    .field label { display: block; font-size: 12px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
    .form-actions { display: flex; gap: 10px; margin-top: 16px; }

    .tabs-bar { display: flex; gap: 4px; margin-bottom: 20px; background: var(--color-border); border-radius: 12px; padding: 4px; }
    .tab { flex: 1; padding: 10px; border: none; border-radius: 10px; background: transparent; font: inherit; font-size: 14px; font-weight: 700; cursor: pointer; color: var(--color-text-muted); transition: 0.15s; }
    .tab.active { background: #fff; color: var(--helma-teal); box-shadow: 0 2px 8px rgba(0,0,0,0.06); }

    .budget-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 14px; }
    .budget-card { display: flex; flex-direction: column; gap: 10px; }
    .budget-top { display: flex; justify-content: space-between; align-items: center; }
    .budget-cat { font-weight: 700; font-size: 16px; text-transform: capitalize; }
    .budget-month { font-size: 12px; color: var(--color-text-muted); padding: 4px 10px; background: var(--color-surface-soft); border-radius: 999px; }
    .budget-amount { font-size: 26px; font-weight: 800; color: var(--helma-teal); }
    .budget-actions { display: flex; gap: 6px; }
    .btn-sm { padding: 6px 10px; border-radius: 8px; border: 1px solid var(--color-border); background: #fff; font-size: 12px; font-weight: 600; cursor: pointer; }
    .btn-sm:hover { border-color: var(--helma-teal); color: var(--helma-teal); }
    .btn-danger { color: var(--color-danger); }

    .rings-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
    .ring-card { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 24px 16px; }
    .ring-visual { position: relative; width: 80px; height: 80px; }
    .ring-svg { width: 80px; height: 80px; }
    .ring-pct { position: absolute; inset: 0; display: grid; place-items: center; font-size: 16px; font-weight: 800; color: var(--helma-teal); }
    .ring-pct.over { color: var(--color-danger); }
    .ring-info { text-align: center; display: flex; flex-direction: column; gap: 4px; }
    .ring-cat { font-weight: 700; font-size: 15px; text-transform: capitalize; }
    .ring-detail { font-size: 13px; color: var(--color-text-muted); }
    .ring-remaining { font-size: 12px; font-weight: 700; color: var(--color-success); }
    .ring-remaining.over { color: var(--color-danger); }

    .chart-card { min-height: 300px; }
    .chart-card canvas { max-height: 280px; }
    .loading, .empty { padding: 30px; text-align: center; color: var(--color-text-muted); }

    @media (max-width: 700px) { .form-grid, .trust-row { grid-template-columns: 1fr; } }
  `]
})
export class BudgetsPage implements OnInit {
  private readonly api = inject(BudgetApiService);
  private readonly auth = inject(AuthStorageService);

  budgets = signal<Budget[]>([]);
  trust = signal<TrustBudgetResponse | null>(null);
  bva = signal<any[]>([]);
  loading = signal(false);
  showForm = false;
  editingId: number | null = null;
  activeTab = 'plan';
  form = { category: '', limitAmount: 0, monthStart: new Date().toISOString().slice(0, 10) };

  @ViewChild('insightsChart') insightsCanvas!: ElementRef<HTMLCanvasElement>;
  private insightsChartInstance: any = null;

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
    if (!userId) return;
    this.loading.set(true);
    const monthStr = this.currentMonthStr();

    this.api.getBudgets(userId).subscribe({
      next: (b) => { this.budgets.set(b); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
    this.api.getTrustBudget(userId).subscribe({ next: (t) => this.trust.set(t) });
    this.api.getBudgetVsActual(userId, monthStr).subscribe({ next: (bva) => this.bva.set(bva) });
  }

  loadInsights(): void {
    setTimeout(() => {
      if (!this.insightsCanvas || !this.bva().length) return;
      if (this.insightsChartInstance) this.insightsChartInstance.destroy();
      const data = this.bva();
      this.insightsChartInstance = new Chart(this.insightsCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels: data.map(b => b.category),
          datasets: [
            { label: 'Budget', data: data.map(b => b.budgetLimit), backgroundColor: 'rgba(15,107,104,0.3)', borderColor: '#0F6B68', borderWidth: 1, borderRadius: 6 },
            { label: 'Spent', data: data.map(b => b.actualSpent), backgroundColor: data.map(b => b.usagePercent > 100 ? 'rgba(214,69,69,0.7)' : 'rgba(212,166,42,0.7)'), borderRadius: 6 }
          ]
        },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, scales: { y: { beginAtZero: true } } }
      });
    }, 100);
  }

  ringDash(pct: number): string {
    const circ = 2 * Math.PI * 34;
    const fill = Math.min(pct, 100) / 100 * circ;
    return `${fill} ${circ}`;
  }

  currentMonthStr(): string {
    const now = new Date();
    return now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-01';
  }

  submit(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    if (this.editingId) {
      this.api.updateBudget(this.editingId, { limitAmount: this.form.limitAmount, monthStart: this.form.monthStart, category: this.form.category }).subscribe({ next: () => { this.resetForm(); this.load(); } });
    } else {
      this.api.createBudget({ userId, ...this.form } as BudgetCreate).subscribe({ next: () => { this.resetForm(); this.load(); } });
    }
  }

  edit(b: Budget): void {
    this.editingId = b.id;
    this.form = { category: b.category, limitAmount: b.limitAmount, monthStart: b.monthStart };
    this.showForm = true;
  }

  remove(id: number): void {
    if (!confirm('Delete this budget?')) return;
    this.api.deleteBudget(id).subscribe({ next: () => this.load() });
  }

  resetForm(): void {
    this.editingId = null;
    this.form = { category: '', limitAmount: 0, monthStart: new Date().toISOString().slice(0, 10) };
    this.showForm = false;
  }
}
