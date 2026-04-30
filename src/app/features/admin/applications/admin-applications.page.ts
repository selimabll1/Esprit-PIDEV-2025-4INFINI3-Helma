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
    <section class="applications-page">
      <header class="page-header">
        <div>
          <p class="eyebrow">Review center</p>
          <h1>Applications</h1>
          <p>
            Consult submitted fundraising applications and open each file for detailed review.
          </p>
        </div>

        <button type="button" class="refresh-btn" (click)="load()">
          Refresh
        </button>
      </header>

      <p class="error-message" *ngIf="error()">
        {{ error() }}
      </p>

      <section class="summary-grid">
        <button
          type="button"
          class="summary-card"
          [class.active]="selectedFilter() === 'ALL'"
          (click)="selectedFilter.set('ALL')"
        >
          <span>All</span>
          <strong>{{ totalCount() }}</strong>
        </button>

        <button
          type="button"
          class="summary-card"
          [class.active]="selectedFilter() === 'SUBMITTED'"
          (click)="selectedFilter.set('SUBMITTED')"
        >
          <span>Submitted</span>
          <strong>{{ submittedCount() }}</strong>
        </button>

        <button
          type="button"
          class="summary-card"
          [class.active]="selectedFilter() === 'UNDER_REVIEW'"
          (click)="selectedFilter.set('UNDER_REVIEW')"
        >
          <span>Under review</span>
          <strong>{{ reviewCount() }}</strong>
        </button>

        <button
          type="button"
          class="summary-card"
          [class.active]="selectedFilter() === 'APPROVED'"
          (click)="selectedFilter.set('APPROVED')"
        >
          <span>Approved</span>
          <strong>{{ approvedCount() }}</strong>
        </button>

        <button
          type="button"
          class="summary-card"
          [class.active]="selectedFilter() === 'REJECTED'"
          (click)="selectedFilter.set('REJECTED')"
        >
          <span>Rejected</span>
          <strong>{{ rejectedCount() }}</strong>
        </button>
      </section>

      <section class="toolbar">
        <div class="search-box">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10.5 4a6.5 6.5 0 0 1 5.16 10.45l4.45 4.44-1.42 1.42-4.44-4.45A6.5 6.5 0 1 1 10.5 4Zm0 2a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9Z"/>
          </svg>

          <input
            type="search"
            placeholder="Search applications..."
            [value]="searchTerm()"
            (input)="searchTerm.set($any($event.target).value)"
          />
        </div>
      </section>

      <div class="state-card" *ngIf="loading()">
        Loading applications...
      </div>

      <section class="empty-card" *ngIf="!loading() && !filteredApplications().length">
        <h2>No applications found</h2>
        <p>No applications match the current search or filter.</p>
      </section>

      <section class="applications-list" *ngIf="!loading() && filteredApplications().length">
        <article
          class="application-card"
          *ngFor="let app of filteredApplications(); trackBy: trackByApplicationId"
        >
          <div class="card-main">
            <div class="card-title-row">
              <div>
                <h2>{{ app.businessName || 'Untitled application' }}</h2>
                <p>{{ app.summary || 'No summary provided.' }}</p>
              </div>

              <span class="status-badge" [ngClass]="statusClass(app.status)">
                {{ formatLabel(app.status) }}
              </span>
            </div>

            <div class="meta-grid">
              <div>
                <span>Type</span>
                <strong>{{ formatLabel(app.type) }}</strong>
              </div>

              <div>
                <span>Stage</span>
                <strong>{{ formatLabel(app.stage) }}</strong>
              </div>

              <div>
                <span>Sector</span>
                <strong>{{ formatSector(app) }}</strong>
              </div>

              <div>
                <span>Location</span>
                <strong>{{ formatLocation(app) }}</strong>
              </div>

              <div>
                <span>Funding goal</span>
                <strong>{{ formatMoney(app.fundingGoal, app.currency) }}</strong>
              </div>

              <div>
                <span>Contact</span>
                <strong>{{ contactName(app) }}</strong>
              </div>
            </div>
          </div>

          <aside class="card-side">
            <div>
              <span>Updated</span>
              <strong>{{ app.updatedAt | date: 'mediumDate' }}</strong>
            </div>

            <a [routerLink]="['/admin/applications', app.id]" class="review-link">
              Open review
            </a>
          </aside>
        </article>
      </section>
    </section>
  `,
  styles: [`
    .applications-page {
      display: grid;
      gap: 18px;
      padding-bottom: 36px;
    }

    .page-header,
    .summary-card,
    .toolbar,
    .application-card,
    .state-card,
    .empty-card {
      border: 1px solid rgba(6, 42, 43, 0.08);
      box-shadow: 0 16px 38px rgba(6, 42, 43, 0.07);
    }

    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 26px;
      border-radius: 28px;
      color: white;
      background:
        radial-gradient(circle at top right, rgba(243, 223, 152, 0.22), transparent 34%),
        linear-gradient(135deg, #062a2b, #0b4440);
    }

    .eyebrow {
      margin: 0 0 7px;
      color: #f3df98;
      font-size: 0.74rem;
      font-weight: 950;
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }

    .page-header h1 {
      margin: 0;
      font-size: clamp(1.9rem, 4vw, 2.8rem);
      line-height: 1.05;
      letter-spacing: -0.04em;
    }

    .page-header p:not(.eyebrow) {
      margin: 10px 0 0;
      max-width: 680px;
      color: rgba(255, 255, 255, 0.74);
      line-height: 1.55;
      font-weight: 650;
    }

    .refresh-btn {
      min-height: 44px;
      padding: 0 16px;
      border: 0;
      border-radius: 999px;
      background: #f3df98;
      color: #062a2b;
      font-weight: 950;
      cursor: pointer;
      white-space: nowrap;
    }

    .error-message {
      margin: 0;
      padding: 14px 16px;
      border-radius: 16px;
      background: #fff0f0;
      color: #a43a3a;
      font-weight: 850;
    }

    .summary-grid {
      display: grid;
      grid-template-columns: repeat(5, minmax(0, 1fr));
      gap: 12px;
    }

    .summary-card {
      text-align: left;
      padding: 16px;
      border-radius: 20px;
      background: rgba(255, 255, 255, 0.92);
      cursor: pointer;
      transition: 0.18s ease;
    }

    .summary-card span {
      color: #667672;
      font-size: 0.82rem;
      font-weight: 850;
    }

    .summary-card strong {
      display: block;
      margin-top: 8px;
      color: #062a2b;
      font-size: 1.8rem;
      line-height: 1;
    }

    .summary-card:hover,
    .summary-card.active {
      transform: translateY(-1px);
      border-color: rgba(6, 42, 43, 0.16);
      background: #fffaf0;
    }

    .summary-card.active {
      box-shadow: 0 18px 36px rgba(6, 42, 43, 0.1);
    }

    .toolbar {
      padding: 12px;
      border-radius: 22px;
      background: rgba(255, 255, 255, 0.9);
    }

    .search-box {
      min-height: 48px;
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 0 14px;
      border-radius: 16px;
      background: #f7fbf8;
      border: 1px solid rgba(6, 42, 43, 0.08);
    }

    .search-box svg {
      width: 19px;
      height: 19px;
      fill: #6d7d78;
      flex: 0 0 auto;
    }

    .search-box input {
      width: 100%;
      border: 0;
      outline: 0;
      background: transparent;
      color: #062a2b;
      font: inherit;
      font-weight: 750;
    }

    .state-card,
    .empty-card {
      padding: 24px;
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.92);
      color: #687571;
      font-weight: 800;
    }

    .empty-card h2 {
      margin: 0 0 6px;
      color: #062a2b;
    }

    .empty-card p {
      margin: 0;
    }

    .applications-list {
      display: grid;
      gap: 14px;
    }

    .application-card {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 180px;
      gap: 18px;
      padding: 18px;
      border-radius: 24px;
      background:
        radial-gradient(circle at top right, rgba(243, 223, 152, 0.12), transparent 36%),
        rgba(255, 255, 255, 0.94);
      transition: 0.18s ease;
    }

    .application-card:hover {
      transform: translateY(-1px);
      box-shadow: 0 20px 48px rgba(6, 42, 43, 0.11);
    }

    .card-main {
      min-width: 0;
      display: grid;
      gap: 14px;
    }

    .card-title-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
    }

    .card-title-row h2 {
      margin: 0;
      color: #062a2b;
      font-size: 1.15rem;
      letter-spacing: -0.025em;
    }

    .card-title-row p {
      margin: 7px 0 0;
      color: #687571;
      line-height: 1.45;
      font-size: 0.9rem;
      font-weight: 650;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .status-badge {
      flex: 0 0 auto;
      min-height: 30px;
      display: inline-flex;
      align-items: center;
      padding: 0 10px;
      border-radius: 999px;
      background: #edf1f0;
      color: #40514d;
      font-size: 0.72rem;
      font-weight: 950;
      white-space: nowrap;
    }

    .status-submitted {
      background: #fff4d8;
      color: #8b6d20;
    }

    .status-review {
      background: #e9f1ff;
      color: #2f5f9f;
    }

    .status-approved {
      background: #e9f8ef;
      color: #257246;
    }

    .status-rejected {
      background: #fff0f0;
      color: #9b3131;
    }

    .meta-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 9px;
    }

    .meta-grid div,
    .card-side div {
      min-width: 0;
      display: grid;
      gap: 4px;
      padding: 11px;
      border-radius: 16px;
      background: #f8fbf8;
      border: 1px solid rgba(6, 42, 43, 0.06);
    }

    .meta-grid span,
    .card-side span {
      color: #73817d;
      font-size: 0.7rem;
      font-weight: 900;
      text-transform: uppercase;
      letter-spacing: 0.055em;
    }

    .meta-grid strong,
    .card-side strong {
      color: #062a2b;
      font-size: 0.88rem;
      line-height: 1.25;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .card-side {
      display: grid;
      align-content: space-between;
      gap: 12px;
    }

    .review-link {
      min-height: 42px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0 14px;
      border-radius: 999px;
      background: linear-gradient(135deg, #062a2b, #0b4440);
      color: #f3df98;
      text-decoration: none;
      font-weight: 950;
      white-space: nowrap;
      box-shadow: 0 12px 24px rgba(6, 42, 43, 0.15);
    }

    @media (max-width: 1180px) {
      .summary-grid {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }

      .application-card {
        grid-template-columns: 1fr;
      }

      .card-side {
        grid-template-columns: 1fr auto;
        align-items: center;
      }
    }

    @media (max-width: 760px) {
      .page-header,
      .card-title-row {
        flex-direction: column;
        align-items: flex-start;
      }

      .summary-grid,
      .meta-grid,
      .card-side {
        grid-template-columns: 1fr;
      }

      .review-link {
        width: 100%;
      }
    }
  `]
})
export class AdminApplicationsPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly applications = signal<ApplicationRaiseResponse[]>([]);
  readonly selectedFilter = signal<FilterKey>('ALL');
  readonly searchTerm = signal('');

  readonly totalCount = computed(() => this.applications().length);

  readonly submittedCount = computed(() =>
    this.applications().filter((app) => this.normalize(app.status) === 'SUBMITTED').length
  );

  readonly reviewCount = computed(() =>
    this.applications().filter((app) => this.normalize(app.status) === 'UNDER_REVIEW').length
  );

  readonly approvedCount = computed(() =>
    this.applications().filter((app) => this.normalize(app.status) === 'APPROVED').length
  );

  readonly rejectedCount = computed(() =>
    this.applications().filter((app) => this.normalize(app.status) === 'REJECTED').length
  );

  readonly filteredApplications = computed(() => {
    const filter = this.selectedFilter();
    const search = this.searchTerm().trim().toLowerCase();

    return this.applications().filter((app) => {
      const matchesFilter =
        filter === 'ALL' || this.normalize(app.status) === filter;

      const searchable = [
        app.businessName,
        app.summary,
        app.type,
        app.status,
        app.stage,
        app.sector,
        app.subSector,
        app.governorate,
        app.city,
        app.contactFirstName,
        app.contactLastName,
        app.contactEmail,
        app.fundingGoal
      ]
        .join(' ')
        .toLowerCase();

      const matchesSearch = !search || searchable.includes(search);

      return matchesFilter && matchesSearch;
    });
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
          const sorted = [...(apps ?? [])].sort(
            (a, b) => this.dateValue(b.updatedAt) - this.dateValue(a.updatedAt)
          );

          this.applications.set(sorted);
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  trackByApplicationId(_index: number, app: ApplicationRaiseResponse): number {
    return app.id;
  }

  normalize(value: unknown): string {
    return String(value ?? '').trim().toUpperCase();
  }

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }

    return String(value)
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatMoney(
    value: number | null | undefined,
    currency: string | null | undefined
  ): string {
    if (value === null || value === undefined) {
      return '—';
    }

    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: currency || 'TND',
      maximumFractionDigits: 0
    }).format(value);
  }

  formatSector(app: ApplicationRaiseResponse): string {
    const sector = this.formatLabel(app.sector);
    const subSector = this.formatLabel(app.subSector);

    if (sector === '—' && subSector === '—') {
      return '—';
    }

    if (subSector === '—') {
      return sector;
    }

    return `${sector} / ${subSector}`;
  }

  formatLocation(app: ApplicationRaiseResponse): string {
    const parts = [app.governorate, app.city].filter(Boolean);
    return parts.length ? parts.join(', ') : '—';
  }

  contactName(app: ApplicationRaiseResponse): string {
    const parts = [app.contactFirstName, app.contactLastName].filter(Boolean);

    if (parts.length) {
      return parts.join(' ');
    }

    return app.contactEmail || '—';
  }

  statusClass(status: ApplicationRaiseStatus | string | null | undefined): string {
    switch (this.normalize(status)) {
      case 'SUBMITTED':
        return 'status-submitted';

      case 'UNDER_REVIEW':
        return 'status-review';

      case 'APPROVED':
        return 'status-approved';

      case 'REJECTED':
        return 'status-rejected';

      default:
        return '';
    }
  }

  private dateValue(value: string | null | undefined): number {
    const time = new Date(value ?? '').getTime();
    return Number.isNaN(time) ? 0 : time;
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