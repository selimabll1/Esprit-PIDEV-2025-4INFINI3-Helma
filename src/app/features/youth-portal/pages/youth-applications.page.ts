import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  ApplicationRaiseResponse,
  ApplicationRaiseStatus,
  CrowdfundingType,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

type StatusFilter = 'ALL' | ApplicationRaiseStatus;
type TypeFilter = 'ALL' | CrowdfundingType;
type SortOption =
  | 'status'
  | 'updated_desc'
  | 'updated_asc'
  | 'name_asc'
  | 'name_desc'
  | 'goal_desc'
  | 'goal_asc';

@Component({
  selector: 'app-youth-applications-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="applications-page">
      <header class="hero-card">
        <div class="hero-content">
          <span class="eyebrow">Crowdfunding</span>

          <div class="hero-main">
            <div>
              <h1>My Applications</h1>
              <p>
                Search, manage drafts, and follow the status of your crowdfunding applications.
              </p>
            </div>

            <a
              class="btn btn-primary"
              [routerLink]="['/youth/applications/new']"
            >
              + New application
            </a>
          </div>

          <div class="stats-grid">
            <div class="stat-card">
              <strong>{{ applications.length }}</strong>
              <span>Total</span>
            </div>

            <div class="stat-card">
              <strong>{{ countByStatus(applicationStatus.DRAFT) }}</strong>
              <span>Drafts</span>
            </div>

            <div class="stat-card">
              <strong>
                {{
                  countByStatus(applicationStatus.SUBMITTED) +
                    countByStatus(applicationStatus.UNDER_REVIEW)
                }}
              </strong>
              <span>In review</span>
            </div>

            <div class="stat-card">
              <strong>{{ countByStatus(applicationStatus.APPROVED) }}</strong>
              <span>Approved</span>
            </div>
          </div>
        </div>
      </header>

      <section class="toolbar-card">
        <div class="search-wrap">
          <span class="search-icon">⌕</span>

          <input
            type="search"
            [(ngModel)]="searchTerm"
            (ngModelChange)="applyFilters()"
            placeholder="Search by project name, type, sector, subcategory, status, city..."
          />
        </div>

        <div class="filters-grid">
          <label class="field">
            <span>Status</span>
            <select [(ngModel)]="statusFilter" (ngModelChange)="applyFilters()">
              <option value="ALL">All statuses</option>
              <option [value]="applicationStatus.DRAFT">Draft</option>
              <option [value]="applicationStatus.SUBMITTED">Submitted</option>
              <option [value]="applicationStatus.UNDER_REVIEW">Under review</option>
              <option [value]="applicationStatus.APPROVED">Approved</option>
              <option [value]="applicationStatus.REJECTED">Rejected</option>
            </select>
          </label>

          <label class="field">
            <span>Type</span>
            <select [(ngModel)]="typeFilter" (ngModelChange)="applyFilters()">
              <option value="ALL">All types</option>
              <option [value]="crowdfundingType.DONATION">Donation</option>
              <option [value]="crowdfundingType.EQUITY">Equity</option>
            </select>
          </label>

          <label class="field">
            <span>Sort by</span>
            <select [(ngModel)]="sortOption" (ngModelChange)="applyFilters()">
              <option value="status">Status priority</option>
              <option value="updated_desc">Newest updated</option>
              <option value="updated_asc">Oldest updated</option>
              <option value="name_asc">Project name A-Z</option>
              <option value="name_desc">Project name Z-A</option>
              <option value="goal_desc">Funding goal high-low</option>
              <option value="goal_asc">Funding goal low-high</option>
            </select>
          </label>
        </div>
      </section>

      <p class="error-message" *ngIf="error">
        {{ error }}
      </p>

      <div class="loading-card" *ngIf="loading">
        <div class="loader"></div>
        <span>Loading your applications...</span>
      </div>

      <section
        class="empty-card"
        *ngIf="!loading && filteredApplications.length === 0"
      >
        <div class="empty-icon">🍂</div>
        <h2>No applications found</h2>
        <p>Try another search or create your first crowdfunding application.</p>

        <a class="btn btn-primary" [routerLink]="['/youth/applications/new']">
          Start new application
        </a>
      </section>

      <section
        class="applications-grid"
        *ngIf="!loading && filteredApplications.length > 0"
      >
        <article
          class="application-card"
          *ngFor="let app of filteredApplications; trackBy: trackByApplicationId"
          [class.rejected-card]="app.status === applicationStatus.REJECTED"
          [class.draft-card]="app.status === applicationStatus.DRAFT"
          tabindex="0"
          role="button"
          (click)="openApplication(app)"
          (keydown.enter)="openApplication(app)"
        >
          <div class="card-top">
            <div class="title-group">
              <span
                class="type-pill"
                [class.equity]="app.type === crowdfundingType.EQUITY"
              >
                {{ app.type ? formatLabel(app.type) : 'Type not selected' }}
              </span>

              <h2>{{ app.businessName || 'Untitled draft' }}</h2>
            </div>

            <span class="status-badge" [ngClass]="statusClass(app.status)">
              <span class="status-dot"></span>
              {{ formatLabel(app.status) }}
            </span>
          </div>

          <p class="summary">
            {{
              app.summary ||
                'No summary added yet. Continue the draft to complete this application.'
            }}
          </p>

          <div class="info-grid">
            <div class="info-item">
              <span>Sector</span>
              <strong>{{ formatLabel(app.sector) }}</strong>
            </div>

            <div class="info-item">
              <span>Subcategory</span>
              <strong>{{ formatLabel(app.subSector) }}</strong>
            </div>

            <div class="info-item">
              <span>Stage</span>
              <strong>{{ formatLabel(app.stage) }}</strong>
            </div>

            <div class="info-item">
              <span>Funding goal</span>
              <strong>{{ formatMoney(app.fundingGoal, app.currency) }}</strong>
            </div>

            <div class="info-item">
              <span>Team</span>
              <strong>{{ app.teamSize ?? '—' }}</strong>
            </div>

            <div class="info-item">
              <span>Location</span>
              <strong>{{ formatLocation(app) }}</strong>
            </div>
          </div>

          <div class="progress-area">
            <div class="progress-row">
              <span>Application completion</span>
              <strong>{{ app.applicationCompletionPercent ?? 0 }}%</strong>
            </div>

            <div class="progress-track">
              <span
                class="progress-fill"
                [style.width.%]="clampPercent(app.applicationCompletionPercent)"
              ></span>
            </div>

            <div class="progress-row document-row">
              <span>Documents</span>
              <strong>{{ app.documentCompletionPercent ?? 0 }}%</strong>
            </div>

            <div class="progress-track small">
              <span
                class="progress-fill document"
                [style.width.%]="clampPercent(app.documentCompletionPercent)"
              ></span>
            </div>
          </div>

          <div class="tags-row" *ngIf="app.tags && app.tags.length > 0">
            <span *ngFor="let tag of app.tags.slice(0, 3)">
              #{{ formatLabel(tag) }}
            </span>
          </div>

          <div class="card-footer">
            <div class="date-block">
              <span>Last updated</span>
              <strong>{{ app.updatedAt | date: 'mediumDate' }}</strong>
            </div>

            <div class="actions">
              <a
                class="btn btn-secondary"
                *ngIf="app.status === applicationStatus.DRAFT"
                [routerLink]="['/youth/applications', app.id, 'edit']"
                (click)="$event.stopPropagation()"
              >
                Continue
              </a>

              <a
                class="btn btn-ghost"
                *ngIf="app.status !== applicationStatus.DRAFT"
                [routerLink]="['/youth/applications', app.id, 'edit']"
                [queryParams]="{ mode: 'view' }"
                (click)="$event.stopPropagation()"
              >
                View
              </a>

              <a
                class="btn btn-primary"
                *ngIf="app.status === applicationStatus.APPROVED"
                [routerLink]="['/youth/campaigns/create', app.id]"
                (click)="$event.stopPropagation()"
              >
                Campaign
              </a>

              <button
                class="btn btn-danger"
                type="button"
                *ngIf="app.status === applicationStatus.DRAFT"
                [disabled]="deleteLoadingId === app.id"
                (click)="openDeleteModal(app, $event)"
              >
                Delete
              </button>
            </div>
          </div>

          <div
            class="rejected-stamp"
            *ngIf="app.status === applicationStatus.REJECTED"
          >
            Rejected
          </div>
        </article>
      </section>

      <div
        class="modal-backdrop"
        *ngIf="deleteModalOpen"
        (click)="closeDeleteModal()"
      >
        <div
          class="delete-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="deleteDraftTitle"
          (click)="$event.stopPropagation()"
        >
          <div class="modal-icon">!</div>

          <h2 id="deleteDraftTitle">Delete draft?</h2>

          <p>
            You are about to permanently delete
            <strong>{{ draftToDelete?.businessName || 'Untitled draft' }}</strong>.
            This action cannot be undone.
          </p>

          <div class="modal-actions">
            <button
              type="button"
              class="btn btn-ghost"
              [disabled]="deleteLoadingId !== null"
              (click)="closeDeleteModal()"
            >
              Cancel
            </button>

            <button
              type="button"
              class="btn btn-danger strong-danger"
              [disabled]="deleteLoadingId !== null"
              (click)="confirmDeleteDraft()"
            >
              {{ deleteLoadingId !== null ? 'Deleting...' : 'Yes, delete draft' }}
            </button>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      .applications-page {
        display: grid;
        gap: 24px;
      }

      .hero-card {
        position: relative;
        overflow: hidden;
        border-radius: 32px;
        padding: 34px;
        background:
          radial-gradient(
            circle at 12% 18%,
            rgba(245, 190, 92, 0.36),
            transparent 26%
          ),
          radial-gradient(
            circle at 90% 5%,
            rgba(185, 130, 45, 0.22),
            transparent 28%
          ),
          linear-gradient(135deg, #fffaf0 0%, #fff7df 45%, #f7ebc3 100%);
        border: 1px solid rgba(184, 130, 38, 0.18);
        box-shadow: 0 24px 60px rgba(74, 54, 18, 0.12);
      }

      .hero-card::before,
      .hero-card::after {
        content: '';
        position: absolute;
        width: 250px;
        height: 250px;
        pointer-events: none;
        opacity: 0.42;
        background:
          radial-gradient(
            ellipse at center,
            rgba(184, 130, 38, 0.32) 0 24%,
            transparent 25%
          ),
          radial-gradient(
            ellipse at center,
            rgba(218, 165, 62, 0.28) 0 24%,
            transparent 25%
          ),
          radial-gradient(
            ellipse at center,
            rgba(157, 111, 28, 0.2) 0 24%,
            transparent 25%
          );
        background-size:
          58px 94px,
          72px 110px,
          52px 88px;
        background-position:
          0 0,
          64px 28px,
          128px 4px;
        transform: rotate(-24deg);
      }

      .hero-card::before {
        right: -70px;
        top: -70px;
      }

      .hero-card::after {
        left: -95px;
        bottom: -105px;
        transform: rotate(140deg);
        opacity: 0.24;
      }

      .hero-content {
        position: relative;
        z-index: 1;
        display: grid;
        gap: 26px;
      }

      .hero-main {
        display: flex;
        justify-content: space-between;
        gap: 20px;
        align-items: flex-start;
        flex-wrap: wrap;
      }

      .eyebrow {
        display: inline-flex;
        width: fit-content;
        margin-bottom: 4px;
        padding: 7px 13px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.72);
        color: #9a6a17;
        font-size: var(--fs-caption);
        font-weight: 900;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        border: 1px solid rgba(184, 130, 38, 0.18);
      }

      h1 {
        margin: 0;
        font-size: clamp(2rem, 4vw, 3.25rem);
        line-height: 1;
        color: #2f2414;
        letter-spacing: -0.05em;
      }

      .hero-main p {
        margin: 12px 0 0;
        max-width: 640px;
        color: #6f5a35;
        line-height: 1.7;
        font-weight: 600;
      }

      .stats-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(120px, 1fr));
        gap: 14px;
      }

      .stat-card {
        padding: 16px;
        border-radius: 22px;
        background: rgba(255, 255, 255, 0.68);
        border: 1px solid rgba(184, 130, 38, 0.14);
        backdrop-filter: blur(10px);
      }

      .stat-card strong {
        display: block;
        font-size: 1.6rem;
        color: #2f2414;
        line-height: 1;
      }

      .stat-card span {
        display: block;
        margin-top: 6px;
        color: #7a673e;
        font-size: 0.86rem;
        font-weight: 800;
      }

      .toolbar-card {
        display: grid;
        gap: 18px;
        padding: 20px;
        border-radius: 28px;
        background: rgba(255, 255, 255, 0.9);
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 40px rgba(15, 23, 42, 0.07);
      }

      .search-wrap {
        position: relative;
        max-width: 760px;
        width: 100%;
        margin: 0 auto;
      }

      .search-icon {
        position: absolute;
        left: 20px;
        top: 50%;
        transform: translateY(-50%);
        color: #9a6a17;
        font-size: 1.35rem;
        font-weight: 900;
      }

      .search-wrap input {
        width: 100%;
        min-height: 58px;
        border: 1px solid rgba(184, 130, 38, 0.22);
        border-radius: 999px;
        padding: 0 22px 0 54px;
        background: #fffaf0;
        color: #2f2414;
        font: inherit;
        font-weight: 700;
        outline: none;
      }

      .search-wrap input:focus {
        border-color: rgba(184, 130, 38, 0.55);
        background: #fff;
        box-shadow: 0 0 0 5px rgba(218, 165, 62, 0.15);
      }

      .filters-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(180px, 1fr));
        gap: 14px;
      }

      .field {
        display: grid;
        gap: 8px;
      }

      .field span {
        color: var(--color-text-muted);
        font-size: 0.78rem;
        font-weight: 900;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      .field select {
        width: 100%;
        min-height: 46px;
        border: 1px solid rgba(15, 23, 42, 0.1);
        border-radius: 16px;
        padding: 0 14px;
        background: #fff;
        color: var(--color-text);
        font: inherit;
        font-weight: 800;
        outline: none;
      }

      .applications-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 18px;
      }

      .application-card {
        position: relative;
        overflow: hidden;
        display: grid;
        gap: 18px;
        padding: 22px;
        border-radius: 28px;
        background:
          linear-gradient(
            180deg,
            rgba(255, 255, 255, 0.96),
            rgba(255, 252, 245, 0.96)
          ),
          radial-gradient(
            circle at 92% 0%,
            rgba(218, 165, 62, 0.15),
            transparent 32%
          );
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 20px 48px rgba(15, 23, 42, 0.08);
        cursor: pointer;
        transform: scale(1);
        transition:
          transform 0.22s ease,
          box-shadow 0.22s ease,
          border-color 0.22s ease,
          background 0.22s ease;
      }

      .application-card:hover,
      .application-card:focus-visible {
        transform: scale(1.025);
        box-shadow: 0 30px 70px rgba(15, 23, 42, 0.14);
        border-color: rgba(184, 130, 38, 0.3);
        outline: none;
      }

      .application-card::before {
        content: '';
        position: absolute;
        inset: 0 0 auto;
        height: 5px;
        background: linear-gradient(90deg, #d6a13d, #f5d58a, #b9822d);
      }

      .rejected-card {
        filter: grayscale(0.9);
        background: linear-gradient(
          180deg,
          rgba(248, 248, 248, 0.95),
          rgba(236, 236, 236, 0.95)
        );
        border-color: rgba(107, 114, 128, 0.28);
      }

      .rejected-card::before {
        background: repeating-linear-gradient(
          90deg,
          #6b7280 0,
          #6b7280 16px,
          #9ca3af 16px,
          #9ca3af 32px
        );
      }

      .card-top {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 14px;
        flex-wrap: wrap;
      }

      .title-group {
        display: grid;
        gap: 10px;
        min-width: 0;
      }

      .title-group h2 {
        margin: 0;
        color: #1f2937;
        font-size: 1.32rem;
        line-height: 1.15;
        letter-spacing: -0.03em;
        word-break: break-word;
      }

      .type-pill {
        display: inline-flex;
        width: fit-content;
        padding: 6px 11px;
        border-radius: 999px;
        background: #ecfdf5;
        color: #047857;
        font-size: 0.72rem;
        font-weight: 900;
        text-transform: uppercase;
        letter-spacing: 0.07em;
      }

      .type-pill.equity {
        background: #eff6ff;
        color: #1d4ed8;
      }

      .status-badge {
        display: inline-flex;
        align-items: center;
        gap: 7px;
        padding: 8px 12px;
        border-radius: 999px;
        font-size: 0.76rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        white-space: nowrap;
      }

      .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: currentColor;
      }

      .status-draft {
        background: #fff7ed;
        color: #b45309;
      }

      .status-submitted {
        background: #eff6ff;
        color: #1d4ed8;
      }

      .status-review {
        background: #fff7ed;
        color: #c2410c;
      }

      .status-approved {
        background: #ecfdf5;
        color: #047857;
      }

      .status-rejected {
        background: #f3f4f6;
        color: #4b5563;
      }

      .summary {
        margin: 0;
        color: #667085;
        line-height: 1.65;
        font-weight: 600;
        min-height: 52px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .info-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 10px;
      }

      .info-item {
        min-width: 0;
        padding: 12px;
        border-radius: 18px;
        background: #f8fafc;
        border: 1px solid rgba(15, 23, 42, 0.06);
      }

      .info-item span {
        display: block;
        color: #8a94a6;
        font-size: 0.72rem;
        font-weight: 900;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        margin-bottom: 6px;
      }

      .info-item strong {
        display: block;
        color: #28303f;
        font-size: 0.88rem;
        font-weight: 900;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .progress-area {
        display: grid;
        gap: 8px;
        padding: 14px;
        border-radius: 20px;
        background: #fffaf0;
        border: 1px solid rgba(184, 130, 38, 0.14);
      }

      .progress-row {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        color: #7a673e;
        font-size: 0.84rem;
        font-weight: 900;
      }

      .document-row {
        margin-top: 6px;
      }

      .progress-track {
        height: 9px;
        border-radius: 999px;
        background: rgba(184, 130, 38, 0.14);
        overflow: hidden;
      }

      .progress-track.small {
        height: 7px;
      }

      .progress-fill {
        display: block;
        height: 100%;
        border-radius: inherit;
        background: linear-gradient(90deg, #d6a13d, #f2c86b);
      }

      .progress-fill.document {
        background: linear-gradient(90deg, #0f766e, #2dd4bf);
      }

      .tags-row {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .tags-row span {
        padding: 6px 10px;
        border-radius: 999px;
        background: #f1f5f9;
        color: #475569;
        font-size: 0.76rem;
        font-weight: 850;
      }

      .card-footer {
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
        gap: 14px;
        flex-wrap: wrap;
        padding-top: 4px;
      }

      .date-block {
        display: grid;
        gap: 3px;
      }

      .date-block span {
        color: #98a2b3;
        font-size: 0.74rem;
        font-weight: 900;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .date-block strong {
        color: #344054;
        font-size: 0.9rem;
      }

      .actions {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
        justify-content: flex-end;
      }

      .btn {
        min-height: 42px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        border: 0;
        border-radius: 999px;
        padding: 0 16px;
        font: inherit;
        font-weight: 900;
        text-decoration: none;
        cursor: pointer;
      }

      .btn:disabled {
        cursor: not-allowed;
        opacity: 0.65;
      }

      .btn-primary {
        color: #271a08;
        background: linear-gradient(135deg, #f7d77c, #d6a13d);
        box-shadow: 0 14px 30px rgba(184, 130, 38, 0.24);
      }

      .btn-secondary {
        color: #271a08;
        background: #fff0c7;
        border: 1px solid rgba(184, 130, 38, 0.18);
      }

      .btn-ghost {
        color: #344054;
        background: #f2f4f7;
      }

      .btn-danger {
        color: #991b1b;
        background: #fee2e2;
        border: 1px solid rgba(239, 68, 68, 0.16);
      }

      .strong-danger {
        background: linear-gradient(135deg, #ef4444, #b91c1c);
        color: #ffffff;
        box-shadow: 0 14px 30px rgba(185, 28, 28, 0.24);
      }

      .rejected-stamp {
        position: absolute;
        right: -36px;
        bottom: 26px;
        transform: rotate(-18deg);
        padding: 8px 52px;
        background: rgba(75, 85, 99, 0.12);
        color: rgba(75, 85, 99, 0.55);
        border: 2px solid rgba(75, 85, 99, 0.22);
        font-size: 1.1rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.12em;
        pointer-events: none;
      }

      .loading-card,
      .empty-card {
        display: grid;
        justify-items: center;
        text-align: center;
        gap: 14px;
        padding: 36px 24px;
        border-radius: 28px;
        background: #fff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 40px rgba(15, 23, 42, 0.07);
        color: #475467;
        font-weight: 800;
      }

      .empty-icon {
        width: 72px;
        height: 72px;
        display: grid;
        place-items: center;
        border-radius: 24px;
        background: #fff7ed;
        font-size: 2rem;
      }

      .empty-card h2 {
        margin: 0;
        color: #1f2937;
      }

      .empty-card p {
        max-width: 520px;
        margin: 0;
        line-height: 1.7;
      }

      .loader {
        width: 34px;
        height: 34px;
        border-radius: 50%;
        border: 4px solid rgba(184, 130, 38, 0.16);
        border-top-color: #d6a13d;
        animation: spin 0.75s linear infinite;
      }

      .error-message {
        margin: 0;
        padding: 14px 16px;
        border-radius: 18px;
        background: #fef2f2;
        color: #b42318;
        font-weight: 900;
        border: 1px solid rgba(239, 68, 68, 0.16);
      }

      .modal-backdrop {
        position: fixed;
        inset: 0;
        z-index: 1000;
        display: grid;
        place-items: center;
        padding: 20px;
        background: rgba(15, 23, 42, 0.52);
        backdrop-filter: blur(8px);
        animation: fadeIn 0.18s ease;
      }

      .delete-modal {
        width: min(440px, 100%);
        display: grid;
        justify-items: center;
        gap: 16px;
        padding: 28px;
        border-radius: 30px;
        background:
          radial-gradient(
            circle at top,
            rgba(254, 226, 226, 0.9),
            transparent 48%
          ),
          #ffffff;
        border: 1px solid rgba(239, 68, 68, 0.16);
        box-shadow: 0 34px 90px rgba(15, 23, 42, 0.28);
        text-align: center;
        animation: modalPop 0.2s ease;
      }

      .modal-icon {
        width: 64px;
        height: 64px;
        display: grid;
        place-items: center;
        border-radius: 22px;
        background: #fee2e2;
        color: #b91c1c;
        font-size: 2rem;
        font-weight: 950;
        box-shadow: inset 0 0 0 1px rgba(239, 68, 68, 0.12);
      }

      .delete-modal h2 {
        margin: 0;
        color: #1f2937;
        font-size: 1.55rem;
        letter-spacing: -0.04em;
      }

      .delete-modal p {
        margin: 0;
        max-width: 340px;
        color: #667085;
        line-height: 1.7;
        font-weight: 650;
      }

      .delete-modal p strong {
        color: #991b1b;
      }

      .modal-actions {
        width: 100%;
        display: flex;
        justify-content: center;
        gap: 12px;
        flex-wrap: wrap;
        margin-top: 4px;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
        }

        to {
          opacity: 1;
        }
      }

      @keyframes modalPop {
        from {
          opacity: 0;
          transform: translateY(10px) scale(0.96);
        }

        to {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
      }

      @media (max-width: 1100px) {
        .applications-grid {
          grid-template-columns: 1fr;
        }
      }

      @media (max-width: 760px) {
        .hero-card {
          padding: 24px;
          border-radius: 26px;
        }

        .stats-grid,
        .filters-grid,
        .info-grid {
          grid-template-columns: 1fr;
        }

        .applications-grid {
          grid-template-columns: 1fr;
        }

        .application-card {
          padding: 18px;
          border-radius: 24px;
        }

        .actions {
          width: 100%;
        }

        .actions .btn {
          flex: 1;
        }
      }
    `,
  ],
})
export class YouthApplicationsPageComponent implements OnInit {
  private readonly crowdfundingService = inject(CrowdfundingService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly applicationStatus = ApplicationRaiseStatus;
  readonly crowdfundingType = CrowdfundingType;

  applications: ApplicationRaiseResponse[] = [];
  filteredApplications: ApplicationRaiseResponse[] = [];

  loading = false;
  error: string | null = null;
  deleteLoadingId: number | null = null;

  searchTerm = '';
  statusFilter: StatusFilter = 'ALL';
  typeFilter: TypeFilter = 'ALL';
  sortOption: SortOption = 'status';

  deleteModalOpen = false;
  draftToDelete: ApplicationRaiseResponse | null = null;

  ngOnInit(): void {
    const filter = this.route.snapshot.queryParamMap.get('filter');

    if (filter === 'draft') {
      this.statusFilter = ApplicationRaiseStatus.DRAFT;
    }

    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;

    this.crowdfundingService
      .listMyApplications({
        sortBy: 'updatedAt',
        sortDir: 'desc',
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (items) => {
          this.applications = items ?? [];
          this.applyFilters();
        },
        error: () => {
          this.error = 'Unable to load applications.';
          this.applications = [];
          this.filteredApplications = [];
        },
      });
  }

  applyFilters(): void {
    const search = this.normalize(this.searchTerm);

    this.filteredApplications = this.applications
      .filter((app) => {
        const matchesStatus =
          this.statusFilter === 'ALL' || app.status === this.statusFilter;

        const matchesType =
          this.typeFilter === 'ALL' || app.type === this.typeFilter;

        const searchableText = this.normalize(
          [
            app.businessName,
            app.type,
            app.status,
            app.sector,
            app.subSector,
            app.stage,
            app.summary,
            app.problemStatement,
            app.solution,
            app.targetCustomers,
            app.useOfFunds,
            app.governorate,
            app.city,
            app.fundingGoal,
            app.teamSize,
            ...(app.tags ?? []),
          ].join(' '),
        );

        const matchesSearch = !search || searchableText.includes(search);

        return matchesStatus && matchesType && matchesSearch;
      })
      .sort((a, b) => this.compareApplications(a, b));
  }

  openApplication(app: ApplicationRaiseResponse): void {
    if (app.status === ApplicationRaiseStatus.DRAFT) {
      this.router.navigate(['/youth/applications', app.id, 'edit']);
      return;
    }

    this.router.navigate(['/youth/applications', app.id, 'edit'], {
      queryParams: { mode: 'view' },
    });
  }

  openDeleteModal(app: ApplicationRaiseResponse, event?: MouseEvent): void {
    event?.stopPropagation();

    if (app.status !== ApplicationRaiseStatus.DRAFT) {
      return;
    }

    this.draftToDelete = app;
    this.deleteModalOpen = true;
  }

  closeDeleteModal(): void {
    if (this.deleteLoadingId !== null) {
      return;
    }

    this.deleteModalOpen = false;
    this.draftToDelete = null;
  }

  confirmDeleteDraft(): void {
    const app = this.draftToDelete;

    if (!app || app.status !== ApplicationRaiseStatus.DRAFT) {
      return;
    }

    this.deleteLoadingId = app.id;
    this.error = null;

    this.crowdfundingService
      .deleteDraft(app.id)
      .pipe(finalize(() => (this.deleteLoadingId = null)))
      .subscribe({
        next: () => {
          this.applications = this.applications.filter(
            (item) => item.id !== app.id,
          );
          this.applyFilters();
          this.deleteModalOpen = false;
          this.draftToDelete = null;
        },
        error: () => {
          this.error = 'Unable to delete this draft. Please try again.';
        },
      });
  }

  countByStatus(status: ApplicationRaiseStatus): number {
    return this.applications.filter((app) => app.status === status).length;
  }

  statusClass(status: ApplicationRaiseStatus): string {
    switch (status) {
      case ApplicationRaiseStatus.DRAFT:
        return 'status-draft';

      case ApplicationRaiseStatus.SUBMITTED:
        return 'status-submitted';

      case ApplicationRaiseStatus.UNDER_REVIEW:
        return 'status-review';

      case ApplicationRaiseStatus.APPROVED:
        return 'status-approved';

      case ApplicationRaiseStatus.REJECTED:
        return 'status-rejected';

      default:
        return 'status-draft';
    }
  }

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return 'Not selected';
    }

    return String(value)
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatLocation(app: ApplicationRaiseResponse): string {
    const parts = [app.governorate, app.city].filter(Boolean);
    return parts.length ? parts.join(', ') : 'Not selected';
  }

  formatMoney(
    value: number | null | undefined,
    currency: string | null | undefined,
  ): string {
    if (value === null || value === undefined) {
      return '—';
    }

    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: currency || 'TND',
      maximumFractionDigits: 0,
    }).format(value);
  }

  clampPercent(value: number | null | undefined): number {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return 0;
    }

    return Math.max(0, Math.min(100, value));
  }

  trackByApplicationId(_index: number, app: ApplicationRaiseResponse): number {
    return app.id;
  }

  private compareApplications(
    a: ApplicationRaiseResponse,
    b: ApplicationRaiseResponse,
  ): number {
    switch (this.sortOption) {
      case 'status':
        return (
          this.statusWeight(a.status) - this.statusWeight(b.status) ||
          this.dateValue(b.updatedAt) - this.dateValue(a.updatedAt)
        );

      case 'updated_desc':
        return this.dateValue(b.updatedAt) - this.dateValue(a.updatedAt);

      case 'updated_asc':
        return this.dateValue(a.updatedAt) - this.dateValue(b.updatedAt);

      case 'name_asc':
        return (a.businessName || '').localeCompare(b.businessName || '');

      case 'name_desc':
        return (b.businessName || '').localeCompare(a.businessName || '');

      case 'goal_desc':
        return (b.fundingGoal ?? 0) - (a.fundingGoal ?? 0);

      case 'goal_asc':
        return (a.fundingGoal ?? 0) - (b.fundingGoal ?? 0);

      default:
        return 0;
    }
  }

  private statusWeight(status: ApplicationRaiseStatus): number {
    const order: Record<ApplicationRaiseStatus, number> = {
      [ApplicationRaiseStatus.DRAFT]: 1,
      [ApplicationRaiseStatus.UNDER_REVIEW]: 2,
      [ApplicationRaiseStatus.SUBMITTED]: 3,
      [ApplicationRaiseStatus.APPROVED]: 4,
      [ApplicationRaiseStatus.REJECTED]: 5,
    };

    return order[status] ?? 99;
  }

  private dateValue(value: string | null | undefined): number {
    if (!value) {
      return 0;
    }

    return new Date(value).getTime() || 0;
  }

  private normalize(value: unknown): string {
    return String(value ?? '')
      .toLowerCase()
      .trim();
  }
}