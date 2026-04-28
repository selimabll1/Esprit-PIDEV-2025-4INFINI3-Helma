import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  ApplicationRaiseResponse,
  ApplicationRaiseStatus
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

@Component({
  selector: 'app-youth-applications-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <span class="eyebrow">Applications</span>
          <h1>All Applications</h1>
          <p>Review your drafts, submitted applications, and updates.</p>
        </div>

        <a class="btn btn-primary" [routerLink]="['/youth/applications/new']">New Raise Form</a>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="card" *ngIf="loading()">Loading applications...</div>

      <div class="card" *ngIf="!loading() && !filteredApplications().length">
        No applications found.
      </div>

      <section class="cards" *ngIf="!loading() && filteredApplications().length">
        <article class="card app-card" *ngFor="let app of filteredApplications()">
          <div class="row">
            <h2>{{ app.businessName }}</h2>
            <span class="badge">{{ formatLabel(app.status) }}</span>
          </div>

          <p>{{ app.summary || 'No summary yet.' }}</p>

          <div class="meta">
            <span><strong>Type:</strong> {{ formatLabel(app.type) }}</span>
            <span><strong>Goal:</strong> {{ app.fundingGoal ?? '—' }}</span>
            <span><strong>Updated:</strong> {{ app.updatedAt | date:'mediumDate' }}</span>
          </div>

          <div class="actions">
            <a
              class="btn btn-secondary"
              *ngIf="app.status === applicationStatus.DRAFT"
              [routerLink]="['/youth/applications', app.id, 'edit']"
            >
              Edit
            </a>
          </div>
        </article>
      </section>
    </section>
  `,
  styles: [`
    .page {
      display: grid;
      gap: 24px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
      flex-wrap: wrap;
    }

    .eyebrow {
      display: inline-flex;
      margin-bottom: 10px;
      padding: 6px 12px;
      border-radius: 999px;
      background: var(--helma-teal-soft);
      color: var(--helma-teal);
      font-size: var(--fs-caption);
      font-weight: 800;
      text-transform: uppercase;
    }

    .cards {
      display: grid;
      gap: 16px;
    }

    .app-card {
      display: grid;
      gap: 14px;
    }

    .row {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: center;
      flex-wrap: wrap;
    }

    .meta {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      color: var(--color-text-muted);
    }

    .badge {
      display: inline-flex;
      padding: 6px 10px;
      border-radius: 999px;
      background: var(--color-surface-soft);
      font-size: var(--fs-caption);
      font-weight: 800;
    }

    .error {
      color: var(--color-danger);
      font-weight: 700;
    }
  `]
})
export class YouthApplicationsPageComponent implements OnInit {
  private readonly crowdfundingService = inject(CrowdfundingService);
  private readonly route = inject(ActivatedRoute);

  readonly applicationStatus = ApplicationRaiseStatus;
  readonly applications = signal<ApplicationRaiseResponse[]>([]);
  readonly filteredApplications = signal<ApplicationRaiseResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const filter = this.route.snapshot.queryParamMap.get('filter');

    this.loading.set(true);
    this.error.set(null);

    this.crowdfundingService.listMyApplications({ sortBy: 'updatedAt', sortDir: 'desc' })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (items) => {
          this.applications.set(items);

          if (filter === 'draft') {
            this.filteredApplications.set(
              items.filter((item) => item.status === ApplicationRaiseStatus.DRAFT)
            );
          } else {
            this.filteredApplications.set(items);
          }
        },
        error: () => this.error.set('Unable to load applications.')
      });
  }

formatLabel(value: string | null | undefined): string {
  if (!value) {
    return 'Not selected yet';
  }

  return value
    .toLowerCase()
    .split('_')
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}
}