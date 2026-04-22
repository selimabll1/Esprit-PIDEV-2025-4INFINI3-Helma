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

@Component({
  selector: 'app-admin-application-review-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <div class="header">
        <div>
          <a class="back" routerLink="/admin">← Back to applications</a>
          <h1>Application review</h1>
          <p *ngIf="application()">
            {{ application()!.businessName }} · {{ application()!.type }}
          </p>
        </div>

        <div class="actions" *ngIf="application()">
          <button
            *ngFor="let status of availableTransitions()"
            type="button"
            [class.review]="status === applicationStatus.UNDER_REVIEW"
            [class.approve]="status === applicationStatus.APPROVED"
            [class.reject]="status === applicationStatus.REJECTED"
            (click)="changeStatus(status)"
            [disabled]="savingStatus()"
          >
            {{ savingStatus() ? 'Saving...' : formatEnumLabel(status) }}
          </button>
        </div>
      </div>

      <p class="success" *ngIf="success()">{{ success() }}</p>
      <p class="error" *ngIf="error()">{{ error() }}</p>
      <div class="state-card" *ngIf="loading()">Loading application...</div>

      <div class="review-layout" *ngIf="!loading() && application() as app">
        <section class="left">
          <article class="card">
            <div class="top">
              <div>
                <div class="pill type">{{ app.type }}</div>
                <h2>{{ app.businessName }}</h2>
              </div>
              <div class="pill status" [class]="app.status.toLowerCase()">
                {{ formatEnumLabel(app.status) }}
              </div>
            </div>

            <div class="details">
              <p><strong>Sector:</strong> {{ app.sector ? formatEnumLabel(app.sector) : '—' }}</p>
              <p><strong>Sub-sector:</strong> {{ app.subSector ? formatEnumLabel(app.subSector) : '—' }}</p>
              <p><strong>Tags:</strong> {{ formatTagList(app.tags) }}</p>
              <p><strong>Website:</strong> {{ app.website || '—' }}</p>
              <p><strong>Funding goal:</strong> {{ app.fundingGoal ?? '—' }} {{ app.currency }}</p>
              <p><strong>Country:</strong> {{ app.country }}</p>
              <p><strong>Company number:</strong> {{ app.companyNumber || '—' }}</p>
              <p><strong>Customer count:</strong> {{ app.customerCount ?? '—' }}</p>
              <p><strong>Contact:</strong> {{ app.contactFirstName }} {{ app.contactLastName }}</p>
              <p><strong>Email:</strong> {{ app.contactEmail }}</p>
              <p><strong>Phone:</strong> {{ app.contactPhone || '—' }}</p>
            </div>

            <div class="summary">
              <h3>Summary</h3>
              <p>{{ app.summary || '—' }}</p>
            </div>
          </article>

          <article class="card" *ngIf="app.equityDetail">
            <h3>Equity details</h3>
            <div class="details">
              <p><strong>Legal name:</strong> {{ app.equityDetail.companyLegalName }}</p>
              <p><strong>Registration no:</strong> {{ app.equityDetail.companyRegistrationNumber }}</p>
              <p><strong>CNRE URL:</strong> {{ app.equityDetail.cnreProfileUrl }}</p>
              <p><strong>Equity offered:</strong> {{ app.equityDetail.equityOfferedPercent ?? '—' }}</p>
              <p><strong>Pre-money valuation:</strong> {{ app.equityDetail.preMoneyValuation ?? '—' }}</p>
              <p><strong>Min investment:</strong> {{ app.equityDetail.minInvestment ?? '—' }}</p>
            </div>
          </article>

          <article class="card">
            <h3>Documents</h3>

            <div class="empty" *ngIf="!app.documents?.length">
              No documents uploaded.
            </div>

            <div class="doc-list" *ngIf="app.documents?.length">
              <button
                type="button"
                class="doc-item"
                *ngFor="let doc of app.documents"
                [class.active]="selectedDocumentType() === doc.docType"
                (click)="previewDocument(doc.docType)"
              >
                <div>
                  <strong>{{ formatEnumLabel(doc.docType) }}</strong>
                  <p>{{ doc.fileName }}</p>
                  <small>{{ formatBytes(doc.sizeBytes) }}</small>
                </div>
              </button>
            </div>
          </article>
        </section>

        <section class="right">
          <article class="preview-card">
            <div class="preview-header">
              <h3>Document preview</h3>
              <span *ngIf="selectedDocumentType()">
                {{ formatEnumLabel(selectedDocumentType()!) }}
              </span>
            </div>

            <div class="preview-state" *ngIf="previewLoading()">Loading document...</div>
            <div class="preview-state error-box" *ngIf="previewError()">{{ previewError() }}</div>

            <div class="preview-empty" *ngIf="!previewLoading() && !previewUrl() && !previewError()">
              Select a document to preview it inline.
            </div>

            <object
              *ngIf="previewUrl()"
              [data]="previewUrl()"
              type="application/pdf"
              class="pdf-frame"
            >
              <div class="preview-empty">
                Preview could not be embedded in this browser.
              </div>
            </object>
          </article>
        </section>
      </div>
    </section>
  `,
  styles: [`
    .page {
      display: grid;
      gap: 20px;
    }

    .header {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      align-items: flex-start;
    }

    .back {
      display: inline-block;
      margin-bottom: 8px;
      color: #0b3b3c;
      text-decoration: none;
      font-weight: 600;
    }

    .header h1 {
      margin: 0;
      color: #062a2b;
    }

    .header p {
      margin: 8px 0 0;
      color: #5c6b73;
    }

    .actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }

    .actions button {
      min-height: 42px;
      border: 0;
      border-radius: 10px;
      padding: 0 14px;
      cursor: pointer;
      color: white;
      font-weight: 700;
    }

    .actions .review {
      background: #2c5ea8;
    }

    .actions .approve {
      background: #1f7a43;
    }

    .actions .reject {
      background: #b03a2e;
    }

    .review-layout {
      display: grid;
      grid-template-columns: minmax(320px, 430px) 1fr;
      gap: 20px;
      align-items: start;
    }

    .left {
      display: grid;
      gap: 16px;
    }

    .card,
    .preview-card,
    .state-card {
      background: white;
      border-radius: 18px;
      padding: 20px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.06);
    }

    .top {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
      margin-bottom: 16px;
    }

    .card h2,
    .card h3,
    .preview-card h3 {
      margin: 0;
      color: #062a2b;
    }

    .details {
      display: grid;
      gap: 8px;
    }

    .details p,
    .summary p {
      margin: 0;
      color: #33444d;
    }

    .summary {
      margin-top: 16px;
      display: grid;
      gap: 10px;
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

    .doc-list {
      display: grid;
      gap: 10px;
      margin-top: 12px;
    }

    .doc-item {
      width: 100%;
      text-align: left;
      border: 1px solid #e6ecef;
      border-radius: 14px;
      background: #fbfcfd;
      padding: 14px;
      cursor: pointer;
    }

    .doc-item.active {
      border-color: #2a9d8f;
      box-shadow: 0 0 0 3px rgba(42,157,143,0.12);
    }

    .doc-item p,
    .doc-item small {
      margin: 4px 0 0;
      color: #5c6b73;
    }

    .preview-header {
      display: flex;
      justify-content: space-between;
      gap: 10px;
      align-items: center;
      margin-bottom: 16px;
    }

    .pdf-frame {
      width: 100%;
      height: 78vh;
      border: 1px solid #e6ecef;
      border-radius: 12px;
      background: white;
    }

    .preview-state,
    .preview-empty,
    .empty {
      padding: 16px;
      border-radius: 12px;
      background: #f6f8f9;
      color: #5c6b73;
    }

    .error,
    .error-box {
      color: #c0392b;
    }

    .success {
      color: #1f7a43;
      font-weight: 600;
      margin: 0;
    }

    @media (max-width: 1100px) {
      .review-layout {
        grid-template-columns: 1fr;
      }

      .header {
        flex-direction: column;
      }

      .pdf-frame {
        height: 65vh;
      }
    }
  `]
})
export class AdminApplicationReviewPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CrowdfundingService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sanitizer = inject(DomSanitizer);

  readonly applicationStatus = ApplicationRaiseStatus;

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

  availableTransitions(): ApplicationRaiseStatus[] {
    const current = this.application()?.status;

    if (current === ApplicationRaiseStatus.SUBMITTED) {
      return [
        ApplicationRaiseStatus.UNDER_REVIEW,
        ApplicationRaiseStatus.APPROVED,
        ApplicationRaiseStatus.REJECTED
      ];
    }

    if (current === ApplicationRaiseStatus.UNDER_REVIEW) {
      return [
        ApplicationRaiseStatus.APPROVED,
        ApplicationRaiseStatus.REJECTED
      ];
    }

    return [];
  }

  changeStatus(status: ApplicationRaiseStatus): void {
    if (!this.applicationId) return;

    this.savingStatus.set(true);
    this.error.set('');
    this.success.set('');

    this.service
      .adminPatchApplicationStatus(this.applicationId, status)
      .pipe(finalize(() => this.savingStatus.set(false)))
      .subscribe({
        next: (app) => {
          this.application.set(app);
          this.success.set(`Application moved to ${this.formatEnumLabel(status)}.`);
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  previewDocument(type: DocumentType): void {
    if (!this.applicationId) return;

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

  formatBytes(value: number | null | undefined): string {
    if (!value || value < 1024) return `${value ?? 0} B`;
    if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
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