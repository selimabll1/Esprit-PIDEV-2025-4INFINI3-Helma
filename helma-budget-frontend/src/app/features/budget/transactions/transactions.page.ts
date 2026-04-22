import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { BudgetApiService } from '../../../core/services/budget-api.service';
import { AuthStorageService } from '../../../core/services/auth-storage.service';
import { Transaction, TransactionCreate, TransactionType } from '../../../core/models/budget.models';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-transactions-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="page-header">
      <div>
        <span class="eyebrow">Transactions</span>
        <h1>Income &amp; Expenses</h1>
      </div>
      <button class="btn btn-primary" (click)="showForm = !showForm">
        {{ showForm ? 'Cancel' : '+ Add Transaction' }}
      </button>
    </header>

    <div class="card form-card" *ngIf="showForm">
      <h2>{{ editingId ? 'Edit Transaction' : 'New Transaction' }}</h2>
      <div class="form-grid">
        <div class="field">
          <label>Amount (TND)</label>
          <input class="input" type="number" min="0.01" step="0.01"
                 [(ngModel)]="form.amount" placeholder="0.00" />
        </div>
        <div class="field">
          <label>Type</label>
          <select class="select" [(ngModel)]="form.type">
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
          </select>
        </div>

        <div class="field full">
          <label>
            Category
            <span class="ai-badge" *ngIf="aiSuggested">
              ✨ AI suggested
              <button class="ai-badge-dismiss" (click)="aiSuggested = false">✕</button>
            </span>
          </label>
          <div class="category-wrap">
            <input class="input" [class.ai-glow]="aiSuggested" type="text"
                   [(ngModel)]="form.category"
                   placeholder="food, transport, salary…"
                   (input)="aiSuggested = false" />
            <div class="category-spinner" *ngIf="detectingCategory()">
              <span class="spinner-dot"></span>
              <span>Detecting…</span>
            </div>
          </div>
        </div>

        <div class="field full">
          <label>Receipt <span class="optional-tag">optional · auto-detects category</span></label>
          <div class="receipt-upload-area"
               [class.has-image]="receiptPreview"
               (click)="receiptInput.click()"
               (dragover)="$event.preventDefault()"
               (drop)="onDrop($event)">
            <input #receiptInput type="file" accept="image/*" style="display:none"
                   (change)="onFileChange($event)" />
            <div class="receipt-placeholder" *ngIf="!receiptPreview">
              <div class="receipt-icon">🧾</div>
              <span class="receipt-hint">Click or drag &amp; drop a receipt</span>
              <span class="receipt-hint-sub">PNG, JPG, WEBP — max 2 MB</span>
            </div>
            <div class="receipt-preview-wrap" *ngIf="receiptPreview" (click)="$event.stopPropagation()">
              <img [src]="receiptPreview" class="receipt-preview-img" alt="Receipt" />
              <button class="receipt-remove" (click)="removeReceipt()">✕ Remove</button>
            </div>
          </div>
          <div class="receipt-error" *ngIf="receiptError">{{ receiptError }}</div>
        </div>
      </div>

      <div class="form-actions">
        <button class="btn btn-primary" (click)="submit()" [disabled]="saving() || detectingCategory()">
          {{ detectingCategory() ? 'Detecting category…' : editingId ? 'Update' : 'Add' }}
        </button>
        <button class="btn btn-secondary" (click)="resetForm()">Reset</button>
      </div>
    </div>

    <div class="summary-row">
      <div class="summary-pill income">
        <span>Total Income</span>
        <strong>{{ totalIncome() | number:'1.2-2' }} TND</strong>
      </div>
      <div class="summary-pill expense">
        <span>Total Expenses</span>
        <strong>{{ totalExpense() | number:'1.2-2' }} TND</strong>
      </div>
      <div class="summary-pill net" [class.positive]="net() >= 0" [class.negative]="net() < 0">
        <span>Net</span>
        <strong>{{ net() | number:'1.2-2' }} TND</strong>
      </div>
    </div>

    <div class="card table-card">
      <div class="filter-row">
        <select class="select filter-select" [(ngModel)]="filterType" (ngModelChange)="applyFilter()">
          <option value="ALL">All types</option>
          <option value="INCOME">Income only</option>
          <option value="EXPENSE">Expense only</option>
        </select>
        <input class="input filter-input" type="text" [(ngModel)]="filterCategory"
               (input)="applyFilter()" placeholder="Filter by category…" />
      </div>

      <div class="loading" *ngIf="loading()">Loading…</div>
      <div class="empty" *ngIf="!loading() && !filtered().length">No transactions found</div>

      <table *ngIf="filtered().length">
        <thead>
          <tr>
            <th>Date</th><th>Type</th><th>Category</th>
            <th class="right">Amount</th>
            <th class="center">Receipt</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let tx of filtered()">
            <td>{{ tx.txnDate | date:'mediumDate' }}</td>
            <td><span class="type-badge" [class.income]="tx.type==='INCOME'" [class.expense]="tx.type==='EXPENSE'">{{ tx.type }}</span></td>
            <td>{{ tx.category }}</td>
            <td class="right" [class.positive]="tx.type==='INCOME'" [class.negative]="tx.type==='EXPENSE'">
              {{ tx.type === 'INCOME' ? '+' : '-' }}{{ tx.amount | number:'1.2-2' }}
            </td>
            <td class="center">
              <button *ngIf="tx.receiptUrl" class="receipt-thumb-btn" (click)="openReceipt(tx.receiptUrl!)">
                <img [src]="tx.receiptUrl" class="receipt-thumb" alt="receipt" />
              </button>
              <span *ngIf="!tx.receiptUrl" class="no-receipt">—</span>
            </td>
            <td>
              <div class="actions">
                <button class="btn-sm" (click)="edit(tx)">Edit</button>
                <button class="btn-sm btn-danger" (click)="remove(tx.id)">Delete</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="lightbox" *ngIf="lightboxUrl" (click)="lightboxUrl = null">
      <div class="lightbox-inner" (click)="$event.stopPropagation()">
        <button class="lightbox-close" (click)="lightboxUrl = null">✕</button>
        <img [src]="lightboxUrl" class="lightbox-img" alt="Receipt" />
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
    .page-header h1 { margin-bottom: 0; }
    .eyebrow { display: inline-flex; padding: 6px 10px; border-radius: 999px; background: var(--helma-mint); color: var(--helma-teal); font-size: 12px; font-weight: 800; margin-bottom: 8px; }
    .form-card { margin-bottom: 20px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .full { grid-column: 1 / -1; }
    .field label { display: flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
    .optional-tag { font-size: 10px; font-weight: 600; color: #aaa; text-transform: none; letter-spacing: 0; background: #f0f0f0; padding: 2px 7px; border-radius: 999px; }
    .form-actions { display: flex; gap: 10px; margin-top: 16px; }
    .ai-badge { display: inline-flex; align-items: center; gap: 5px; background: linear-gradient(135deg,#e8f4f4,#fdf6e3); border: 1px solid #c8ddd0; border-radius: 999px; padding: 2px 10px; font-size: 11px; font-weight: 700; color: var(--helma-teal); text-transform: none; letter-spacing: 0; animation: popIn 0.2s ease; }
    .ai-badge-dismiss { background: none; border: none; cursor: pointer; font-size: 10px; color: #aaa; padding: 0; }
    .ai-badge-dismiss:hover { color: var(--color-danger); }
    @keyframes popIn { from { transform: scale(0.8); opacity: 0; } to { transform: scale(1); opacity: 1; } }
    .category-wrap { position: relative; }
    .ai-glow { border-color: var(--helma-teal) !important; box-shadow: 0 0 0 3px rgba(15,107,104,0.12); }
    .category-spinner { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--helma-teal); font-weight: 600; pointer-events: none; }
    .spinner-dot { width: 12px; height: 12px; border-radius: 50%; border: 2px solid rgba(15,107,104,0.2); border-top-color: var(--helma-teal); animation: spin 0.7s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .receipt-upload-area { border: 2px dashed var(--color-border); border-radius: 16px; min-height: 120px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: border-color 0.15s, background 0.15s; background: var(--color-surface-soft,#f8f9fa); overflow: hidden; }
    .receipt-upload-area:hover { border-color: var(--helma-teal); background: rgba(15,107,104,0.03); }
    .receipt-upload-area.has-image { border-style: solid; border-color: var(--helma-teal); min-height: 180px; cursor: default; }
    .receipt-placeholder { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 20px; }
    .receipt-icon { font-size: 32px; }
    .receipt-hint { font-size: 13px; font-weight: 600; color: var(--color-text-muted); }
    .receipt-hint-sub { font-size: 11px; color: #aaa; }
    .receipt-preview-wrap { position: relative; width: 100%; display: flex; flex-direction: column; align-items: center; padding: 12px; gap: 10px; }
    .receipt-preview-img { max-height: 200px; max-width: 100%; border-radius: 10px; object-fit: contain; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
    .receipt-remove { background: #fff0f0; border: 1px solid #ffc0c0; color: var(--color-danger); border-radius: 8px; padding: 6px 14px; font-size: 12px; font-weight: 700; cursor: pointer; }
    .receipt-remove:hover { background: var(--color-danger); color: #fff; }
    .receipt-error { color: var(--color-danger); font-size: 12px; margin-top: 6px; font-weight: 600; }
    .summary-row { display: grid; grid-template-columns: repeat(3,1fr); gap: 14px; margin-bottom: 20px; }
    .summary-pill { padding: 16px; border-radius: 14px; background: #fff; border: 1px solid var(--color-border); display: flex; justify-content: space-between; align-items: center; }
    .summary-pill span { font-size: 13px; color: var(--color-text-muted); font-weight: 600; }
    .summary-pill strong { font-size: 18px; }
    .summary-pill.income strong { color: var(--color-success); }
    .summary-pill.expense strong { color: var(--color-danger); }
    .summary-pill.positive strong { color: var(--color-success); }
    .summary-pill.negative strong { color: var(--color-danger); }
    .table-card { padding: 20px; }
    .filter-row { display: flex; gap: 12px; margin-bottom: 16px; }
    .filter-select { max-width: 180px; }
    .filter-input { max-width: 260px; }
    h2 { font-size: 14px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 16px; }
    table { width: 100%; border-collapse: collapse; font-size: 14px; }
    th { text-align: left; padding: 10px 8px; font-size: 11px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid var(--color-border); }
    td { padding: 12px 8px; border-bottom: 1px solid var(--color-border); vertical-align: middle; }
    .right { text-align: right; } .center { text-align: center; }
    tr:hover td { background: var(--color-surface-soft); }
    .positive { color: var(--color-success); font-weight: 700; }
    .negative { color: var(--color-danger); font-weight: 700; }
    .type-badge { padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 800; }
    .type-badge.income { background: #eaf8f1; color: var(--color-success); }
    .type-badge.expense { background: #fff0f0; color: var(--color-danger); }
    .actions { display: flex; gap: 6px; }
    .btn-sm { padding: 6px 10px; border-radius: 8px; border: 1px solid var(--color-border); background: #fff; font-size: 12px; font-weight: 600; cursor: pointer; }
    .btn-sm:hover { border-color: var(--helma-teal); color: var(--helma-teal); }
    .btn-danger { color: var(--color-danger); }
    .btn-danger:hover { border-color: var(--color-danger) !important; color: var(--color-danger) !important; }
    .no-receipt { color: #ccc; font-size: 16px; }
    .receipt-thumb-btn { background: none; border: none; cursor: pointer; padding: 2px; border-radius: 6px; transition: transform 0.15s; }
    .receipt-thumb-btn:hover { transform: scale(1.08); }
    .receipt-thumb { width: 36px; height: 36px; object-fit: cover; border-radius: 6px; border: 1.5px solid var(--color-border); display: block; }
    .lightbox { position: fixed; inset: 0; background: rgba(0,0,0,0.75); display: flex; align-items: center; justify-content: center; z-index: 9999; animation: fadeIn 0.15s ease; }
    .lightbox-inner { position: relative; background: #fff; border-radius: 20px; padding: 16px; max-width: 90vw; max-height: 90vh; display: flex; flex-direction: column; align-items: center; gap: 12px; }
    .lightbox-close { position: absolute; top: 10px; right: 12px; background: #f0f0f0; border: none; border-radius: 50%; width: 32px; height: 32px; font-size: 14px; cursor: pointer; display: grid; place-items: center; font-weight: 700; }
    .lightbox-close:hover { background: #e0e0e0; }
    .lightbox-img { max-width: 80vw; max-height: 80vh; border-radius: 12px; object-fit: contain; }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    .loading, .empty { padding: 30px; text-align: center; color: var(--color-text-muted); }
    @media (max-width: 700px) { .summary-row, .form-grid { grid-template-columns: 1fr; } }
  `]
})
export class TransactionsPage implements OnInit {
  private readonly api = inject(BudgetApiService);
  private readonly auth = inject(AuthStorageService);
  private readonly http = inject(HttpClient);

  transactions = signal<Transaction[]>([]);
  filtered = signal<Transaction[]>([]);
  loading = signal(false);
  saving = signal(false);
  detectingCategory = signal(false);

  showForm = false;
  editingId: number | null = null;
  form = { amount: 0, category: '', type: 'INCOME' as TransactionType };
  filterType = 'ALL';
  filterCategory = '';

  totalIncome = signal(0);
  totalExpense = signal(0);
  net = signal(0);

  receiptPreview: string | null = null;
  receiptError = '';
  lightboxUrl: string | null = null;
  aiSuggested = false;

  ngOnInit(): void { this.load(); }

  load(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    this.loading.set(true);
    this.api.getTransactions(userId).subscribe({
      next: (txs) => {
        this.transactions.set(txs.sort((a, b) => new Date(b.txnDate).getTime() - new Date(a.txnDate).getTime()));
        this.computeTotals(txs);
        this.applyFilter();
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  computeTotals(txs: Transaction[]): void {
    const inc = txs.filter(t => t.type === 'INCOME').reduce((s, t) => s + t.amount, 0);
    const exp = txs.filter(t => t.type === 'EXPENSE').reduce((s, t) => s + t.amount, 0);
    this.totalIncome.set(inc); this.totalExpense.set(exp); this.net.set(inc - exp);
  }

  applyFilter(): void {
    let list = this.transactions();
    if (this.filterType !== 'ALL') list = list.filter(t => t.type === this.filterType);
    if (this.filterCategory.trim()) {
      const q = this.filterCategory.toLowerCase();
      list = list.filter(t => t.category.toLowerCase().includes(q));
    }
    this.filtered.set(list);
  }

  onFileChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    this.processFile(file);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.processFile(event.dataTransfer?.files?.[0]);
  }

  processFile(file: File | undefined): void {
    this.receiptError = '';
    this.aiSuggested = false;
    if (!file) return;
    if (!file.type.startsWith('image/')) { this.receiptError = 'Only image files are allowed.'; return; }
    if (file.size > 2 * 1024 * 1024) { this.receiptError = 'Image must be under 2 MB.'; return; }
    const reader = new FileReader();
    reader.onload = () => {
      this.receiptPreview = reader.result as string;
      if (this.form.type === 'EXPENSE') {
        this.detectCategory(this.receiptPreview);
      }
    };
    reader.readAsDataURL(file);
  }

  detectCategory(base64Image: string): void {
    this.detectingCategory.set(true);
    this.http.post<{ category: string }>(
      `${environment.apiBaseUrl}/transactions/suggest-category`,
      { base64Image }
    ).subscribe({
      next: (res) => {
        if (res?.category && res.category !== 'other') {
          this.form.category = res.category;
          this.aiSuggested = true;
        }
        this.detectingCategory.set(false);
      },
      error: () => this.detectingCategory.set(false)
    });
  }

  removeReceipt(): void {
    this.receiptPreview = null;
    this.receiptError = '';
    this.aiSuggested = false;
  }

  openReceipt(url: string): void { this.lightboxUrl = url; }

  submit(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    if (!this.form.amount || this.form.amount <= 0) {
      alert('Please enter a valid amount.'); return;
    }
    if (!this.form.category?.trim()) {
      alert('Please enter a category.'); return;
    }
    this.saving.set(true);
    if (this.editingId) {
      this.api.updateTransaction(this.editingId, {
        amount: this.form.amount, category: this.form.category,
        type: this.form.type, receiptUrl: this.receiptPreview ?? null
      }).subscribe({
        next: () => { this.resetForm(); this.load(); this.saving.set(false); },
        error: () => this.saving.set(false)
      });
    } else {
      this.api.createTransaction({
        userId, amount: this.form.amount, category: this.form.category,
        type: this.form.type, receiptUrl: this.receiptPreview ?? null
      } as TransactionCreate).subscribe({
        next: () => { this.resetForm(); this.load(); this.saving.set(false); },
        error: () => this.saving.set(false)
      });
    }
  }

  edit(tx: Transaction): void {
    this.editingId = tx.id;
    this.form = { amount: tx.amount, category: tx.category, type: tx.type };
    this.receiptPreview = tx.receiptUrl ?? null;
    this.aiSuggested = false;
    this.showForm = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  remove(id: number): void {
    if (!confirm('Delete this transaction?')) return;
    this.api.deleteTransaction(id).subscribe({ next: () => this.load() });
  }

  resetForm(): void {
    this.editingId = null;
    this.form = { amount: 0, category: '', type: 'INCOME' };
    this.receiptPreview = null; this.receiptError = ''; this.aiSuggested = false;
    this.showForm = false;
  }
}
