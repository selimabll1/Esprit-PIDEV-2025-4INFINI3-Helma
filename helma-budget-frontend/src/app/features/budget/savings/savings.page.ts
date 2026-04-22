import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BudgetApiService } from '../../../core/services/budget-api.service';
import { AuthStorageService } from '../../../core/services/auth-storage.service';
import { SavingsGoal, SavingsGoalCreate } from '../../../core/models/budget.models';

@Component({
  selector: 'app-savings-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="page-header">
      <div>
        <span class="eyebrow">Savings</span>
        <h1>Savings Goals</h1>
      </div>
      <button class="btn btn-primary" (click)="showForm = !showForm">
        {{ showForm ? 'Cancel' : '+ New Goal' }}
      </button>
    </header>

    <!-- ── Add form ────────────────────────────── -->
    <div class="card form-card" *ngIf="showForm">
      <h2>Create Savings Goal</h2>
      <div class="form-grid">
        <div class="field full">
          <label>Goal Name</label>
          <input class="input" type="text" [(ngModel)]="form.name" placeholder="Buy equipment, launch campaign…" />
        </div>
        <div class="field">
          <label>Target Amount (TND)</label>
          <input class="input" type="number" min="1" step="0.01" [(ngModel)]="form.targetAmount" />
        </div>
        <div class="field">
          <label>Deadline</label>
          <input class="input" type="date" [(ngModel)]="form.deadline" />
        </div>
      </div>
      <div class="form-actions">
        <button class="btn btn-primary" (click)="submit()">Create</button>
      </div>
    </div>

    <div class="loading" *ngIf="loading()">Loading…</div>
    <div class="empty card" *ngIf="!loading() && !goals().length">
      No savings goals yet. Create one to start saving towards your objectives.
    </div>

    <!-- ── Goals grid ──────────────────────────── -->
    <div class="goals-grid" *ngIf="goals().length">
      <div class="card goal-card" *ngFor="let g of goals()" [class.completed]="g.completed">
        <div class="goal-top">
          <span class="goal-name">{{ g.name }}</span>
          <span class="goal-status" *ngIf="g.completed">Completed</span>
        </div>

        <div class="goal-progress">
          <div class="progress-info">
            <span>{{ g.currentAmount | number:'1.0-0' }} TND</span>
            <span>{{ g.targetAmount | number:'1.0-0' }} TND</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" [style.width.%]="pct(g)"></div>
          </div>
          <span class="pct-label">{{ pct(g) | number:'1.0-0' }}%</span>
        </div>

        <div class="goal-meta">
          <span *ngIf="g.deadline">Deadline: {{ g.deadline }}</span>
          <span *ngIf="g.weeklyTarget">Weekly target: {{ g.weeklyTarget | number:'1.2-2' }} TND</span>
        </div>

        <div class="goal-actions">
          <button class="btn btn-secondary btn-small" (click)="addProgress(g.id)" *ngIf="!g.completed">
            + Add Progress
          </button>
          <button class="btn-sm btn-danger" (click)="remove(g.id)">Delete</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
    .page-header h1 { margin-bottom: 0; }
    .eyebrow { display: inline-flex; padding: 6px 10px; border-radius: 999px; background: var(--helma-mint); color: var(--helma-teal); font-size: 12px; font-weight: 800; margin-bottom: 8px; }
    h2 { font-size: 14px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 16px; }

    .form-card { margin-bottom: 20px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .full { grid-column: 1 / -1; }
    .field label { display: block; font-size: 12px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
    .form-actions { display: flex; gap: 10px; margin-top: 16px; }

    .goals-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }

    .goal-card { display: flex; flex-direction: column; gap: 16px; }
    .goal-card.completed { opacity: 0.7; border-color: var(--color-success); }
    .goal-top { display: flex; justify-content: space-between; align-items: center; }
    .goal-name { font-size: 18px; font-weight: 700; }
    .goal-status { padding: 4px 10px; border-radius: 999px; background: #eaf8f1; color: var(--color-success); font-size: 12px; font-weight: 800; }

    .goal-progress { display: flex; flex-direction: column; gap: 6px; }
    .progress-info { display: flex; justify-content: space-between; font-size: 13px; font-weight: 600; }
    .progress-track { height: 10px; background: var(--color-border); border-radius: 999px; overflow: hidden; }
    .progress-fill { height: 100%; background: linear-gradient(90deg, var(--helma-teal), var(--helma-gold)); border-radius: 999px; transition: width 0.4s ease; }
    .pct-label { font-size: 12px; font-weight: 800; color: var(--helma-teal); }

    .goal-meta { font-size: 13px; color: var(--color-text-muted); display: flex; flex-direction: column; gap: 4px; }
    .goal-actions { display: flex; gap: 8px; }
    .btn-small { font-size: 12px; padding: 8px 14px; }
    .btn-sm { padding: 6px 10px; border-radius: 8px; border: 1px solid var(--color-border); background: #fff; font-size: 12px; font-weight: 600; cursor: pointer; }
    .btn-danger { color: var(--color-danger); }
    .btn-danger:hover { border-color: var(--color-danger); }
    .loading, .empty { padding: 30px; text-align: center; color: var(--color-text-muted); }
  `]
})
export class SavingsPage implements OnInit {
  private readonly api = inject(BudgetApiService);
  private readonly auth = inject(AuthStorageService);

  goals = signal<SavingsGoal[]>([]);
  loading = signal(false);
  showForm = false;
  form = { name: '', targetAmount: 0, deadline: '' };

  ngOnInit(): void { this.load(); }

  load(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    this.loading.set(true);
    this.api.getSavingsGoals(userId).subscribe({
      next: (g) => { this.goals.set(g.sort((a, b) => Number(a.completed) - Number(b.completed))); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  pct(g: SavingsGoal): number {
    return g.targetAmount > 0 ? Math.min(100, (g.currentAmount / g.targetAmount) * 100) : 0;
  }

  submit(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    const req: SavingsGoalCreate = { userId, ...this.form };
    this.api.createSavingsGoal(req).subscribe({ next: () => { this.showForm = false; this.load(); } });
  }

  addProgress(goalId: number): void {
    const amountStr = prompt('Amount to add?');
    if (!amountStr) return;
    const amount = Number(amountStr);
    if (isNaN(amount) || amount <= 0) return;
    this.api.addGoalProgress(goalId, amount).subscribe({ next: () => this.load() });
  }

  remove(id: number): void {
    if (!confirm('Delete this savings goal?')) return;
    this.api.deleteSavingsGoal(id).subscribe({ next: () => this.load() });
  }
}
