import { CommonModule, Location } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  AppTag,
  SECTOR_OPTIONS,
  SUB_SECTOR_OPTIONS_BY_SECTOR,
  Sector,
  SubSector,
  TAG_OPTIONS,
} from '../../../core/models/application-taxonomy';
import {
  ApplicationDocumentResponse,
  ApplicationRaiseDetailsStepRequest,
  ApplicationRaiseDraftStep,
  ApplicationRaiseResponse,
  CrowdfundingType,
  DocumentType,
  ProjectStage,
  EquityDetailUpsertRequest,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';
import { SessionService } from '../../../core/services/session.service';
import { UserProfileService } from '../../../core/services/user-profile.service';

type WizardStepId = 1 | 2 | 3 | 4;

type WizardStep = {
  id: WizardStepId;
  label: string;
  caption: string;
  description: string;
};

type DocumentRequirement = {
  type: DocumentType;
  label: string;
  description: string;
  required: boolean;
  acceptedFormats?: string;
};

type RneShortDetailsResponse = {
  typeRegistre?: string | null;
  categorieRegistre?: string | null;
  idUnique?: string | null;
  nomCommercialAr?: string | null;
  nomCommercialFr?: string | null;
  denominationLatin?: string | null;
  denomination?: string | null;
  rueAr?: string | null;
  rueFr?: string | null;
  codePostal?: string | null;
  villeAr?: string | null;
  villeFr?: string | null;
  objetActivitePrincipaleAr?: string | null;
  objetActivitePrincipaleFr?: string | null;
  status?: string | null;
};

type ReverseGeocodeResponse = {
  address?: {
    city?: string | null;
    town?: string | null;
    village?: string | null;
    municipality?: string | null;
    county?: string | null;
    state?: string | null;
    state_district?: string | null;
    region?: string | null;
  } | null;
};

type TunisiaLocationOption = {
  governorate: string;
  cities: string[];
};

const TUNISIA_LOCATION_OPTIONS: readonly TunisiaLocationOption[] = [
  {
    governorate: 'Tunis',
    cities: [
      'Tunis',
      'Carthage',
      'La Marsa',
      'Le Bardo',
      'El Menzah',
      'El Omrane',
      'Sidi El Bechir',
      'La Goulette',
    ],
  },
  {
    governorate: 'Ariana',
    cities: [
      'Ariana',
      'Raoued',
      'Sidi Thabet',
      'La Soukra',
      'Ettadhamen',
      'Kalaat El Andalous',
    ],
  },
  {
    governorate: 'Ben Arous',
    cities: [
      'Ben Arous',
      'Megrine',
      'Radès',
      'Ezzahra',
      'Hammam Lif',
      'Mornag',
      'Bou Mhel El Bassatine',
    ],
  },
  {
    governorate: 'Manouba',
    cities: [
      'Manouba',
      'Den Den',
      'Douar Hicher',
      'Oued Ellil',
      'Mornaguia',
      'Tebourba',
      'Borj El Amri',
    ],
  },
  {
    governorate: 'Nabeul',
    cities: [
      'Nabeul',
      'Hammamet',
      'Dar Chaabane',
      'Béni Khiar',
      'Korba',
      'Menzel Temime',
      'Kelibia',
      'Soliman',
      'Grombalia',
    ],
  },
  {
    governorate: 'Zaghouan',
    cities: ['Zaghouan', 'Zriba', 'El Fahs', 'Bir Mcherga', 'Nadhour'],
  },
  {
    governorate: 'Bizerte',
    cities: [
      'Bizerte',
      'Menzel Bourguiba',
      'Mateur',
      'Ras Jebel',
      'Sejnane',
      'Tinja',
      'Ghar El Melh',
    ],
  },
  {
    governorate: 'Béja',
    cities: [
      'Béja',
      'Medjez El Bab',
      'Téboursouk',
      'Testour',
      'Nefza',
      'Amdoun',
    ],
  },
  {
    governorate: 'Jendouba',
    cities: [
      'Jendouba',
      'Tabarka',
      'Aïn Draham',
      'Bou Salem',
      'Fernana',
      'Ghardimaou',
    ],
  },
  {
    governorate: 'Le Kef',
    cities: [
      'Le Kef',
      'Dahmani',
      'Tajerouine',
      'Sers',
      'Sakiet Sidi Youssef',
      'Nebeur',
    ],
  },
  {
    governorate: 'Siliana',
    cities: ['Siliana', 'Bou Arada', 'Gaâfour', 'Makthar', 'Rouhia', 'Kesra'],
  },
  {
    governorate: 'Sousse',
    cities: [
      'Sousse',
      'Hammam Sousse',
      'Akouda',
      'Kalâa Kebira',
      'Kalâa Seghira',
      'Msaken',
      'Enfidha',
    ],
  },
  {
    governorate: 'Monastir',
    cities: [
      'Monastir',
      'Moknine',
      'Jemmal',
      'Ksar Hellal',
      'Téboulba',
      'Sayada',
      'Sahline',
      'Bekalta',
    ],
  },
  {
    governorate: 'Mahdia',
    cities: [
      'Mahdia',
      'Ksour Essef',
      'Chebba',
      'El Jem',
      'Bou Merdes',
      'Melloulèche',
      'Rejiche',
    ],
  },
  {
    governorate: 'Sfax',
    cities: [
      'Sfax',
      'Sakiet Ezzit',
      'Sakiet Eddaier',
      'Chihia',
      'Gremda',
      'El Ain',
      'Agareb',
      'Menzel Chaker',
      'Kerkennah',
    ],
  },
  {
    governorate: 'Kairouan',
    cities: [
      'Kairouan',
      'Haffouz',
      'Oueslatia',
      'Sbikha',
      'Bou Hajla',
      'Nasrallah',
    ],
  },
  {
    governorate: 'Kasserine',
    cities: ['Kasserine', 'Sbeitla', 'Fériana', 'Thala', 'Foussana', 'Haidra'],
  },
  {
    governorate: 'Sidi Bouzid',
    cities: [
      'Sidi Bouzid',
      'Meknassy',
      'Regueb',
      'Jilma',
      'Menzel Bouzaiane',
      'Bir El Hafey',
    ],
  },
  {
    governorate: 'Gabès',
    cities: ['Gabès', 'Métouia', 'El Hamma', 'Mareth', 'Matmata', 'Ghannouch'],
  },
  {
    governorate: 'Médenine',
    cities: [
      'Médenine',
      'Djerba Midoun',
      'Djerba Houmt Souk',
      'Zarzis',
      'Ben Gardane',
      'Beni Khedache',
    ],
  },
  {
    governorate: 'Tataouine',
    cities: ['Tataouine', 'Ghomrassen', 'Remada', 'Bir Lahmar', 'Dhiba'],
  },
  {
    governorate: 'Gafsa',
    cities: ['Gafsa', 'Métlaoui', 'Redeyef', 'Moularès', 'El Ksar', 'Mdhilla'],
  },
  {
    governorate: 'Tozeur',
    cities: ['Tozeur', 'Nefta', 'Degache', 'Tamerza', 'Hazoua'],
  },
  {
    governorate: 'Kébili',
    cities: ['Kébili', 'Douz', 'Souk Lahad', 'Faouar'],
  },
];

const DONATION_DOCUMENTS: readonly DocumentRequirement[] = [
  {
    type: DocumentType.ID_CARD,
    label: 'Identity document',
    description: 'Proof of identity for the application owner.',
    required: true,
    acceptedFormats: 'PDF, JPG, PNG',
  },
  {
    type: DocumentType.PROJECT_PITCH_DECK,
    label: 'Project pitch deck',
    description:
      'A short presentation of the project, vision, and funding use.',
    required: true,
    acceptedFormats: 'PDF preferred',
  },
  {
    type: DocumentType.BANK_RIB,
    label: 'Bank RIB',
    description:
      'Optional bank account details that can help payout verification later.',
    required: false,
    acceptedFormats: 'PDF, JPG, PNG',
  },
  {
    type: DocumentType.FINANCIAL_STATEMENTS,
    label: 'Supporting financial proof',
    description:
      'Optional supporting material that can strengthen your application.',
    required: false,
    acceptedFormats: 'PDF preferred',
  },
];

const EQUITY_DOCUMENTS: readonly DocumentRequirement[] = [
  {
    type: DocumentType.ID_CARD,
    label: 'Identity document',
    description: 'Proof of identity for the application owner.',
    required: true,
    acceptedFormats: 'PDF, JPG, PNG',
  },
  {
    type: DocumentType.PROJECT_PITCH_DECK,
    label: 'Project pitch deck',
    description: 'A structured investor-facing presentation of the company.',
    required: true,
    acceptedFormats: 'PDF preferred',
  },
  {
    type: DocumentType.CNRE_EXTRACT,
    label: 'CNRE extract',
    description: 'Official company registry extract.',
    required: true,
    acceptedFormats: 'PDF preferred',
  },
  {
    type: DocumentType.SHAREHOLDERS_CAP_TABLE,
    label: 'Shareholders cap table',
    description: 'Current ownership structure of the company.',
    required: true,
    acceptedFormats: 'PDF, XLSX, image',
  },
  {
    type: DocumentType.FINANCIAL_STATEMENTS,
    label: 'Financial statements',
    description: 'Financial performance material used for investor review.',
    required: true,
    acceptedFormats: 'PDF preferred',
  },
  {
    type: DocumentType.BANK_RIB,
    label: 'Bank RIB',
    description: 'Bank account details for verification and disbursement.',
    required: true,
    acceptedFormats: 'PDF, JPG, PNG',
  },
];

@Component({
  selector: 'app-youth-application-raise-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page">
      <header class="page-header card animate-in">
        <div class="page-header__top">
          <div class="page-header__copy">
            <span class="eyebrow">Raise Wizard</span>
            <h1>
              {{
                isEditMode()
                  ? 'Continue your application'
                  : 'Create a new application'
              }}
            </h1>
            <p>
              Move step by step, save automatically, and submit only when your
              application is ready.
            </p>
          </div>

          <div class="progress-circle">
            <div
              class="progress-circle__ring"
              [style.--progress-value]="progressPercent()"
            >
              <div class="progress-circle__inner">
                <strong>{{ progressPercent() }}%</strong>
                <span
                  >{{ completedCompletionItems() }}/{{
                    totalCompletionItems()
                  }}
                  done</span
                >
              </div>
            </div>
          </div>
        </div>

        <section class="step-overview">
          <div class="step-overview__meta">
            <div>
              <small>Current step</small>
              <strong
                >Step {{ currentStep() }} of 4 —
                {{ currentStepData().label }}</strong
              >
            </div>
            <span class="step-overview__pill">{{
              currentStepData().caption
            }}</span>
          </div>

          <div class="gold-steps" aria-label="Application progress">
            <button
              *ngFor="let step of steps"
              type="button"
              class="gold-step"
              [class.gold-step--active]="currentStep() === step.id"
              [class.gold-step--done]="furthestStep() > step.id"
              [disabled]="step.id > furthestStep()"
              (click)="goToStep(step.id)"
            >
              <span class="gold-step__number">{{ step.id }}</span>
              <span class="gold-step__copy">
                <strong>{{ step.label }}</strong>
                <small>{{ step.caption }}</small>
              </span>
            </button>
          </div>

          <div class="segmented-progress" aria-hidden="true">
            <span
              class="segmented-progress__segment"
              *ngFor="let step of steps"
              [class.segmented-progress__segment--active]="
                currentStep() >= step.id
              "
            ></span>
          </div>

          <div class="step-info-card animate-pop">
            <div class="step-info-card__left">
              <span class="step-info-card__label">About this step</span>
              <h3>{{ currentStepData().label }}</h3>
              <p>{{ currentStepData().description }}</p>
            </div>

            <div class="step-info-card__right">
              <div class="step-mini-stats">
                <div class="step-mini-stat">
                  <strong>{{ currentStep() }}</strong>
                  <span>Current</span>
                </div>
                <div class="step-mini-stat">
                  <strong>4</strong>
                  <span>Total</span>
                </div>
                <div class="step-mini-stat">
                  <strong>{{ progressPercent() }}%</strong>
                  <span>Progress</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </header>

      <p class="error animate-pop" *ngIf="error()">{{ error() }}</p>
      <p class="success animate-pop" *ngIf="success()">{{ success() }}</p>

      <div class="loading-screen animate-in" *ngIf="loading()">
        <div class="loading-screen__logo-wrap">
          <div class="loading-screen__orbit"></div>
          <div class="loading-screen__logo">
            <img
              src="logo/helma-logo.png"
              alt="Helma logo"
              class="loading-screen__logo-image"
            />
          </div>
        </div>
        <h2>Loading your application</h2>
        <p>Please wait while we prepare your draft.</p>
      </div>

      <form class="wizard" *ngIf="!loading()" [formGroup]="form" novalidate>
        <article class="card step-card animate-in" *ngIf="currentStep() === 1">
          <header class="step-card__header">
            <div>
              <span class="eyebrow eyebrow--soft">Step 1</span>
              <h2>Contact information</h2>
              <p>
                Choose whether to use your profile info or provide custom
                contact details.
              </p>
            </div>
          </header>

          <div class="choice-grid">
            <button
              type="button"
              class="choice-card"
              [class.active]="useProfileInfo()"
              (click)="selectContactSource(true)"
            >
              <div class="choice-card__topline"></div>
              <span class="choice-card__badge">Profile</span>
              <strong>Use my profile</strong>
              <span>Prefill from your existing profile information.</span>
            </button>

            <button
              type="button"
              class="choice-card"
              [class.active]="!useProfileInfo()"
              (click)="selectContactSource(false)"
            >
              <div class="choice-card__topline"></div>
              <span class="choice-card__badge">Custom</span>
              <strong>Use other contact info</strong>
              <span>Provide a different contact person for this raise.</span>
            </button>
          </div>

          <section
            class="profile-preview animate-pop"
            *ngIf="useProfileInfo(); else customContactFields"
          >
            <div class="profile-preview__header">
              <div>
                <span class="profile-preview__eyebrow">Using profile</span>
                <h3>Your profile information will be used</h3>
                <p>
                  These contact details will be submitted with this application.
                </p>
              </div>

              <button
                type="button"
                class="btn btn-ghost btn-sm"
                (click)="selectContactSource(false)"
              >
                Use custom info
              </button>
            </div>

            <div class="profile-preview__grid">
              <div class="profile-preview__item">
                <small>First name</small>
                <strong>{{
                  displayValue(form.controls.contactFirstName.value)
                }}</strong>
              </div>

              <div class="profile-preview__item">
                <small>Last name</small>
                <strong>{{
                  displayValue(form.controls.contactLastName.value)
                }}</strong>
              </div>

              <div class="profile-preview__item">
                <small>Title</small>
                <strong>{{
                  displayValue(form.controls.contactTitle.value)
                }}</strong>
              </div>

              <div class="profile-preview__item">
                <small>Email</small>
                <strong>{{
                  displayValue(form.controls.contactEmail.value)
                }}</strong>
              </div>

              <div class="profile-preview__item">
                <small>Phone</small>
                <strong>{{
                  displayValue(form.controls.contactPhone.value)
                }}</strong>
              </div>
            </div>
          </section>

          <ng-template #customContactFields>
            <div class="form-grid animate-pop">
              <label
                class="field"
                [class.field--error]="showControlError('contactFirstName')"
              >
                <span>First name *</span>
                <input
                  class="input"
                  type="text"
                  formControlName="contactFirstName"
                  maxlength="80"
                  (input)="sanitizeAlphaField('contactFirstName', $event)"
                />
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="
                    showControlError('contactFirstName')
                  "
                >
                  {{
                    showControlError('contactFirstName')
                      ? controlError('contactFirstName')
                      : ' '
                  }}
                </small>
              </label>

              <label
                class="field"
                [class.field--error]="showControlError('contactLastName')"
              >
                <span>Last name *</span>
                <input
                  class="input"
                  type="text"
                  formControlName="contactLastName"
                  maxlength="80"
                  (input)="sanitizeAlphaField('contactLastName', $event)"
                />
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="
                    showControlError('contactLastName')
                  "
                >
                  {{
                    showControlError('contactLastName')
                      ? controlError('contactLastName')
                      : ' '
                  }}
                </small>
              </label>

              <label
                class="field"
                [class.field--error]="showControlError('contactTitle')"
              >
                <span>Title</span>
                <input
                  class="input"
                  type="text"
                  formControlName="contactTitle"
                  maxlength="120"
                  (input)="sanitizeTextField('contactTitle', $event, 120)"
                />
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="
                    showControlError('contactTitle')
                  "
                >
                  {{
                    showControlError('contactTitle')
                      ? controlError('contactTitle')
                      : ' '
                  }}
                </small>
              </label>

              <label
                class="field"
                [class.field--error]="showControlError('contactEmail')"
              >
                <span>Email *</span>
                <input
                  class="input"
                  type="email"
                  formControlName="contactEmail"
                  maxlength="180"
                  (input)="sanitizeEmailField('contactEmail', $event)"
                />
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="
                    showControlError('contactEmail')
                  "
                >
                  {{
                    showControlError('contactEmail')
                      ? controlError('contactEmail')
                      : ' '
                  }}
                </small>
              </label>

              <label
                class="field"
                [class.field--error]="showControlError('contactPhone')"
              >
                <span>Phone</span>
                <input
                  class="input"
                  type="text"
                  formControlName="contactPhone"
                  maxlength="30"
                  (input)="sanitizePhoneField('contactPhone', $event)"
                />
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="
                    showControlError('contactPhone')
                  "
                >
                  {{
                    showControlError('contactPhone')
                      ? controlError('contactPhone')
                      : ' '
                  }}
                </small>
              </label>
            </div>
          </ng-template>

          <footer class="step-card__footer">
            <a class="btn btn-ghost" routerLink="/youth/applications">Cancel</a>
            <button
              class="btn btn-primary"
              type="button"
              [disabled]="saving() || !isContactStepReady()"
              (click)="continueFromContact()"
            >
              {{ saving() ? 'Saving...' : 'Continue' }}
            </button>
          </footer>
        </article>

        <article class="card step-card animate-in" *ngIf="currentStep() === 2">
          <header class="step-card__header">
            <div>
              <span class="eyebrow eyebrow--soft">Step 2</span>
              <h2>Choose raise type</h2>
              <p>Select the fundraising model for this application.</p>
            </div>
          </header>

          <div class="choice-grid choice-grid--types">
            <button
              type="button"
              class="choice-card choice-card--type"
              [class.active]="currentType() === crowdfundingType.DONATION"
              [class.choice-card--locked]="
                isTypeOptionDisabled(crowdfundingType.DONATION)
              "
              [disabled]="isTypeOptionDisabled(crowdfundingType.DONATION)"
              (click)="setType(crowdfundingType.DONATION)"
            >
              <div class="choice-card__topline"></div>
              <span class="choice-card__badge">Community</span>
              <strong>Donation</strong>
              <span>Community-backed support without equity.</span>
            </button>

            <button
              type="button"
              class="choice-card choice-card--type"
              [class.active]="currentType() === crowdfundingType.EQUITY"
              [class.choice-card--locked]="
                isTypeOptionDisabled(crowdfundingType.EQUITY)
              "
              [disabled]="isTypeOptionDisabled(crowdfundingType.EQUITY)"
              (click)="setType(crowdfundingType.EQUITY)"
            >
              <div class="choice-card__topline"></div>
              <span class="choice-card__badge">Investor</span>
              <strong>Equity</strong>
              <span>Investor-backed fundraising with ownership details.</span>
            </button>
          </div>

          <p class="type-lock-note animate-pop" *ngIf="lockedType()">
            Raise type is locked as
            <strong>{{ formatLabel(lockedType()!) }}</strong>
            because this draft already has type-specific details.
          </p>

          <footer class="step-card__footer">
            <button
              class="btn btn-ghost"
              type="button"
              (click)="goToPreviousStep()"
            >
              Back
            </button>
            <button
              class="btn btn-primary"
              type="button"
              [disabled]="saving() || !isTypeStepReady()"
              (click)="continueFromType()"
            >
              {{ saving() ? 'Saving...' : 'Continue' }}
            </button>
          </footer>
        </article>

        <article class="card step-card animate-in" *ngIf="currentStep() === 3">
          <header class="step-card__header">
            <div>
              <span class="eyebrow eyebrow--soft">Step 3</span>
              <h2>Application details</h2>
              <p>Complete the business and raise information.</p>
            </div>
          </header>

          <div class="friendly-grid">
            <label
              class="field"
              [class.field--error]="showControlError('businessName')"
            >
              <div class="field-head">
                <span>Business name *</span>
                <span
                  class="char-badge"
                  [class.char-badge--warn]="
                    isNearCharacterLimit('businessName', 160)
                  "
                >
                  <strong>{{ characterCount('businessName') }}</strong>
                  <small>/160</small>
                </span>
              </div>
              <input
                class="input"
                type="text"
                formControlName="businessName"
                maxlength="160"
                (input)="sanitizeTextField('businessName', $event, 160)"
              />
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="showControlError('businessName')"
              >
                {{
                  showControlError('businessName')
                    ? controlError('businessName')
                    : ' '
                }}
              </small>
            </label>

            <label
              class="field"
              [class.field--error]="showControlError('sector')"
            >
              <span>Sector *</span>
              <select class="select" formControlName="sector">
                <option value="">Select sector</option>
                <option *ngFor="let sector of sectorOptions" [value]="sector">
                  {{ formatLabel(sector) }}
                </option>
              </select>
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="showControlError('sector')"
              >
                {{ showControlError('sector') ? controlError('sector') : ' ' }}
              </small>
            </label>

            <label
              class="field"
              [class.field--error]="showControlError('subSector')"
            >
              <span>Sub-sector *</span>
              <select class="select" formControlName="subSector">
                <option value="">Select sub-sector</option>
                <option *ngFor="let sub of availableSubSectors()" [value]="sub">
                  {{ formatLabel(sub) }}
                </option>
              </select>
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="showControlError('subSector')"
              >
                {{
                  showControlError('subSector')
                    ? controlError('subSector')
                    : ' '
                }}
              </small>
            </label>

            <label
              class="field"
              [class.field--error]="showControlError('fundingGoal')"
            >
              <span>Funding goal *</span>
              <input
                class="input"
                type="number"
                formControlName="fundingGoal"
                min="500"
                step="0.001"
                (input)="sanitizeDecimalField('fundingGoal', $event, 3)"
              />
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="showControlError('fundingGoal')"
              >
                {{
                  showControlError('fundingGoal')
                    ? controlError('fundingGoal')
                    : ' '
                }}
              </small>
            </label>

            <label
              class="field"
              [class.field--error]="showControlError('stage')"
            >
              <span>Project stage *</span>
              <select class="select" formControlName="stage">
                <option value="">Select stage</option>
                <option
                  *ngFor="let stage of projectStageOptions"
                  [value]="stage"
                >
                  {{ formatLabel(stage) }}
                </option>
              </select>
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="showControlError('stage')"
              >
                {{ showControlError('stage') ? controlError('stage') : ' ' }}
              </small>
            </label>

            <label
              class="field"
              [class.field--error]="showControlError('teamSize')"
            >
              <span>Team size *</span>
              <input
                class="input"
                type="number"
                formControlName="teamSize"
                min="1"
                step="1"
                (input)="sanitizeIntegerField('teamSize', $event)"
              />
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="showControlError('teamSize')"
              >
                {{
                  showControlError('teamSize') ? controlError('teamSize') : ' '
                }}
              </small>
            </label>
          </div>

          <section class="location-card animate-pop">
            <div class="location-card__header">
              <div>
                <span class="location-card__eyebrow">Location</span>
                <h3>City and governorate</h3>
                <p>
                  Select your governorate and city, or use your current GPS
                  location.
                </p>
              </div>

              <button
                type="button"
                class="btn btn-ghost btn-sm location-button"
                [disabled]="gpsLoading()"
                (click)="fillLocationFromGps()"
              >
                <span class="location-button__icon" aria-hidden="true">➤</span>
                <span>{{
                  gpsLoading() ? 'Detecting...' : 'Use my location'
                }}</span>
              </button>
            </div>

            <div class="form-grid">
              <label
                class="field"
                [class.field--error]="showControlError('governorate')"
              >
                <span>Governorate *</span>
                <select class="select" formControlName="governorate">
                  <option value="">Select governorate</option>
                  <option
                    *ngFor="let governorate of governorateOptions"
                    [value]="governorate"
                  >
                    {{ governorate }}
                  </option>
                </select>
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="showControlError('governorate')"
                >
                  {{
                    showControlError('governorate')
                      ? controlError('governorate')
                      : ' '
                  }}
                </small>
              </label>

              <label
                class="field"
                [class.field--error]="showControlError('city')"
              >
                <span>City *</span>
                <select
                  class="select"
                  formControlName="city"
                  [disabled]="!form.controls.governorate.value"
                >
                  <option value="">
                    {{
                      form.controls.governorate.value
                        ? 'Select city'
                        : 'Select governorate first'
                    }}
                  </option>
                  <option *ngFor="let city of availableCities()" [value]="city">
                    {{ city }}
                  </option>
                </select>
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="showControlError('city')"
                >
                  {{ showControlError('city') ? controlError('city') : ' ' }}
                </small>
              </label>
            </div>

            <small class="field-error" *ngIf="gpsError()">{{
              gpsError()
            }}</small>
          </section>

          <label
            class="field"
            [class.field--error]="showControlError('summary')"
          >
            <div class="field-head">
              <span>Short summary *</span>
              <span
                class="char-badge"
                [class.char-badge--warn]="isNearCharacterLimit('summary', 255)"
              >
                <strong>{{ characterCount('summary') }}</strong>
                <small>/255</small>
              </span>
            </div>
            <textarea
              class="textarea"
              rows="4"
              formControlName="summary"
              maxlength="255"
              placeholder="Describe your idea in a short, clear way."
              (input)="sanitizeTextField('summary', $event, 255)"
            ></textarea>
            <small
              class="field-error field-error--slot"
              [class.field-error--visible]="showControlError('summary')"
            >
              {{ showControlError('summary') ? controlError('summary') : ' ' }}
            </small>
          </label>

          <div class="story-grid">
            <label
              class="field"
              [class.field--error]="showControlError('problemStatement')"
            >
              <div class="field-head">
                <span>Problem statement *</span>
                <span
                  class="char-badge"
                  [class.char-badge--warn]="
                    isNearCharacterLimit('problemStatement', 3000)
                  "
                >
                  <strong>{{ characterCount('problemStatement') }}</strong>
                  <small>/3000</small>
                </span>
              </div>
              <textarea
                class="textarea"
                rows="5"
                formControlName="problemStatement"
                maxlength="3000"
                placeholder="What problem are you solving and why does it matter?"
                (input)="sanitizeTextField('problemStatement', $event, 3000)"
              ></textarea>
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="
                  showControlError('problemStatement')
                "
              >
                {{
                  showControlError('problemStatement')
                    ? controlError('problemStatement')
                    : ' '
                }}
              </small>
            </label>

            <label
              class="field"
              [class.field--error]="showControlError('solution')"
            >
              <div class="field-head">
                <span>Solution *</span>
                <span
                  class="char-badge"
                  [class.char-badge--warn]="
                    isNearCharacterLimit('solution', 3000)
                  "
                >
                  <strong>{{ characterCount('solution') }}</strong>
                  <small>/3000</small>
                </span>
              </div>
              <textarea
                class="textarea"
                rows="5"
                formControlName="solution"
                maxlength="3000"
                placeholder="Describe your product, service, or initiative."
                (input)="sanitizeTextField('solution', $event, 3000)"
              ></textarea>
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="showControlError('solution')"
              >
                {{
                  showControlError('solution') ? controlError('solution') : ' '
                }}
              </small>
            </label>

            <label
              class="field"
              [class.field--error]="showControlError('targetCustomers')"
            >
              <div class="field-head">
                <span>Target customers / beneficiaries *</span>
                <span
                  class="char-badge"
                  [class.char-badge--warn]="
                    isNearCharacterLimit('targetCustomers', 2000)
                  "
                >
                  <strong>{{ characterCount('targetCustomers') }}</strong>
                  <small>/2000</small>
                </span>
              </div>
              <textarea
                class="textarea"
                rows="4"
                formControlName="targetCustomers"
                maxlength="2000"
                placeholder="Who will use or benefit from this project?"
                (input)="sanitizeTextField('targetCustomers', $event, 2000)"
              ></textarea>
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="
                  showControlError('targetCustomers')
                "
              >
                {{
                  showControlError('targetCustomers')
                    ? controlError('targetCustomers')
                    : ' '
                }}
              </small>
            </label>

            <label
              class="field"
              [class.field--error]="showControlError('useOfFunds')"
            >
              <div class="field-head">
                <span>Use of funds *</span>
                <span
                  class="char-badge"
                  [class.char-badge--warn]="
                    isNearCharacterLimit('useOfFunds', 3000)
                  "
                >
                  <strong>{{ characterCount('useOfFunds') }}</strong>
                  <small>/3000</small>
                </span>
              </div>
              <textarea
                class="textarea"
                rows="5"
                formControlName="useOfFunds"
                maxlength="3000"
                placeholder="Explain how the requested funding will be spent."
                (input)="sanitizeTextField('useOfFunds', $event, 3000)"
              ></textarea>
              <small
                class="field-error field-error--slot"
                [class.field-error--visible]="showControlError('useOfFunds')"
              >
                {{
                  showControlError('useOfFunds')
                    ? controlError('useOfFunds')
                    : ' '
                }}
              </small>
            </label>
          </div>

          <details class="optional-panel">
            <summary>Optional extra details</summary>
            <div class="form-grid optional-panel__content">
              <label
                class="field"
                [class.field--error]="showControlError('website')"
              >
                <span>Website</span>
                <input
                  class="input"
                  type="text"
                  formControlName="website"
                  maxlength="255"
                  (input)="sanitizeUrlField('website', $event)"
                />
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="showControlError('website')"
                >
                  {{
                    showControlError('website') ? controlError('website') : ' '
                  }}
                </small>
              </label>

              <label
                class="field"
                [class.field--error]="showControlError('customerCount')"
              >
                <span>Customer count</span>
                <input
                  class="input"
                  type="number"
                  formControlName="customerCount"
                  min="0"
                  step="1"
                  (input)="sanitizeIntegerField('customerCount', $event)"
                />
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="
                    showControlError('customerCount')
                  "
                >
                  {{
                    showControlError('customerCount')
                      ? controlError('customerCount')
                      : ' '
                  }}
                </small>
              </label>
            </div>
          </details>

          <div class="field" [class.field--error]="showTagError()">
            <div class="field-head field-head--inline">
              <span>Tags *</span>
              <span class="tag-counter"
                >{{ selectedTags().length }}/3 selected</span
              >
            </div>
            <div class="tag-grid">
              <button
                type="button"
                class="tag-pill"
                *ngFor="let tag of tagOptions"
                [class.selected]="selectedTags().includes(tag)"
                [class.tag-pill--disabled]="isTagDisabled(tag)"
                [disabled]="isTagDisabled(tag)"
                (click)="toggleTag(tag)"
              >
                {{ formatLabel(tag) }}
              </button>
            </div>
            <small class="field-hint">Choose between 1 and 3 tags.</small>
            <small class="field-error" *ngIf="showTagError()">
              Select at least one tag.
            </small>
          </div>

          <section
            class="equity-block"
            *ngIf="currentType() === crowdfundingType.EQUITY"
          >
            <div class="section-divider">
              <h3>Equity details</h3>
            </div>

            <section class="rne-card animate-pop">
              <div class="rne-card__header">
                <div>
                  <span class="rne-card__eyebrow">RNE lookup</span>
                  <h4>Autofill company details</h4>
                  <p>
                    Enter the RNE identifier and fetch the official company
                    details automatically.
                  </p>
                </div>

                <span class="rne-card__status" *ngIf="rneLoading()"
                  >Verifying...</span
                >
              </div>

              <div class="rne-card__controls">
                <label
                  class="field field--compact"
                  [class.field--error]="!!rneError()"
                >
                  <span>RNE identifier</span>
                  <input
                    class="input"
                    type="text"
                    formControlName="rneId"
                    maxlength="30"
                    placeholder="Example: RNE11854"
                    (input)="sanitizeCodeField('rneId', $event, 30)"
                  />
                  <small class="field-error" *ngIf="rneError()">{{
                    rneError()
                  }}</small>
                </label>

                <button
                  type="button"
                  class="btn btn-primary"
                  [disabled]="rneLoading() || !canFetchRne()"
                  (click)="fetchRneDetails()"
                >
                  {{ rneLoading() ? 'Checking...' : 'Verify company' }}
                </button>
              </div>

              <div
                class="rne-card__result animate-pop"
                *ngIf="rnePreview() as preview"
              >
                <div class="rne-card__result-item">
                  <small>Registry ID</small>
                  <strong>{{ preview.idUnique || '—' }}</strong>
                </div>
                <div class="rne-card__result-item">
                  <small>Company name</small>
                  <strong>{{
                    preview.denominationLatin ||
                      preview.denomination ||
                      preview.nomCommercialFr ||
                      preview.nomCommercialAr ||
                      '—'
                  }}</strong>
                </div>
                <div class="rne-card__result-item">
                  <small>Status</small>
                  <strong>{{ preview.status || '—' }}</strong>
                </div>
              </div>
            </section>

            <section class="readonly-company-card animate-pop">
              <div class="readonly-company-card__header">
                <div>
                  <span class="readonly-company-card__eyebrow"
                    >Official RNE data</span
                  >
                  <h4>Company details</h4>
                  <p>
                    These details are filled automatically from the RNE lookup
                    and cannot be edited manually.
                  </p>
                </div>
              </div>

              <div class="readonly-company-card__grid">
                <div class="readonly-company-card__item">
                  <small>Company legal name</small>
                  <strong>{{
                    form.controls.companyLegalName.value ||
                      'Verify company first'
                  }}</strong>
                </div>

                <div class="readonly-company-card__item">
                  <small>Registration number</small>
                  <strong>
                    {{
                      form.controls.companyRegistrationNumber.value ||
                        'Verify company first'
                    }}
                  </strong>
                </div>

                <div
                  class="readonly-company-card__item readonly-company-card__item--wide"
                >
                  <small>CNRE profile URL</small>

                  <a
                    *ngIf="
                      form.controls.cnreProfileUrl.value;
                      else missingCnreUrl
                    "
                    [href]="form.controls.cnreProfileUrl.value"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {{ form.controls.cnreProfileUrl.value }}
                  </a>

                  <ng-template #missingCnreUrl>
                    <strong>Verify company first</strong>
                  </ng-template>
                </div>
              </div>

              <small
                class="field-error"
                *ngIf="
                  showControlError('companyLegalName') ||
                  showControlError('companyRegistrationNumber') ||
                  showControlError('cnreProfileUrl')
                "
              >
                Please fetch the company details from RNE before continuing.
              </small>
            </section>

            <div class="form-grid">
              <section class="calculated-equity-card">
                <small>Calculated equity offered</small>
                <strong>{{ calculatedEquityOfferedPercentLabel() }}</strong>
                <span>
                  Calculated from funding goal ÷ (pre-money valuation + funding
                  goal).
                </span>
              </section>

              <label
                class="field"
                [class.field--error]="showControlError('preMoneyValuation')"
              >
                <span>Pre-money valuation</span>
                <input
                  class="input"
                  type="number"
                  formControlName="preMoneyValuation"
                  min="0"
                  step="0.001"
                  (input)="sanitizeDecimalField('preMoneyValuation', $event, 3)"
                />
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="
                    showControlError('preMoneyValuation')
                  "
                >
                  {{
                    showControlError('preMoneyValuation')
                      ? controlError('preMoneyValuation')
                      : ' '
                  }}
                </small>
              </label>

              <label
                class="field"
                [class.field--error]="showControlError('minInvestment')"
              >
                <span>Minimum investment</span>
                <input
                  class="input"
                  type="number"
                  formControlName="minInvestment"
                  min="0"
                  step="0.001"
                  (input)="sanitizeDecimalField('minInvestment', $event, 3)"
                />
                <small
                  class="field-error field-error--slot"
                  [class.field-error--visible]="
                    showControlError('minInvestment')
                  "
                >
                  {{
                    showControlError('minInvestment')
                      ? controlError('minInvestment')
                      : ' '
                  }}
                </small>
              </label>
            </div>
          </section>

          <footer class="step-card__footer">
            <button
              class="btn btn-ghost"
              type="button"
              (click)="goToPreviousStep()"
            >
              Back
            </button>
            <button
              class="btn btn-primary"
              type="button"
              [disabled]="saving() || !isDetailsStepReady()"
              (click)="continueFromDetails()"
            >
              {{ saving() ? 'Saving...' : 'Continue' }}
            </button>
          </footer>
        </article>

        <article class="card step-card animate-in" *ngIf="currentStep() === 4">
          <header class="step-card__header">
            <div>
              <span class="eyebrow eyebrow--soft">Step 4</span>
              <h2>Documents & submission</h2>
              <p>
                Upload the required files first. Optional files can strengthen
                your application.
              </p>
            </div>
          </header>

          <section class="documents-summary">
            <div class="documents-summary__metric animate-pop">
              <strong
                >{{ requiredUploadedCount() }}/{{
                  requiredDocuments().length
                }}</strong
              >
              <span>Required completed</span>
            </div>

            <div class="documents-summary__metric animate-pop">
              <strong>{{ documents().length }}</strong>
              <span>Total uploaded</span>
            </div>

            <div
              class="documents-summary__metric documents-summary__metric--warn animate-pop"
              *ngIf="missingRequiredDocuments().length"
            >
              <strong>{{ missingRequiredDocuments().length }}</strong>
              <span>Still missing</span>
            </div>

            <div
              class="documents-summary__metric documents-summary__metric--ok animate-pop"
              *ngIf="!missingRequiredDocuments().length"
            >
              <strong>Ready</strong>
              <span>All required docs uploaded</span>
            </div>
          </section>

          <section class="documents-group">
            <div class="documents-group__header">
              <div>
                <h3>Required documents</h3>
                <p>These are needed before you can submit the application.</p>
              </div>
            </div>

            <div class="document-list">
              <article
                class="document-card document-card--required animate-pop"
                *ngFor="let doc of requiredDocuments()"
              >
                <div class="document-card__top">
                  <div class="document-card__copy">
                    <div class="document-card__title-row">
                      <strong>{{ doc.label }}</strong>
                      <span class="status-badge status-badge--required"
                        >Required</span
                      >
                    </div>
                    <small>{{ doc.description }}</small>
                    <em>{{ doc.acceptedFormats }}</em>
                  </div>

                  <span
                    class="upload-state"
                    [class.upload-state--done]="documentFor(doc.type)"
                    [class.upload-state--pending]="!documentFor(doc.type)"
                  >
                    {{ documentFor(doc.type) ? 'Uploaded' : 'Pending' }}
                  </span>
                </div>

                <div
                  class="document-card__bottom"
                  *ngIf="documentFor(doc.type) as existing; else requiredUpload"
                >
                  <div class="uploaded-file">
                    <span class="uploaded-file__name">{{
                      existing.fileName
                    }}</span>
                  </div>

                  <div class="document-actions">
                    <button
                      class="btn btn-ghost btn-sm"
                      type="button"
                      (click)="previewDocument(doc.type)"
                    >
                      View
                    </button>
                    <button
                      class="btn btn-ghost btn-sm btn-danger"
                      type="button"
                      (click)="removeDocument(doc.type)"
                    >
                      Remove
                    </button>
                  </div>
                </div>

                <ng-template #requiredUpload>
                  <label class="upload-box">
                    <input
                      class="upload-box__input"
                      type="file"
                      [accept]="'.pdf,application/pdf,image/*'"
                      (change)="onFileSelected(doc.type, $event)"
                    />
                    <span class="upload-box__text">Upload {{ doc.label }}</span>
                  </label>
                </ng-template>
              </article>
            </div>
          </section>

          <section class="documents-group" *ngIf="optionalDocuments().length">
            <div class="documents-group__header">
              <div>
                <h3>Optional / bonus documents</h3>
                <p>
                  These are not required, but they can improve credibility and
                  help review.
                </p>
              </div>
            </div>

            <div class="document-list">
              <article
                class="document-card document-card--optional animate-pop"
                *ngFor="let doc of optionalDocuments()"
              >
                <div class="document-card__top">
                  <div class="document-card__copy">
                    <div class="document-card__title-row">
                      <strong>{{ doc.label }}</strong>
                      <span class="status-badge status-badge--optional"
                        >Optional</span
                      >
                    </div>
                    <small>{{ doc.description }}</small>
                    <em>{{ doc.acceptedFormats }}</em>
                  </div>

                  <span
                    class="upload-state"
                    [class.upload-state--done]="documentFor(doc.type)"
                    [class.upload-state--pending]="!documentFor(doc.type)"
                  >
                    {{ documentFor(doc.type) ? 'Uploaded' : 'Bonus' }}
                  </span>
                </div>

                <div
                  class="document-card__bottom"
                  *ngIf="documentFor(doc.type) as existing; else optionalUpload"
                >
                  <div class="uploaded-file">
                    <span class="uploaded-file__name">{{
                      existing.fileName
                    }}</span>
                  </div>

                  <div class="document-actions">
                    <button
                      class="btn btn-ghost btn-sm"
                      type="button"
                      (click)="previewDocument(doc.type)"
                    >
                      View
                    </button>
                    <button
                      class="btn btn-ghost btn-sm btn-danger"
                      type="button"
                      (click)="removeDocument(doc.type)"
                    >
                      Remove
                    </button>
                  </div>
                </div>

                <ng-template #optionalUpload>
                  <label class="upload-box upload-box--optional">
                    <input
                      class="upload-box__input"
                      type="file"
                      [accept]="'.pdf,application/pdf,image/*'"
                      (change)="onFileSelected(doc.type, $event)"
                    />
                    <span class="upload-box__text">Add bonus document</span>
                  </label>
                </ng-template>
              </article>
            </div>
          </section>

          <label class="terms-box animate-pop">
            <input type="checkbox" formControlName="acceptedTerms" />
            <span
              >I confirm the information is accurate and ready for
              submission.</span
            >
          </label>

          <div
            class="missing-box animate-pop"
            *ngIf="missingRequiredDocuments().length"
          >
            <strong>Missing required files:</strong>
            <span>{{ missingRequiredDocumentLabels() }}</span>
          </div>

          <footer class="step-card__footer">
            <button
              class="btn btn-ghost"
              type="button"
              (click)="goToPreviousStep()"
            >
              Back
            </button>
            <button
              class="btn btn-primary"
              type="button"
              [disabled]="saving() || !canSubmitDocuments()"
              (click)="submitApplication()"
            >
              {{ saving() ? 'Submitting...' : 'Submit application' }}
            </button>
          </footer>
        </article>
      </form>
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      @property --progress-value {
        syntax: '<number>';
        inherits: false;
        initial-value: 0;
      }

      .page {
        display: grid;
        gap: 24px;
        padding-bottom: 20px;
      }

      .card {
        border: 1px solid rgba(229, 231, 235, 0.92);
        border-radius: 30px;
        background: linear-gradient(
          180deg,
          rgba(255, 255, 255, 0.98),
          rgba(255, 252, 244, 0.98)
        );
        box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
      }

      .page-header {
        display: grid;
        gap: 22px;
        padding: 26px;
        border-color: rgba(212, 166, 42, 0.18);
        overflow: hidden;
        position: relative;
      }

      .page-header::after {
        content: '';
        position: absolute;
        inset: 0;
        background:
          radial-gradient(
            circle at top right,
            rgba(212, 166, 42, 0.12),
            transparent 28%
          ),
          radial-gradient(
            circle at bottom left,
            rgba(212, 166, 42, 0.09),
            transparent 26%
          );
        pointer-events: none;
      }

      .page-header__top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 18px;
        position: relative;
        z-index: 1;
      }

      .page-header__copy {
        display: grid;
        gap: 10px;
        max-width: 760px;
      }

      .page-header__copy h1 {
        margin: 0;
        font-size: clamp(1.8rem, 3vw, 2.3rem);
        line-height: 1.1;
        color: #111827;
      }

      .page-header__copy p {
        margin: 0;
        color: #6b7280;
        line-height: 1.65;
        max-width: 650px;
      }

      .progress-circle {
        flex-shrink: 0;
      }

      .progress-circle__ring {
        --progress-value: 0;
        width: 132px;
        height: 132px;
        border-radius: 50%;
        display: grid;
        place-items: center;
        background: conic-gradient(
          #d4a62a 0 calc(var(--progress-value) * 1%),
          rgba(212, 166, 42, 0.14) calc(var(--progress-value) * 1%) 100%
        );
        box-shadow:
          inset 0 0 0 1px rgba(212, 166, 42, 0.12),
          0 10px 30px rgba(212, 166, 42, 0.16);
        position: relative;
        transition:
          --progress-value 0.35s ease,
          transform 0.18s ease,
          box-shadow 0.18s ease;
      }

      .progress-circle__ring::before {
        content: '';
        position: absolute;
        inset: 10px;
        border-radius: 50%;
        background: linear-gradient(
          180deg,
          rgba(255, 255, 255, 0.98),
          rgba(255, 251, 239, 0.96)
        );
        box-shadow: inset 0 0 0 1px rgba(212, 166, 42, 0.1);
      }

      .progress-circle__inner {
        position: relative;
        z-index: 1;
        display: grid;
        gap: 2px;
        text-align: center;
      }

      .progress-circle__inner strong {
        font-size: 1.35rem;
        color: #8d6a08;
      }

      .progress-circle__inner span {
        font-size: 0.8rem;
        font-weight: 700;
        color: #8b8b8b;
      }

      .step-overview {
        display: grid;
        gap: 16px;
        position: relative;
        z-index: 1;
      }

      .step-overview__meta {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }

      .step-overview__meta small {
        display: block;
        margin-bottom: 4px;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        font-size: 0.72rem;
        font-weight: 800;
        color: #b08912;
      }

      .step-overview__meta strong {
        color: #1f2937;
        font-size: 1rem;
      }

      .step-overview__pill {
        min-height: 36px;
        display: inline-flex;
        align-items: center;
        padding: 0 14px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.16);
        color: #8d6a08;
        font-weight: 800;
        font-size: 0.82rem;
      }

      .gold-steps {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 12px;
      }

      .gold-step {
        min-height: 84px;
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 14px;
        border: 1px solid rgba(212, 166, 42, 0.24);
        border-radius: 22px;
        background: linear-gradient(
          180deg,
          rgba(255, 248, 227, 0.86),
          rgba(255, 255, 255, 0.96)
        );
        cursor: pointer;
        text-align: left;
        transition:
          transform 0.18s ease,
          box-shadow 0.18s ease,
          border-color 0.18s ease;
        box-shadow: 0 10px 24px rgba(212, 166, 42, 0.08);
      }

      .gold-step:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 16px 28px rgba(212, 166, 42, 0.12);
      }

      .gold-step:disabled {
        opacity: 0.72;
        cursor: not-allowed;
      }

      .gold-step--active,
      .gold-step--done {
        background: linear-gradient(180deg, #f7d774, #d4a62a);
        border-color: rgba(194, 148, 18, 0.7);
        box-shadow: 0 18px 30px rgba(212, 166, 42, 0.24);
      }

      .gold-step__number {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: grid;
        place-items: center;
        background: rgba(255, 255, 255, 0.76);
        color: #8d6a08;
        font-weight: 900;
        flex-shrink: 0;
        box-shadow: inset 0 0 0 1px rgba(212, 166, 42, 0.22);
      }

      .gold-step--active .gold-step__number,
      .gold-step--done .gold-step__number {
        background: rgba(255, 255, 255, 0.92);
      }

      .gold-step__copy {
        display: grid;
        gap: 2px;
        min-width: 0;
      }

      .gold-step__copy strong {
        color: #433217;
        font-size: 0.95rem;
      }

      .gold-step__copy small {
        color: rgba(67, 50, 23, 0.74);
        line-height: 1.35;
        font-size: 0.76rem;
      }

      .segmented-progress {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 8px;
      }

      .segmented-progress__segment {
        height: 10px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.14);
        overflow: hidden;
        transition:
          background 0.24s ease,
          transform 0.24s ease;
      }

      .segmented-progress__segment--active {
        background: linear-gradient(90deg, #f7d774, #d4a62a);
        box-shadow: 0 8px 16px rgba(212, 166, 42, 0.18);
        transform: scaleY(1.05);
      }

      .step-info-card {
        display: grid;
        grid-template-columns: 1.6fr 1fr;
        gap: 18px;
        align-items: stretch;
        padding: 18px;
        border-radius: 24px;
        background: linear-gradient(
          135deg,
          rgba(255, 248, 227, 0.88),
          rgba(255, 255, 255, 0.98)
        );
        border: 1px solid rgba(212, 166, 42, 0.2);
        box-shadow: 0 14px 28px rgba(212, 166, 42, 0.08);
      }

      .step-info-card__label {
        display: inline-flex;
        padding: 6px 10px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.16);
        color: #8d6a08;
        font-weight: 800;
        font-size: 0.74rem;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        width: fit-content;
        margin-bottom: 10px;
      }

      .step-info-card h3 {
        margin: 0 0 8px;
        color: #1f2937;
      }

      .step-info-card p {
        margin: 0;
        color: #6b7280;
        line-height: 1.7;
      }

      .step-mini-stats {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 12px;
        height: 100%;
      }

      .step-mini-stat {
        display: grid;
        place-items: center;
        padding: 14px;
        border-radius: 20px;
        background: rgba(255, 255, 255, 0.85);
        border: 1px solid rgba(212, 166, 42, 0.18);
        text-align: center;
      }

      .step-mini-stat strong {
        font-size: 1.1rem;
        color: #8d6a08;
      }

      .step-mini-stat span {
        color: #7b7280;
        font-size: 0.78rem;
        font-weight: 700;
      }

      .wizard {
        display: grid;
      }

      .step-card {
        display: grid;
        gap: 22px;
        padding: 24px;
        border-color: rgba(212, 166, 42, 0.12);
      }

      .step-card__header h2 {
        margin: 8px 0;
        color: #111827;
      }

      .step-card__header p {
        margin: 0;
        color: #6b7280;
        line-height: 1.65;
      }

      .step-card__footer {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        flex-wrap: wrap;
        padding-top: 8px;
        border-top: 1px solid rgba(229, 231, 235, 0.8);
      }

      .eyebrow {
        display: inline-flex;
        width: fit-content;
        padding: 7px 12px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.16);
        color: #8d6a08;
        font-size: 0.78rem;
        font-weight: 800;
        letter-spacing: 0.05em;
        text-transform: uppercase;
      }

      .eyebrow--soft {
        background: rgba(212, 166, 42, 0.12);
        color: #8d6a08;
      }

      .choice-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 16px;
      }

      .choice-card {
        min-height: 136px;
        display: grid;
        gap: 10px;
        padding: 20px;
        border-radius: 24px;
        border: 1px solid rgba(229, 231, 235, 0.95);
        background: rgba(255, 255, 255, 0.96);
        text-align: left;
        cursor: pointer;
        transition: 0.18s ease;
        box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
      }

      .choice-card:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
      }

      .choice-card:disabled {
        cursor: not-allowed;
        opacity: 0.58;
        filter: saturate(0.7);
      }

      .choice-card--locked {
        background: linear-gradient(
          180deg,
          rgba(249, 250, 251, 0.96),
          rgba(255, 255, 255, 0.96)
        );
      }

      .type-lock-note {
        margin: 0;
        padding: 12px 14px;
        border-radius: 16px;
        background: rgba(17, 24, 39, 0.04);
        color: #4b5563;
        font-weight: 700;
      }

      .type-lock-note strong {
        color: #8d6a08;
      }

      .choice-card.active {
        border-color: rgba(212, 166, 42, 0.3);
        background: linear-gradient(
          180deg,
          rgba(255, 246, 219, 0.88),
          rgba(255, 255, 255, 0.98)
        );
        box-shadow: 0 16px 30px rgba(212, 166, 42, 0.12);
      }

      .choice-card__topline {
        width: 56px;
        height: 5px;
        border-radius: 999px;
        background: linear-gradient(90deg, #f7d774, #d4a62a);
        box-shadow: 0 8px 16px rgba(212, 166, 42, 0.16);
      }

      .choice-card__badge {
        width: fit-content;
        display: inline-flex;
        align-items: center;
        min-height: 28px;
        padding: 0 10px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.12);
        color: #8d6a08;
        font-size: 0.74rem;
        font-weight: 800;
        letter-spacing: 0.04em;
        text-transform: uppercase;
      }

      .choice-card strong {
        font-size: 1rem;
        color: #111827;
      }

      .choice-card span {
        color: #6b7280;
        line-height: 1.6;
      }

      .profile-preview {
        display: grid;
        gap: 16px;
        padding: 20px;
        border-radius: 24px;
        border: 1px solid rgba(212, 166, 42, 0.18);
        background: linear-gradient(
          180deg,
          rgba(255, 247, 223, 0.7),
          rgba(255, 255, 255, 0.98)
        );
        box-shadow: 0 14px 28px rgba(212, 166, 42, 0.08);
      }

      .profile-preview__header {
        display: flex;
        justify-content: space-between;
        gap: 16px;
        align-items: flex-start;
        flex-wrap: wrap;
      }

      .profile-preview__eyebrow {
        display: inline-flex;
        min-height: 28px;
        align-items: center;
        padding: 0 10px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.14);
        color: #8d6a08;
        font-size: 0.72rem;
        font-weight: 800;
        letter-spacing: 0.04em;
        text-transform: uppercase;
        margin-bottom: 10px;
      }

      .profile-preview__header h3 {
        margin: 0 0 6px;
        color: #111827;
      }

      .profile-preview__header p {
        margin: 0;
        color: #6b7280;
        line-height: 1.6;
      }

      .profile-preview__grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 14px;
      }

      .profile-preview__item {
        display: grid;
        gap: 6px;
        padding: 14px 16px;
        border-radius: 18px;
        background: rgba(255, 255, 255, 0.9);
        border: 1px solid rgba(212, 166, 42, 0.12);
      }

      .profile-preview__item small {
        font-size: 0.72rem;
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: #8b8b8b;
      }

      .profile-preview__item strong {
        color: #111827;
        line-height: 1.5;
        word-break: break-word;
      }

      .field {
        display: grid;
        gap: 8px;
        position: relative;
      }

      .field > span {
        font-size: 0.76rem;
        font-weight: 800;
        text-transform: uppercase;
        color: #111827;
        letter-spacing: 0.04em;
      }

      .field--compact {
        align-content: start;
      }

      .field--error .input,
      .field--error .select,
      .field--error .textarea {
        border-color: #ef4444;
        box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.08);
        background: #fffdfd;
      }

      .field-error {
        color: #dc2626;
        font-size: 0.82rem;
        font-weight: 700;
        line-height: 1.45;
      }

      .field-error--slot {
        min-height: 20px;
        display: block;
        opacity: 0;
        transform: translateY(-2px);
        transition:
          opacity 0.18s ease,
          transform 0.18s ease;
      }

      .field-error--visible {
        opacity: 1;
        transform: translateY(0);
      }

      .form-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 16px;
      }

      .friendly-grid,
      .story-grid {
        display: grid;
        gap: 16px;
      }

      .friendly-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      .story-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      .field-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
      }

      .field-head--inline {
        margin-bottom: 8px;
      }

      .field-head .char-badge {
        position: absolute;
        top: 34px;
        right: 12px;
        z-index: 2;
      }

      .char-badge {
        width: 42px;
        height: 42px;
        border-radius: 50%;
        display: grid;
        place-items: center;
        background: radial-gradient(
          circle at 32% 24%,
          #ffffff,
          #fff8e4 58%,
          #f4d675
        );
        border: 1px solid rgba(212, 166, 42, 0.36);
        box-shadow:
          0 10px 20px rgba(212, 166, 42, 0.14),
          inset 0 1px 0 rgba(255, 255, 255, 0.9);
        flex-shrink: 0;
        pointer-events: none;
        transition:
          transform 0.18s ease,
          box-shadow 0.18s ease;
      }

      .field:focus-within .char-badge {
        transform: scale(1.04);
        box-shadow:
          0 14px 26px rgba(212, 166, 42, 0.2),
          inset 0 1px 0 rgba(255, 255, 255, 0.92);
      }

      .char-badge strong {
        display: block;
        line-height: 1;
        font-size: 0.76rem;
        color: #8d6a08;
      }

      .char-badge small {
        display: block;
        line-height: 1;
        font-size: 0.55rem;
        color: #8b8b8b;
        font-weight: 900;
      }
      .location-button {
        gap: 8px;
      }

      .location-button__icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 22px;
        height: 22px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.14);
        color: #8d6a08;
        font-size: 0.76rem;
        transform: rotate(-35deg);
      }

      .char-badge--warn {
        border-color: rgba(234, 88, 12, 0.36);
        background: radial-gradient(
          circle at 32% 24%,
          #ffffff,
          #fff1df 58%,
          #fdba74
        );
      }

      .location-card,
      .optional-panel {
        border-radius: 24px;
        border: 1px solid rgba(212, 166, 42, 0.16);
        background: rgba(255, 255, 255, 0.92);
        box-shadow: 0 12px 24px rgba(15, 23, 42, 0.05);
      }

      .location-card {
        display: grid;
        gap: 16px;
        padding: 20px;
        background: linear-gradient(
          180deg,
          rgba(255, 248, 227, 0.62),
          rgba(255, 255, 255, 0.98)
        );
      }

      .location-card__header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 16px;
        flex-wrap: wrap;
      }

      .location-card__header h3 {
        margin: 0 0 6px;
        color: #111827;
      }

      .location-card__header p {
        margin: 0;
        color: #6b7280;
      }

      .location-card__eyebrow {
        display: inline-flex;
        min-height: 28px;
        align-items: center;
        padding: 0 10px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.14);
        color: #8d6a08;
        font-size: 0.72rem;
        font-weight: 800;
        letter-spacing: 0.04em;
        text-transform: uppercase;
        margin-bottom: 10px;
      }

      .optional-panel {
        overflow: hidden;
      }

      .optional-panel summary {
        cursor: pointer;
        list-style: none;
        padding: 18px 20px;
        font-weight: 800;
        color: #111827;
      }

      .optional-panel summary::-webkit-details-marker {
        display: none;
      }

      .optional-panel__content {
        padding: 0 20px 20px;
      }

      .field-hint {
        color: #6b7280;
        font-size: 0.82rem;
        font-weight: 600;
      }

      .tag-counter {
        min-height: 30px;
        display: inline-flex;
        align-items: center;
        padding: 0 12px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.12);
        color: #8d6a08;
        font-weight: 800;
        font-size: 0.76rem;
      }

      .tag-pill--disabled {
        opacity: 0.45;
        cursor: not-allowed;
        transform: none !important;
      }

      .form-grid--full {
        grid-template-columns: 1fr;
      }

      .number-stepper {
        display: grid;
        grid-template-columns: 44px 1fr 44px;
        gap: 8px;
        align-items: center;
      }

      .number-stepper__btn {
        width: 44px;
        height: 44px;
        border: 1px solid rgba(212, 166, 42, 0.22);
        border-radius: 16px;
        background: linear-gradient(180deg, #fff8e3, #ffffff);
        color: #8d6a08;
        font-size: 1.2rem;
        font-weight: 900;
        cursor: pointer;
        box-shadow: 0 8px 16px rgba(212, 166, 42, 0.08);
        transition:
          transform 0.18s ease,
          box-shadow 0.18s ease,
          border-color 0.18s ease;
      }

      .number-stepper__btn:hover {
        transform: translateY(-1px) scale(1.03);
        box-shadow: 0 14px 24px rgba(212, 166, 42, 0.14);
        border-color: rgba(212, 166, 42, 0.42);
      }

      .number-stepper__input {
        text-align: center;
        padding-right: 16px;
      }

      .field--wide {
        grid-column: 1 / -1;
      }

      .input,
      .select,
      .textarea {
        width: 100%;
        min-height: 50px;
        padding: 0 16px;
        padding-right: 68px;
        border-radius: 18px;
        border: 1px solid rgba(229, 231, 235, 0.98);
        background: linear-gradient(
          180deg,
          rgba(255, 255, 255, 0.98),
          rgba(255, 253, 247, 0.98)
        );
        font: inherit;
        font-weight: 700;
        letter-spacing: 0.01em;
        color: #111827;
        box-sizing: border-box;
        transition:
          border-color 0.18s ease,
          box-shadow 0.18s ease,
          transform 0.18s ease;
      }

      .input[type='number'] {
        appearance: textfield;
        -moz-appearance: textfield;
      }

      .input[type='number']::-webkit-outer-spin-button,
      .input[type='number']::-webkit-inner-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }

      .input:focus,
      .select:focus,
      .textarea:focus {
        outline: none;
        border-color: rgba(212, 166, 42, 0.7);
        box-shadow: 0 0 0 4px rgba(212, 166, 42, 0.12);
      }

      .textarea {
        min-height: 136px;
        padding: 16px;
        padding-right: 72px;
        resize: vertical;
        line-height: 1.65;
      }

      .input::placeholder,
      .textarea::placeholder {
        color: #9ca3af;
        font-weight: 600;
      }

      .input:hover,
      .select:hover,
      .textarea:hover {
        border-color: rgba(212, 166, 42, 0.34);
      }

      .tag-grid {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
      }

      .tag-pill {
        min-height: 42px;
        padding: 0 14px;
        border-radius: 999px;
        border: 1px solid #e5e7eb;
        background: #fff;
        font: inherit;
        font-weight: 700;
        cursor: pointer;
        transition: 0.18s ease;
      }

      .tag-pill:hover {
        transform: translateY(-1px);
      }

      .tag-pill.selected {
        background: linear-gradient(180deg, #f7d774, #efd16a);
        border-color: rgba(212, 166, 42, 0.36);
        color: #6f5306;
        box-shadow: 0 10px 20px rgba(212, 166, 42, 0.14);
      }

      .section-divider {
        padding-top: 8px;
        border-top: 1px solid rgba(229, 231, 235, 0.8);
      }

      .section-divider h3 {
        margin: 0;
      }

      .rne-card {
        display: grid;
        gap: 16px;
        padding: 20px;
        border-radius: 24px;
        border: 1px solid rgba(212, 166, 42, 0.18);
        background: linear-gradient(
          180deg,
          rgba(255, 247, 223, 0.78),
          rgba(255, 255, 255, 0.98)
        );
        box-shadow: 0 14px 28px rgba(212, 166, 42, 0.08);
      }

      .rne-card__header {
        display: flex;
        justify-content: space-between;
        gap: 14px;
        align-items: flex-start;
        flex-wrap: wrap;
      }

      .rne-card__header h4 {
        margin: 0 0 6px;
        color: #111827;
      }

      .rne-card__header p {
        margin: 0;
        color: #6b7280;
        line-height: 1.6;
      }

      .rne-card__eyebrow {
        display: inline-flex;
        min-height: 28px;
        align-items: center;
        padding: 0 10px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.14);
        color: #8d6a08;
        font-size: 0.72rem;
        font-weight: 800;
        letter-spacing: 0.04em;
        text-transform: uppercase;
        margin-bottom: 10px;
      }

      .rne-card__status {
        min-height: 32px;
        display: inline-flex;
        align-items: center;
        padding: 0 12px;
        border-radius: 999px;
        background: rgba(212, 166, 42, 0.14);
        color: #8d6a08;
        font-weight: 800;
        font-size: 0.82rem;
      }

      .rne-card__controls {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 14px;
        align-items: end;
      }

      .rne-card__result {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 14px;
      }

      .rne-card__result-item {
        display: grid;
        gap: 6px;
        padding: 14px 16px;
        border-radius: 18px;
        background: rgba(255, 255, 255, 0.9);
        border: 1px solid rgba(212, 166, 42, 0.12);
      }

      .rne-card__result-item small {
        font-size: 0.72rem;
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: #8b8b8b;
      }

      .rne-card__result-item strong {
        color: #111827;
        line-height: 1.5;
        word-break: break-word;
      }

      .readonly-company-card {
        display: grid;
        gap: 16px;
        padding: 20px;
        border-radius: 24px;
        border: 1px solid rgba(17, 24, 39, 0.08);
        background: linear-gradient(
          180deg,
          rgba(249, 250, 251, 0.95),
          rgba(255, 255, 255, 0.98)
        );
        box-shadow: 0 14px 28px rgba(17, 24, 39, 0.06);
      }

      .readonly-company-card__header {
        display: flex;
        justify-content: space-between;
        gap: 14px;
        align-items: flex-start;
        flex-wrap: wrap;
      }

      .readonly-company-card__header h4 {
        margin: 0 0 6px;
        color: #111827;
      }

      .readonly-company-card__header p {
        margin: 0;
        color: #6b7280;
        line-height: 1.6;
      }

      .readonly-company-card__eyebrow {
        display: inline-flex;
        min-height: 28px;
        align-items: center;
        padding: 0 10px;
        border-radius: 999px;
        background: rgba(17, 24, 39, 0.06);
        color: #374151;
        font-size: 0.72rem;
        font-weight: 800;
        letter-spacing: 0.04em;
        text-transform: uppercase;
        margin-bottom: 10px;
      }

      .readonly-company-card__grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 14px;
      }

      .readonly-company-card__item {
        display: grid;
        gap: 6px;
        padding: 14px 16px;
        border-radius: 18px;
        background: rgba(255, 255, 255, 0.92);
        border: 1px solid rgba(17, 24, 39, 0.08);
      }

      .readonly-company-card__item--wide {
        grid-column: 1 / -1;
      }

      .readonly-company-card__item small {
        font-size: 0.72rem;
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: #8b8b8b;
      }

      .readonly-company-card__item strong,
      .readonly-company-card__item a {
        color: #111827;
        line-height: 1.5;
        word-break: break-word;
        font-weight: 800;
      }

      .readonly-company-card__item a {
        color: #8d6a08;
        text-decoration: none;
      }

      .readonly-company-card__item a:hover {
        text-decoration: underline;
      }

      .calculated-equity-card {
        display: grid;
        gap: 8px;
        min-height: 104px;
        padding: 16px;
        border-radius: 18px;
        border: 1px solid rgba(212, 166, 42, 0.18);
        background: linear-gradient(
          180deg,
          rgba(255, 248, 227, 0.88),
          rgba(255, 255, 255, 0.98)
        );
        align-content: center;
      }

      .calculated-equity-card small {
        font-size: 0.72rem;
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: #8b8b8b;
      }

      .calculated-equity-card strong {
        color: #8d6a08;
        font-size: 1.35rem;
      }

      .calculated-equity-card span {
        color: #6b7280;
        line-height: 1.5;
        font-size: 0.88rem;
      }

      .documents-summary {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 14px;
      }

      .documents-summary__metric {
        display: grid;
        gap: 6px;
        padding: 18px;
        border-radius: 22px;
        background: rgba(255, 255, 255, 0.9);
        border: 1px solid rgba(212, 166, 42, 0.14);
        box-shadow: 0 12px 24px rgba(15, 23, 42, 0.05);
      }

      .documents-summary__metric strong {
        font-size: 1.25rem;
        color: #111827;
      }

      .documents-summary__metric span {
        color: #6b7280;
        font-weight: 600;
      }

      .documents-summary__metric--warn strong {
        color: #b45309;
      }

      .documents-summary__metric--ok strong {
        color: #166534;
      }

      .documents-group {
        display: grid;
        gap: 14px;
      }

      .documents-group__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 14px;
      }

      .documents-group__header h3 {
        margin: 0 0 6px;
        color: #111827;
      }

      .documents-group__header p {
        margin: 0;
        color: #6b7280;
      }

      .document-list {
        display: grid;
        gap: 14px;
      }

      .document-card {
        display: grid;
        gap: 14px;
        padding: 18px;
        border-radius: 22px;
        border: 1px solid rgba(212, 166, 42, 0.14);
        background: rgba(255, 255, 255, 0.96);
        box-shadow: 0 12px 24px rgba(15, 23, 42, 0.05);
        transition:
          transform 0.18s ease,
          box-shadow 0.18s ease;
      }

      .document-card:hover {
        transform: translateY(-1px);
        box-shadow: 0 18px 30px rgba(15, 23, 42, 0.08);
      }

      .document-card--required {
        border-color: rgba(212, 166, 42, 0.24);
      }

      .document-card--optional {
        border-style: dashed;
      }

      .document-card__top {
        display: flex;
        justify-content: space-between;
        gap: 16px;
        align-items: flex-start;
      }

      .document-card__copy {
        display: grid;
        gap: 6px;
        min-width: 0;
      }

      .document-card__title-row {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;
      }

      .document-card__copy strong {
        color: #111827;
        font-size: 1rem;
      }

      .document-card__copy small {
        color: #6b7280;
        line-height: 1.6;
      }

      .document-card__copy em {
        color: #94a3b8;
        font-style: normal;
        font-size: 0.88rem;
      }

      .document-card__bottom {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        align-items: center;
        flex-wrap: wrap;
      }

      .status-badge {
        min-height: 30px;
        padding: 0 10px;
        border-radius: 999px;
        font-size: 0.74rem;
        font-weight: 800;
        display: inline-flex;
        align-items: center;
      }

      .status-badge--required {
        background: rgba(212, 166, 42, 0.18);
        color: #8d6a08;
      }

      .status-badge--optional {
        background: rgba(212, 166, 42, 0.1);
        color: #8d6a08;
      }

      .upload-state {
        min-height: 34px;
        padding: 0 12px;
        border-radius: 999px;
        font-weight: 800;
        font-size: 0.82rem;
        display: inline-flex;
        align-items: center;
        white-space: nowrap;
      }

      .upload-state--done {
        background: #ecfdf3;
        color: #166534;
      }

      .upload-state--pending {
        background: #fff7ed;
        color: #b45309;
      }

      .uploaded-file {
        min-height: 40px;
        display: inline-flex;
        align-items: center;
        padding: 0 12px;
        border-radius: 999px;
        background: #f8fafc;
        border: 1px solid rgba(229, 231, 235, 0.92);
      }

      .uploaded-file__name {
        font-weight: 700;
        color: #111827;
      }

      .upload-box {
        min-height: 88px;
        display: grid;
        place-items: center;
        gap: 8px;
        padding: 16px;
        border-radius: 20px;
        border: 1.5px dashed rgba(212, 166, 42, 0.36);
        background: linear-gradient(
          180deg,
          rgba(255, 247, 223, 0.76),
          rgba(255, 255, 255, 0.98)
        );
        cursor: pointer;
        text-align: center;
        transition: 0.18s ease;
      }

      .upload-box:hover {
        border-color: rgba(212, 166, 42, 0.62);
        transform: translateY(-1px);
        box-shadow: 0 16px 30px rgba(212, 166, 42, 0.12);
      }

      .upload-box--optional {
        background: linear-gradient(
          180deg,
          rgba(250, 250, 250, 0.96),
          rgba(255, 255, 255, 0.98)
        );
      }

      .upload-box__input {
        display: none;
      }

      .upload-box__text {
        font-weight: 800;
        color: #111827;
      }

      .document-actions {
        display: flex;
        gap: 8px;
        margin-top: 10px;
        flex-wrap: wrap;
      }

      .terms-box {
        display: flex;
        gap: 10px;
        align-items: flex-start;
        padding: 16px;
        border-radius: 18px;
        background: rgba(255, 255, 255, 0.9);
        border: 1px solid rgba(212, 166, 42, 0.14);
        font-weight: 600;
      }

      .missing-box {
        display: grid;
        gap: 6px;
        padding: 16px 18px;
        border-radius: 18px;
        background: #fff7ed;
        border: 1px solid #fed7aa;
        color: #9a3412;
      }

      .loading-screen {
        min-height: 360px;
        display: grid;
        place-items: center;
        align-content: center;
        gap: 16px;
        padding: 28px;
        border-radius: 30px;
        background: linear-gradient(
          180deg,
          rgba(255, 255, 255, 0.98),
          rgba(255, 250, 238, 0.98)
        );
        border: 1px solid rgba(212, 166, 42, 0.14);
        box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
        text-align: center;
      }

      .loading-screen__logo-wrap {
        width: 120px;
        height: 120px;
        position: relative;
        display: grid;
        place-items: center;
      }

      .loading-screen__orbit {
        position: absolute;
        inset: 0;
        border-radius: 50%;
        border: 8px solid rgba(212, 166, 42, 0.16);
        border-top-color: #d4a62a;
        border-right-color: #eac34d;
        animation: spin 1s linear infinite;
        box-shadow: 0 0 0 10px rgba(212, 166, 42, 0.06);
      }

      .loading-screen__logo {
        width: 76px;
        height: 76px;
        display: grid;
        place-items: center;
        border-radius: 50%;
        background: linear-gradient(180deg, #f7d774, #d4a62a);
        box-shadow: 0 16px 28px rgba(212, 166, 42, 0.22);
        padding: 14px;
        box-sizing: border-box;
      }

      .loading-screen__logo-image {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }

      .loading-screen h2 {
        margin: 0;
        color: #111827;
      }

      .loading-screen p {
        margin: 0;
        color: #6b7280;
      }

      .btn {
        min-height: 46px;
        padding: 0 18px;
        border: 0;
        border-radius: 999px;
        font: inherit;
        font-weight: 800;
        cursor: pointer;
        text-decoration: none;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        transition:
          transform 0.18s ease,
          box-shadow 0.18s ease,
          opacity 0.18s ease;
      }

      .btn:hover:not(:disabled) {
        transform: translateY(-1px);
      }

      .btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
        box-shadow: none;
      }

      .btn-primary {
        background: linear-gradient(180deg, #f7d774, #d4a62a);
        color: #5a4200;
        box-shadow: 0 12px 24px rgba(212, 166, 42, 0.18);
      }

      .btn-ghost {
        background: rgba(255, 255, 255, 0.94);
        color: #111827;
        border: 1px solid #e5e7eb;
      }

      .btn-sm {
        min-height: 36px;
        padding: 0 12px;
        border-radius: 14px;
        font-size: 0.9rem;
      }

      .btn-danger {
        color: #b91c1c;
      }

      .error,
      .success {
        padding: 16px 18px;
        border-radius: 18px;
        font-weight: 700;
        box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
      }

      .error {
        color: #b91c1c;
        background: #fff1f2;
        border: 1px solid #fecdd3;
      }

      .success {
        color: #166534;
        background: #f0fdf4;
        border: 1px solid #bbf7d0;
      }

      .animate-in {
        animation: fadeUp 0.36s ease both;
      }

      .animate-pop {
        animation: popIn 0.28s ease both;
      }

      @keyframes fadeUp {
        from {
          opacity: 0;
          transform: translateY(16px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      @keyframes popIn {
        from {
          opacity: 0;
          transform: scale(0.98) translateY(8px);
        }
        to {
          opacity: 1;
          transform: scale(1) translateY(0);
        }
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      @media (max-width: 1100px) {
        .page-header__top,
        .step-info-card,
        .documents-summary {
          grid-template-columns: 1fr;
        }

        .page-header__top {
          flex-direction: column;
          align-items: stretch;
        }

        .progress-circle {
          display: flex;
          justify-content: center;
        }
      }

      @media (max-width: 980px) {
        .gold-steps,
        .choice-grid,
        .form-grid,
        .friendly-grid,
        .story-grid,
        .friendly-grid,
        .story-grid,
        .documents-summary,
        .step-mini-stats,
        .profile-preview__grid,
        .rne-card__result,
        .rne-card__controls {
          grid-template-columns: 1fr;
        }

        .step-card__footer,
        .document-card__top,
        .document-card__bottom,
        .step-overview__meta,
        .profile-preview__header,
        .rne-card__header {
          align-items: stretch;
        }
      }
    `,
  ],
})
export class YouthApplicationRaiseFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly crowdfundingService = inject(CrowdfundingService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly sessionService = inject(SessionService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly location = inject(Location);

  readonly crowdfundingType = CrowdfundingType;
  readonly projectStage = ProjectStage;
  readonly projectStageOptions = Object.values(ProjectStage);
  readonly sectorOptions = SECTOR_OPTIONS;
  readonly maxTags = 3;
  readonly tagOptions = TAG_OPTIONS;
  readonly locationOptions = TUNISIA_LOCATION_OPTIONS;
  readonly governorateOptions = TUNISIA_LOCATION_OPTIONS.map(
    (item) => item.governorate,
  );

  readonly steps: readonly WizardStep[] = [
    {
      id: 1,
      label: 'Contact',
      caption: 'Who should we contact?',
      description:
        'Choose the contact person for this application. You can reuse your profile information or provide another contact for communication and follow-up.',
    },
    {
      id: 2,
      label: 'Type',
      caption: 'Donation or equity',
      description:
        'Select the type of raise you want to create. Donation is community support, while equity is investor-based fundraising with ownership details.',
    },
    {
      id: 3,
      label: 'Details',
      caption: 'Business information',
      description:
        'Add the core information of your initiative: project identity, stage, problem, solution, funding goal, and equity details when needed.',
    },
    {
      id: 4,
      label: 'Documents',
      caption: 'Upload and submit',
      description:
        'Upload all required documents, optionally add bonus files, confirm the information, and submit your application for review.',
    },
  ];

  readonly currentId = signal<number | null>(null);
  readonly lockedType = signal<CrowdfundingType | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly selectedTags = signal<AppTag[]>([]);
  readonly useProfileInfo = signal(true);
  readonly currentStep = signal<WizardStepId>(1);
  readonly furthestStep = signal<WizardStepId>(1);
  readonly documents = signal<ApplicationDocumentResponse[]>([]);
  readonly rneLoading = signal(false);
  readonly rneError = signal<string | null>(null);
  readonly rnePreview = signal<RneShortDetailsResponse | null>(null);
  readonly attemptedTagValidation = signal(false);
  readonly gpsLoading = signal(false);
  readonly gpsError = signal<string | null>(null);
  readonly formVersion = signal(0);

  readonly totalCompletionItems = computed(() => {
    this.formVersion();

    let total = 0;
    total += 3; // contact essentials
    total += 1; // type
    total += 13; // main details
    total += 1; // tags
    total += 1; // accepted terms
    total += this.requiredDocuments().length;

    if (this.currentType() === CrowdfundingType.EQUITY) {
      total += 5;
    }

    return total;
  });

  readonly completedCompletionItems = computed(() => {
    this.formVersion();

    let completed = 0;

    if (this.isControlCompleted('contactFirstName')) completed++;
    if (this.isControlCompleted('contactLastName')) completed++;
    if (this.isControlCompleted('contactEmail')) completed++;
    if (this.currentType()) completed++;

    [
      'businessName',
      'sector',
      'subSector',
      'fundingGoal',
      'stage',
      'teamSize',
      'governorate',
      'city',
      'summary',
      'problemStatement',
      'solution',
      'targetCustomers',
      'useOfFunds',
    ].forEach((name) => {
      if (this.isControlCompleted(name)) completed++;
    });

    if (this.selectedTags().length > 0) completed++;
    if (this.form.controls.acceptedTerms.value) completed++;
    completed += this.requiredUploadedCount();

    if (this.currentType() === CrowdfundingType.EQUITY) {
      [
        'companyLegalName',
        'companyRegistrationNumber',
        'cnreProfileUrl',
        'preMoneyValuation',
        'minInvestment',
      ].forEach((name) => {
        if (this.isControlCompleted(name)) completed++;
      });
    }

    return completed;
  });

  readonly progressPercent = computed(() => {
    const total = this.totalCompletionItems();
    return total > 0
      ? Math.round((this.completedCompletionItems() / total) * 100)
      : 0;
  });
  readonly currentStepData = computed(
    () =>
      this.steps.find((step) => step.id === this.currentStep()) ??
      this.steps[0],
  );

  readonly form = this.fb.group({
    type: [
      CrowdfundingType.DONATION as CrowdfundingType | null,
      [Validators.required],
    ],

    contactFirstName: [
      '',
      [Validators.required, Validators.minLength(2), Validators.maxLength(80)],
    ],
    contactLastName: [
      '',
      [Validators.required, Validators.minLength(2), Validators.maxLength(80)],
    ],
    contactTitle: ['', [Validators.maxLength(120)]],
    contactEmail: [
      '',
      [Validators.required, Validators.email, Validators.maxLength(180)],
    ],
    contactPhone: ['', [Validators.maxLength(30)]],

    businessName: [
      '',
      [Validators.required, Validators.minLength(2), Validators.maxLength(160)],
    ],
    website: ['', [Validators.maxLength(255)]],
    sector: ['' as Sector | '' | null, [Validators.required]],
    subSector: ['' as SubSector | '' | null, [Validators.required]],
    summary: [
      '',
      [
        Validators.required,
        Validators.minLength(10),
        Validators.maxLength(255),
      ],
    ],
    fundingGoal: [
      null as number | null,
      [Validators.required, Validators.min(500)],
    ],
    customerCount: [null as number | null, [Validators.min(0)]],
    stage: ['' as ProjectStage | '' | null, [Validators.required]],
    teamSize: [null as number | null, [Validators.min(1), Validators.max(500)]],
    governorate: ['', [Validators.required, Validators.maxLength(80)]],
    city: ['', [Validators.required, Validators.maxLength(80)]],
    problemStatement: [
      '',
      [
        Validators.required,
        Validators.minLength(20),
        Validators.maxLength(3000),
      ],
    ],
    solution: [
      '',
      [
        Validators.required,
        Validators.minLength(20),
        Validators.maxLength(3000),
      ],
    ],
    targetCustomers: [
      '',
      [
        Validators.required,
        Validators.minLength(10),
        Validators.maxLength(2000),
      ],
    ],
    useOfFunds: [
      '',
      [
        Validators.required,
        Validators.minLength(20),
        Validators.maxLength(3000),
      ],
    ],

    acceptedTerms: [false],

    rneId: ['', [Validators.maxLength(30)]],

    companyLegalName: ['', [Validators.maxLength(160)]],
    companyRegistrationNumber: ['', [Validators.maxLength(120)]],
    cnreProfileUrl: ['', [Validators.maxLength(255)]],
    equityOfferedPercent: [null as number | null],
    preMoneyValuation: [null as number | null, [Validators.min(0)]],
    minInvestment: [null as number | null, [Validators.min(0)]],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const queryType = this.route.snapshot.queryParamMap.get('type');

    if (queryType?.toUpperCase() === CrowdfundingType.EQUITY) {
      this.form.patchValue({ type: CrowdfundingType.EQUITY });
    }

    this.form.controls.sector.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((sector) => {
        const currentSubSector = this.form.controls.subSector.value as
          | SubSector
          | ''
          | null;
        const allowed = sector
          ? (SUB_SECTOR_OPTIONS_BY_SECTOR[sector as Sector] ?? [])
          : [];
        if (
          currentSubSector &&
          !allowed.includes(currentSubSector as SubSector)
        ) {
          this.form.patchValue({ subSector: '' });
        }
      });

    this.form.controls.governorate.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((governorate) => {
        const currentCity = this.normalizeOptional(
          this.form.controls.city.value,
        );
        const cities =
          this.locationOptions.find((item) => item.governorate === governorate)
            ?.cities ?? [];

        if (currentCity && !cities.includes(currentCity)) {
          this.form.patchValue({ city: '' });
        }
      });

    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.formVersion.update((value) => value + 1));

    this.updateEquityValidators();

    if (id) {
      this.currentId.set(Number(id));
      void this.loadDraft(Number(id));
      return;
    }

    void this.hydrateProfileContact();
  }

  isEditMode(): boolean {
    return this.currentId() !== null;
  }

  currentType(): CrowdfundingType | null {
    return this.form.controls.type.value ?? null;
  }

  availableSubSectors(): SubSector[] {
    const sector = this.form.controls.sector.value as Sector | '' | null;
    return sector
      ? [...(SUB_SECTOR_OPTIONS_BY_SECTOR[sector as Sector] ?? [])]
      : [];
  }

  availableCities(): string[] {
    const governorate = this.normalizeOptional(
      this.form.controls.governorate.value,
    );

    if (!governorate) {
      return [];
    }

    return (
      this.locationOptions.find((item) => item.governorate === governorate)
        ?.cities ?? []
    );
  }

  documentCatalog(): readonly DocumentRequirement[] {
    return this.currentType() === CrowdfundingType.EQUITY
      ? EQUITY_DOCUMENTS
      : DONATION_DOCUMENTS;
  }

  requiredDocuments(): readonly DocumentRequirement[] {
    return this.documentCatalog().filter((doc) => doc.required);
  }

  optionalDocuments(): readonly DocumentRequirement[] {
    return this.documentCatalog().filter((doc) => !doc.required);
  }

  requiredUploadedCount(): number {
    return this.requiredDocuments().filter((doc) => this.documentFor(doc.type))
      .length;
  }

  missingRequiredDocuments(): readonly DocumentRequirement[] {
    return this.requiredDocuments().filter(
      (doc) => !this.documentFor(doc.type),
    );
  }

  missingRequiredDocumentLabels(): string {
    return this.missingRequiredDocuments()
      .map((doc) => doc.label)
      .join(', ');
  }

  canSubmitDocuments(): boolean {
    return (
      this.missingRequiredDocuments().length === 0 &&
      !!this.form.controls.acceptedTerms.value
    );
  }

  documentFor(type: DocumentType): ApplicationDocumentResponse | undefined {
    return this.documents().find((item) => item.docType === type);
  }

  displayValue(value: string | null | undefined): string {
    const normalized = this.normalizeOptional(value);
    return normalized ?? 'Not available';
  }

  control(name: string) {
    return this.form.get(name);
  }

  showControlError(name: string): boolean {
    const control = this.control(name);
    return !!control && control.invalid && control.touched;
  }

  controlError(name: string): string {
    const control = this.control(name);
    if (!control?.errors) {
      return '';
    }

    if (control.errors['required']) return 'This field is required.';
    if (control.errors['email']) return 'Enter a valid email address.';
    if (control.errors['minlength'])
      return `Minimum ${control.errors['minlength'].requiredLength} characters required.`;
    if (control.errors['maxlength'])
      return `Maximum ${control.errors['maxlength'].requiredLength} characters allowed.`;
    if (control.errors['min'])
      return `Value must be at least ${control.errors['min'].min}.`;
    if (control.errors['max'])
      return `Value must be at most ${control.errors['max'].max}.`;

    return 'This field is invalid.';
  }

  showTagError(): boolean {
    return this.attemptedTagValidation() && this.selectedTags().length === 0;
  }

  isTagDisabled(tag: AppTag): boolean {
    return (
      !this.selectedTags().includes(tag) &&
      this.selectedTags().length >= this.maxTags
    );
  }

  characterCount(controlName: string): number {
    this.formVersion();
    const value = this.control(controlName)?.value;
    return typeof value === 'string' ? value.length : 0;
  }

  isNearCharacterLimit(controlName: string, maxLength: number): boolean {
    return this.characterCount(controlName) >= Math.floor(maxLength * 0.8);
  }

  private isControlCompleted(controlName: string): boolean {
    this.formVersion();
    const control = this.control(controlName);

    if (!control || control.invalid) {
      return false;
    }

    const value = control.value;

    if (typeof value === 'string') {
      return value.trim().length > 0;
    }

    if (typeof value === 'number') {
      return Number.isFinite(value);
    }

    if (typeof value === 'boolean') {
      return value;
    }

    return value != null;
  }

  isContactStepReady(): boolean {
    if (this.useProfileInfo()) {
      return (
        !!this.form.controls.contactFirstName.valid &&
        !!this.form.controls.contactLastName.valid &&
        !!this.form.controls.contactEmail.valid
      );
    }

    return (
      !!this.form.controls.contactFirstName.valid &&
      !!this.form.controls.contactLastName.valid &&
      !!this.form.controls.contactEmail.valid &&
      !!this.form.controls.contactTitle.valid &&
      !!this.form.controls.contactPhone.valid
    );
  }

  isTypeStepReady(): boolean {
    return !!this.currentType();
  }

  isTypeOptionDisabled(type: CrowdfundingType): boolean {
    const locked = this.lockedType();
    return !!locked && locked !== type;
  }

  isDetailsStepReady(): boolean {
    const baseReady =
      !!this.form.controls.businessName.valid &&
      !!this.form.controls.sector.valid &&
      !!this.form.controls.subSector.valid &&
      !!this.form.controls.summary.valid &&
      !!this.form.controls.problemStatement.valid &&
      !!this.form.controls.solution.valid &&
      !!this.form.controls.targetCustomers.valid &&
      !!this.form.controls.useOfFunds.valid &&
      !!this.form.controls.stage.valid &&
      !!this.form.controls.fundingGoal.valid &&
      !!this.form.controls.website.valid &&
      !!this.form.controls.customerCount.valid &&
      !!this.form.controls.teamSize.valid &&
      !!this.form.controls.governorate.valid &&
      !!this.form.controls.city.valid &&
      this.selectedTags().length > 0 &&
      this.selectedTags().length <= this.maxTags;

    if (this.currentType() !== CrowdfundingType.EQUITY) {
      return baseReady;
    }

    return (
      baseReady &&
      !!this.normalizeOptional(this.form.controls.companyLegalName.value) &&
      !!this.normalizeOptional(
        this.form.controls.companyRegistrationNumber.value,
      ) &&
      !!this.normalizeOptional(this.form.controls.cnreProfileUrl.value) &&
      !!this.form.controls.companyLegalName.valid &&
      !!this.form.controls.companyRegistrationNumber.valid &&
      !!this.form.controls.cnreProfileUrl.valid &&
      this.form.controls.preMoneyValuation.value != null &&
      this.form.controls.minInvestment.value != null &&
      !!this.form.controls.preMoneyValuation.valid &&
      !!this.form.controls.minInvestment.valid &&
      this.calculatedEquityOfferedPercent() !== null
    );
  }

  calculatedEquityOfferedPercent(): number | null {
    const fundingGoal = this.form.controls.fundingGoal.value;
    const preMoneyValuation = this.form.controls.preMoneyValuation.value;

    if (
      fundingGoal == null ||
      preMoneyValuation == null ||
      fundingGoal <= 0 ||
      preMoneyValuation <= 0
    ) {
      return null;
    }

    const postMoneyValuation = preMoneyValuation + fundingGoal;
    return Number(((fundingGoal / postMoneyValuation) * 100).toFixed(3));
  }

  calculatedEquityOfferedPercentLabel(): string {
    const value = this.calculatedEquityOfferedPercent();
    return value == null ? 'Enter valuation first' : `${value}%`;
  }

  canFetchRne(): boolean {
    const raw = this.normalizeOptional(this.form.controls.rneId.value);
    return !!raw && raw.length >= 4;
  }

  goToStep(step: WizardStepId): void {
    if (step <= this.furthestStep()) {
      this.currentStep.set(step);
      this.clearMessages();
    }
  }

  goToPreviousStep(): void {
    const previous = Math.max(1, this.currentStep() - 1) as WizardStepId;
    this.currentStep.set(previous);
    this.clearMessages();
  }

  setType(type: CrowdfundingType): void {
    if (this.isTypeOptionDisabled(type)) {
      return;
    }

    this.form.patchValue({ type });
    this.clearMessages();
    this.rneError.set(null);

    if (type !== CrowdfundingType.EQUITY) {
      this.rnePreview.set(null);
    }

    this.updateEquityValidators();
  }

  toggleTag(tag: AppTag): void {
    const current = this.selectedTags();

    if (current.includes(tag)) {
      this.selectedTags.set(current.filter((item) => item !== tag));
      return;
    }

    if (current.length >= this.maxTags) {
      this.error.set(`Choose up to ${this.maxTags} tags only.`);
      return;
    }

    this.clearMessages();
    this.selectedTags.set([...current, tag]);
  }

  selectContactSource(useProfile: boolean): void {
    this.useProfileInfo.set(useProfile);
    this.clearMessages();

    if (useProfile) {
      void this.hydrateProfileContact();
    } else {
      [
        'contactFirstName',
        'contactLastName',
        'contactTitle',
        'contactEmail',
        'contactPhone',
      ].forEach((name) => this.control(name)?.markAsUntouched());
    }
  }

  sanitizeAlphaField(controlName: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const cleaned = input.value
      .replace(/[^a-zA-ZÀ-ÿ'\\-\\s]/g, '')
      .replace(/\\s{2,}/g, ' ');
    this.patchSanitizedValue(controlName, cleaned, input);
  }

  sanitizeTextField(
    controlName: string,
    event: Event,
    maxLength: number,
  ): void {
    const input = event.target as HTMLInputElement | HTMLTextAreaElement;
    const cleaned = input.value.replace(/\\s{2,}/g, ' ').slice(0, maxLength);
    this.patchSanitizedValue(controlName, cleaned, input);
  }

  sanitizeEmailField(controlName: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const cleaned = input.value
      .replace(/\\s+/g, '')
      .toLowerCase()
      .slice(0, 180);
    this.patchSanitizedValue(controlName, cleaned, input);
  }

  sanitizePhoneField(controlName: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const cleaned = input.value.replace(/[^0-9+\\-\\s()]/g, '').slice(0, 30);
    this.patchSanitizedValue(controlName, cleaned, input);
  }

  sanitizeUrlField(controlName: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const cleaned = input.value.replace(/\\s+/g, '').slice(0, 255);
    this.patchSanitizedValue(controlName, cleaned, input);
  }

  sanitizeCodeField(
    controlName: string,
    event: Event,
    maxLength: number,
  ): void {
    const input = event.target as HTMLInputElement;
    const cleaned = input.value
      .replace(/[^a-zA-Z0-9\\-_/\\.\\s]/g, '')
      .slice(0, maxLength);
    this.patchSanitizedValue(controlName, cleaned, input);
  }

  adjustNumericControl(
    controlName: string,
    delta: number,
    min: number,
    max: number,
  ): void {
    const control = this.control(controlName);
    const current = Number(control?.value ?? 0);
    const next = Math.min(
      max,
      Math.max(min, (Number.isFinite(current) ? current : 0) + delta),
    );

    this.form.patchValue({ [controlName]: next });
    control?.markAsTouched();
    control?.updateValueAndValidity();
  }

  sanitizeIntegerField(controlName: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const raw = input.value.replace(/[^0-9]/g, '');
    this.patchNumericValue(controlName, raw, input, false);
  }

  sanitizeDecimalField(
    controlName: string,
    event: Event,
    decimals: number,
  ): void {
    const input = event.target as HTMLInputElement;
    const raw = input.value.replace(/[^0-9.]/g, '');
    const parts = raw.split('.');
    const cleaned =
      parts.length <= 1
        ? parts[0]
        : `${parts[0]}.${parts.slice(1).join('').slice(0, decimals)}`;
    this.patchNumericValue(controlName, cleaned, input, true);
  }

  sanitizePercentageField(controlName: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const raw = input.value.replace(/[^0-9.]/g, '');
    const parts = raw.split('.');
    const cleaned =
      parts.length <= 1
        ? parts[0]
        : `${parts[0]}.${parts.slice(1).join('').slice(0, 3)}`;

    let value: number | null = cleaned ? Number(cleaned) : null;
    if (value !== null && Number.isFinite(value)) {
      value = Math.min(100, Math.max(0, value));
    } else {
      value = null;
    }

    this.form.patchValue({ [controlName]: value }, { emitEvent: false });
    input.value = value === null ? '' : String(value);
  }

  async fetchRneDetails(): Promise<void> {
    const rneId = this.normalizeOptional(this.form.controls.rneId.value);

    if (!rneId) {
      this.rneError.set(
        'Enter the RNE identifier before verifying the company.',
      );
      return;
    }

    this.rneLoading.set(true);
    this.rneError.set(null);

    try {
      const result = await firstValueFrom(
        this.http.get<RneShortDetailsResponse>(
          `/api/crowdfunding/rne/short-details/${encodeURIComponent(rneId)}`,
        ),
      );

      this.rnePreview.set(result);

      const companyLegalName =
        this.normalizeOptional(result.denominationLatin) ||
        this.normalizeOptional(result.denomination) ||
        this.normalizeOptional(result.nomCommercialFr) ||
        this.normalizeOptional(result.nomCommercialAr) ||
        '';

      const registryId = this.normalizeOptional(result.idUnique) || rneId;
      const cnreProfileUrl = this.buildCnrePublicProfileUrl(registryId);

      this.form.patchValue({
        companyLegalName,
        companyRegistrationNumber: registryId,
        cnreProfileUrl,
        businessName:
          this.normalizeOptional(this.form.controls.businessName.value) ||
          companyLegalName,
      });

      [
        'companyLegalName',
        'companyRegistrationNumber',
        'cnreProfileUrl',
        'businessName',
      ].forEach((name) => {
        this.control(name)?.markAsTouched();
        this.control(name)?.updateValueAndValidity();
      });

      this.success.set(
        'Company verified. Official RNE details were applied to the application.',
      );
    } catch {
      this.rneError.set(
        'Company verification failed. Check the RNE identifier and try again.',
      );
    } finally {
      this.rneLoading.set(false);
    }
  }

  private buildCnrePublicProfileUrl(registryId: string): string {
    const cleanRegistryId = encodeURIComponent(registryId.trim());

    return `https://www.registre-entreprises.tn/rne-public/#/recherche-pm/recherche-resultat/${cleanRegistryId}`;
  }

  async fillLocationFromGps(): Promise<void> {
    if (!navigator.geolocation) {
      this.gpsError.set(
        'Geolocation is not supported on this device or browser.',
      );
      return;
    }

    this.gpsLoading.set(true);
    this.gpsError.set(null);
    this.clearMessages();

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const { latitude, longitude } = position.coords;
          const result = await firstValueFrom(
            this.http.get<ReverseGeocodeResponse>(
              `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}&accept-language=en`,
            ),
          );

          const address = result.address ?? {};

          const rawGovernorate =
            this.normalizeOptional(address.state) ||
            this.normalizeOptional(address.state_district) ||
            this.normalizeOptional(address.region) ||
            this.normalizeOptional(address.county);

          const rawCity =
            this.normalizeOptional(address.city) ||
            this.normalizeOptional(address.town) ||
            this.normalizeOptional(address.village) ||
            this.normalizeOptional(address.municipality) ||
            this.normalizeOptional(address.county);

          const governorate = this.matchGovernorate(rawGovernorate);
          const city = this.matchCity(rawCity, governorate);

          if (!governorate) {
            throw new Error('Governorate could not be matched.');
          }

          this.form.patchValue({
            governorate,
            city: city ?? '',
          });

          ['governorate', 'city'].forEach((name) => {
            this.control(name)?.markAsTouched();
            this.control(name)?.updateValueAndValidity();
          });

          this.success.set(
            'City and governorate filled from your current location.',
          );
        } catch {
          this.gpsError.set(
            'Unable to match your GPS location to the governorate and city list.',
          );
        } finally {
          this.gpsLoading.set(false);
        }
      },
      () => {
        this.gpsLoading.set(false);
        this.gpsError.set(
          'Location access was blocked. Please allow GPS access and try again.',
        );
      },
      {
        enableHighAccuracy: true,
        timeout: 12000,
        maximumAge: 300000,
      },
    );
  }

  private matchGovernorate(value: string | null): string | null {
    if (!value) {
      return null;
    }

    const normalizedValue = this.normalizeLocationName(value);

    return (
      this.locationOptions.find((item) => {
        const normalizedGovernorate = this.normalizeLocationName(
          item.governorate,
        );
        return (
          normalizedValue.includes(normalizedGovernorate) ||
          normalizedGovernorate.includes(normalizedValue)
        );
      })?.governorate ?? null
    );
  }

  private matchCity(
    value: string | null,
    governorate: string | null,
  ): string | null {
    if (!value || !governorate) {
      return null;
    }

    const normalizedValue = this.normalizeLocationName(value);
    const cities =
      this.locationOptions.find((item) => item.governorate === governorate)
        ?.cities ?? [];

    return (
      cities.find((city) => {
        const normalizedCity = this.normalizeLocationName(city);
        return (
          normalizedValue.includes(normalizedCity) ||
          normalizedCity.includes(normalizedValue)
        );
      }) ?? null
    );
  }

  private normalizeLocationName(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .toLowerCase()
      .replace(/governorate/g, '')
      .replace(/gouvernorat/g, '')
      .replace(/[^a-z0-9]/g, '');
  }

  async continueFromContact(): Promise<void> {
    this.markContactStepTouched();

    if (!this.isContactStepReady()) {
      this.error.set('Please complete the required contact information.');
      return;
    }

    this.clearMessages();
    this.saving.set(true);

    try {
      const id = await this.ensureDraftExists();

      const saved = await firstValueFrom(
        this.crowdfundingService.saveContactStep(id, {
          useProfileContact: this.useProfileInfo(),
          contactFirstName: this.form.controls.contactFirstName.value!.trim(),
          contactLastName: this.form.controls.contactLastName.value!.trim(),
          contactTitle: this.normalizeOptional(
            this.form.controls.contactTitle.value,
          ),
          contactEmail: this.form.controls.contactEmail.value!.trim(),
          contactPhone: this.normalizeOptional(
            this.form.controls.contactPhone.value,
          ),
        }),
      );

      this.applyLoadedApplication(saved);
      this.advanceToStep(2);
    } catch (error) {
      this.error.set(
        this.extractErrorMessage(error, 'Unable to save contact information.'),
      );
    } finally {
      this.saving.set(false);
    }
  }

  async continueFromType(): Promise<void> {
    if (!this.isTypeStepReady()) {
      this.error.set('Please choose a raise type.');
      return;
    }

    this.clearMessages();
    this.saving.set(true);

    try {
      const id = await this.ensureDraftExists();

      const saved = await firstValueFrom(
        this.crowdfundingService.saveTypeStep(id, {
          type: this.currentType()!,
        }),
      );

      this.applyLoadedApplication(saved);
      this.lockedType.set(saved.type ?? this.currentType());
      this.advanceToStep(3);
    } catch (error) {
      this.error.set(
        this.extractErrorMessage(error, 'Unable to save raise type.'),
      );
    } finally {
      this.saving.set(false);
    }
  }

  async continueFromDetails(): Promise<void> {
    this.markDetailsStepTouched();
    this.attemptedTagValidation.set(true);

    if (!this.isDetailsStepReady()) {
      this.error.set(
        'Please complete the required details for this application.',
      );
      return;
    }

    this.clearMessages();
    this.saving.set(true);

    try {
      const id = await this.ensureDraftExists();
      const saved = await firstValueFrom(
        this.crowdfundingService.saveDetailsStep(
          id,
          this.buildDetailsPayload(false),
        ),
      );

      this.applyLoadedApplication(saved);
      await this.refreshDocuments();
      this.advanceToStep(4);
    } catch (error) {
      this.error.set(
        this.extractErrorMessage(error, 'Unable to save application details.'),
      );
    } finally {
      this.saving.set(false);
    }
  }

  async submitApplication(): Promise<void> {
    if (!this.currentId()) {
      this.error.set('Complete the earlier steps first.');
      return;
    }

    if (!this.form.controls.acceptedTerms.value) {
      this.error.set(
        'Please confirm the information before submission. Optional bonus documents are not required.',
      );
      this.form.controls.acceptedTerms.markAsTouched();
      return;
    }

    this.clearMessages();
    this.saving.set(true);

    try {
      await firstValueFrom(
        this.crowdfundingService.saveDetailsStep(
          this.currentId()!,
          this.buildDetailsPayload(true),
        ),
      );

      const submitted = await firstValueFrom(
        this.crowdfundingService.submitDraft(this.currentId()!),
      );

      this.applyLoadedApplication(submitted);
      this.success.set('Application submitted successfully.');
    } catch (error) {
      this.error.set(
        this.extractErrorMessage(error, 'Unable to submit the application.'),
      );
    } finally {
      this.saving.set(false);
    }
  }

  async onFileSelected(type: DocumentType, event: Event): Promise<void> {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file || !this.currentId()) {
      return;
    }

    this.clearMessages();
    this.saving.set(true);

    try {
      await firstValueFrom(
        this.crowdfundingService.uploadDocument(this.currentId()!, type, file),
      );
      await this.refreshDocuments();
      this.success.set(`${this.formatLabel(type)} uploaded.`);
    } catch (error) {
      this.error.set(
        this.extractErrorMessage(error, 'Unable to upload document.'),
      );
    } finally {
      this.saving.set(false);
      (event.target as HTMLInputElement).value = '';
    }
  }

  async removeDocument(type: DocumentType): Promise<void> {
    if (!this.currentId()) {
      return;
    }

    this.clearMessages();
    this.saving.set(true);

    try {
      await firstValueFrom(
        this.crowdfundingService.deleteDocument(this.currentId()!, type),
      );
      await this.refreshDocuments();
      this.success.set(`${this.formatLabel(type)} removed.`);
    } catch (error) {
      this.error.set(
        this.extractErrorMessage(error, 'Unable to remove document.'),
      );
    } finally {
      this.saving.set(false);
    }
  }

  async previewDocument(type: DocumentType): Promise<void> {
    if (!this.currentId()) {
      return;
    }

    try {
      const blob = await firstValueFrom(
        this.crowdfundingService.fetchDocumentBlob(this.currentId()!, type),
      );

      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener,noreferrer');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch {
      this.error.set('Unable to open this document.');
    }
  }

  formatLabel(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private async loadDraft(id: number): Promise<void> {
    this.loading.set(true);
    this.clearMessages();

    try {
      const app = await firstValueFrom(
        this.crowdfundingService.getApplicationById(id),
      );
      this.applyLoadedApplication(app);

      const step = this.stepFromResponse(app);
      this.currentStep.set(step);
      this.furthestStep.set(step);

      await this.refreshDocuments();
    } catch {
      this.error.set('Unable to load this application.');
    } finally {
      this.loading.set(false);
    }
  }

  private applyLoadedApplication(app: ApplicationRaiseResponse): void {
    this.currentId.set(app.id);

    if (
      app.type &&
      app.draftStep &&
      app.draftStep !== ApplicationRaiseDraftStep.CONTACT &&
      app.draftStep !== ApplicationRaiseDraftStep.TYPE
    ) {
      this.lockedType.set(app.type);
    }

    this.useProfileInfo.set(app.useProfileContact ?? true);
    this.selectedTags.set(app.tags ?? []);
    this.documents.set(app.documents ?? []);

    this.form.patchValue({
      type: app.type ?? this.form.controls.type.value,
      businessName: app.businessName ?? '',
      website: app.website ?? '',
      sector: app.sector ?? '',
      subSector: app.subSector ?? '',
      summary: app.summary ?? '',
      problemStatement: app.problemStatement ?? '',
      solution: app.solution ?? '',
      targetCustomers: app.targetCustomers ?? '',
      useOfFunds: app.useOfFunds ?? '',
      stage: app.stage ?? '',
      fundingGoal: app.fundingGoal ?? null,
      customerCount: app.customerCount ?? null,
      teamSize: app.teamSize ?? null,
      governorate: app.governorate ?? '',
      city: app.city ?? '',

      contactFirstName: app.contactFirstName ?? '',
      contactLastName: app.contactLastName ?? '',
      contactTitle: app.contactTitle ?? '',
      contactEmail: app.contactEmail ?? this.sessionService.email() ?? '',
      contactPhone: app.contactPhone ?? '',

      acceptedTerms: app.acceptedTerms ?? false,

      companyLegalName: app.equityDetail?.companyLegalName ?? '',
      companyRegistrationNumber:
        app.equityDetail?.companyRegistrationNumber ?? '',
      cnreProfileUrl: app.equityDetail?.cnreProfileUrl ?? '',
      equityOfferedPercent: app.equityDetail?.equityOfferedPercent ?? null,
      preMoneyValuation: app.equityDetail?.preMoneyValuation ?? null,
      minInvestment: app.equityDetail?.minInvestment ?? null,
    });
  }

  private async refreshDocuments(): Promise<void> {
    if (!this.currentId()) {
      this.documents.set([]);
      return;
    }

    const docs = await firstValueFrom(
      this.crowdfundingService.listDocuments(this.currentId()!),
    );
    this.documents.set(docs);
  }

  private async ensureDraftExists(): Promise<number> {
    if (this.currentId()) {
      return this.currentId()!;
    }

    const created = await firstValueFrom(
      this.crowdfundingService.createEmptyDraft(),
    );
    this.currentId.set(created.id);
    this.location.replaceState(`/youth/applications/${created.id}/edit`);

    return created.id;
  }

  private async hydrateProfileContact(): Promise<void> {
    try {
      const profile = await firstValueFrom(
        this.userProfileService.getMyProfile(),
      );

      this.form.patchValue({
        contactFirstName: this.cleanAlpha(profile.firstName ?? ''),
        contactLastName: this.cleanAlpha(profile.lastName ?? ''),
        contactTitle: this.cleanText(profile.occupation ?? '', 120),
        contactEmail: this.cleanEmail(this.sessionService.email() ?? ''),
        contactPhone: this.cleanPhone(profile.phone ?? ''),
      });
    } catch {
      this.error.set('Unable to retrieve profile information.');
    }
  }

  private buildDetailsPayload(
    acceptedTerms: boolean,
  ): ApplicationRaiseDetailsStepRequest {
    return {
      businessName: this.form.controls.businessName.value!.trim(),
      website: this.normalizeOptional(this.form.controls.website.value),
      sector: this.form.controls.sector.value as Sector,
      subSector: this.form.controls.subSector.value as SubSector,
      tags: this.selectedTags(),
      stage: this.form.controls.stage.value as ProjectStage,
      summary: this.form.controls.summary.value!.trim(),
      problemStatement: this.form.controls.problemStatement.value!.trim(),
      solution: this.form.controls.solution.value!.trim(),
      targetCustomers: this.form.controls.targetCustomers.value!.trim(),
      useOfFunds: this.form.controls.useOfFunds.value!.trim(),
      fundingGoal: this.form.controls.fundingGoal.value ?? null,
      customerCount: this.form.controls.customerCount.value ?? null,
      teamSize: this.form.controls.teamSize.value ?? null,
      governorate: this.normalizeOptional(this.form.controls.governorate.value),
      city: this.normalizeOptional(this.form.controls.city.value),
      equityDetail:
        this.currentType() === CrowdfundingType.EQUITY
          ? this.buildEquityDetailPayload()
          : null,
      acceptedTerms,
    };
  }

  private buildEquityDetailPayload(): EquityDetailUpsertRequest {
    return {
      companyLegalName: this.form.controls.companyLegalName.value?.trim() ?? '',
      companyRegistrationNumber:
        this.form.controls.companyRegistrationNumber.value?.trim() ?? '',
      cnreProfileUrl: this.form.controls.cnreProfileUrl.value?.trim() ?? '',
      preMoneyValuation: this.form.controls.preMoneyValuation.value ?? null,
      minInvestment: this.form.controls.minInvestment.value ?? null,
    };
  }

  private markContactStepTouched(): void {
    [
      'contactFirstName',
      'contactLastName',
      'contactEmail',
      'contactTitle',
      'contactPhone',
    ].forEach((name) => {
      this.control(name)?.markAsTouched();
      this.control(name)?.updateValueAndValidity();
    });
  }

  private markDetailsStepTouched(): void {
    this.updateEquityValidators();

    [
      'businessName',
      'website',
      'sector',
      'subSector',
      'summary',
      'problemStatement',
      'solution',
      'targetCustomers',
      'useOfFunds',
      'stage',
      'fundingGoal',
      'customerCount',
      'teamSize',
      'governorate',
      'city',
      'companyLegalName',
      'companyRegistrationNumber',
      'cnreProfileUrl',
      'preMoneyValuation',
      'minInvestment',
    ].forEach((name) => {
      this.control(name)?.markAsTouched();
      this.control(name)?.updateValueAndValidity();
    });
  }

  private updateEquityValidators(): void {
    if (this.currentType() === CrowdfundingType.EQUITY) {
      this.form.controls.companyLegalName.setValidators([
        Validators.required,
        Validators.maxLength(160),
      ]);
      this.form.controls.companyRegistrationNumber.setValidators([
        Validators.required,
        Validators.maxLength(120),
      ]);
      this.form.controls.cnreProfileUrl.setValidators([
        Validators.required,
        Validators.maxLength(800),
      ]);
      this.form.controls.preMoneyValuation.setValidators([
        Validators.required,
        Validators.min(1),
      ]);
      this.form.controls.minInvestment.setValidators([
        Validators.required,
        Validators.min(1),
      ]);
    } else {
      this.form.controls.companyLegalName.setValidators([
        Validators.maxLength(160),
      ]);
      this.form.controls.companyRegistrationNumber.setValidators([
        Validators.maxLength(120),
      ]);
      this.form.controls.cnreProfileUrl.setValidators([
        Validators.maxLength(800),
      ]);
      this.form.controls.preMoneyValuation.setValidators([Validators.min(0)]);
      this.form.controls.minInvestment.setValidators([Validators.min(0)]);
    }

    [
      this.form.controls.companyLegalName,
      this.form.controls.companyRegistrationNumber,
      this.form.controls.cnreProfileUrl,
      this.form.controls.preMoneyValuation,
      this.form.controls.minInvestment,
    ].forEach((control) => control.updateValueAndValidity());
  }

  private stepFromResponse(app: ApplicationRaiseResponse): WizardStepId {
    if (app.draftStep) {
      switch (app.draftStep) {
        case ApplicationRaiseDraftStep.CONTACT:
          return 1;
        case ApplicationRaiseDraftStep.TYPE:
          return 2;
        case ApplicationRaiseDraftStep.DETAILS:
          return 3;
        case ApplicationRaiseDraftStep.DOCUMENTS:
          return 4;
      }
    }

    if (app.documents?.length) return 4;
    if (app.businessName || app.summary) return 3;
    if (app.type) return 2;
    return 1;
  }

  private advanceToStep(step: WizardStepId): void {
    this.currentStep.set(step);
    this.furthestStep.set(Math.max(this.furthestStep(), step) as WizardStepId);
  }

  private patchSanitizedValue(
    controlName: string,
    value: string,
    input: HTMLInputElement | HTMLTextAreaElement,
  ): void {
    this.form.patchValue({ [controlName]: value }, { emitEvent: false });
    input.value = value;
  }

  private patchNumericValue(
    controlName: string,
    value: string,
    input: HTMLInputElement,
    allowDecimal: boolean,
  ): void {
    const parsed =
      value === ''
        ? null
        : allowDecimal
          ? Number(value)
          : Number.parseInt(value, 10);

    const finalValue =
      parsed !== null && Number.isFinite(parsed) ? parsed : null;
    this.form.patchValue({ [controlName]: finalValue }, { emitEvent: false });
    input.value = value;
  }

  private cleanAlpha(value: string): string {
    return value
      .replace(/[^a-zA-ZÀ-ÿ'\\-\\s]/g, '')
      .replace(/\\s{2,}/g, ' ')
      .trim();
  }

  private cleanText(value: string, maxLength: number): string {
    return value
      .replace(/\\s{2,}/g, ' ')
      .trim()
      .slice(0, maxLength);
  }

  private cleanPhone(value: string): string {
    return value
      .replace(/[^0-9+\\-\\s()]/g, '')
      .slice(0, 30)
      .trim();
  }

  private cleanEmail(value: string): string {
    return value.replace(/\\s+/g, '').toLowerCase().slice(0, 180);
  }

  private normalizeOptional(value: string | null | undefined): string | null {
    if (value == null) return null;
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private clearMessages(): void {
    this.error.set(null);
    this.success.set(null);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      const message = error.error?.message;
      if (typeof message === 'string' && message.trim()) {
        return message;
      }

      const fieldErrors = error.error?.fieldErrors;
      if (fieldErrors && typeof fieldErrors === 'object') {
        const first = Object.values(fieldErrors)[0];
        if (typeof first === 'string' && first.trim()) {
          return first;
        }
      }
    }

    if (error instanceof Error && error.message.trim()) {
      return error.message;
    }

    return fallback;
  }
}
