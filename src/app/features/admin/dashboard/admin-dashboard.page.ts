import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, of, timeout } from 'rxjs';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';
import { SessionService } from '../../../core/services/session.service';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="dashboard">
      <div class="hero">
        <div>
          <p class="eyebrow">Back office</p>
          <h1>{{ title() }}</h1>
          <p class="hero__text">
            Monitor submitted applications, review founder activity, and manage platform operations.
          </p>
        </div>

        <a *ngIf="isAdmin()" routerLink="/admin/compliance/new" class="hero__action">
          Add compliance worker
        </a>
      </div>

      <div class="state state--loading" *ngIf="loading()">
        Loading dashboard data...
      </div>

      <div class="state state--error" *ngIf="error()">
        {{ error() }}
      </div>

      <div class="stats">
        <article class="stat-card">
          <span>Total applications</span>
          <strong>{{ totalApplications() }}</strong>
        </article>

        <article class="stat-card">
          <span>Submitted</span>
          <strong>{{ submittedApplications() }}</strong>
        </article>

        <article class="stat-card">
          <span>Under review</span>
          <strong>{{ reviewApplications() }}</strong>
        </article>

        <article class="stat-card">
          <span>Approved</span>
          <strong>{{ approvedApplications() }}</strong>
        </article>

        <article class="stat-card">
          <span>Rejected</span>
          <strong>{{ rejectedApplications() }}</strong>
        </article>
      </div>

      <div class="content-grid">
        <section class="panel">
          <div class="panel__head">
            <div>
              <p class="eyebrow">Applications</p>
              <h2>Latest applications</h2>
            </div>

            <a routerLink="/admin/applications" class="panel__link">View all</a>
          </div>

          <div class="empty" *ngIf="!latestApplications().length && !loading()">
            No submitted applications yet.
          </div>

          <div class="application-list" *ngIf="latestApplications().length">
            <a
              *ngFor="let app of latestApplications()"
              class="application-row"
              [routerLink]="['/admin/applications', app.id]"
            >
              <div class="application-row__main">
                <strong>{{ app.businessName || 'Untitled application' }}</strong>
                <span>
                  {{ beautify(app.type) }}
                  <ng-container *ngIf="app.stage"> · {{ beautify(app.stage) }}</ng-container>
                </span>
              </div>

              <div class="application-row__meta">
                <span class="status" [attr.data-status]="normalize(app.status)">
                  {{ beautify(app.status) }}
                </span>
                <small>{{ formatDate(app.submittedAt || app.createdAt) }}</small>
              </div>
            </a>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <div>
              <p class="eyebrow">Review queue</p>
              <h2>Application status</h2>
            </div>
          </div>

          <div class="status-list">
            <div class="status-item">
              <span>Submitted</span>
              <strong>{{ submittedApplications() }}</strong>
            </div>

            <div class="status-item">
              <span>Under review</span>
              <strong>{{ reviewApplications() }}</strong>
            </div>

            <div class="status-item">
              <span>Approved</span>
              <strong>{{ approvedApplications() }}</strong>
            </div>

            <div class="status-item">
              <span>Rejected</span>
              <strong>{{ rejectedApplications() }}</strong>
            </div>
          </div>
        </section>
      </div>

      <section class="quick-panel">
        <a routerLink="/admin/applications">
          <strong>Applications</strong>
          <span>Review submitted founder applications</span>
        </a>

        <a routerLink="/admin/payments">
          <strong>Payments</strong>
          <span>Open payment monitoring page</span>
        </a>

        <a *ngIf="isAdmin()" routerLink="/admin/compliance/new">
          <strong>Compliance</strong>
          <span>Create a compliance worker account</span>
        </a>
      </section>
    </section>
  `,
  styles: [`
    .dashboard {
      display: grid;
      gap: 22px;
      padding-bottom: 40px;
    }

    .hero,
    .stat-card,
    .panel,
    .quick-panel a,
    .state {
      border: 1px solid rgba(6, 42, 43, 0.08);
      box-shadow: 0 18px 44px rgba(6, 42, 43, 0.08);
    }

    .hero {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 18px;
      padding: 28px;
      border-radius: 30px;
      color: white;
      background:
        radial-gradient(circle at top right, rgba(243, 223, 152, 0.24), transparent 34%),
        linear-gradient(135deg, #062a2b, #0b4440);
      overflow: hidden;
    }

    .eyebrow {
      margin: 0 0 7px;
      color: #b9922f;
      font-size: 0.74rem;
      font-weight: 900;
      letter-spacing: 0.1em;
      text-transform: uppercase;
    }

    .hero .eyebrow {
      color: #f3df98;
    }

    h1,
    h2 {
      margin: 0;
    }

    h1 {
      font-size: clamp(2rem, 4vw, 3rem);
      line-height: 1.05;
      letter-spacing: -0.04em;
    }

    h2 {
      color: #062a2b;
      font-size: 1.35rem;
      letter-spacing: -0.02em;
    }

    .hero__text {
      max-width: 660px;
      margin: 12px 0 0;
      color: rgba(255, 255, 255, 0.76);
      line-height: 1.6;
    }

    .hero__action {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 46px;
      padding: 0 18px;
      border-radius: 999px;
      background: #f3df98;
      color: #062a2b;
      text-decoration: none;
      font-weight: 900;
      white-space: nowrap;
    }

    .state {
      padding: 15px 18px;
      border-radius: 18px;
      font-weight: 800;
      background: rgba(255, 255, 255, 0.92);
    }

    .state--loading {
      color: #52615e;
    }

    .state--error {
      color: #a43a3a;
      background: #fff1f1;
    }

    .stats {
      display: grid;
      grid-template-columns: repeat(5, minmax(0, 1fr));
      gap: 14px;
    }

    .stat-card {
      padding: 18px;
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.94);
    }

    .stat-card span {
      display: block;
      color: #667672;
      font-size: 0.86rem;
      font-weight: 800;
    }

    .stat-card strong {
      display: block;
      margin-top: 9px;
      color: #062a2b;
      font-size: 2.05rem;
      line-height: 1;
    }

    .content-grid {
      display: grid;
      grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
      gap: 18px;
    }

    .panel {
      padding: 22px;
      border-radius: 28px;
      background: rgba(255, 255, 255, 0.94);
    }

    .panel__head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 14px;
      margin-bottom: 18px;
    }

    .panel__link {
      color: #8b6d20;
      font-weight: 900;
      text-decoration: none;
    }

    .empty {
      padding: 22px;
      border-radius: 20px;
      background: #f8f5ec;
      color: #687571;
      font-weight: 800;
    }

    .application-list {
      display: grid;
      gap: 11px;
    }

    .application-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 14px;
      padding: 15px;
      border-radius: 20px;
      background: #faf7ef;
      color: inherit;
      text-decoration: none;
      transition: 0.18s ease;
    }

    .application-row:hover {
      background: #f3eedf;
      transform: translateY(-1px);
    }

    .application-row__main {
      min-width: 0;
      display: grid;
      gap: 4px;
    }

    .application-row__main strong {
      color: #062a2b;
      font-size: 0.98rem;
    }

    .application-row__main span {
      color: #687571;
      font-size: 0.84rem;
      font-weight: 750;
    }

    .application-row__meta {
      display: grid;
      justify-items: end;
      gap: 6px;
      flex: 0 0 auto;
    }

    .application-row__meta small {
      color: #7a8683;
      font-weight: 700;
    }

    .status {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 30px;
      padding: 0 10px;
      border-radius: 999px;
      background: #edf1f0;
      color: #40514d;
      font-size: 0.72rem;
      font-weight: 900;
      white-space: nowrap;
    }

    .status[data-status="SUBMITTED"] {
      background: #fff4d8;
      color: #8b6d20;
    }

    .status[data-status="UNDER_REVIEW"] {
      background: #e9f1ff;
      color: #2f5f9f;
    }

    .status[data-status="APPROVED"] {
      background: #e9f8ef;
      color: #257246;
    }

    .status[data-status="REJECTED"] {
      background: #fff0f0;
      color: #9b3131;
    }

    .status-list {
      display: grid;
      gap: 12px;
    }

    .status-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      padding: 15px;
      border-radius: 20px;
      background: #faf7ef;
    }

    .status-item span {
      color: #687571;
      font-weight: 850;
    }

    .status-item strong {
      color: #062a2b;
      font-size: 1.3rem;
    }

    .quick-panel {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 14px;
    }

    .quick-panel a {
      display: grid;
      gap: 6px;
      padding: 18px;
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.94);
      color: inherit;
      text-decoration: none;
      transition: 0.18s ease;
    }

    .quick-panel a:hover {
      transform: translateY(-1px);
      background: #fffaf0;
    }

    .quick-panel strong {
      color: #062a2b;
      font-size: 1rem;
    }

    .quick-panel span {
      color: #687571;
      font-size: 0.88rem;
      font-weight: 750;
      line-height: 1.45;
    }

    @media (max-width: 1180px) {
      .stats {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }

      .content-grid {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 760px) {
      .hero,
      .panel__head,
      .application-row {
        flex-direction: column;
        align-items: flex-start;
      }

      .application-row__meta {
        justify-items: start;
      }

      .stats,
      .quick-panel {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class AdminDashboardPageComponent implements OnInit {
  private readonly crowdfundingService = inject(CrowdfundingService);
  private readonly sessionService = inject(SessionService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly applications = signal<any[]>([]);

  readonly isAdmin = computed(() => this.sessionService.role() === 'ADMIN');

  readonly title = computed(() => {
    const role = this.sessionService.role();

    if (role === 'ADMIN') {
      return 'Admin dashboard';
    }

    if (role === 'COMPLIANCE') {
      return 'Compliance dashboard';
    }

    return 'Back office dashboard';
  });

  readonly totalApplications = computed(() => this.applications().length);

  readonly submittedApplications = computed(() =>
    this.applications().filter((app) => this.normalize(app.status) === 'SUBMITTED').length
  );

  readonly reviewApplications = computed(() =>
    this.applications().filter((app) => this.normalize(app.status) === 'UNDER_REVIEW').length
  );

  readonly approvedApplications = computed(() =>
    this.applications().filter((app) => this.normalize(app.status) === 'APPROVED').length
  );

  readonly rejectedApplications = computed(() =>
    this.applications().filter((app) => this.normalize(app.status) === 'REJECTED').length
  );

  readonly latestApplications = computed(() =>
    [...this.applications()]
      .sort((a, b) => this.toDate(b?.submittedAt || b?.updatedAt || b?.createdAt) - this.toDate(a?.submittedAt || a?.updatedAt || a?.createdAt))
      .slice(0, 6)
  );

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    this.crowdfundingService.adminListApplications().pipe(
      timeout(7000),
      catchError((err) => {
        console.error('[AdminDashboard] applications failed:', err);
        this.error.set('Could not load applications.');
        return of([]);
      })
    ).subscribe({
      next: (applications) => {
        this.applications.set(this.asArray(applications));
      },
      error: (err) => {
        console.error('[AdminDashboard] subscription error:', err);
        this.applications.set([]);
        this.loading.set(false);
      },
      complete: () => {
        this.loading.set(false);
      }
    });
  }

  normalize(value: unknown): string {
    return String(value ?? '').trim().toUpperCase();
  }

  beautify(value: unknown): string {
    const normalized = String(value ?? 'Unknown').trim();

    if (!normalized) {
      return 'Unknown';
    }

    return normalized
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatDate(value: unknown): string {
    if (!value) {
      return '—';
    }

    const date = new Date(String(value));

    if (Number.isNaN(date.getTime())) {
      return '—';
    }

    return date.toLocaleDateString();
  }

  toDate(value: unknown): number {
    const time = new Date(String(value ?? '')).getTime();
    return Number.isNaN(time) ? 0 : time;
  }

  private asArray(value: unknown): any[] {
    if (Array.isArray(value)) {
      return value;
    }

    if (value && typeof value === 'object') {
      const objectValue = value as any;

      if (Array.isArray(objectValue.content)) {
        return objectValue.content;
      }

      if (Array.isArray(objectValue.data)) {
        return objectValue.data;
      }

      if (Array.isArray(objectValue.items)) {
        return objectValue.items;
      }
    }

    return [];
  }
}