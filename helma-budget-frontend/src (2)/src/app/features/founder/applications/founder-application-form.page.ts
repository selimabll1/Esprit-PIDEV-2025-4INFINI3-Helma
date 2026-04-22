import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule
} from '@angular/forms';
import {
  ActivatedRoute,
  Router,
  RouterLink,
  RouterLinkActive
} from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  ApplicationDocumentResponse,
  ApplicationRaiseCreateRequest,
  ApplicationRaiseResponse,
  ApplicationRaiseStatus,
  CrowdfundingType,
  DocumentType,
  EquityApplicationCreateRequest
} from '../../../core/models/crowdfunding.models';
import {
  AppTag,
  Sector,
  SubSector,
  SECTOR_OPTIONS,
  SUB_SECTOR_OPTIONS_BY_SECTOR,
  TAG_OPTIONS
} from '../../../core/models/application-taxonomy';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

const MAX_PDF_SIZE_BYTES = 10 * 1024 * 1024;

@Component({
  selector: 'app-founder-application-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, RouterLinkActive],
  template: `
    <section class="page">
      <div class="header">
        <div>
          <a class="back" routerLink="/founder">← Back to applications</a>

          <div class="title-row">
            <h1>
              {{ mode() === 'create' ? 'Create' : 'Edit' }}
              {{ currentType() === crowdfundingType.EQUITY ? 'equity' : 'donation' }}
              application
            </h1>

            <span class="status-badge" *ngIf="currentStatus()">
              {{ formatEnumLabel(currentStatus()!) }}
            </span>
          </div>

          <p>
            Save the draft, then manage documents below. Documents can only be changed while the application is in DRAFT.
          </p>
        </div>

        <div class="switcher" *ngIf="mode() === 'create'">
          <a
            routerLink="/founder/applications/new/donation"
            routerLinkActive="active"
          >
            Donation
          </a>
          <a
            routerLink="/founder/applications/new/equity"
            routerLinkActive="active"
          >
            Equity
          </a>
        </div>
      </div>

      <p class="success" *ngIf="success()">{{ success() }}</p>
      <p class="error" *ngIf="error()">{{ error() }}</p>
      <div class="state-card" *ngIf="loading()">Loading application...</div>

      <form class="form" [formGroup]="form" (ngSubmit)="save()" *ngIf="!loading()">
        <section class="card">
          <h2>Business information</h2>

          <div class="grid">
            <div class="field">
              <label>Business name *</label>
              <input type="text" formControlName="businessName" />
              <small *ngIf="showError('businessName')">
                Business name must be between 2 and 150 characters.
              </small>
            </div>

            <div class="field">
              <label>Sector *</label>
              <select formControlName="sector">
                <option value="">Select a sector</option>
                <option *ngFor="let sector of sectorOptions" [value]="sector">
                  {{ formatEnumLabel(sector) }}
                </option>
              </select>
              <small *ngIf="showError('sector')">
                Sector is required.
              </small>
            </div>

            <div class="field">
              <label>Sub-sector *</label>
              <select formControlName="subSector" [disabled]="!form.get('sector')?.value">
                <option value="">Select a sub-sector</option>
                <option *ngFor="let subSector of availableSubSectorOptions()" [value]="subSector">
                  {{ formatEnumLabel(subSector) }}
                </option>
              </select>
              <small *ngIf="showError('subSector')">
                Choose a valid sub-sector for the selected sector.
              </small>
            </div>

            <div class="field">
              <label>Website</label>
              <input type="text" formControlName="website" />
              <small *ngIf="showError('website')">
                Website must be a valid URL.
              </small>
            </div>

            <div class="field">
              <label>Funding goal (TND) *</label>
              <input type="number" step="0.001" formControlName="fundingGoal" />
              <small *ngIf="showError('fundingGoal')">
                Funding goal must be at least 500.
              </small>
            </div>

            <div class="field">
              <label>Customer count</label>
              <input type="number" formControlName="customerCount" />
              <small *ngIf="showError('customerCount')">
                Customer count cannot be negative.
              </small>
            </div>

            <div class="field" *ngIf="currentType() === crowdfundingType.EQUITY">
              <label>Company number *</label>
              <input type="text" formControlName="companyNumber" />
              <small *ngIf="showError('companyNumber')">
                Company number is required for equity.
              </small>
            </div>
          </div>

          <div class="field">
            <label>Tags *</label>
            <div class="tag-grid">
              <label class="tag-option" *ngFor="let tag of tagOptions">
                <input
                  type="checkbox"
                  [checked]="isTagSelected(tag)"
                  (change)="toggleTag(tag, $any($event.target).checked)"
                  [disabled]="!isEditable()"
                />
                <span>{{ formatEnumLabel(tag) }}</span>
              </label>
            </div>
            <small *ngIf="showError('tags')">
              Select at least one tag.
            </small>
          </div>

          <div class="field">
            <label>Summary *</label>
            <textarea rows="5" formControlName="summary"></textarea>
            <small *ngIf="showError('summary')">
              Summary must be between 10 and 255 characters.
            </small>
          </div>
        </section>

        <section class="card">
          <h2>Contact information</h2>

          <div class="grid">
            <div class="field">
              <label>First name *</label>
              <input type="text" formControlName="contactFirstName" />
              <small *ngIf="showError('contactFirstName')">
                First name must be between 2 and 80 characters.
              </small>
            </div>

            <div class="field">
              <label>Last name *</label>
              <input type="text" formControlName="contactLastName" />
              <small *ngIf="showError('contactLastName')">
                Last name must be between 2 and 80 characters.
              </small>
            </div>

            <div class="field">
              <label>Title</label>
              <input type="text" formControlName="contactTitle" />
              <small *ngIf="showError('contactTitle')">
                Title must be at most 80 characters.
              </small>
            </div>

            <div class="field">
              <label>Email *</label>
              <input type="email" formControlName="contactEmail" />
              <small *ngIf="showError('contactEmail')">
                Please enter a valid email.
              </small>
            </div>

            <div class="field">
              <label>Tunisian phone</label>
              <input type="text" formControlName="contactPhone" />
              <small *ngIf="showError('contactPhone')">
                Use 8 digits, or prefix with +216 / 216.
              </small>
            </div>
          </div>
        </section>

        <section
          class="card"
          *ngIf="currentType() === crowdfundingType.EQUITY"
          formGroupName="equityDetail"
        >
          <h2>Equity details</h2>

          <div class="grid">
            <div class="field">
              <label>Company legal name *</label>
              <input type="text" formControlName="companyLegalName" />
              <small *ngIf="showNestedError('equityDetail', 'companyLegalName')">
                Company legal name is required.
              </small>
            </div>

            <div class="field">
              <label>Company registration number *</label>
              <input type="text" formControlName="companyRegistrationNumber" />
              <small *ngIf="showNestedError('equityDetail', 'companyRegistrationNumber')">
                Company registration number is required.
              </small>
            </div>

            <div class="field">
              <label>CNRE profile URL *</label>
              <input type="text" formControlName="cnreProfileUrl" />
              <small *ngIf="showNestedError('equityDetail', 'cnreProfileUrl')">
                CNRE URL must start with http:// or https://
              </small>
            </div>

            <div class="field">
              <label>Equity offered (%)</label>
              <input type="number" step="0.01" formControlName="equityOfferedPercent" />
              <small *ngIf="showNestedError('equityDetail', 'equityOfferedPercent')">
                Must be between 0.01 and 100
              </small>
            </div>

            <div class="field">
              <label>Pre-money valuation</label>
              <input type="number" step="0.001" formControlName="preMoneyValuation" />
            </div>

            <div class="field">
              <label>Minimum investment</label>
              <input type="number" step="0.001" formControlName="minInvestment" />
            </div>
          </div>
        </section>

        <section class="card">
          <h2>Terms</h2>

          <label class="checkbox">
            <input type="checkbox" formControlName="acceptedTerms" />
            <span>I accept the platform terms *</span>
          </label>

          <small *ngIf="showError('acceptedTerms')">
            Accepted terms are required.
          </small>
        </section>

        <section class="card">
          <h2>Documents</h2>

          <ng-container *ngIf="hasSavedDraft(); else saveFirstBlock">
            <p class="hint">
              Upload PDF only, max 10 MB. Uploading the same document type again replaces the old one.
            </p>

            <div class="doc-upload">
              <div class="field">
                <label>Document type</label>
                <select
                  [value]="selectedDocType() ?? ''"
                  (change)="onDocTypeChange($any($event.target).value)"
                >
                  <option value="">Select document type</option>
                  <option *ngFor="let type of allowedDocumentTypes()" [value]="type">
                    {{ formatEnumLabel(type) }}
                  </option>
                </select>
              </div>

              <div class="field">
                <label>PDF file</label>
                <input
                  type="file"
                  accept="application/pdf,.pdf"
                  (change)="onFileSelected($event)"
                />
              </div>

              <div class="upload-actions">
                <button
                  type="button"
                  (click)="uploadDocument()"
                  [disabled]="uploadingDocument() || !canUploadDocument()"
                >
                  {{ uploadingDocument() ? 'Uploading...' : 'Upload document' }}
                </button>
              </div>
            </div>

            <p class="success" *ngIf="documentSuccess()">{{ documentSuccess() }}</p>
            <p class="error" *ngIf="documentError()">{{ documentError() }}</p>

            <div class="doc-checklist" *ngIf="currentType() === crowdfundingType.EQUITY">
              <h3>Equity required before submit</h3>

              <div class="check-grid">
                <div
                  class="check-item"
                  *ngFor="let type of equityRequiredDocumentTypes"
                  [class.done]="hasDocumentType(type)"
                >
                  <span>{{ hasDocumentType(type) ? '✓' : '•' }}</span>
                  <span>{{ formatEnumLabel(type) }}</span>
                </div>
              </div>

              <p class="hint" *ngIf="missingRequiredEquityDocLabels().length">
                Missing:
                {{ missingRequiredEquityDocLabels().join(', ') }}
              </p>
            </div>

            <div class="doc-list" *ngIf="documents().length; else noDocsBlock">
              <article class="doc-card" *ngFor="let doc of documents()">
                <div>
                  <strong>{{ formatEnumLabel(doc.docType) }}</strong>
                  <p>{{ doc.fileName }}</p>
                  <small>
                    {{ formatBytes(doc.sizeBytes) }} •
                    {{ doc.createdAt | date:'medium' }}
                  </small>
                </div>

                <button
                  type="button"
                  class="danger"
                  (click)="deleteDocument(doc.docType)"
                  [disabled]="uploadingDocument() || !isEditable()"
                >
                  Delete
                </button>
              </article>
            </div>

            <ng-template #noDocsBlock>
              <div class="empty-box">
                No documents uploaded yet.
              </div>
            </ng-template>
          </ng-container>

          <ng-template #saveFirstBlock>
            <div class="empty-box">
              Save this application first, then upload documents here.
            </div>
          </ng-template>
        </section>

        <div class="footer-actions">
          <a routerLink="/founder">Back</a>
          <button type="submit" [disabled]="saving() || !isEditable()">
            {{ saving() ? 'Saving...' : mode() === 'create' ? 'Save draft' : 'Update draft' }}
          </button>
        </div>
      </form>
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

    .title-row {
      display: flex;
      gap: 12px;
      align-items: center;
      flex-wrap: wrap;
    }

    .header h1 {
      margin: 0;
      color: #062a2b;
    }

    .header p {
      margin: 8px 0 0;
      color: #5c6b73;
      max-width: 760px;
    }

    .status-badge {
      display: inline-flex;
      align-items: center;
      min-height: 30px;
      padding: 0 10px;
      border-radius: 999px;
      background: #eef3f5;
      color: #455a64;
      font-weight: 700;
      font-size: 0.8rem;
    }

    .switcher {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .switcher a,
    .footer-actions a,
    .footer-actions button,
    .upload-actions button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 42px;
      padding: 0 14px;
      border-radius: 10px;
      text-decoration: none;
      border: 0;
      cursor: pointer;
      background: #eef3f5;
      color: #062a2b;
      font-weight: 700;
    }

    .switcher a.active {
      background: #2a9d8f;
      color: #062a2b;
    }

    .footer-actions button,
    .upload-actions button {
      background: #062a2b;
      color: white;
    }

    .form {
      display: grid;
      gap: 18px;
    }

    .card,
    .state-card {
      background: white;
      border-radius: 18px;
      padding: 22px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.06);
    }

    .card h2,
    .doc-checklist h3 {
      margin: 0 0 16px;
      color: #062a2b;
      font-size: 1.1rem;
    }

    .doc-checklist h3 {
      font-size: 1rem;
      margin-top: 8px;
    }

    .grid {
      display: grid;
      gap: 16px;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    }

    .field {
      display: grid;
      gap: 8px;
    }

    label {
      font-weight: 600;
      color: #062a2b;
    }

    input,
    textarea,
    select {
      width: 100%;
      border: 1px solid #d8dfe3;
      border-radius: 10px;
      padding: 12px;
      font: inherit;
      background: #fff;
    }

    textarea {
      resize: vertical;
    }

    input:focus,
    textarea:focus,
    select:focus {
      outline: none;
      border-color: #2a9d8f;
      box-shadow: 0 0 0 3px rgba(42,157,143,0.12);
    }

    .checkbox {
      display: flex;
      gap: 10px;
      align-items: center;
    }

    .checkbox input {
      width: auto;
    }

    .tag-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 10px;
    }

    .tag-option {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px;
      border: 1px solid #d8dfe3;
      border-radius: 10px;
      background: #f8fbfb;
      cursor: pointer;
    }

    .tag-option input {
      width: auto;
      margin: 0;
    }

    .doc-upload {
      display: grid;
      gap: 14px;
      grid-template-columns: 1.2fr 1fr auto;
      align-items: end;
      margin-bottom: 18px;
    }

    .doc-list {
      display: grid;
      gap: 12px;
      margin-top: 16px;
    }

    .doc-card {
      display: flex;
      justify-content: space-between;
      gap: 14px;
      align-items: center;
      padding: 14px 16px;
      border: 1px solid #e6ecef;
      border-radius: 14px;
      background: #fbfcfd;
    }

    .doc-card p,
    .doc-card small {
      margin: 4px 0 0;
      color: #5c6b73;
    }

    .danger {
      background: #c0392b !important;
      color: white !important;
    }

    .empty-box {
      padding: 16px;
      border-radius: 12px;
      background: #f6f8f9;
      color: #5c6b73;
    }

    .check-grid {
      display: grid;
      gap: 10px;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    }

    .check-item {
      display: flex;
      gap: 10px;
      align-items: center;
      padding: 12px;
      border-radius: 12px;
      background: #fff5d7;
      color: #8a6d1d;
      font-weight: 600;
    }

    .check-item.done {
      background: #e8f7ef;
      color: #1f7a43;
    }

    .hint {
      margin: 0;
      color: #5c6b73;
    }

    .footer-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      flex-wrap: wrap;
    }

    .success {
      color: #1f7a43;
      margin: 0;
      font-weight: 600;
    }

    .error,
    small {
      color: #c0392b;
      margin: 0;
    }

    @media (max-width: 960px) {
      .header {
        flex-direction: column;
      }

      .doc-upload {
        grid-template-columns: 1fr;
      }

      .doc-card {
        flex-direction: column;
        align-items: flex-start;
      }
    }
  `]
})
export class FounderApplicationFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(CrowdfundingService);
  private readonly destroyRef = inject(DestroyRef);

  readonly crowdfundingType = CrowdfundingType;
  readonly applicationStatus = ApplicationRaiseStatus;

  readonly sectorOptions = SECTOR_OPTIONS;
  readonly tagOptions = TAG_OPTIONS;

  readonly donationDocumentTypes: DocumentType[] = [
    DocumentType.ID_CARD,
    DocumentType.PROJECT_PITCH_DECK
  ];

  readonly equityDocumentTypes: DocumentType[] = [
    DocumentType.ID_CARD,
    DocumentType.CNRE_EXTRACT,
    DocumentType.SHAREHOLDERS_CAP_TABLE,
    DocumentType.FINANCIAL_STATEMENTS,
    DocumentType.BANK_RIB
  ];

  readonly equityRequiredDocumentTypes: DocumentType[] = [
    DocumentType.CNRE_EXTRACT,
    DocumentType.SHAREHOLDERS_CAP_TABLE,
    DocumentType.FINANCIAL_STATEMENTS,
    DocumentType.BANK_RIB
  ];

  readonly mode = signal<'create' | 'edit'>('create');
  readonly currentType = signal<CrowdfundingType>(CrowdfundingType.DONATION);
  readonly currentStatus = signal<ApplicationRaiseStatus | null>(null);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly uploadingDocument = signal(false);
  readonly selectedSector = signal<Sector | null>(null);

  readonly error = signal('');
  readonly success = signal('');
  readonly documentError = signal('');
  readonly documentSuccess = signal('');

  readonly documents = signal<ApplicationDocumentResponse[]>([]);
  readonly selectedDocType = signal<DocumentType | null>(null);
  readonly selectedFile = signal<File | null>(null);

  private applicationId: number | null = null;

  readonly hasSavedDraft = computed(
    () => this.mode() === 'edit' && this.applicationId !== null
  );

  readonly allowedDocumentTypes = computed(() =>
    this.currentType() === CrowdfundingType.EQUITY
      ? this.equityDocumentTypes
      : this.donationDocumentTypes
  );

  readonly missingRequiredEquityDocs = computed(() => {
    if (this.currentType() !== CrowdfundingType.EQUITY) {
      return [];
    }

    const present = new Set(this.documents().map((doc) => doc.docType));
    return this.equityRequiredDocumentTypes.filter((type) => !present.has(type));
  });

  readonly missingRequiredEquityDocLabels = computed(() =>
    this.missingRequiredEquityDocs().map((type) => this.formatEnumLabel(type))
  );

  readonly availableSubSectorOptions = computed(() => {
    const sector = this.selectedSector();
    if (!sector) return [] as readonly SubSector[];
    return SUB_SECTOR_OPTIONS_BY_SECTOR[sector] ?? [];
  });

  readonly form = this.fb.group({
    businessName: [''],
    companyNumber: [''],
    website: [''],
    sector: [''],
    subSector: [''],
    tags: new FormControl<AppTag[]>([], {
      nonNullable: true
    }),
    summary: [''],
    fundingGoal: [null as number | null],
    customerCount: [null as number | null],
    contactFirstName: [''],
    contactLastName: [''],
    contactTitle: [''],
    contactEmail: [''],
    contactPhone: [''],
    acceptedTerms: [false],
    equityDetail: this.fb.group({
      companyLegalName: [''],
      companyRegistrationNumber: [''],
      cnreProfileUrl: [''],
      equityOfferedPercent: [null as number | null],
      preMoneyValuation: [null as number | null],
      minInvestment: [null as number | null]
    })
  });

  ngOnInit(): void {
    this.form
      .get('sector')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((sector) => this.onSectorChanged(sector as Sector | '' | null));

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const idParam = params.get('id');
        const typeParam = params.get('type');

        this.error.set('');
        this.success.set('');
        this.documentError.set('');
        this.documentSuccess.set('');

        if (idParam) {
          this.mode.set('edit');
          this.applicationId = Number(idParam);
          this.loadForEdit(this.applicationId);
          return;
        }

        this.mode.set('create');
        this.applicationId = null;
        this.currentStatus.set(ApplicationRaiseStatus.DRAFT);
        this.currentType.set(
          typeParam === 'equity'
            ? CrowdfundingType.EQUITY
            : CrowdfundingType.DONATION
        );

        this.documents.set([]);
        this.selectedDocType.set(null);
        this.selectedFile.set(null);
        this.resetForm();
        this.applyTypeValidators();
      });
  }

  save(): void {
    if (!this.isEditable()) {
      this.error.set('Only DRAFT applications can be edited.');
      return;
    }

    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    const commonPayload = this.buildCommonPayload();

    const request$ =
      this.currentType() === CrowdfundingType.EQUITY
        ? this.buildEquityRequest(commonPayload)
        : this.buildDonationRequest(commonPayload);

    request$
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (saved) => {
          const wasCreate = this.mode() === 'create';

          this.applicationId = saved.id;
          this.mode.set('edit');
          this.patchFromResponse(saved);

          this.success.set(
            wasCreate
              ? 'Draft created successfully. You can now upload documents.'
              : 'Draft updated successfully.'
          );

          if (wasCreate) {
            void this.router.navigate(['/founder/applications', saved.id, 'edit']);
          }
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  uploadDocument(): void {
    if (!this.applicationId) {
      this.documentError.set('Save the draft first.');
      return;
    }

    if (!this.isEditable()) {
      this.documentError.set('Documents can only be changed while the application is DRAFT.');
      return;
    }

    const type = this.selectedDocType();
    const file = this.selectedFile();

    if (!type || !file) {
      this.documentError.set('Please select a document type and a file.');
      return;
    }

    if (!this.allowedDocumentTypes().includes(type)) {
      this.documentError.set('That document type is not allowed for this application type.');
      return;
    }

    if (!this.isPdf(file)) {
      this.documentError.set('Only PDF files are allowed.');
      return;
    }

    if (file.size > MAX_PDF_SIZE_BYTES) {
      this.documentError.set('File too large. Max is 10 MB.');
      return;
    }

    this.uploadingDocument.set(true);
    this.documentError.set('');
    this.documentSuccess.set('');

    this.service
      .uploadDocument(this.applicationId, type, file)
      .pipe(finalize(() => this.uploadingDocument.set(false)))
      .subscribe({
        next: () => {
          this.documentSuccess.set(`${this.formatEnumLabel(type)} uploaded successfully.`);
          this.selectedFile.set(null);
          this.refreshDocuments();
        },
        error: (err: HttpErrorResponse) => {
          this.documentError.set(this.extractError(err));
        }
      });
  }

  deleteDocument(type: DocumentType): void {
    if (!this.applicationId) {
      return;
    }

    if (!this.isEditable()) {
      this.documentError.set('Documents can only be changed while the application is DRAFT.');
      return;
    }

    if (!window.confirm(`Delete ${this.formatEnumLabel(type)}?`)) {
      return;
    }

    this.uploadingDocument.set(true);
    this.documentError.set('');
    this.documentSuccess.set('');

    this.service
      .deleteDocument(this.applicationId, type)
      .pipe(finalize(() => this.uploadingDocument.set(false)))
      .subscribe({
        next: () => {
          this.documentSuccess.set(`${this.formatEnumLabel(type)} deleted.`);
          this.refreshDocuments();
        },
        error: (err: HttpErrorResponse) => {
          this.documentError.set(this.extractError(err));
        }
      });
  }

  onDocTypeChange(value: string): void {
    this.selectedDocType.set(value ? (value as DocumentType) : null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    this.selectedFile.set(file);
  }

  canUploadDocument(): boolean {
    return !!this.selectedDocType() && !!this.selectedFile() && this.isEditable();
  }

  hasDocumentType(type: DocumentType): boolean {
    return this.documents().some((doc) => doc.docType === type);
  }

  isEditable(): boolean {
    return this.currentStatus() === null || this.currentStatus() === ApplicationRaiseStatus.DRAFT;
  }

  showError(_controlName: string): boolean {
    return false;
  }

  showNestedError(_groupName: string, _controlName: string): boolean {
    return false;
  }

  isTagSelected(tag: AppTag): boolean {
    const selected = (this.form.get('tags')?.value as AppTag[] | null) ?? [];
    return selected.includes(tag);
  }

  toggleTag(tag: AppTag, checked: boolean): void {
    const control = this.form.get('tags');
    if (!control) return;

    const current = new Set(((control.value as AppTag[] | null) ?? []).filter(Boolean));
    if (checked) {
      current.add(tag);
    } else {
      current.delete(tag);
    }

    control.setValue(Array.from(current));
    control.markAsDirty();
    control.markAsTouched();
    control.updateValueAndValidity();
  }

  formatEnumLabel(value: string): string {
    return value
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  formatBytes(value: number | null | undefined): string {
    if (!value || value < 1024) return `${value ?? 0} B`;
    if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
    return `${(value / (1024 * 1024)).toFixed(1)} MB`;
  }

  private loadForEdit(id: number): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    this.service
      .getApplicationById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (app) => this.patchFromResponse(app),
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractError(err));
        }
      });
  }

  private onSectorChanged(sector: Sector | '' | null): void {
    this.selectedSector.set(sector || null);

    const subSectorControl = this.form.get('subSector');
    if (!subSectorControl) return;

    const allowed = sector ? (SUB_SECTOR_OPTIONS_BY_SECTOR[sector] ?? []) : [];
    if (!allowed.includes(subSectorControl.value as SubSector)) {
      subSectorControl.setValue('');
    }
  }

  private patchFromResponse(app: ApplicationRaiseResponse): void {
    this.currentType.set(app.type);
    this.currentStatus.set(app.status);
    this.documents.set([...(app.documents ?? [])]);
    this.selectedDocType.set(this.allowedDocumentTypes()[0] ?? null);

    this.resetForm();

    this.form.patchValue({
      businessName: app.businessName ?? '',
      companyNumber: app.companyNumber ?? '',
      website: app.website ?? '',
      sector: app.sector ?? '',
      subSector: app.subSector ?? '',
      tags: [...(app.tags ?? [])],
      summary: app.summary ?? '',
      fundingGoal: app.fundingGoal ?? null,
      customerCount: app.customerCount ?? null,
      contactFirstName: app.contactFirstName ?? '',
      contactLastName: app.contactLastName ?? '',
      contactTitle: app.contactTitle ?? '',
      contactEmail: app.contactEmail ?? '',
      contactPhone: app.contactPhone ?? '',
      acceptedTerms: !!app.acceptedTerms,
      equityDetail: {
        companyLegalName: app.equityDetail?.companyLegalName ?? '',
        companyRegistrationNumber: app.equityDetail?.companyRegistrationNumber ?? '',
        cnreProfileUrl: app.equityDetail?.cnreProfileUrl ?? '',
        equityOfferedPercent: app.equityDetail?.equityOfferedPercent ?? null,
        preMoneyValuation: app.equityDetail?.preMoneyValuation ?? null,
        minInvestment: app.equityDetail?.minInvestment ?? null
      }
    });

    this.selectedSector.set(app.sector ?? null);
    this.applyTypeValidators();
  }

  private refreshDocuments(): void {
    if (!this.applicationId) {
      return;
    }

    this.service.listDocuments(this.applicationId).subscribe({
      next: (docs) => this.documents.set(docs),
      error: (err: HttpErrorResponse) => {
        this.documentError.set(this.extractError(err));
      }
    });
  }

  private buildCommonPayload(): ApplicationRaiseCreateRequest {
    const raw = this.form.getRawValue();

    return {
      type: this.currentType(),
      businessName: this.normalizeRequired(raw.businessName),
      companyNumber:
        this.currentType() === CrowdfundingType.EQUITY
          ? this.normalizeRequired(raw.companyNumber)
          : null,
      website: this.normalizeOptional(raw.website),
      sector: this.normalizeRequired(raw.sector) as Sector,
      subSector: this.normalizeRequired(raw.subSector) as SubSector,
      tags: [...new Set(((raw.tags ?? []) as AppTag[]).filter(Boolean))],
      summary: this.normalizeRequired(raw.summary),
      fundingGoal: this.toNumber(raw.fundingGoal),
      customerCount: this.toNumber(raw.customerCount),
      contactFirstName: this.normalizeRequired(raw.contactFirstName),
      contactLastName: this.normalizeRequired(raw.contactLastName),
      contactTitle: this.normalizeOptional(raw.contactTitle),
      contactEmail: this.normalizeRequired(raw.contactEmail),
      contactPhone: this.normalizeOptional(raw.contactPhone),
      acceptedTerms: !!raw.acceptedTerms
    };
  }

  private buildDonationRequest(commonPayload: ApplicationRaiseCreateRequest) {
    const payload: ApplicationRaiseCreateRequest = {
      ...commonPayload,
      type: CrowdfundingType.DONATION,
      companyNumber: null
    };

    if (this.mode() === 'edit' && this.applicationId !== null) {
      return this.service.updateDonationDraft(this.applicationId, payload);
    }

    return this.service.createDonationDraft(payload);
  }

  private buildEquityRequest(commonPayload: ApplicationRaiseCreateRequest) {
    const raw = this.form.getRawValue();

    const payload: EquityApplicationCreateRequest = {
      application: {
        ...commonPayload,
        type: CrowdfundingType.EQUITY,
        companyNumber: this.normalizeRequired(raw.companyNumber)
      },
      equityDetail: {
        companyLegalName: this.normalizeRequired(raw.equityDetail?.companyLegalName),
        companyRegistrationNumber: this.normalizeRequired(
          raw.equityDetail?.companyRegistrationNumber
        ),
        cnreProfileUrl: this.normalizeRequired(raw.equityDetail?.cnreProfileUrl),
        equityOfferedPercent: this.toNumber(raw.equityDetail?.equityOfferedPercent),
        preMoneyValuation: this.toNumber(raw.equityDetail?.preMoneyValuation),
        minInvestment: this.toNumber(raw.equityDetail?.minInvestment)
      }
    };

    if (this.mode() === 'edit' && this.applicationId !== null) {
      return this.service.updateEquityDraft(this.applicationId, payload);
    }

    return this.service.createEquityDraft(payload);
  }

  private applyTypeValidators(): void {
    return;
  }

  private resetForm(): void {
    this.selectedSector.set(null);

    this.form.reset({
      businessName: '',
      companyNumber: '',
      website: '',
      sector: '',
      subSector: '',
      tags: [],
      summary: '',
      fundingGoal: null,
      customerCount: null,
      contactFirstName: '',
      contactLastName: '',
      contactTitle: '',
      contactEmail: '',
      contactPhone: '',
      acceptedTerms: false,
      equityDetail: {
        companyLegalName: '',
        companyRegistrationNumber: '',
        cnreProfileUrl: '',
        equityOfferedPercent: null,
        preMoneyValuation: null,
        minInvestment: null
      }
    });
  }

  private normalizeRequired(value: unknown): string {
    return String(value ?? '').trim();
  }

  private normalizeOptional(value: unknown): string | null {
    const normalized = String(value ?? '').trim();
    return normalized ? normalized : null;
  }

  private toNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private isPdf(file: File): boolean {
    const type = file.type?.toLowerCase() ?? '';
    const name = file.name?.toLowerCase() ?? '';
    return type === 'application/pdf' || name.endsWith('.pdf');
  }

  private extractError(err: HttpErrorResponse): string {
    if (err.error?.fieldErrors && typeof err.error.fieldErrors === 'object') {
      const messages = Object.values(err.error.fieldErrors).filter(Boolean);
      if (messages.length) {
        return messages.join(', ');
      }
    }

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