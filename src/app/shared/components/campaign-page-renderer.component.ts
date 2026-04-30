import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import {
  CampaignBlockSize,
  CampaignBuilderBlock,
  CampaignContentJson,
  CampaignPageDocumentResponse,
  CampaignPageResponse,
  CampaignStyleJson,
} from '../../core/models/crowdfunding.models';

@Component({
  selector: 'app-campaign-page-renderer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="public-campaign" [ngStyle]="canvasStyles">
      <section
        class="campaign-block"
        *ngFor="let block of content.blocks; trackBy: trackByBlockId"
        [ngClass]="['block-' + block.type, 'size-' + blockSize(block)]"
      >
        <ng-container [ngSwitch]="block.type">
          <section
            class="hero"
            [ngClass]="'hero-layout-' + style.heroLayout"
            *ngSwitchCase="'hero'"
          >
            <div class="hero-copy">
              <span class="type-pill">{{
                formatLabel(campaign.applicationType)
              }}</span>
              <h1>
                {{ block.title || campaign.title || campaign.businessName }}
              </h1>
              <p>
                {{ block.subtitle || campaign.subtitle || campaign.summary }}
              </p>
            </div>
            <img
              *ngIf="campaign.coverMediaUrl"
              [src]="campaign.coverMediaUrl"
              alt="Campaign cover"
            />
          </section>

          <h2 *ngSwitchCase="'header'">{{ block.content }}</h2>
          <h3 *ngSwitchCase="'subheader'">{{ block.content }}</h3>
          <p class="paragraph" *ngSwitchCase="'paragraph'">
            {{ block.content }}
          </p>

          <figure *ngSwitchCase="'image'" class="media-figure">
            <img
              *ngIf="block.url"
              [src]="block.url"
              [alt]="block.label || 'Campaign image'"
            />
            <figcaption *ngIf="block.label">{{ block.label }}</figcaption>
          </figure>

          <div class="video-shell" *ngSwitchCase="'youtube'">
            <ng-container
              *ngIf="youtubeEmbed(block.url) as videoUrl; else noVideo"
            >
              <iframe
                [src]="videoUrl"
                title="Campaign video"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen
              ></iframe>
            </ng-container>
            <ng-template #noVideo>
              <p class="muted">YouTube link not configured.</p>
            </ng-template>
          </div>

          <a
            *ngSwitchCase="'document'"
            class="document-link"
            [href]="documentHref(block)"
            target="_blank"
            rel="noopener"
            [class.disabled]="!documentCanOpen(block)"
            [attr.aria-disabled]="!documentCanOpen(block)"
          >
            <span>PDF</span>
            <strong>
              {{
                block.label ||
                  documentForBlock(block)?.label ||
                  documentForBlock(block)?.fileName ||
                  'Campaign document'
              }}
            </strong>
            <em *ngIf="!allowDocumentDownload">Preview only</em>
          </a>

          <section class="funding-card" *ngSwitchCase="'funding_cta'">
            <span>Funding goal</span>
            <strong>{{
              formatMoney(campaign.fundingGoal, campaign.currency)
            }}</strong>
            <p>
              {{
                formatMoney(campaign.investorsPledgedAmount, campaign.currency)
              }}
              pledged so far.
            </p>
          </section>

          <hr *ngSwitchCase="'divider'" />
        </ng-container>
      </section>
    </article>
  `,
  styles: [
    `
      .public-campaign {
        width: min(1220px, calc(100% - 28px));
        margin: 0 auto;
        display: grid;
        gap: 24px;
        padding: clamp(18px, 3vw, 44px) 0;
        color: #111827;
      }

      .campaign-block {
        width: 100%;
        min-width: 0;
        justify-self: center;
      }

      .size-compact {
        max-width: 720px;
      }
      .size-normal {
        max-width: 920px;
      }
      .size-wide {
        max-width: 1100px;
      }
      .size-full {
        max-width: none;
      }

      .hero {
        display: grid;
        gap: 28px;
        align-items: center;
        padding: clamp(32px, 6vw, 78px);
        border-radius: 36px;
        background:
          radial-gradient(
            circle at 10% 0%,
            color-mix(in srgb, var(--accent) 24%, transparent),
            transparent 36%
          ),
          linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 30px 90px rgba(15, 23, 42, 0.12);
      }

      .hero-layout-centered,
      .hero-layout-editorial {
        justify-items: center;
        text-align: center;
      }

      .hero-layout-split {
        grid-template-columns: minmax(0, 0.92fr) minmax(320px, 1.08fr);
        text-align: left;
      }

      .hero-copy {
        display: grid;
        gap: 16px;
      }

      .hero h1 {
        margin: 0;
        max-width: 930px;
        font-size: clamp(2.8rem, 7vw, 6.8rem);
        line-height: 0.88;
        letter-spacing: -0.085em;
        color: inherit;
      }

      .hero p {
        margin: 0;
        max-width: 760px;
        color: #667085;
        font-size: clamp(1rem, 2vw, 1.26rem);
        line-height: 1.75;
        font-weight: 700;
      }

      .hero img,
      .media-figure img {
        width: 100%;
        max-height: 620px;
        object-fit: cover;
        border-radius: 30px;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 44px rgba(15, 23, 42, 0.12);
      }

      .hero-layout-centered img,
      .hero-layout-editorial img {
        max-width: 980px;
      }

      .type-pill {
        width: fit-content;
        display: inline-flex;
        padding: 7px 12px;
        border-radius: 999px;
        background: #fff7ed;
        color: #9a6a17;
        font-size: 0.76rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.08em;
      }

      .hero-layout-centered .type-pill,
      .hero-layout-editorial .type-pill {
        justify-self: center;
      }

      h2,
      h3,
      .paragraph,
      .document-link,
      .funding-card {
        border-radius: 28px;
      }

      h2 {
        margin: 0;
        padding: clamp(18px, 3vw, 32px);
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 50px rgba(15, 23, 42, 0.07);
        font-size: clamp(2rem, 4vw, 3.5rem);
        line-height: 1;
        letter-spacing: -0.065em;
      }

      h3 {
        margin: 0;
        padding: clamp(16px, 2.4vw, 26px);
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        color: #475467;
        font-size: clamp(1.25rem, 2.3vw, 1.75rem);
        line-height: 1.35;
      }

      .paragraph {
        margin: 0;
        padding: clamp(18px, 3vw, 34px);
        background: #ffffff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        color: #475467;
        line-height: 1.9;
        font-size: 1.06rem;
        font-weight: 580;
        white-space: pre-line;
      }

      .media-figure {
        display: grid;
        gap: 10px;
        margin: 0;
      }

      figcaption {
        text-align: center;
        color: #98a2b3;
        font-weight: 750;
      }

      .video-shell {
        position: relative;
        overflow: hidden;
        border-radius: 32px;
        background: #111827;
        aspect-ratio: 16 / 9;
        box-shadow: 0 22px 60px rgba(15, 23, 42, 0.16);
      }

      iframe {
        width: 100%;
        height: 100%;
        border: 0;
      }

      .muted {
        margin: 0;
        color: #98a2b3;
        display: grid;
        place-items: center;
        height: 100%;
        font-weight: 800;
      }

      .document-link {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 18px;
        background: #ffffff;
        color: #1f2937;
        text-decoration: none;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 50px rgba(15, 23, 42, 0.07);
      }

      .document-link span {
        width: 50px;
        height: 50px;
        display: grid;
        place-items: center;
        border-radius: 16px;
        background: #fff0c7;
        color: #9a6a17;
        font-size: 0.72rem;
        font-weight: 950;
      }

      .document-link strong {
        font-size: 1rem;
      }

      .document-link.disabled {
        pointer-events: none;
        opacity: 0.55;
      }

      .funding-card {
        display: grid;
        justify-items: center;
        text-align: center;
        gap: 8px;
        padding: clamp(28px, 5vw, 54px);
        background: linear-gradient(135deg, #fff7ed, #fff0c7);
        border: 1px solid rgba(184, 130, 38, 0.18);
        box-shadow: 0 24px 70px rgba(184, 130, 38, 0.12);
      }

      .funding-card span {
        color: #9a6a17;
        font-size: 0.78rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.08em;
      }

      .funding-card strong {
        color: #1f2937;
        font-size: clamp(2.4rem, 6vw, 4.6rem);
        letter-spacing: -0.06em;
      }

      .funding-card p {
        margin: 0;
        color: #667085;
        font-weight: 800;
      }

      hr {
        width: 100%;
        border: 0;
        border-top: 1px solid rgba(15, 23, 42, 0.1);
      }

      @media (max-width: 860px) {
        .hero-layout-split {
          grid-template-columns: 1fr;
          text-align: center;
        }
        .hero-layout-split .type-pill {
          justify-self: center;
        }
        .public-campaign {
          width: min(100%, calc(100% - 16px));
        }
      }
    `,
  ],
})
export class CampaignPageRendererComponent {
  private readonly sanitizer = inject(DomSanitizer);

  @Input({ required: true }) campaign!: CampaignPageResponse;
  @Input({ required: true }) content!: CampaignContentJson;
  @Input({ required: true }) style!: CampaignStyleJson;
  @Input() allowDocumentDownload = false;

  get canvasStyles(): Record<string, string> {
    return {
      fontFamily: this.style.fontFamily,
      color: this.style.primaryColor,
      '--accent': this.style.accentColor,
    };
  }

  blockSize(block: CampaignBuilderBlock): CampaignBlockSize {
    if (block.size) return block.size;
    switch (block.type) {
      case 'hero':
        return 'full';
      case 'image':
      case 'youtube':
      case 'funding_cta':
      case 'divider':
        return 'wide';
      case 'document':
        return 'compact';
      default:
        return 'normal';
    }
  }

  documentForBlock(
    block: CampaignBuilderBlock,
  ): CampaignPageDocumentResponse | null {
    if (typeof block.documentId !== 'number') return null;
    return (
      this.campaign.publicDocuments.find(
        (doc) => doc.applicationDocumentId === block.documentId,
      ) ?? null
    );
  }

  documentCanOpen(block: CampaignBuilderBlock): boolean {
    return this.allowDocumentDownload && !!this.documentForBlock(block);
  }

  documentHref(block: CampaignBuilderBlock): string {
    const doc = this.documentForBlock(block);
    if (!this.allowDocumentDownload || !doc) return '#';
    return `/api/crowdfunding/public-campaigns/${this.campaign.slug}/documents/${doc.applicationDocumentId}/content`;
  }

  youtubeEmbed(url: string | null | undefined): SafeResourceUrl | null {
    const id = this.extractYouTubeId(url);
    if (!id) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.youtube.com/embed/${id}`,
    );
  }

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '')
      return 'Campaign';
    return String(value)
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatMoney(
    value: number | null | undefined,
    currency: string | null | undefined,
  ): string {
    if (value === null || value === undefined) return '—';
    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: currency || 'TND',
      maximumFractionDigits: 0,
    }).format(value);
  }

  trackByBlockId(_index: number, block: CampaignBuilderBlock): string {
    return block.id;
  }

  private extractYouTubeId(url: string | null | undefined): string | null {
    if (!url) return null;
    const trimmed = url.trim();
    const patterns = [
      /youtube\.com\/watch\?v=([^&]+)/,
      /youtube\.com\/embed\/([^?&]+)/,
      /youtu\.be\/([^?&]+)/,
      /youtube\.com\/shorts\/([^?&]+)/,
    ];

    for (const pattern of patterns) {
      const match = trimmed.match(pattern);
      if (match?.[1]) return match[1];
    }

    return null;
  }
}
