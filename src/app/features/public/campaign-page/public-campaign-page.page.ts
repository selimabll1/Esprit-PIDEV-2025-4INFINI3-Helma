import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  CampaignContentJson,
  CampaignPageResponse,
  CampaignStyleJson,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';
import { CampaignPageRendererComponent } from '../../../shared/components/campaign-page-renderer.component';

const DEFAULT_STYLE: CampaignStyleJson = {
  fontFamily: 'Inter',
  primaryColor: '#111827',
  accentColor: '#C9A227',
  radius: 'large',
  heroLayout: 'centered',
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
        />

        <aside class="cta-card">
          <div>
            <span>Ready to support?</span>
            <h2>{{ campaign.businessName || campaign.title }}</h2>
            <p>
              This public page is connected to a compliance-approved Helma
              application.
            </p>
          </div>
          <a class="btn btn-primary" routerLink="/investor"
            >Open investor portal</a
          >
        </aside>
      </ng-container>
    </section>
  `,
  styles: [
    `
      .public-page {
        display: grid;
        gap: 24px;
        padding: 24px 0 52px;
        background: #f8fafc;
      }
      .cta-card {
        width: min(960px, 100%);
        margin: 0 auto;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 18px;
        flex-wrap: wrap;
        padding: 24px;
        border-radius: 28px;
        background: #111827;
        color: #fff;
      }
      .cta-card span {
        color: #f7d77c;
        font-size: 0.78rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.08em;
      }
      .cta-card h2 {
        margin: 8px 0;
        letter-spacing: -0.04em;
      }
      .cta-card p {
        margin: 0;
        color: #d0d5dd;
        line-height: 1.6;
      }
      .btn {
        min-height: 44px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border-radius: 999px;
        padding: 0 18px;
        text-decoration: none;
        font-weight: 950;
      }
      .btn-primary {
        color: #271a08;
        background: linear-gradient(135deg, #f7d77c, #d6a13d);
      }
      .loading-card {
        width: min(960px, 100%);
        margin: 0 auto;
        padding: 30px;
        border-radius: 24px;
        background: #fff;
        color: #475467;
        font-weight: 850;
        text-align: center;
      }
      .error {
        width: min(960px, 100%);
        margin: 0 auto;
        padding: 14px 16px;
        border-radius: 18px;
        background: #fef2f2;
        color: #b42318;
        border: 1px solid rgba(239, 68, 68, 0.16);
        font-weight: 900;
      }
    `,
  ],
})
export class PublicCampaignPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly crowdfundingService = inject(CrowdfundingService);

  campaign: CampaignPageResponse | null = null;
  content: CampaignContentJson = { blocks: [] };
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
    this.crowdfundingService
      .getPublicCampaignBySlug(slug)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (campaign) => {
          this.campaign = campaign;
          this.content = this.parseContent(campaign.contentJson, campaign);
          this.style = this.parseStyle(campaign.styleJson);
        },
        error: () => {
          this.error = 'This campaign is not published or the link is invalid.';
        },
      });
  }

  private parseContent(
    value: string | null,
    campaign: CampaignPageResponse,
  ): CampaignContentJson {
    try {
      const parsed = JSON.parse(value || '') as Partial<CampaignContentJson>;
      if (Array.isArray(parsed.blocks)) return { blocks: parsed.blocks };
    } catch {
      // fallback below
    }

    return {
      blocks: [
        {
          id: 'hero',
          type: 'hero',
          title: campaign.title || campaign.businessName || 'Campaign',
          subtitle: campaign.subtitle || campaign.summary || '',
        },
      ],
    };
  }

  private parseStyle(value: string | null): CampaignStyleJson {
    try {
      return {
        ...DEFAULT_STYLE,
        ...(JSON.parse(value || '') as Partial<CampaignStyleJson>),
      };
    } catch {
      return { ...DEFAULT_STYLE };
    }
  }
}
