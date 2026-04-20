import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  AppTag,
  SECTOR_OPTIONS,
  SUB_SECTOR_OPTIONS_BY_SECTOR,
  Sector,
  SubSector,
  TAG_OPTIONS
} from '../../../core/models/application-taxonomy';
import {
  ApplicationRaiseResponse,
  ApplicationRaiseSearchCriteria,
  ApplicationRaiseStatus,
  ApplicationSortKey,
  CrowdfundingType,
  SortDirection
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

@Component({
  selector: 'app-founder-applications-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <h1>My applications</h1>
          <p>Manage your donation and equity drafts before submission.</p>
        </div>

        <div class="actions top-actions">
          <a routerLink="/founder/applications/new/donation">+ Donation draft</a>
          <a routerLink="/founder/applications/new/equity">+ Equity draft</a>
        </div>
      </header>

      <div class="toolbar-card">
        <div class="toolbar-grid search-grid">
          <label class="field field-wide">
            <span>Search</span>
            <input
              type="text"
              placeholder="Business name, summary, company number, or contact"
              [(ngModel)]="search"
              (keyup.enter)="loadApplications()"
            />
          </label>

          <label class="field">
            <span>Status</span>
            <select [(ngModel)]="statusFilter">
              <option value="">All statuses</option>
              <option *ngFor="let status of statusOptions" [value]="status">{{ formatLabel(status) }}</option>
            </select>
          </label>

          <label class="field">
            <span>Type</span>
            <select [(ngModel)]="typeFilter">
              <option value="">All types</option>
              <option *ngFor="let type of crowdfundingTypes" [value]="type">{{ formatLabel(type) }}</option>
            </select>
          </label>

          <label class="field">
            <span>Sector</span>
            <select [(ngModel)]="sectorFilter" (ngModelChange)="onSectorChange($event)">
              <option value="">All sectors</option>
              <option *ngFor="let sector of sectorOptions" [value]="sector">{{ formatLabel(sector) }}</option>
            </select>
          </label>

          <label class="field">
            <span>Sub-sector</span>
            <select [(ngModel)]="subSectorFilter">
              <option value="">All sub-sectors</option>
              <option *ngFor="let subSector of availableSubSectors" [value]="subSector">
                {{ formatLabel(subSector) }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>Tag</span>
            <select [(ngModel)]="tagFilter">
              <option value="">All tags</option>
              <option *ngFor="let tag of tagOptions" [value]="tag">{{ formatLabel(tag) }}</option>
            </select>
          </label>

          <label class="field">
            <span>Sort by</span>
            <select [(ngModel)]="sortBy">
              <option *ngFor="let option of sortOptions" [value]="option.value">{{ option.label }}</option>
            </select>
          </label>

          <label class="field">
            <span>Direction</span>
            <select [(ngModel)]="sortDir">
              <option value="desc">Descending</option>
              <option value="asc">Ascending</option>
            </select>
          </label>
        </div>

        <div class="toolbar-actions">
          <button type="button" (click)="loadApplications()">Apply</button>
          <button type="button" class="secondary" (click)="resetFilters()">Reset</button>
        </div>
      </div>

      <p class="error" *ngIf="error()">{{ error() }}</p>

      <div class="state-card" *ngIf="loading()">Loading applications...</div>

      <div class="empty" *ngIf="!loading() && !applications().length">
        <h2>No applications found</h2>
        <p>Try changing the search or filter criteria.</p>
      </div>

      <div class="grid" *ngIf="!loading() && applications().length">
        <article class="card" *ngFor="let app of applications()">
          <div class="card-top">
            <div>
              <div class="pill type">{{ app.type }}</div>
              <h2>{{ app.businessName }}</h2>
            </div>

            <div class="pill status" [class]="app.status.toLowerCase()">
              {{ formatLabel(app.status) }}
            </div>
          </div>

          <div class="meta">
            <p><strong>Sector:</strong> {{ app.sector ? formatLabel(app.sector) : '—' }}</p>
            <p><strong>Sub-sector:</strong> {{ app.subSector ? formatLabel(app.subSector) : '—' }}</p>
            <p><strong>Tags:</strong> {{ formatTags(app.tags) }}</p>
            <p><strong>Funding goal:</strong> {{ app.fundingGoal ?? '—' }} {{ app.currency || 'TND' }}</p>
            <p><strong>Created:</strong> {{ app.createdAt | date:'medium' }}</p>
            <p><strong>Updated:</strong> {{ app.updatedAt | date:'medium' }}</p>
          </div>

          <div class="card-actions">
            <a
              *ngIf="app.status === applicationStatus.DRAFT"
              [routerLink]="['/founder/applications', app.id, 'edit']"
            >
              Edit
            </a>

            <button
              *ngIf="app.status === applicationStatus.DRAFT"
              type="button"
              (click)="submit(app)"
              [disabled]="busyId() === app.id"
            >
              Submit
            </button>

            <button
              *ngIf="app.status === applicationStatus.DRAFT"
              type="button"
              class="danger"
              (click)="deleteDraft(app)"
              [disabled]="busyId() === app.id"
            >
              Delete
            </button>
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
      gap: 20px;
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

    .toolbar-card,
    .card,
    .empty,
    .state-card {
      background: white;
      border-radius: 18px;
      padding: 20px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.06);
    }

    .toolbar-grid {
      display: grid;
      gap: 14px;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    }

    .search-grid .field-wide {
      grid-column: 1 / -1;
    }

    .field {
      display: grid;
      gap: 8px;
    }

    .field span {
      font-size: 0.86rem;
      font-weight: 700;
      color: #33444d;
    }

    .field input,
    .field select {
      width: 100%;
      min-height: 44px;
      border: 1px solid #d8dfe3;
      border-radius: 10px;
      padding: 0 12px;
      font: inherit;
      background: white;
    }

    .actions,
    .card-actions,
    .toolbar-actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .top-actions a,
    .card-actions a,
    .card-actions button,
    .toolbar-actions button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 42px;
      padding: 0 14px;
      border-radius: 10px;
      border: 0;
      text-decoration: none;
      cursor: pointer;
      background: #062a2b;
      color: white;
      font-weight: 600;
    }

    .toolbar-actions {
      margin-top: 16px;
    }

    .toolbar-actions .secondary {
      background: #eef3f5;
      color: #062a2b;
    }

    .card-actions .danger {
      background: #c0392b;
    }

    .grid {
      display: grid;
      gap: 16px;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    }

    .card {
      display: grid;
      gap: 16px;
    }

    .card-top {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
    }

    .card h2 {
      margin: 10px 0 0;
      font-size: 1.15rem;
      color: #062a2b;
    }

    .meta {
      display: grid;
      gap: 8px;
      color: #33444d;
    }

    .meta p {
      margin: 0;
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

    .status.draft {
      background: #eef3f5;
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
export class FounderApplicationsPageComponent implements OnInit {
  private readonly service = inject(CrowdfundingService);

  readonly loading = signal(false);
  readonly busyId = signal<number | null>(null);
  readonly error = signal('');
  readonly applications = signal<ApplicationRaiseResponse[]>([]);

  readonly applicationStatus = ApplicationRaiseStatus;
  readonly crowdfundingType = CrowdfundingType;
  readonly statusOptions = Object.values(ApplicationRaiseStatus);
  readonly crowdfundingTypes = Object.values(CrowdfundingType);
  readonly sectorOptions = SECTOR_OPTIONS;
  readonly tagOptions = TAG_OPTIONS;
  readonly allSubSectorOptions = Object.values(SubSector);
  readonly sortOptions: ReadonlyArray<{ value: ApplicationSortKey; label: string }> = [
    { value: 'updatedAt', label: 'Last updated' },
    { value: 'createdAt', label: 'Created date' },
    { value: 'businessName', label: 'Business name' },
    { value: 'status', label: 'Status' },
    { value: 'fundingGoal', label: 'Funding goal' },
    { value: 'type', label: 'Type' }
  ];

  search = '';
  statusFilter: ApplicationRaiseStatus | '' = '';
  typeFilter: CrowdfundingType | '' = '';
  sectorFilter: Sector | '' = '';
  subSectorFilter: SubSector | '' = '';
  tagFilter: AppTag | '' = '';
  sortBy: ApplicationSortKey = 'updatedAt';
  sortDir: SortDirection = 'desc';

  get availableSubSectors(): readonly SubSector[] {
    if (!this.sectorFilter) return this.allSubSectorOptions;
    return SUB_SECTOR_OPTIONS_BY_SECTOR[this.sectorFilter] ?? this.allSubSectorOptions;
  }

  ngOnInit(): void {
    this.loadApplications();
  }

  onSectorChange(value: string): void {
    this.sectorFilter = (value as Sector | '') || '';
    if (this.subSectorFilter && !this.availableSubSectors.includes(this.subSectorFilter as SubSector)) {
      this.subSectorFilter = '';
    }
  }

  resetFilters(): void {
    this.search = '';
    this.statusFilter = '';
    this.typeFilter = '';
    this.sectorFilter = '';
    this.subSectorFilter = '';
    this.tagFilter = '';
    this.sortBy = 'updatedAt';
    this.sortDir = 'desc';
    this.loadApplications();
  }

  loadApplications(): void {
    this.loading.set(true);
    this.error.set('');

    this.service
      .listMyApplications(this.buildCriteria())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (apps) => this.applications.set(apps),
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  submit(app: ApplicationRaiseResponse): void {
    if (!window.confirm(`Submit "${app.businessName}" for review?`)) {
      return;
    }

    this.busyId.set(app.id);
    this.error.set('');

    this.service
      .submitApplication(app.id)
      .pipe(finalize(() => this.busyId.set(null)))
      .subscribe({
        next: () => this.loadApplications(),
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  deleteDraft(app: ApplicationRaiseResponse): void {
    if (!window.confirm(`Delete "${app.businessName}" draft?`)) {
      return;
    }

    this.busyId.set(app.id);
    this.error.set('');

    const request$ =
      app.type === CrowdfundingType.EQUITY
        ? this.service.deleteEquityDraft(app.id)
        : this.service.deleteDonationDraft(app.id);

    request$
      .pipe(finalize(() => this.busyId.set(null)))
      .subscribe({
        next: () => this.loadApplications(),
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  formatLabel(value: string): string {
    return value
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  formatTags(tags: string[] | null | undefined): string {
    if (!tags?.length) return '—';
    return tags.map((tag) => this.formatLabel(tag)).join(', ');
  }

  private buildCriteria(): ApplicationRaiseSearchCriteria {
    return {
      search: this.search,
      status: this.statusFilter || null,
      type: this.typeFilter || null,
      sector: this.sectorFilter || null,
      subSector: this.subSectorFilter || null,
      tag: this.tagFilter || null,
      sortBy: this.sortBy,
      sortDir: this.sortDir
    };
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
