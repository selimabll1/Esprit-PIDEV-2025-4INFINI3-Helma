import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { PledgeResponse } from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

@Component({
  selector: 'app-investor-my-pledges-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>My pledges</h1>
          <p>Track your current pledge statuses across approved campaigns.</p>
        </div>

        <a class="secondary-link" routerLink="/investor/my-payments">Open payments</a>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>
      <div class="state-card" *ngIf="loading()">Loading pledges...</div>

      <div class="empty" *ngIf="!loading() && !pledges().length">
        You do not have any pledges yet.
      </div>

      <div class="grid" *ngIf="!loading() && pledges().length">
        <article class="card" *ngFor="let pledge of pledges()">
          <div class="top">
            <div>
              <div class="pill type">{{ pledge.campaignType }}</div>
              <h2>{{ pledge.campaignBusinessName }}</h2>
            </div>

            <div class="pill status" [class]="pledge.status.toLowerCase()">
              {{ pledge.status }}
            </div>
          </div>

          <div class="meta">
            <p><strong>Your amount:</strong> {{ pledge.amount }} {{ pledge.currency }}</p>
            <p><strong>Campaign goal:</strong> {{ pledge.campaignFundingGoal ?? '—' }} {{ pledge.currency }}</p>
            <p><strong>Campaign raised:</strong> {{ pledge.campaignInvestorsPledgedAmount ?? '—' }} {{ pledge.currency }}</p>
            <p *ngIf="pledge.campaignType === 'EQUITY'">
              <strong>Min investment:</strong> {{ pledge.campaignMinInvestment ?? '—' }} {{ pledge.currency }}
            </p>
            <p><strong>Updated:</strong> {{ pledge.updatedAt | date:'medium' }}</p>
          </div>

          <p class="message" *ngIf="pledge.message">
            “{{ pledge.message }}”
          </p>

          <div class="actions">
            <a [routerLink]="['/investor/campaigns', pledge.applicationRaiseId]">Open campaign</a>
            <a class="secondary" routerLink="/investor/my-payments">Manage payments</a>
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

    .secondary-link {
      display: inline-flex;
      align-items: center;
      min-height: 42px;
      padding: 0 14px;
      border-radius: 10px;
      text-decoration: none;
      background: #eef3f5;
      color: #062a2b;
      font-weight: 700;
    }

    .grid {
      display: grid;
      gap: 16px;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    }

    .card,
    .empty,
    .state-card {
      background: white;
      border-radius: 18px;
      padding: 20px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.06);
    }

    .card {
      display: grid;
      gap: 16px;
    }

    .top {
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

    .meta {
      display: grid;
      gap: 8px;
    }

    .meta p,
    .message {
      margin: 0;
      color: #33444d;
    }

    .message {
      color: #5c6b73;
      font-style: italic;
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

    .status {
      background: #eef3f5;
      color: #455a64;
    }

    .status.pending {
      background: #fff5d7;
      color: #8a6d1d;
    }

    .status.paid {
      background: #e8f7ef;
      color: #1f7a43;
    }

    .status.failed,
    .status.canceled,
    .status.refunded {
      background: #fdecec;
      color: #b03a2e;
    }

    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
    }

    .actions a {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 42px;
      padding: 0 14px;
      border-radius: 10px;
      text-decoration: none;
      background: #062a2b;
      color: white;
      font-weight: 700;
    }

    .actions a.secondary {
      background: #eef3f5;
      color: #062a2b;
    }

    .error {
      margin: 0;
      color: #c0392b;
      font-weight: 600;
    }
  `]
})
export class InvestorMyPledgesPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly pledges = signal<PledgeResponse[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.service
      .listMyPledges()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (pledges) => this.pledges.set(pledges),
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  private extractError(err: HttpErrorResponse): string {
    const fieldErrors = err.error?.fieldErrors;
    if (fieldErrors && typeof fieldErrors === 'object') {
      const joined = Object.values(fieldErrors)
        .filter((value): value is string => typeof value === 'string' && !!value.trim())
        .join(' ');
      if (joined) return joined;
    }

    return (
      err.error?.message ||
      err.error?.error ||
      (typeof err.error === 'string' ? err.error : null) ||
      'Something went wrong.'
    );
  }
}