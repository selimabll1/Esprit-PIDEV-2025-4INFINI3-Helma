import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  CampaignPageResponse,
  CampaignStyleJson,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';
import { CampaignPageRendererComponent } from '../../../shared/components/campaign-page-renderer.component';

interface StudioContentJson {
  version?: number;
  editor?: string;
  sections?: unknown[];
  blocks?: unknown[];
}

const DEFAULT_STYLE: CampaignStyleJson = {
  fontFamily: 'Inter',
  primaryColor: '#111827',
  accentColor: '#C9A227',
  radius: 'large',
  heroLayout: 'split',
  buttonStyle: 'pill',
};

@Component({
  selector: 'app-youth-campaign-preview-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CampaignPageRendererComponent],
  template: `
    <section class="preview-page">
      <header class="preview-bar">
        <div>
          <a class="back-link" routerLink="/youth/campaigns">← Campaigns</a>
          <span class="eyebrow">Preview mode</span>
          <h1>{{ campaign?.title || campaign?.businessName || 'Campaign preview' }}</h1>
          <p>
            This is how your campaign page will look after compliance publication.
            Documents are shown as preview-only here.
          </p>
        </div>

        <div class="actions" *ngIf="campaign">
          <a class="btn btn-ghost" routerLink="/youth/campaigns">Campaigns</a>
          <a class="btn btn-primary" [routerLink]="['/youth/campaigns', campaign.id, 'builder']">
            Edit studio
          </a>
        </div>
      </header>

      <p class="error" *ngIf="error">{{ error }}</p>
      <div class="loading-card" *ngIf="loading">Loading preview...</div>

      <app-campaign-page-renderer
        *ngIf="!loading && campaign"
        [campaign]="campaign"
        [content]="content"
        [style]="style"
        [allowDocumentDownload]="false"
        [showTrustHeader]="true"
        [showSectionLabels]="false"
      />
    </section>
  `,
  styles: [
    `
      .preview-page {
        display: grid;
        gap: 22px;
        padding-bottom: 36px;
        background: #f8fafc;
      }

      .preview-bar {
        width: min(1180px, calc(100% - 32px));
        margin: 18px auto 0;
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 18px;
        flex-wrap: wrap;
        padding: 22px 24px;
        border-radius: 28px;
        background: rgba(255, 255, 255, 0.9);
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
        backdrop-filter: blur(16px);
      }

      .back-link {
        display: inline-flex;
        margin-bottom: 10px;
        color: #475467;
        text-decoration: none;
        font-weight: 900;
      }

      .eyebrow {
        display: inline-flex;
        color: #9a6a17;
        font-size: 0.74rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.08em;
      }

      h1 {
        margin: 8px 0;
        color: #111827;
        font-size: clamp(2rem, 4vw, 3.1rem);
        letter-spacing: -0.06em;
      }

      .preview-bar p {
        max-width: 680px;
        margin: 0;
        color: #667085;
        line-height: 1.65;
        font-weight: 650;
      }

      .actions {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
      }

      .btn {
        min-height: 44px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border: 0;
        border-radius: 999px;
        padding: 0 18px;
        color: inherit;
        font: inherit;
        font-weight: 950;
        text-decoration: none;
        cursor: pointer;
      }

      .btn-primary {
        color: #271a08;
        background: linear-gradient(135deg, #f7d77c, #d6a13d);
        box-shadow: 0 14px 30px rgba(184, 130, 38, 0.22);
      }

      .btn-ghost {
        color: #344054;
        background: #f2f4f7;
      }

      .loading-card,
      .error {
        width: min(1180px, calc(100% - 32px));
        margin: 0 auto;
        padding: 18px;
        border-radius: 20px;
        font-weight: 900;
      }

      .loading-card {
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        color: #475467;
        text-align: center;
      }

      .error {
        background: #fef2f2;
        color: #b42318;
        border: 1px solid rgba(239, 68, 68, 0.16);
      }
    `,
  ],
})
export class YouthCampaignPreviewPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly crowdfundingService = inject(CrowdfundingService);

  campaign: CampaignPageResponse | null = null;
  content: StudioContentJson = { sections: [] };
  style: CampaignStyleJson = { ...DEFAULT_STYLE };
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error = 'Campaign id is missing.';
      return;
    }

    this.loading = true;
    this.error = null;

    this.crowdfundingService
      .getCampaignPageBuilder(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (campaign) => {
          this.campaign = campaign;
          this.content = this.parseContent(campaign.contentJson);
          this.style = this.parseStyle(campaign.styleJson);
        },
        error: (err) => {
          console.error('[YouthCampaignPreview] load failed:', err);
          this.error = err?.error?.message || 'Unable to load campaign preview.';
        },
      });
  }

  private parseContent(value: string | null): StudioContentJson {
    try {
      const parsed = JSON.parse(value || '{}') as StudioContentJson;
      if (Array.isArray(parsed.sections) || Array.isArray(parsed.blocks)) {
        return parsed;
      }
    } catch {
      // fallback below
    }

    return { sections: [] };
  }

  private parseStyle(value: string | null): CampaignStyleJson {
    try {
      return { ...DEFAULT_STYLE, ...(JSON.parse(value || '{}') as Partial<CampaignStyleJson>) };
    } catch {
      return { ...DEFAULT_STYLE };
    }
  }
}
