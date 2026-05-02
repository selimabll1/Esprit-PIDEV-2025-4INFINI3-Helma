import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  CampaignPageResponse,
  CampaignStyleJson,
  CrowdfundingType,
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
  selector: 'app-public-campaign-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CampaignPageRendererComponent],
  template: `
    <section class="public-page">
      <p class="error" *ngIf="error">{{ error }}</p>
      <div class="loading-card" *ngIf="loading">Loading campaign...</div>

      <ng-container *ngIf="!loading && campaign">
        <app-campaign-page-renderer
          [campaign]="campaign"
          [content]="content"
          [style]="style"
          [allowDocumentDownload]="true"
          [showTrustHeader]="true"
          [showSectionLabels]="false"
        />

        <aside class="sticky-support">
          <div>
            <span>{{ campaign.applicationType === crowdfundingType.EQUITY ? 'Investor action' : 'Support action' }}</span>
            <strong>{{ campaign.title || campaign.businessName || 'Campaign' }}</strong>
          </div>
          <a class="support-btn" routerLink="/investor">
            {{ campaign.applicationType === crowdfundingType.EQUITY ? 'Open investor portal' : 'Open donation portal' }}
          </a>
        </aside>
      </ng-container>
    </section>
  `,
  styles: [
    `
      .public-page {
        min-height: 100vh;
        display: grid;
        gap: 0;
        background: #f8fafc;
      }

      .sticky-support {
        position: sticky;
        bottom: 18px;
        z-index: 20;
        width: min(880px, calc(100% - 32px));
        margin: 0 auto 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 14px;
        flex-wrap: wrap;
        padding: 14px 16px;
        border-radius: 999px;
        background: rgba(17, 24, 39, 0.94);
        color: #ffffff;
        box-shadow: 0 24px 70px rgba(17, 24, 39, 0.32);
        backdrop-filter: blur(18px);
      }

      .sticky-support div {
        display: grid;
        gap: 3px;
        min-width: 0;
      }

      .sticky-support span {
        color: #f7d77c;
        font-size: 0.7rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.08em;
      }

      .sticky-support strong {
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-weight: 950;
      }

      .support-btn {
        min-height: 42px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border-radius: 999px;
        padding: 0 16px;
        color: #271a08;
        background: linear-gradient(135deg, #f7d77c, #d6a13d);
        text-decoration: none;
        font-weight: 950;
      }

      .loading-card,
      .error {
        width: min(1180px, calc(100% - 32px));
        margin: 28px auto;
        padding: 20px;
        border-radius: 22px;
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

      @media (max-width: 680px) {
        .sticky-support {
          border-radius: 24px;
        }
      }
    `,
  ],
})
export class PublicCampaignPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly crowdfundingService = inject(CrowdfundingService);

  readonly crowdfundingType = CrowdfundingType;

  campaign: CampaignPageResponse | null = null;
  content: StudioContentJson = { sections: [] };
  style: CampaignStyleJson = { ...DEFAULT_STYLE };
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.error = 'Campaign link is missing.';
      return;
    }

    this.loading = true;
    this.error = null;

    this.crowdfundingService
      .getPublicCampaignBySlug(slug)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (campaign) => {
          this.campaign = campaign;
          this.content = this.parseContent(campaign.contentJson);
          this.style = this.parseStyle(campaign.styleJson);
        },
        error: (err) => {
          console.error('[PublicCampaignPage] load failed:', err);
          this.error =
            err?.error?.message ||
            err?.error?.error ||
            'This campaign is not published or the link is invalid.';
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
