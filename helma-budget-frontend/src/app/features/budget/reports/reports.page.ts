import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BudgetApiService } from '../../../core/services/budget-api.service';
import { AuthStorageService } from '../../../core/services/auth-storage.service';

@Component({
  selector: 'app-reports-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="reports-root">

      <!-- Header -->
      <header class="rp-header">
        <div class="rp-header-left">
          <span class="rp-eyebrow">Reports</span>
          <h1 class="rp-title">Financial Reports</h1>
          <p class="rp-subtitle">Export detailed PDF analysis for any period.</p>
        </div>
      </header>

      <!-- ── MONTHLY REPORT CARD ─────────────────── -->
      <section class="rp-card" [class.rp-card--active]="activeCard === 'monthly'" (click)="activeCard = 'monthly'">
        <div class="rp-card-left">
          <div class="rp-icon rp-icon--teal">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
              <rect x="7" y="14" width="3" height="3" rx="0.5"/>
            </svg>
          </div>
          <div>
            <h2 class="rp-card-title">Monthly Report</h2>
            <p class="rp-card-desc">Cash flow, budget vs actual, health score, burn rate &amp; savings for a specific month.</p>
          </div>
        </div>

        <div class="rp-controls" (click)="$event.stopPropagation()">
          <!-- Month picker -->
          <div class="rp-picker-group">
            <label class="rp-label">Month</label>
            <div class="rp-select-wrap">
              <select class="rp-select" [(ngModel)]="selectedMonthName">
                <option *ngFor="let m of monthNames; let i = index" [value]="i+1">{{ m }}</option>
              </select>
              <span class="rp-select-arrow">▾</span>
            </div>
          </div>

          <!-- Year picker -->
          <div class="rp-picker-group">
            <label class="rp-label">Year</label>
            <div class="rp-select-wrap">
              <select class="rp-select" [(ngModel)]="selectedMonthYear">
                <option *ngFor="let y of availableYears()" [value]="y">{{ y }}</option>
              </select>
              <span class="rp-select-arrow">▾</span>
            </div>
          </div>

          <button class="rp-btn rp-btn--primary" (click)="downloadMonthly()" [disabled]="downloading()">
            <span *ngIf="!downloading() || activeDownload !== 'monthly'">
              <svg class="rp-btn-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M10 3a1 1 0 011 1v7.586l2.293-2.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L9 11.586V4a1 1 0 011-1z"/><path d="M3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z"/></svg>
              Export PDF
            </span>
            <span *ngIf="downloading() && activeDownload === 'monthly'" class="rp-spinner"></span>
          </button>
        </div>
      </section>

      <!-- ── YEARLY REPORT CARD ──────────────────── -->
      <section class="rp-card" [class.rp-card--active]="activeCard === 'yearly'" (click)="activeCard = 'yearly'">
        <div class="rp-card-left">
          <div class="rp-icon rp-icon--gold">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
            </svg>
          </div>
          <div>
            <h2 class="rp-card-title">Annual Report</h2>
            <p class="rp-card-desc">Full year breakdown: monthly totals, averages, best/worst months, budget discipline &amp; forecast.</p>
          </div>
        </div>

        <div class="rp-controls" (click)="$event.stopPropagation()">
          <div class="rp-picker-group">
            <label class="rp-label">Year</label>
            <div class="rp-select-wrap">
              <select class="rp-select rp-select--wide" [(ngModel)]="selectedYear">
                <option *ngFor="let y of availableYears()" [value]="y">{{ y }}</option>
              </select>
              <span class="rp-select-arrow">▾</span>
            </div>
          </div>

          <button class="rp-btn rp-btn--gold" (click)="downloadYearly()" [disabled]="downloading()">
            <span *ngIf="!downloading() || activeDownload !== 'yearly'">
              <svg class="rp-btn-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M10 3a1 1 0 011 1v7.586l2.293-2.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L9 11.586V4a1 1 0 011-1z"/><path d="M3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z"/></svg>
              Export PDF
            </span>
            <span *ngIf="downloading() && activeDownload === 'yearly'" class="rp-spinner"></span>
          </button>
        </div>
      </section>

      <!-- ── FULL CASH FLOW CARD ─────────────────── -->
      <section class="rp-card" [class.rp-card--active]="activeCard === 'full'" (click)="activeCard = 'full'">
        <div class="rp-card-left">
          <div class="rp-icon rp-icon--purple">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
            </svg>
          </div>
          <div>
            <h2 class="rp-card-title">Full Cash Flow Report</h2>
            <p class="rp-card-desc">Complete history with executive summary, all months, 3-month forecast &amp; recommendations.</p>
          </div>
        </div>

        <div class="rp-controls" (click)="$event.stopPropagation()">
          <p class="rp-all-data-note">Covers all available data for your account</p>
          <button class="rp-btn rp-btn--purple" (click)="downloadFull()" [disabled]="downloading()">
            <span *ngIf="!downloading() || activeDownload !== 'full'">
              <svg class="rp-btn-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M10 3a1 1 0 011 1v7.586l2.293-2.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L9 11.586V4a1 1 0 011-1z"/><path d="M3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z"/></svg>
              Export PDF
            </span>
            <span *ngIf="downloading() && activeDownload === 'full'" class="rp-spinner"></span>
          </button>
        </div>
      </section>

      <!-- Status messages -->
      <div class="rp-toast rp-toast--success" *ngIf="successMsg()">
        <svg viewBox="0 0 20 20" fill="currentColor" class="rp-toast-icon"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
        {{ successMsg() }}
      </div>
      <div class="rp-toast rp-toast--error" *ngIf="errorMsg()">
        <svg viewBox="0 0 20 20" fill="currentColor" class="rp-toast-icon"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/></svg>
        {{ errorMsg() }}
      </div>

    </div>
  `,
  styles: [`
    :host { display: block; }

    .reports-root {
      max-width: 860px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    /* ── Header ── */
    .rp-header { margin-bottom: 8px; }
    .rp-eyebrow {
      display: inline-flex; padding: 5px 12px; border-radius: 999px;
      background: var(--helma-mint); color: var(--helma-teal);
      font-size: 11px; font-weight: 800; letter-spacing: 0.6px;
      text-transform: uppercase; margin-bottom: 10px;
    }
    .rp-title { font-size: 26px; font-weight: 800; margin: 0 0 4px; }
    .rp-subtitle { color: var(--color-text-muted); margin: 0; font-size: 14px; }

    /* ── Card ── */
    .rp-card {
      background: #fff;
      border: 1.5px solid var(--color-border);
      border-radius: 20px;
      padding: 24px 28px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
      cursor: pointer;
      transition: border-color 0.18s, box-shadow 0.18s, transform 0.15s;
    }
    .rp-card:hover {
      border-color: #c8d8d7;
      box-shadow: 0 4px 20px rgba(15,107,104,0.08);
      transform: translateY(-1px);
    }
    .rp-card--active {
      border-color: var(--helma-teal) !important;
      box-shadow: 0 0 0 3px rgba(15,107,104,0.10), 0 4px 20px rgba(15,107,104,0.10) !important;
    }

    .rp-card-left {
      display: flex;
      align-items: center;
      gap: 18px;
      flex: 1;
      min-width: 0;
    }

    /* ── Icon ── */
    .rp-icon {
      width: 52px; height: 52px; border-radius: 16px;
      display: grid; place-items: center; flex-shrink: 0;
    }
    .rp-icon svg { width: 24px; height: 24px; }
    .rp-icon--teal { background: var(--helma-teal-soft, #e8f4f4); color: var(--helma-teal); }
    .rp-icon--gold { background: var(--helma-gold-soft, #fdf6e3); color: #b5890a; }
    .rp-icon--purple { background: #f0eeff; color: #6c5ce7; }

    /* ── Card text ── */
    .rp-card-title { font-size: 16px; font-weight: 800; margin: 0 0 4px; }
    .rp-card-desc { font-size: 13px; color: var(--color-text-muted); margin: 0; line-height: 1.5; }

    /* ── Controls (right side) ── */
    .rp-controls {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-shrink: 0;
    }

    .rp-picker-group {
      display: flex;
      flex-direction: column;
      gap: 5px;
    }
    .rp-label {
      font-size: 10px; font-weight: 800; color: var(--color-text-muted);
      text-transform: uppercase; letter-spacing: 0.5px;
    }
    .rp-select-wrap {
      position: relative;
      display: flex;
      align-items: center;
    }
    .rp-select {
      appearance: none;
      -webkit-appearance: none;
      background: var(--color-surface-soft, #f5f7f9);
      border: 1.5px solid var(--color-border);
      border-radius: 10px;
      padding: 8px 32px 8px 12px;
      font-size: 13px;
      font-weight: 700;
      font-family: inherit;
      color: var(--color-text, #1a1a2e);
      cursor: pointer;
      min-width: 90px;
      transition: border-color 0.15s, box-shadow 0.15s;
      outline: none;
    }
    .rp-select--wide { min-width: 80px; }
    .rp-select:focus {
      border-color: var(--helma-teal);
      box-shadow: 0 0 0 3px rgba(15,107,104,0.12);
    }
    .rp-select-arrow {
      position: absolute; right: 10px;
      font-size: 11px; color: var(--color-text-muted);
      pointer-events: none;
    }

    .rp-all-data-note {
      font-size: 12px; color: var(--color-text-muted);
      font-style: italic; margin: 0; max-width: 160px;
      text-align: right;
    }

    /* ── Buttons ── */
    .rp-btn {
      display: inline-flex; align-items: center; justify-content: center;
      gap: 6px; padding: 10px 18px; border-radius: 12px; border: none;
      font-family: inherit; font-size: 13px; font-weight: 800;
      cursor: pointer; white-space: nowrap; min-width: 126px; height: 40px;
      transition: opacity 0.15s, transform 0.12s, box-shadow 0.15s;
    }
    .rp-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
    .rp-btn:active:not(:disabled) { transform: translateY(0); }
    .rp-btn:disabled { opacity: 0.55; cursor: not-allowed; }
    .rp-btn-icon { width: 16px; height: 16px; flex-shrink: 0; }

    .rp-btn--primary { background: var(--helma-teal); color: #fff; }
    .rp-btn--gold    { background: #D4A62A; color: #fff; }
    .rp-btn--purple  { background: #6c5ce7; color: #fff; }

    /* ── Spinner ── */
    .rp-spinner {
      width: 16px; height: 16px; border-radius: 50%;
      border: 2px solid rgba(255,255,255,0.35);
      border-top-color: #fff;
      animation: spin 0.7s linear infinite;
      display: inline-block;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* ── Toast ── */
    .rp-toast {
      display: flex; align-items: center; gap: 10px;
      padding: 13px 18px; border-radius: 14px;
      font-size: 13px; font-weight: 600;
      animation: fadeIn 0.2s ease;
    }
    .rp-toast-icon { width: 18px; height: 18px; flex-shrink: 0; }
    .rp-toast--success { background: #eafaf3; color: #0a7a4a; }
    .rp-toast--error   { background: #fff1f1; color: #c0392b; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }

    /* ── Responsive ── */
    @media (max-width: 700px) {
      .rp-card { flex-direction: column; align-items: flex-start; }
      .rp-controls { flex-wrap: wrap; width: 100%; }
      .rp-btn { flex: 1; }
      .rp-all-data-note { text-align: left; max-width: none; }
    }
  `]
})
export class ReportsPage implements OnInit {
  private readonly api = inject(BudgetApiService);
  private readonly auth = inject(AuthStorageService);

  availableYears = signal<number[]>([new Date().getFullYear()]);
  downloading = signal(false);
  successMsg = signal('');
  errorMsg = signal('');
  activeCard = 'monthly';
  activeDownload = '';

  // Monthly controls
  selectedMonthName = new Date().getMonth() + 1; // 1-12
  selectedMonthYear = new Date().getFullYear();

  // Yearly control
  selectedYear = new Date().getFullYear();

  readonly monthNames = [
    'January','February','March','April','May','June',
    'July','August','September','October','November','December'
  ];

  ngOnInit(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;

    this.api.getAvailableMonths(userId).subscribe({
      next: (months) => {
        if (months?.length) {
          const years = [...new Set(months.map(m => new Date(m + 'T00:00:00').getFullYear()))].sort((a, b) => b - a);
          this.availableYears.set(years);
          this.selectedYear = years[0];
          this.selectedMonthYear = years[0];
          // Pre-select latest month
          const latest = months[months.length - 1];
          const d = new Date(latest + 'T00:00:00');
          this.selectedMonthName = d.getMonth() + 1;
          this.selectedMonthYear = d.getFullYear();
        }
      }
    });
  }

  downloadMonthly(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    const month = String(this.selectedMonthName).padStart(2, '0');
    const dateStr = `${this.selectedMonthYear}-${month}-01`;
    this.start('monthly');
    this.api.downloadMonthlyReport(userId, dateStr).subscribe({
      next: (blob) => this.save(blob, `helma-monthly-${this.selectedMonthYear}-${month}.pdf`),
      error: (e) => this.fail(e)
    });
  }

  downloadYearly(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    this.start('yearly');
    this.api.downloadYearlyReport(userId, this.selectedYear).subscribe({
      next: (blob) => this.save(blob, `helma-yearly-${this.selectedYear}.pdf`),
      error: (e) => this.fail(e)
    });
  }

  downloadFull(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    this.start('full');
    this.api.downloadCashFlowReport(userId).subscribe({
      next: (blob) => this.save(blob, 'helma-full-report.pdf'),
      error: (e) => this.fail(e)
    });
  }

  private start(type: string): void {
    this.activeDownload = type;
    this.downloading.set(true);
    this.successMsg.set('');
    this.errorMsg.set('');
  }

  private save(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
    this.downloading.set(false);
    this.activeDownload = '';
    this.successMsg.set('✓ Downloaded: ' + filename);
    setTimeout(() => this.successMsg.set(''), 4000);
  }

  private fail(e: any): void {
    this.downloading.set(false);
    this.activeDownload = '';
    this.errorMsg.set('Failed to generate report: ' + (e.message || 'Server error'));
    setTimeout(() => this.errorMsg.set(''), 5000);
  }
}
