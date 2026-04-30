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
  selector: 'app-youth-campaign-preview-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CampaignPageRendererComponent],
  template: `
    <section class="preview-page">
      <header class="preview-bar">
        <div>
          <span>Preview mode</span>
          <h1>{{ campaign?.title || 'Campaign preview' }}</h1>
        </div>
        <div class="actions" *ngIf="campaign">
          <a class="btn btn-ghost" routerLink="/youth/campaigns">Campaigns</a>
          <a class="btn btn-primary" [routerLink]="['/youth/campaigns', campaign.id, 'builder']">Edit builder</a>
        </div>
      </header>

      <p class="error" *ngIf="error">{{ error }}</p>
      <div class="loading-card" *ngIf="loading">Loading preview...</div>

      <app-campaign-page-renderer
        *ngIf="!loading && campaign"
        [campaign]="campaign"
        [content]="content"
        [style]="style"
      />
    </section>
  `,
  styles: [
    `
      .preview-page { display:grid; gap:22px; padding-bottom:32px; }
      .preview-bar { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; flex-wrap:wrap; padding:18px 20px; border-radius:24px; background:#fff; border:1px solid rgba(15,23,42,.08); box-shadow:0 16px 38px rgba(15,23,42,.07); }
      .preview-bar span { color:#9a6a17; font-size:.74rem; font-weight:950; text-transform:uppercase; letter-spacing:.08em; }
      .preview-bar h1 { margin:6px 0 0; color:#1f2937; letter-spacing:-.04em; }
      .actions { display:flex; gap:10px; flex-wrap:wrap; }
      .btn { min-height:42px; display:inline-flex; align-items:center; justify-content:center; border:0; border-radius:999px; padding:0 16px; font:inherit; font-weight:950; text-decoration:none; }
      .btn-primary { color:#271a08; background:linear-gradient(135deg,#f7d77c,#d6a13d); box-shadow:0 14px 30px rgba(184,130,38,.22); }
      .btn-ghost { color:#344054; background:#f2f4f7; }
      .loading-card { padding:24px; border-radius:24px; background:#fff; color:#475467; font-weight:850; text-align:center; }
      .error { margin:0; padding:14px 16px; border-radius:18px; background:#fef2f2; color:#b42318; border:1px solid rgba(239,68,68,.16); font-weight:900; }
    `,
  ],
})
export class YouthCampaignPreviewPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly crowdfundingService = inject(CrowdfundingService);

  campaign: CampaignPageResponse | null = null;
  content: CampaignContentJson = { blocks: [] };
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
    this.crowdfundingService
      .getCampaignPageBuilder(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (campaign) => {
          this.campaign = campaign;
          this.content = this.parseContent(campaign.contentJson, campaign);
          this.style = this.parseStyle(campaign.styleJson);
        },
        error: () => {
          this.error = 'Unable to load campaign preview.';
        },
      });
  }

  private parseContent(value: string | null, campaign: CampaignPageResponse): CampaignContentJson {
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
      return { ...DEFAULT_STYLE, ...(JSON.parse(value || '') as Partial<CampaignStyleJson>) };
    } catch {
      return { ...DEFAULT_STYLE };
    }
  }
}
