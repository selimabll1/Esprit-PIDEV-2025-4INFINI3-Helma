import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  ApplicationRaiseResponse,
  ApplicationRaiseStatus
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

type FilterKey = 'ALL' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';

@Component({
  selector: 'app-admin-applications-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>Applications for review</h1>
          <p>Review submitted applications, open their documents, and approve or reject them.</p>
        </div>
      </header>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="filters">
        <button
          *ngFor="let item of filterOptions"
          type="button"
          [class.active]="selectedFilter() === item.key"
          (click)="selectedFilter.set(item.key)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="state-card" *ngIf="loading()">Loading applications...</div>

      <div class="empty" *ngIf="!loading() && !filteredApplications().length">
        No applications found for this filter.
      </div>

      <div class="grid" *ngIf="!loading() && filteredApplications().length">
        <article class="card" *ngFor="let app of filteredApplications()">
          <div class="top">
            <div>
              <div class="pill type">{{ app.type }}</div>
              <h2>{{ app.businessName }}</h2>
            </div>

            <div class="pill status" [class]="app.status.toLowerCase()">
              {{ formatEnumLabel(app.status) }}
            </div>
          </div>

          <div class="meta">
            <p><strong>Sector:</strong> {{ app.sector ? formatEnumLabel(app.sector) : '—' }}</p>
            <p><strong>Sub-sector:</strong> {{ app.subSector ? formatEnumLabel(app.subSector) : '—' }}</p>
            <p><strong>Tags:</strong> {{ formatTagList(app.tags) }}</p>
            <p><strong>Funding goal:</strong> {{ app.fundingGoal ?? '—' }} {{ app.currency }}</p>
            <p><strong>Contact:</strong> {{ app.contactFirstName }} {{ app.contactLastName }}</p>
            <p><strong>Email:</strong> {{ app.contactEmail }}</p>
            <p><strong>Documents:</strong> {{ app.documents.length }}</p>
            <p><strong>Submitted/Updated:</strong> {{ app.updatedAt | date:'medium' }}</p>
          </div>

          <div class="actions">
            <a [routerLink]="['/admin/applications', app.id]">Open review</a>
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

    .filters {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }

    .filters button,
    .actions a {
      min-height: 40px;
      border: 0;
      border-radius: 10px;
      padding: 0 14px;
      cursor: pointer;
      background: #eef3f5;
      color: #062a2b;
      font-weight: 700;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }

    .filters button.active,
    .actions a {
      background: #062a2b;
      color: white;
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

    .meta p {
      margin: 0;
      color: #33444d;
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

    .status.submitted {
      background: #fff5d7;
      color: #8a6d1d;
    }

    .status.under_review {
      background: #e8f2ff;
      color: #2c5ea8;
    }

    .status.approved {
      background: #e8f7ef;
      color: #1f7a43;
    }

    .status.rejected {
      background: #fdecec;
      color: #b03a2e;
    }

    .error {
      margin: 0;
      color: #c0392b;
      font-weight: 600;
    }
  `]
})
export class AdminApplicationsPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly applications = signal<ApplicationRaiseResponse[]>([]);
  readonly selectedFilter = signal<FilterKey>('ALL');

  readonly filterOptions = [
    { key: 'ALL' as const, label: 'All' },
    { key: 'SUBMITTED' as const, label: 'Submitted' },
    { key: 'UNDER_REVIEW' as const, label: 'Under review' },
    { key: 'APPROVED' as const, label: 'Approved' },
    { key: 'REJECTED' as const, label: 'Rejected' }
  ];

  readonly filteredApplications = computed(() => {
    const filter = this.selectedFilter();
    const items = this.applications();

    if (filter === 'ALL') return items;
    return items.filter((app) => app.status === filter);
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.service
      .adminListApplications()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (apps) => {
          const sorted = [...apps].sort((a, b) =>
            (b.updatedAt || '').localeCompare(a.updatedAt || '')
          );
          this.applications.set(sorted);
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
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

  private extractError(err: HttpErrorResponse): string {
    return (
      err.error?.message ||
      err.error?.error ||
      (typeof err.error === 'string' ? err.error : null) ||
      'Something went wrong.'
    );
  }
}