import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { CampaignPageResponse, DocumentType } from '../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../core/services/crowdfunding.service';
import { throwError } from 'rxjs';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-campaign-page-renderer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="campaign-renderer" [ngStyle]="pageStyle">
      <div class="browser-bar" *ngIf="showTrustHeader && campaign">
        <span></span>
        <span></span>
        <span></span>
        <strong>/campaigns/{{ campaign.slug || 'your-campaign' }}</strong>
      </div>

      <section class="campaign-canvas">
        <article
          class="slide-section"
          *ngFor="let section of sections; let sectionIndex = index; trackBy: trackBySection"
          [style.height.px]="sectionHeight(section)"
          [style.background]="sectionBackground(section)"
        >
          <div class="section-label" *ngIf="showSectionLabels">
            {{ sectionLabel(section, sectionIndex) }}
          </div>

          <ng-container *ngIf="sectionElements(section).length > 0; else legacySectionFallback">
            <div
              class="canvas-element"
              *ngFor="let element of sectionElements(section); trackBy: trackByElement"
              [ngClass]="['element-' + elementType(element), isTextual(element) ? 'textual' : '']"
              [ngStyle]="elementStyles(element)"
            >
              <ng-container [ngSwitch]="elementType(element)">
                <div
                  *ngSwitchCase="'title'"
                  class="text-render title-object"
                  [style.text-align]="elementAlign(element)"
                >
                  {{ elementText(element) }}
                </div>

                <div
                  *ngSwitchCase="'text'"
                  class="text-render body-object"
                  [style.text-align]="elementAlign(element)"
                >
                  {{ elementText(element) }}
                </div>

                <div *ngSwitchCase="'image'" class="image-object">
                  <img
                    *ngIf="elementUrl(element); else emptyImage"
                    [src]="elementUrl(element)"
                    [style.object-fit]="elementObjectFit(element)"
                    [alt]="elementText(element) || 'Campaign image'"
                  />
                  <ng-template #emptyImage>
                    <div class="empty-object">🖼️ Image</div>
                  </ng-template>
                </div>

                <div *ngSwitchCase="'video'" class="video-object">
                  <iframe
                    *ngIf="youtubeEmbed(elementUrl(element)) as videoUrl; else emptyVideo"
                    [src]="videoUrl"
                    title="Campaign video"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    allowfullscreen
                  ></iframe>
                  <ng-template #emptyVideo>
                    <div class="empty-object">▶️ Video</div>
                  </ng-template>
                </div>

                <a
                  *ngSwitchCase="'button'"
                  class="button-object"
                  [href]="elementButtonHref(element)"
                  [target]="elementButtonHref(element) === '#' ? '_self' : '_blank'"
                  rel="noopener"
                  [style.text-align]="elementAlign(element)"
                  [style.justify-content]="justifyContent(elementAlign(element))"
                >
                  <span>{{ elementText(element) || defaultCtaLabel }}</span>
                </a>

                <div *ngSwitchCase="'box'" class="box-object"></div>

                <button
                  *ngSwitchCase="'document'"
                  class="document-object"
                  type="button"
                  [class.disabled]="!documentCanOpen(element)"
                  [attr.aria-disabled]="!documentCanOpen(element)"
                  (click)="openPdfPreview($event, element)"
                >
                  <span>PDF</span>
                  <strong>{{ documentLabel(element) }}</strong>
                  <em>{{ documentCanOpen(element) ? 'Preview PDF' : 'Unavailable' }}</em>
                </button>

                <div *ngSwitchCase="'funding'" class="funding-object">
                  <span>Funding goal</span>
                  <strong>{{ formatMoney(campaign?.fundingGoal, campaign?.currency) }}</strong>
                  <small>
                    {{ formatMoney(campaign?.investorsPledgedAmount, campaign?.currency) }} pledged
                  </small>
                </div>

                <div
                  *ngSwitchDefault
                  class="text-render body-object"
                  [style.text-align]="elementAlign(element)"
                >
                  {{ elementText(element) }}
                </div>
              </ng-container>
            </div>
          </ng-container>

          <ng-template #legacySectionFallback>
            <div class="legacy-section-content">
              <span>{{ sectionLabel(section, sectionIndex) }}</span>
              <h2>{{ section.title || section.heading || campaign?.title || 'Campaign section' }}</h2>
              <p>{{ section.body || section.content || section.subtitle || campaign?.summary || '' }}</p>
            </div>
          </ng-template>
        </article>

        <article class="slide-section hero-fallback" *ngIf="sections.length === 0">
          <div class="legacy-section-content">
            <span>{{ formatLabel(campaign?.applicationType) }}</span>
            <h1>{{ campaign?.title || campaign?.businessName || 'Campaign' }}</h1>
            <p>{{ campaign?.subtitle || campaign?.summary || 'This campaign is being prepared.' }}</p>
            <strong>{{ formatMoney(campaign?.fundingGoal, campaign?.currency) }}</strong>
          </div>
        </article>
      </section>

      <div
        class="pdf-backdrop"
        *ngIf="pdfPreviewOpen"
        role="dialog"
        aria-modal="true"
        (click)="closePdfPreview()"
      >
        <section class="pdf-modal" (click)="$event.stopPropagation()">
          <header class="pdf-modal-header">
            <div>
              <span>Document preview</span>
              <strong>{{ pdfPreviewTitle }}</strong>
            </div>

            <div class="pdf-modal-actions">
              <a
                *ngIf="pdfPreviewUrl"
                class="pdf-open-link"
                [href]="pdfPreviewUrl"
                target="_blank"
                rel="noopener"
              >
                Open in new tab
              </a>

              <button type="button" class="pdf-close" (click)="closePdfPreview()">
                ✕
              </button>
            </div>
          </header>

          <div class="pdf-modal-body">
            <div class="pdf-loading" *ngIf="pdfLoading">Loading PDF preview...</div>
            <div class="pdf-error" *ngIf="!pdfLoading && pdfError">{{ pdfError }}</div>

            <object
              *ngIf="!pdfLoading && !pdfError && pdfPreviewSafeUrl"
              class="pdf-object"
              [data]="pdfPreviewSafeUrl"
              type="application/pdf"
            >
              <iframe
                class="pdf-object"
                [src]="pdfPreviewSafeUrl"
                title="PDF preview"
              ></iframe>
            </object>
          </div>
        </section>
      </div>
    </article>
  `,
  styles: [
    `
      :host {
        display: block;
        width: 100%;
      }

      .campaign-renderer {
        width: 100%;
        min-height: 100%;
        background: #ffffff;
        color: #111827;
        overflow: hidden;
      }

      .browser-bar {
        height: 44px;
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 0 18px;
        background: #111827;
        color: #ffffff;
        box-sizing: border-box;
      }

      .browser-bar span {
        width: 11px;
        height: 11px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.28);
      }

      .browser-bar strong {
        margin-left: 10px;
        color: #e5e7eb;
        font-size: 0.82rem;
        font-weight: 900;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .campaign-canvas {
        width: 100%;
        min-height: 760px;
        background: #ffffff;
      }

      .slide-section {
        position: relative;
        width: 100%;
        min-height: 420px;
        overflow: hidden;
        border-bottom: 1px solid rgba(15, 23, 42, 0.08);
        background-image:
          linear-gradient(rgba(15, 23, 42, 0.035) 1px, transparent 1px),
          linear-gradient(90deg, rgba(15, 23, 42, 0.035) 1px, transparent 1px);
        background-size: 24px 24px;
        box-sizing: border-box;
      }

      .section-label {
        position: absolute;
        top: 12px;
        left: 12px;
        z-index: 1000;
        padding: 8px 10px;
        border-radius: 999px;
        background: rgba(17, 24, 39, 0.86);
        color: #ffffff;
        font-size: 0.72rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.06em;
        pointer-events: none;
        backdrop-filter: blur(12px);
      }

      .canvas-element {
        position: absolute;
        box-sizing: border-box;
        display: flex;
        overflow: hidden;
        user-select: text;
      }

      .canvas-element.textual {
        overflow: visible;
      }

      .text-render {
        width: 100%;
        height: 100%;
        margin: 0;
        padding: 0;
        color: inherit;
        background: transparent;
        font: inherit;
        font-size: inherit;
        font-weight: inherit;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        word-break: break-word;
        direction: ltr;
        unicode-bidi: plaintext;
        writing-mode: horizontal-tb;
        box-sizing: border-box;
      }

      .title-object {
        letter-spacing: -0.06em;
        line-height: 0.98;
      }

      .body-object {
        line-height: 1.45;
      }

      .image-object,
      .video-object,
      .box-object,
      .document-object,
      .funding-object,
      .button-object {
        width: 100%;
        height: 100%;
      }

      .image-object img {
        display: block;
        width: 100%;
        height: 100%;
      }

      .video-object iframe {
        display: block;
        width: 100%;
        height: 100%;
        border: 0;
      }

      .empty-object {
        width: 100%;
        height: 100%;
        display: grid;
        place-items: center;
        color: #98a2b3;
        background: rgba(248, 250, 252, 0.82);
        font-weight: 950;
        border: 1px dashed rgba(15, 23, 42, 0.22);
        box-sizing: border-box;
      }

      .button-object {
        border: 0;
        font: inherit;
        font-weight: inherit;
        color: inherit;
        background: transparent;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 12px;
        box-sizing: border-box;
        line-height: 1.1;
        white-space: normal;
        overflow-wrap: anywhere;
        word-break: break-word;
        text-decoration: none;
      }

      .button-object span {
        display: block;
        max-width: 100%;
        pointer-events: none;
      }

      .box-object {
        pointer-events: none;
      }

      .document-object {
        border: 0;
        font: inherit;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        box-sizing: border-box;
        color: inherit;
        text-decoration: none;
      }

      .document-object span {
        width: 42px;
        height: 42px;
        flex: 0 0 42px;
        display: grid;
        place-items: center;
        border-radius: 14px;
        background: #fff7ed;
        color: #9a6a17;
        font-weight: 950;
        font-size: 0.76rem;
      }

      .document-object strong {
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-weight: 950;
      }

      .document-object em {
        margin-left: auto;
        color: #98a2b3;
        font-size: 0.75rem;
        font-style: normal;
        font-weight: 850;
        white-space: nowrap;
      }

      .document-object.disabled {
        pointer-events: none;
      }

      .funding-object {
        display: grid;
        place-items: center;
        text-align: center;
        padding: 16px;
        box-sizing: border-box;
      }

      .funding-object span {
        font-size: 0.72em;
        font-weight: 950;
        letter-spacing: 0.08em;
        text-transform: uppercase;
      }

      .funding-object strong {
        font-size: 1.7em;
        letter-spacing: -0.05em;
      }

      .funding-object small {
        opacity: 0.78;
        font-weight: 850;
      }

      .legacy-section-content {
        min-height: 580px;
        display: grid;
        align-content: center;
        gap: 16px;
        padding: clamp(34px, 8vw, 90px);
        text-align: center;
        box-sizing: border-box;
      }

      .legacy-section-content span {
        color: #9a6a17;
        font-size: 0.78rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.08em;
      }

      .legacy-section-content h1,
      .legacy-section-content h2 {
        margin: 0;
        color: inherit;
        font-size: clamp(2.3rem, 7vw, 5rem);
        line-height: 0.95;
        letter-spacing: -0.07em;
      }

      .legacy-section-content p {
        max-width: 760px;
        margin: 0 auto;
        color: #667085;
        font-size: 1.08rem;
        line-height: 1.8;
        font-weight: 650;
      }

      .hero-fallback {
        background: linear-gradient(135deg, #ffffff, #fff7ed);
      }



      .pdf-backdrop {
        position: fixed;
        inset: 0;
        z-index: 9999;
        display: grid;
        place-items: center;
        padding: 24px;
        background: rgba(15, 23, 42, 0.68);
        backdrop-filter: blur(10px);
        box-sizing: border-box;
      }

      .pdf-modal {
        width: min(1120px, 96vw);
        height: min(860px, 92vh);
        display: grid;
        grid-template-rows: auto 1fr;
        overflow: hidden;
        border-radius: 28px;
        background: #ffffff;
        border: 1px solid rgba(255, 255, 255, 0.16);
        box-shadow: 0 34px 100px rgba(0, 0, 0, 0.34);
      }

      .pdf-modal-header {
        min-height: 68px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 16px;
        padding: 14px 18px;
        background: #111827;
        color: #ffffff;
        box-sizing: border-box;
      }

      .pdf-modal-header div:first-child {
        min-width: 0;
        display: grid;
        gap: 3px;
      }

      .pdf-modal-header span {
        color: #9ca3af;
        font-size: 0.72rem;
        font-weight: 950;
        letter-spacing: 0.08em;
        text-transform: uppercase;
      }

      .pdf-modal-header strong {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 0.98rem;
        font-weight: 950;
      }

      .pdf-modal-actions {
        display: flex;
        align-items: center;
        gap: 10px;
        flex: 0 0 auto;
      }

      .pdf-open-link,
      .pdf-close {
        height: 38px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border: 0;
        border-radius: 999px;
        padding: 0 14px;
        color: #111827;
        background: #ffffff;
        text-decoration: none;
        font: inherit;
        font-size: 0.86rem;
        font-weight: 950;
        cursor: pointer;
      }

      .pdf-close {
        width: 38px;
        padding: 0;
      }

      .pdf-modal-body {
        min-height: 0;
        position: relative;
        background: #1f2937;
      }

      .pdf-object {
        width: 100%;
        height: 100%;
        border: 0;
        display: block;
        background: #ffffff;
      }

      .pdf-loading,
      .pdf-error {
        position: absolute;
        inset: 0;
        display: grid;
        place-items: center;
        padding: 24px;
        color: #ffffff;
        font-weight: 950;
        text-align: center;
        box-sizing: border-box;
      }

      .pdf-error {
        color: #fecaca;
      }

      @media (max-width: 760px) {
        .browser-bar {
          height: 40px;
          padding: 0 12px;
        }

        .slide-section {
          min-height: 560px;
        }

        .document-object {
          gap: 8px;
          padding: 10px;
        }

        .document-object span {
          width: 34px;
          height: 34px;
          flex-basis: 34px;
          border-radius: 11px;
          font-size: 0.68rem;
        }

        .document-object em {
          display: none;
        }
      }
    `,
  ],
})
export class CampaignPageRendererComponent implements OnDestroy {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly crowdfundingService = inject(CrowdfundingService);

  @Input({ required: true }) campaign!: CampaignPageResponse;

  // Flexible by design. New campaigns use sections[], older campaigns may still use blocks[].
  @Input() content: any = { sections: [] };
  @Input() style: any = {};
  @Input() allowDocumentDownload = false;
  @Input() showTrustHeader = true;
  @Input() showSectionLabels = false;

  pdfPreviewOpen = false;
  pdfLoading = false;
  pdfError: string | null = null;
  pdfPreviewTitle = '';
  pdfPreviewUrl: string | null = null;
  pdfPreviewSafeUrl: SafeResourceUrl | null = null;
  private pdfObjectUrl: string | null = null;

  get pageStyle(): Record<string, string> {
    return {
      fontFamily:
        this.style?.fontFamily ||
        'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      color: this.style?.primaryColor || '#111827',
      '--campaign-accent': this.style?.accentColor || '#c9a227',
    } as Record<string, string>;
  }

  get sections(): any[] {
    if (Array.isArray(this.content?.sections)) {
      return this.content.sections;
    }

    if (Array.isArray(this.content?.blocks)) {
      return this.legacyBlocksToSections(this.content.blocks);
    }

    return [];
  }

  get defaultCtaLabel(): string {
    return this.campaign?.applicationType === 'DONATION' ? 'Donate now' : 'Invest now';
  }

  sectionElements(section: any): any[] {
    return Array.isArray(section?.elements) ? section.elements : [];
  }

  sectionHeight(section: any): number {
    const value = Number(section?.height ?? section?.minHeight ?? 640);
    return Number.isFinite(value) ? Math.max(420, value) : 640;
  }

  sectionBackground(section: any): string {
    const background = String(section?.background ?? section?.backgroundColor ?? '#ffffff').trim();

    if (background === 'soft') return 'linear-gradient(135deg, #ffffff, #f8fafc)';
    if (background === 'accent') return 'linear-gradient(135deg, #fff7ed, #fff1c8)';
    if (background === 'dark') return 'radial-gradient(circle at top left, #374151, #111827 62%)';
    if (background === 'plain' || !background) return '#ffffff';

    return background;
  }

  sectionLabel(section: any, index: number): string {
    return this.formatLabel(section?.name || section?.type || section?.kind || `Section ${index + 1}`);
  }

  elementStyles(element: any): Record<string, string> {
    const isText = this.isTextual(element);

    return {
      left: `${this.percent(element?.x, 8)}%`,
      top: `${this.percent(element?.y, 12)}%`,
      width: `${this.percent(element?.width ?? element?.w, 42)}%`,
      height: `${this.percent(element?.height ?? element?.h, 18)}%`,
      zIndex: String(Number(element?.zIndex ?? element?.z ?? 1)),
      color: String(element?.color ?? element?.textColor ?? '#111827'),
      background:
        isText && String(element?.background ?? element?.backgroundColor ?? '') === '#ffffff'
          ? 'transparent'
          : String(element?.background ?? element?.backgroundColor ?? element?.fillColor ?? 'transparent'),
      border: `${Number(element?.borderWidth ?? 0)}px solid ${String(element?.borderColor ?? 'transparent')}`,
      borderRadius: `${Number(element?.radius ?? element?.borderRadius ?? 0)}px`,
      opacity: String(this.normalizeOpacity(element?.opacity)),
      fontSize: `${Number(element?.fontSize ?? (this.elementType(element) === 'title' ? 56 : 20))}px`,
      fontWeight: String(Number(element?.fontWeight ?? (this.elementType(element) === 'title' ? 950 : 650))),
      textAlign: this.elementAlign(element),
      alignItems: isText ? 'flex-start' : 'stretch',
      justifyContent: 'stretch',
      boxShadow: element?.shadow ? '0 22px 54px rgba(15, 23, 42, 0.16)' : 'none',
    };
  }

  isTextual(element: any): boolean {
    return this.elementType(element) === 'title' || this.elementType(element) === 'text';
  }

  elementType(element: any): string {
    return String(element?.type || 'text').toLowerCase();
  }

  elementText(element: any): string {
    return String(element?.text ?? element?.content ?? element?.title ?? element?.label ?? '');
  }

  elementUrl(element: any): string {
    return String(element?.url ?? element?.src ?? '').trim();
  }

  elementButtonHref(element: any): string {
    const href = String(element?.href ?? '').trim();
    return href || '#';
  }

  elementAlign(element: any): string {
    const align = String(element?.align ?? element?.textAlign ?? 'left').toLowerCase();
    return align === 'center' || align === 'right' ? align : 'left';
  }

  elementObjectFit(element: any): string {
    const fit = String(element?.objectFit || 'cover').trim();
    return fit === 'contain' ? 'contain' : 'cover';
  }

  justifyContent(align: string | null | undefined): string {
    if (align === 'left') return 'flex-start';
    if (align === 'right') return 'flex-end';
    return 'center';
  }

  documentForElement(element: any): any | null {
    const id = Number(element?.documentId ?? element?.applicationDocumentId);

    if (!Number.isFinite(id)) {
      return null;
    }

    return this.campaign?.publicDocuments?.find((doc: any) => doc.applicationDocumentId === id) ?? null;
  }

  documentCanOpen(element: any): boolean {
    return !!this.documentForElement(element);
  }

  openPdfPreview(event: Event, element: any): void {
    event.preventDefault();
    event.stopPropagation();

    const doc = this.documentForElement(element);

    if (!doc) {
      this.closePdfObjectUrl();
      this.pdfPreviewOpen = true;
      this.pdfPreviewTitle = 'Document unavailable';
      this.pdfError = 'This document is not available for preview.';
      this.pdfLoading = false;
      this.pdfPreviewUrl = null;
      this.pdfPreviewSafeUrl = null;
      return;
    }

    this.closePdfObjectUrl();
    this.pdfPreviewOpen = true;
    this.pdfLoading = true;
    this.pdfError = null;
    this.pdfPreviewTitle = this.documentLabel(element);
    this.pdfPreviewUrl = null;
    this.pdfPreviewSafeUrl = null;

    this.fetchDocumentBlob(doc)
      .pipe(finalize(() => (this.pdfLoading = false)))
      .subscribe({
        next: (blob) => {
          const pdfBlob =
            blob.type === 'application/pdf'
              ? blob
              : new Blob([blob], { type: 'application/pdf' });

          this.pdfObjectUrl = URL.createObjectURL(pdfBlob);
          this.pdfPreviewUrl = this.pdfObjectUrl;
          this.pdfPreviewSafeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
            this.pdfObjectUrl,
          );
        },
        error: (err) => {
          console.error('[CampaignPageRenderer] PDF preview failed:', err);
          this.pdfError =
            err?.error?.message ||
            'Unable to preview this PDF. The file may be private, unpublished, or unavailable.';
        },
      });
  }

  closePdfPreview(): void {
    this.pdfPreviewOpen = false;
    this.pdfLoading = false;
    this.pdfError = null;
    this.pdfPreviewTitle = '';
    this.pdfPreviewUrl = null;
    this.pdfPreviewSafeUrl = null;
    this.closePdfObjectUrl();
  }

  ngOnDestroy(): void {
    this.closePdfObjectUrl();
  }

  private fetchDocumentBlob(doc: any) {
    const applicationDocumentId = Number(doc?.applicationDocumentId);
    const docType = doc?.docType as DocumentType | undefined;
    const applicationRaiseId = Number(this.campaign?.applicationRaiseId);
    const isPublished = String(this.campaign?.status || '').toUpperCase() === 'PUBLISHED';

    if (isPublished && this.campaign?.slug && Number.isFinite(applicationDocumentId)) {
      return this.crowdfundingService.fetchPublicCampaignDocumentBlob(
        this.campaign.slug,
        applicationDocumentId,
      );
    }

    if (Number.isFinite(applicationRaiseId) && docType) {
      return this.crowdfundingService.fetchDocumentBlob(applicationRaiseId, docType);
    }

    if (this.campaign?.slug && Number.isFinite(applicationDocumentId)) {
      return this.crowdfundingService.fetchPublicCampaignDocumentBlob(
        this.campaign.slug,
        applicationDocumentId,
      );
    }

    return throwError(() => new Error('Missing document information for PDF preview.'));
  }

  private closePdfObjectUrl(): void {
    if (this.pdfObjectUrl) {
      URL.revokeObjectURL(this.pdfObjectUrl);
      this.pdfObjectUrl = null;
    }
  }

  documentLabel(element: any): string {
    const doc = this.documentForElement(element);
    const docType = doc?.docType ? this.formatLabel(doc.docType) : '';
    const fileName = doc?.fileName || '';

    if (docType && fileName) {
      return `${docType} · ${fileName}`;
    }

    if (fileName) {
      return fileName;
    }

    if (docType) {
      return docType;
    }

    const text = this.elementText(element).trim();
    return text && text !== 'Campaign document' ? text : 'Campaign document';
  }

  youtubeEmbed(url: string | null | undefined): SafeResourceUrl | null {
    const id = this.extractYouTubeId(url);

    if (!id) {
      return null;
    }

    return this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube.com/embed/${id}`);
  }

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }

    return String(value)
      .replace(/-/g, '_')
      .toLowerCase()
      .split('_')
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatMoney(value: number | null | undefined, currency: string | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
      return '—';
    }

    return new Intl.NumberFormat('en-TN', {
      style: 'currency',
      currency: currency || 'TND',
      maximumFractionDigits: 0,
    }).format(Number(value));
  }

  trackBySection(index: number, section: any): string {
    return String(section?.id || index);
  }

  trackByElement(index: number, element: any): string {
    return String(element?.id || index);
  }

  private legacyBlocksToSections(blocks: any[]): any[] {
    return blocks.map((block, index) => ({
      id: block?.id || `legacy-section-${index}`,
      type: block?.type || 'story',
      name: this.formatLabel(block?.type || `Section ${index + 1}`),
      height: 640,
      background: index === 0 ? '#fff7ed' : '#ffffff',
      elements: [
        {
          id: `${block?.id || index}-text`,
          type:
            block?.type === 'image'
              ? 'image'
              : block?.type === 'youtube'
                ? 'video'
                : block?.type === 'document'
                  ? 'document'
                  : block?.type === 'funding_cta'
                    ? 'funding'
                    : block?.type === 'header' || block?.type === 'hero'
                      ? 'title'
                      : 'text',
          text: block?.title || block?.content || block?.label || '',
          url: block?.url || '',
          documentId: block?.documentId ?? null,
          x: 10,
          y: 18,
          width: 80,
          height: block?.type === 'youtube' || block?.type === 'image' ? 58 : 24,
          zIndex: 1,
          fontSize: block?.type === 'header' || block?.type === 'hero' ? 54 : 22,
          fontWeight: block?.type === 'header' || block?.type === 'hero' ? 950 : 650,
          color: '#111827',
          background: 'transparent',
          borderColor: 'transparent',
          borderWidth: 0,
          radius: 0,
          opacity: 1,
          align: 'center',
        },
      ],
    }));
  }

  private extractYouTubeId(url: string | null | undefined): string | null {
    if (!url) {
      return null;
    }

    const trimmed = url.trim();
    const patterns = [
      /youtube\.com\/watch\?v=([^&]+)/,
      /youtube\.com\/embed\/([^?&]+)/,
      /youtu\.be\/([^?&]+)/,
      /youtube\.com\/shorts\/([^?&]+)/,
    ];

    for (const pattern of patterns) {
      const match = trimmed.match(pattern);
      if (match?.[1]) {
        return match[1];
      }
    }

    return null;
  }

  private normalizeOpacity(value: unknown): number {
    const parsed = Number(value);

    if (!Number.isFinite(parsed)) {
      return 1;
    }

    if (parsed <= 1) {
      return this.clamp(parsed, 0, 1);
    }

    return this.clamp(parsed, 0, 100) / 100;
  }

  private percent(value: unknown, fallback: number): number {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
      return fallback;
    }
    return this.clamp(parsed, 0, 100);
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(max, Math.max(min, value));
  }
}
