import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  ApplicationRaiseResponse,
  ApplicationRaiseStatus,
  DocumentType
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

type ReviewAction = {
  status: ApplicationRaiseStatus;
  label: string;
  tone: 'review' | 'approve' | 'reject';
};

@Component({
  selector: 'app-admin-application-review-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="review-page">
      <div class="state-card" *ngIf="loading()">
        <span class="loader"></span>
        Loading application...
      </div>

      <p class="success-message" *ngIf="success()">
        {{ success() }}
      </p>

      <p class="error-message" *ngIf="error()">
        {{ error() }}
      </p>

      <ng-container *ngIf="!loading() && application() as app">
        <header class="review-header">
          <div class="review-header__main">
            <div class="review-header__top">
              <a class="back-button" routerLink="/admin/applications">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M15.8 5.4 9.2 12l6.6 6.6-1.4 1.4L6.4 12l8-8 1.4 1.4Z" />
                </svg>
                Back to applications
              </a>

              <span class="status-badge" [ngClass]="statusClass(app.status)">
                <span class="status-dot"></span>
                {{ formatLabel(app.status) }}
              </span>
            </div>

            <div class="review-header__content">
              <div class="title-block">
                <p class="eyebrow">Compliance review</p>
                <h1>{{ app.businessName || 'Untitled application' }}</h1>

                <div class="meta-chips">
                  <span class="meta-chip">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M4 19h16v2H4v-2Zm1-8h3v7H5v-7Zm5-4h3v11h-3V7Zm5 3h3v8h-3v-8ZM3 5l9-4 9 4v2H3V5Z"/>
                    </svg>
                    {{ formatLabel(app.type) }}
                  </span>

                  <span class="meta-chip" *ngIf="app.stage">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M12 2 4 5v6c0 5 3.4 9.7 8 11 4.6-1.3 8-6 8-11V5l-8-3Zm0 2.1 6 2.2v4.7c0 4.1-2.6 8-6 9.2-3.4-1.2-6-5.1-6-9.2V6.3l6-2.2Z"/>
                    </svg>
                    {{ formatLabel(app.stage) }}
                  </span>

                  <span class="meta-chip" *ngIf="app.sector || app.subSector">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M4 4h7v7H4V4Zm9 0h7v4h-7V4ZM4 13h4v7H4v-7Zm6 0h10v7H10v-7Z"/>
                    </svg>
                    {{ formatSector(app) }}
                  </span>

                  <span class="meta-chip" *ngIf="app.updatedAt">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M12 1.8A10.2 10.2 0 1 0 22.2 12 10.21 10.21 0 0 0 12 1.8Zm.8 5.1v4.75l3.3 1.96-.8 1.34-4.1-2.45V6.9Z"/>
                    </svg>
                    Updated {{ app.updatedAt | date: 'mediumDate' }}
                  </span>
                </div>
              </div>

              <aside class="action-panel">
                <div class="action-panel__header">
                  <p class="eyebrow dark">Workflow</p>
                  <h2>{{ reviewStageTitle(app.status) }}</h2>
                </div>

                <p class="action-panel__text">
                  {{ reviewStageDescription(app.status) }}
                </p>

                <div class="action-panel__buttons" *ngIf="reviewActions().length">
                  <button
                    *ngFor="let action of reviewActions()"
                    type="button"
                    class="action-button"
                    [class.action-button--review]="action.tone === 'review'"
                    [class.action-button--approve]="action.tone === 'approve'"
                    [class.action-button--reject]="action.tone === 'reject'"
                    (click)="changeStatus(action.status)"
                    [disabled]="savingStatus()"
                  >
                    <svg *ngIf="action.tone === 'review'" viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M5 4h14v2H5V4Zm0 4h14v2H5V8Zm0 4h9v2H5v-2Zm0 4h7v2H5v-2Zm11.6 3.2-3.1-3.1 1.4-1.4 1.7 1.7 3.7-3.7 1.4 1.4-5.1 5.1Z"/>
                    </svg>

                    <svg *ngIf="action.tone === 'approve'" viewBox="0 0 24 24" aria-hidden="true">
                      <path d="m9.2 16.2-4-4 1.4-1.4 2.6 2.6 8.2-8.2 1.4 1.4-9.6 9.6Z"/>
                    </svg>

                    <svg *ngIf="action.tone === 'reject'" viewBox="0 0 24 24" aria-hidden="true">
                      <path d="m6.4 5 5.6 5.6L17.6 5 19 6.4 13.4 12l5.6 5.6-1.4 1.4-5.6-5.6L6.4 19 5 17.6l5.6-5.6L5 6.4 6.4 5Z"/>
                    </svg>

                    {{ savingStatus() ? 'Saving...' : action.label }}
                  </button>
                </div>

                <div class="final-state" *ngIf="!reviewActions().length">
                  This application is already in a final state.
                </div>
              </aside>
            </div>
          </div>
        </header>

        <main class="review-layout">
          <section class="review-right">
            <article class="panel workspace-panel">
              <div class="workspace-head">
                <div class="panel-head panel-head--flat">
                  <div class="panel-head__icon">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M6 2h9l5 5v15H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Zm8 1.5V8h4.5L14 3.5ZM7 12h10v2H7v-2Zm0 4h10v2H7v-2Z"/>
                    </svg>
                  </div>

                  <div>
                    <p class="eyebrow dark">Documents</p>
                    <h2>Compliance workspace</h2>
                  </div>
                </div>

                <div class="workspace-meta">
                  <span class="doc-count">
                    {{ documents().length }} {{ documents().length === 1 ? 'file' : 'files' }}
                  </span>

                  <span class="selected-doc-pill" *ngIf="selectedDocument()">
                    {{ formatLabel(selectedDocument()!.docType) }}
                  </span>
                </div>
              </div>

              <div class="workspace-empty" *ngIf="!documents().length">
                No documents uploaded for this application.
              </div>

              <div class="workspace-body" *ngIf="documents().length">
                <div class="document-rail">
                  <button
                    type="button"
                    class="document-item"
                    *ngFor="let doc of documents()"
                    [class.active]="selectedDocumentType() === doc.docType"
                    (click)="previewDocument(doc.docType)"
                  >
                    <div class="document-item__title">
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M6 2h9l5 5v15H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Zm8 1.5V8h4.5L14 3.5Z"/>
                      </svg>
                      <span>{{ formatLabel(doc.docType) }}</span>
                    </div>

                    <small class="document-item__name">{{ doc.fileName }}</small>
                    <small class="document-item__size">{{ formatBytes(doc.sizeBytes) }}</small>
                  </button>
                </div>

                <div class="preview-panel">
                  <div class="preview-state" *ngIf="previewLoading()">
                    Loading document...
                  </div>

                  <div class="preview-state preview-state--error" *ngIf="previewError()">
                    {{ previewError() }}
                  </div>

                  <div
                    class="preview-empty"
                    *ngIf="!previewLoading() && !previewUrl() && !previewError()"
                  >
                    Select a document to preview it.
                  </div>

                  <object
                    *ngIf="previewUrl()"
                    [data]="previewUrl()"
                    type="application/pdf"
                    class="pdf-frame"
                  >
                    <div class="preview-empty">
                      This browser could not embed the document preview.
                    </div>
                  </object>
                </div>
              </div>
            </article>
          </section>

          <section class="review-left">
            <article class="panel">
              <div class="panel-head">
                <div class="panel-head__icon">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M6 3h12a1 1 0 0 1 1 1v16.3a.55.55 0 0 1-.85.46L12 16.8l-6.15 3.96A.55.55 0 0 1 5 20.3V4a1 1 0 0 1 1-1Zm2.2 4.2v2h7.6v-2H8.2Zm0 4.1v2h7.6v-2H8.2Z"/>
                  </svg>
                </div>

                <div>
                  <p class="eyebrow dark">Snapshot</p>
                  <h2>Application overview</h2>
                </div>
              </div>

              <div class="info-grid">
                <div class="info-item">
                  <span>Funding goal</span>
                  <strong>{{ formatMoney(app.fundingGoal, app.currency) }}</strong>
                </div>

                <div class="info-item">
                  <span>Location</span>
                  <strong>{{ formatLocation(app) }}</strong>
                </div>

                <div class="info-item">
                  <span>Team size</span>
                  <strong>{{ app.teamSize ?? '—' }}</strong>
                </div>

                <div class="info-item">
                  <span>Website</span>
                  <ng-container *ngIf="app.website; else noWebsite">
                    <a [href]="app.website" target="_blank" rel="noreferrer">
                      Open website
                    </a>
                  </ng-container>
                  <ng-template #noWebsite>
                    <strong>—</strong>
                  </ng-template>
                </div>
              </div>

              <div class="text-block" *ngIf="app.summary">
                <span>Founder summary</span>
                <p>{{ app.summary }}</p>
              </div>
            </article>

            <article class="panel">
              <div class="panel-head">
                <div class="panel-head__icon">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-4.2 0-7.2 2.25-7.2 5v1h14.4v-1c0-2.75-3-5-7.2-5Z"/>
                  </svg>
                </div>

                <div>
                  <p class="eyebrow dark">Founder</p>
                  <h2>Contact details</h2>
                </div>
              </div>

              <div class="founder-card">
                <div class="founder-avatar">
                  {{ contactInitials(app) }}
                </div>

                <div class="founder-meta">
                  <strong>{{ contactName(app) }}</strong>
                  <span>{{ app.contactTitle || 'Founder contact' }}</span>
                </div>
              </div>

              <div class="stack-grid">
                <div class="info-item">
                  <span>Email</span>
                  <strong>{{ app.contactEmail || '—' }}</strong>
                </div>

                <div class="info-item">
                  <span>Phone</span>
                  <strong>{{ app.contactPhone || '—' }}</strong>
                </div>
              </div>
            </article>

            <article class="panel narrative-panel">
              <div class="panel-head">
                <div class="panel-head__icon">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M5 4h14v2H5V4Zm0 4h14v2H5V8Zm0 4h14v2H5v-2Zm0 4h9v2H5v-2Z"/>
                  </svg>
                </div>

                <div>
                  <p class="eyebrow dark">Business case</p>
                  <h2>Submitted answers</h2>
                </div>
              </div>

              <div class="answer-list">
                <section *ngIf="app.problemStatement">
                  <h3>Problem statement</h3>
                  <p>{{ app.problemStatement }}</p>
                </section>

                <section *ngIf="app.solution">
                  <h3>Solution</h3>
                  <p>{{ app.solution }}</p>
                </section>

                <section *ngIf="app.targetCustomers">
                  <h3>Target customers</h3>
                  <p>{{ app.targetCustomers }}</p>
                </section>

                <section *ngIf="app.useOfFunds">
                  <h3>Use of funds</h3>
                  <p>{{ app.useOfFunds }}</p>
                </section>

                <section
                  *ngIf="
                    !app.problemStatement &&
                    !app.solution &&
                    !app.targetCustomers &&
                    !app.useOfFunds
                  "
                >
                  <h3>No extended narrative</h3>
                  <p>The founder did not provide detailed narrative answers.</p>
                </section>
              </div>
            </article>

            <article class="panel" *ngIf="app.equityDetail">
              <div class="panel-head">
                <div class="panel-head__icon">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 19h16v2H4v-2Zm1-8h3v7H5v-7Zm5-4h3v11h-3V7Zm5 3h3v8h-3v-8ZM3 5l9-4 9 4v2H3V5Z"/>
                  </svg>
                </div>

                <div>
                  <p class="eyebrow dark">Verification</p>
                  <h2>Equity company details</h2>
                </div>
              </div>

              <div class="info-grid">
                <div class="info-item">
                  <span>Legal name</span>
                  <strong>{{ app.equityDetail.companyLegalName || '—' }}</strong>
                </div>

                <div class="info-item">
                  <span>Registration number</span>
                  <strong>{{ app.equityDetail.companyRegistrationNumber || '—' }}</strong>
                </div>

                <div class="info-item">
                  <span>Equity offered</span>
                  <strong>{{ app.equityDetail.equityOfferedPercent ?? '—' }}%</strong>
                </div>

                <div class="info-item">
                  <span>Minimum investment</span>
                  <strong>{{ formatMoney(app.equityDetail.minInvestment, app.currency) }}</strong>
                </div>

                <div class="info-item">
                  <span>Pre-money valuation</span>
                  <strong>{{ formatMoney(app.equityDetail.preMoneyValuation, app.currency) }}</strong>
                </div>

                <div class="info-item">
                  <span>CNRE profile</span>
                  <ng-container *ngIf="app.equityDetail.cnreProfileUrl; else noCnre">
                    <a
                      [href]="app.equityDetail.cnreProfileUrl"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Open profile
                    </a>
                  </ng-container>
                  <ng-template #noCnre>
                    <strong>—</strong>
                  </ng-template>
                </div>
              </div>
            </article>
          </section>
        </main>
      </ng-container>
    </section>
  `,
  styles: [`
    :host {
      display: block;
    }

    .review-page {
      display: grid;
      gap: 18px;
      padding-bottom: 36px;
    }

    .review-header,
    .panel,
    .state-card,
    .success-message,
    .error-message {
      border: 1px solid rgba(6, 42, 43, 0.08);
      box-shadow: 0 16px 38px rgba(6, 42, 43, 0.07);
    }

    .state-card,
    .success-message,
    .error-message {
      margin: 0;
      padding: 14px 16px;
      border-radius: 18px;
      font-weight: 850;
      background: rgba(255, 255, 255, 0.94);
    }

    .state-card {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #52615e;
    }

    .success-message {
      color: #257246;
      background: #edf9f1;
    }

    .error-message {
      color: #a43a3a;
      background: #fff0f0;
    }

    .loader {
      width: 20px;
      height: 20px;
      border: 3px solid rgba(6, 42, 43, 0.12);
      border-top-color: #062a2b;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .review-header {
      overflow: hidden;
      border-radius: 28px;
      background:
        radial-gradient(circle at top right, rgba(243, 223, 152, 0.18), transparent 32%),
        linear-gradient(180deg, rgba(247, 250, 249, 0.98), rgba(255, 255, 255, 0.98));
    }

    .review-header__main {
      display: grid;
      gap: 18px;
      padding: 22px;
    }

    .review-header__top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 14px;
    }

    .review-header__content {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 360px;
      gap: 18px;
      align-items: start;
    }

    .back-button {
      min-height: 42px;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 0 14px;
      border-radius: 999px;
      border: 1px solid rgba(6, 42, 43, 0.12);
      background: #ffffff;
      color: #0b4440;
      text-decoration: none;
      font-weight: 900;
      white-space: nowrap;
      transition: 0.18s ease;
    }

    .back-button:hover {
      transform: translateY(-1px);
      background: #f8fbf8;
    }

    .back-button svg {
      width: 18px;
      height: 18px;
      fill: currentColor;
    }

    .status-badge,
    .meta-chip,
    .doc-count,
    .selected-doc-pill {
      min-height: 30px;
      display: inline-flex;
      align-items: center;
      gap: 7px;
      padding: 0 11px;
      border-radius: 999px;
      font-size: 0.74rem;
      font-weight: 900;
      white-space: nowrap;
    }

    .status-badge {
      background: #edf3f1;
      color: #40514d;
    }

    .status-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: currentColor;
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

    .title-block {
      min-width: 0;
      display: grid;
      gap: 10px;
    }

    .eyebrow {
      margin: 0;
      color: #0b7068;
      font-size: 0.72rem;
      font-weight: 950;
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }

    .eyebrow.dark {
      color: #b9922f;
    }

    h1,
    h2,
    h3 {
      margin: 0;
    }

    h1 {
      color: #062a2b;
      font-size: clamp(2rem, 4vw, 2.85rem);
      line-height: 1.05;
      letter-spacing: -0.045em;
    }

    h2 {
      color: #062a2b;
      font-size: 1.15rem;
      letter-spacing: -0.02em;
    }

    h3 {
      color: #062a2b;
      font-size: 0.95rem;
    }

    .meta-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .meta-chip {
      background: #edf5f1;
      color: #0b4440;
    }

    .meta-chip svg {
      width: 16px;
      height: 16px;
      fill: currentColor;
    }

    .action-panel {
      display: grid;
      gap: 12px;
      padding: 16px;
      border-radius: 22px;
      background:
        radial-gradient(circle at top right, rgba(243, 223, 152, 0.12), transparent 46%),
        linear-gradient(180deg, #ffffff, #f8fbf8);
      border: 1px solid rgba(6, 42, 43, 0.08);
    }

    .action-panel__header {
      display: grid;
      gap: 5px;
    }

    .action-panel__text {
      margin: 0;
      color: #687571;
      line-height: 1.5;
      font-weight: 700;
    }

    .action-panel__buttons {
      display: grid;
      gap: 9px;
    }

    .action-button {
      min-height: 44px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 0 14px;
      border: 0;
      border-radius: 14px;
      color: white;
      font-weight: 950;
      cursor: pointer;
      transition: 0.18s ease;
    }

    .action-button svg {
      width: 18px;
      height: 18px;
      fill: currentColor;
    }

    .action-button:hover:not(:disabled) {
      transform: translateY(-1px);
    }

    .action-button:disabled {
      opacity: 0.65;
      cursor: not-allowed;
    }

    .action-button--review {
      background: linear-gradient(135deg, #062a2b, #0b4440);
      color: #f3df98;
    }

    .action-button--approve {
      background: #257246;
    }

    .action-button--reject {
      background: #b54141;
    }

    .final-state {
      padding: 12px 13px;
      border-radius: 14px;
      background: #f7fbf8;
      color: #687571;
      font-weight: 800;
    }

    .review-layout {
      display: flex;
      flex-direction: column;
      gap: 18px;
      min-width: 0;
    }

    .review-left,
    .review-right {
      width: 100%;
      min-width: 0;
    }

    .review-left {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 16px;
      align-items: start;
    }

    .review-right {
      display: block;
      position: static;
      order: -1;
    }

    .panel {
      display: grid;
      gap: 14px;
      padding: 18px;
      border-radius: 24px;
      background:
        radial-gradient(circle at top right, rgba(243, 223, 152, 0.08), transparent 38%),
        rgba(255, 255, 255, 0.96);
    }

    .panel-head {
      display: flex;
      align-items: flex-start;
      gap: 11px;
    }

    .panel-head--flat {
      gap: 11px;
    }

    .panel-head__icon {
      width: 38px;
      height: 38px;
      display: grid;
      place-items: center;
      flex: 0 0 auto;
      border-radius: 14px;
      background: #edf5f1;
      color: #0b4440;
    }

    .panel-head__icon svg {
      width: 20px;
      height: 20px;
      fill: currentColor;
    }

    .info-grid,
    .stack-grid {
      display: grid;
      gap: 10px;
    }

    .info-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .stack-grid {
      grid-template-columns: 1fr;
    }

    .info-item {
      min-width: 0;
      display: grid;
      gap: 5px;
      padding: 12px;
      border-radius: 16px;
      background: #f8fbf8;
      border: 1px solid rgba(6, 42, 43, 0.06);
    }

    .info-item span,
    .text-block span {
      color: #73817d;
      font-size: 0.7rem;
      font-weight: 900;
      text-transform: uppercase;
      letter-spacing: 0.055em;
    }

    .info-item strong,
    .info-item a {
      color: #062a2b;
      font-size: 0.9rem;
      line-height: 1.3;
      font-weight: 900;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .info-item a {
      color: #8b6d20;
      text-decoration: none;
    }

    .info-item a:hover {
      text-decoration: underline;
    }

    .text-block {
      display: grid;
      gap: 6px;
      padding: 14px;
      border-radius: 16px;
      background: #fffaf0;
      border: 1px solid rgba(185, 146, 47, 0.12);
    }

    .text-block p {
      margin: 0;
      color: #52615e;
      line-height: 1.52;
      font-weight: 650;
    }

    .founder-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px;
      border-radius: 18px;
      background: linear-gradient(180deg, #f8fbf8, #fffaf0);
      border: 1px solid rgba(6, 42, 43, 0.06);
    }

    .founder-avatar {
      width: 48px;
      height: 48px;
      display: grid;
      place-items: center;
      border-radius: 16px;
      flex: 0 0 auto;
      background: linear-gradient(135deg, #062a2b, #0b4440);
      color: #f3df98;
      font-weight: 950;
      letter-spacing: 0.04em;
    }

    .founder-meta {
      min-width: 0;
      display: grid;
      gap: 4px;
    }

    .founder-meta strong {
      color: #062a2b;
      font-size: 1rem;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .founder-meta span {
      color: #687571;
      font-size: 0.84rem;
      font-weight: 750;
    }

    .answer-list {
      display: grid;
      gap: 10px;
    }

    .answer-list section {
      display: grid;
      gap: 6px;
      padding: 13px;
      border-radius: 18px;
      background: #f8fbf8;
      border: 1px solid rgba(6, 42, 43, 0.06);
    }

    .answer-list p {
      margin: 0;
      color: #52615e;
      line-height: 1.55;
      font-weight: 650;
    }

    .narrative-panel {
      grid-column: 1 / -1;
    }

    .workspace-panel {
      gap: 16px;
      min-width: 0;
      width: 100%;
    }

    .workspace-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
    }

    .workspace-meta {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 8px;
    }

    .doc-count {
      background: #edf3f1;
      color: #0b4440;
    }

    .selected-doc-pill {
      background: #fff4d8;
      color: #8b6d20;
    }

    .workspace-empty,
    .preview-empty,
    .preview-state {
      padding: 14px;
      border-radius: 16px;
      background: #f7fbf8;
      color: #687571;
      font-weight: 800;
    }

    .preview-state--error {
      background: #fff0f0;
      color: #a43a3a;
    }

    .workspace-body {
      display: grid;
      grid-template-columns: 280px minmax(0, 1fr);
      gap: 16px;
      min-width: 0;
      width: 100%;
    }

    .document-rail {
      display: grid;
      gap: 9px;
      align-content: start;
      max-height: 760px;
      overflow-y: auto;
      overflow-x: hidden;
      padding-right: 3px;
    }

    .document-item {
      width: 100%;
      display: grid;
      gap: 6px;
      text-align: left;
      padding: 12px;
      border-radius: 16px;
      border: 1px solid rgba(6, 42, 43, 0.08);
      background: #fbfdfc;
      cursor: pointer;
      transition: 0.18s ease;
    }

    .document-item:hover {
      transform: translateY(-1px);
      background: #f8fbf8;
    }

    .document-item.active {
      border-color: rgba(6, 42, 43, 0.22);
      background: #fffaf0;
      box-shadow: 0 0 0 3px rgba(243, 223, 152, 0.2);
    }

    .document-item__title {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;
    }

    .document-item__title svg {
      width: 18px;
      height: 18px;
      fill: #0b4440;
      flex: 0 0 auto;
    }

    .document-item__title span {
      color: #062a2b;
      font-size: 0.88rem;
      font-weight: 950;
      line-height: 1.25;
    }

    .document-item__name,
    .document-item__size {
      display: block;
      color: #687571;
      font-weight: 750;
      line-height: 1.35;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .preview-panel {
      min-width: 0;
      display: grid;
      gap: 12px;
      align-content: start;
    }

    .pdf-frame {
      display: block;
      width: 100%;
      height: 780px;
      min-height: 680px;
      max-height: 82vh;
      border: 1px solid rgba(6, 42, 43, 0.1);
      border-radius: 16px;
      background: white;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    @media (max-width: 1320px) {
      .review-header__content {
        grid-template-columns: 1fr;
      }

      .workspace-body {
        grid-template-columns: 240px minmax(0, 1fr);
      }

      .pdf-frame {
        height: 720px;
        min-height: 620px;
        max-height: 80vh;
      }
    }

    @media (max-width: 980px) {
      .review-left {
        grid-template-columns: 1fr;
      }

      .workspace-body {
        grid-template-columns: 1fr;
      }

      .document-rail {
        max-height: none;
      }

      .pdf-frame {
        height: 680px;
        min-height: 560px;
      }
    }

    @media (max-width: 760px) {
      .review-header__top,
      .workspace-head {
        flex-direction: column;
        align-items: flex-start;
      }

      .workspace-meta {
        justify-content: flex-start;
      }

      .review-header {
        border-radius: 24px;
      }

      .review-header__main {
        padding: 18px;
      }

      .info-grid {
        grid-template-columns: 1fr;
      }

      .pdf-frame {
        height: 62vh;
        min-height: 460px;
      }
    }
  `]
})
export class AdminApplicationReviewPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CrowdfundingService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sanitizer = inject(DomSanitizer);

  readonly loading = signal(false);
  readonly savingStatus = signal(false);
  readonly previewLoading = signal(false);

  readonly error = signal('');
  readonly success = signal('');
  readonly previewError = signal('');

  readonly application = signal<ApplicationRaiseResponse | null>(null);
  readonly selectedDocumentType = signal<DocumentType | null>(null);
  readonly previewUrl = signal<SafeResourceUrl | null>(null);

  private applicationId: number | null = null;
  private objectUrl: string | null = null;

  readonly documents = computed(() => this.application()?.documents ?? []);

  readonly selectedDocument = computed(() => {
    const current = this.selectedDocumentType();
    return this.documents().find((doc) => doc.docType === current) ?? null;
  });

  readonly reviewActions = computed<ReviewAction[]>(() => {
    const status = this.application()?.status;

    if (status === ApplicationRaiseStatus.SUBMITTED) {
      return [
        {
          status: ApplicationRaiseStatus.UNDER_REVIEW,
          label: 'Start review',
          tone: 'review'
        }
      ];
    }

    if (status === ApplicationRaiseStatus.UNDER_REVIEW) {
      return [
        {
          status: ApplicationRaiseStatus.APPROVED,
          label: 'Approve application',
          tone: 'approve'
        },
        {
          status: ApplicationRaiseStatus.REJECTED,
          label: 'Reject application',
          tone: 'reject'
        }
      ];
    }

    return [];
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const id = Number(params.get('id'));
        this.applicationId = Number.isFinite(id) ? id : null;

        if (this.applicationId) {
          this.load(this.applicationId);
        }
      });
  }

  ngOnDestroy(): void {
    this.revokeObjectUrl();
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.previewError.set('');

    this.service
      .getApplicationById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (app) => {
          this.application.set(app);

          if (app.documents?.length) {
            const current = this.selectedDocumentType();
            const stillExists = app.documents.some((d) => d.docType === current);
            this.previewDocument(stillExists ? current! : app.documents[0].docType);
          } else {
            this.selectedDocumentType.set(null);
            this.revokeObjectUrl();
            this.previewUrl.set(null);
          }
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  changeStatus(status: ApplicationRaiseStatus): void {
    if (!this.applicationId) {
      return;
    }

    this.savingStatus.set(true);
    this.error.set('');
    this.success.set('');

    this.service
      .adminPatchApplicationStatus(this.applicationId, status)
      .pipe(finalize(() => this.savingStatus.set(false)))
      .subscribe({
        next: (app) => {
          this.application.set(app);
          this.success.set(`Application moved to ${this.formatLabel(status)}.`);
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  previewDocument(type: DocumentType): void {
    if (!this.applicationId) {
      return;
    }

    this.selectedDocumentType.set(type);
    this.previewLoading.set(true);
    this.previewError.set('');
    this.revokeObjectUrl();
    this.previewUrl.set(null);

    this.service
      .fetchDocumentBlob(this.applicationId, type)
      .pipe(finalize(() => this.previewLoading.set(false)))
      .subscribe({
        next: (blob) => {
          this.objectUrl = URL.createObjectURL(blob);
          this.previewUrl.set(
            this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl)
          );
        },
        error: (err: HttpErrorResponse) => {
          this.previewError.set(this.extractError(err));
        }
      });
  }

  reviewStageTitle(status: ApplicationRaiseStatus | string | null | undefined): string {
    switch (this.normalize(status)) {
      case 'SUBMITTED':
        return 'Ready to begin review';
      case 'UNDER_REVIEW':
        return 'Decision required';
      case 'APPROVED':
        return 'Approved file';
      case 'REJECTED':
        return 'Rejected file';
      default:
        return 'Review status';
    }
  }

  reviewStageDescription(status: ApplicationRaiseStatus | string | null | undefined): string {
    switch (this.normalize(status)) {
      case 'SUBMITTED':
        return 'The application is waiting for compliance review. Start the review when you are ready to inspect the submitted documents.';
      case 'UNDER_REVIEW':
        return 'The file is under assessment. After verifying the documents and information, approve or reject the application.';
      case 'APPROVED':
        return 'This application has been approved and no further review action is required here.';
      case 'REJECTED':
        return 'This application has been rejected and is no longer in the active review workflow.';
      default:
        return 'Review the submitted information and update the application status when appropriate.';
    }
  }

  normalize(value: unknown): string {
    return String(value ?? '').trim().toUpperCase();
  }

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }

    return String(value)
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
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
    return parts.length ? parts.join(' ') : 'No contact name';
  }

  contactInitials(app: ApplicationRaiseResponse): string {
    const name = this.contactName(app);

    if (name === 'No contact name') {
      return 'NA';
    }

    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
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

  formatBytes(value: number | null | undefined): string {
    if (!value || value < 1024) {
      return `${value ?? 0} B`;
    }

    if (value < 1024 * 1024) {
      return `${(value / 1024).toFixed(1)} KB`;
    }

    return `${(value / (1024 * 1024)).toFixed(1)} MB`;
  }

  private revokeObjectUrl(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }

  private extractError(err: HttpErrorResponse): string {
    if (err.error?.errors && Array.isArray(err.error.errors)) {
      return err.error.errors.join(', ');
    }

    return (
      err.error?.message ||
      err.error?.error ||
      (typeof err.error === 'string' ? err.error : null) ||
      'Something went wrong.'
    );
  }
}