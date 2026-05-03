import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs/operators';
import {
  ApplicationDocumentResponse,
  CampaignBlockSize,
  CampaignBuilderBlock,
  CampaignBlockType,
  CampaignContentJson,
  CampaignPageResponse,
  CampaignPageStatus,
  CampaignStyleJson,
  DocumentType,
} from '../../../core/models/crowdfunding.models';
import { CrowdfundingService } from '../../../core/services/crowdfunding.service';

const DEFAULT_STYLE: CampaignStyleJson = {
  fontFamily: 'Inter',
  primaryColor: '#111827',
  accentColor: '#C9A227',
  radius: 'large',
  heroLayout: 'centered',
  buttonStyle: 'pill',
};

@Component({
  selector: 'app-youth-campaign-builder-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="builder-page">
      <header class="builder-topbar">
        <div>
          <span class="eyebrow">Campaign Studio</span>
          <h1>{{ campaign?.title || 'Campaign page' }}</h1>
          <p *ngIf="campaign">
            Design the public story for
            <strong>{{ campaign.businessName || 'your approved application' }}</strong>.
          </p>
        </div>

        <div class="topbar-actions">
          <a class="btn btn-ghost" routerLink="/youth/campaigns">Back</a>
          <a
            class="btn btn-secondary"
            *ngIf="campaign"
            [routerLink]="['/youth/campaigns', campaign.id, 'preview']"
          >
            Preview
          </a>
          <button class="btn btn-primary" type="button" [disabled]="!canEdit || saving" (click)="save()">
            {{ saving ? 'Saving...' : 'Save draft' }}
          </button>
        </div>
      </header>

      <p class="error" *ngIf="error">{{ error }}</p>
      <p class="success" *ngIf="success">{{ success }}</p>

      <div class="loading-card" *ngIf="loading">
        <span class="loader"></span>
        Loading campaign studio...
      </div>

      <div class="studio-grid" *ngIf="!loading && campaign">
        <main class="canvas-shell">
          <div class="browser-preview-bar">
            <span></span>
            <span></span>
            <span></span>
            <strong>/campaigns/{{ campaign.slug || 'your-link' }}</strong>
          </div>

          <section class="campaign-canvas" [ngStyle]="canvasStyles">
            <article
              class="campaign-block"
              *ngFor="let block of content.blocks; let i = index; trackBy: trackByBlockId"
              [ngClass]="blockClasses(block, i)"
              (dragover)="onDragOver(i, $event)"
              (dragleave)="onDragLeave(i)"
              (drop)="onDrop(i, $event)"
            >
              <div class="block-controls" *ngIf="canEdit">
                <button
                  class="drag-handle"
                  type="button"
                  draggable="true"
                  title="Drag to reorder"
                  (dragstart)="onDragStart(i, $event)"
                  (dragend)="onDragEnd()"
                >
                  ⋮⋮
                </button>

                <span class="block-type">{{ formatLabel(block.type) }}</span>

                <div class="size-control" aria-label="Block size">
                  <button
                    type="button"
                    *ngFor="let size of blockSizes"
                    [class.active]="blockSize(block) === size.value"
                    (click)="setBlockSize(block, size.value)"
                  >
                    {{ size.label }}
                  </button>
                </div>

                <button type="button" class="icon-btn" [disabled]="i === 0" (click)="moveBlock(i, -1)">↑</button>
                <button type="button" class="icon-btn" [disabled]="i === content.blocks.length - 1" (click)="moveBlock(i, 1)">↓</button>
                <button type="button" class="icon-btn danger" (click)="removeBlock(i)">×</button>
              </div>

              <ng-container [ngSwitch]="block.type">
                <section class="hero-block" [ngClass]="'hero-layout-' + style.heroLayout" *ngSwitchCase="'hero'">
                  <div class="hero-copy">
                    <span class="type-pill">{{ formatLabel(campaign.applicationType) }}</span>
                    <input
                      class="hero-title-input"
                      [(ngModel)]="block.title"
                      [readonly]="!canEdit"
                      placeholder="Campaign headline"
                    />
                    <textarea
                      class="hero-subtitle-input"
                      [(ngModel)]="block.subtitle"
                      [readonly]="!canEdit"
                      rows="3"
                      placeholder="Short emotional subtitle"
                    ></textarea>
                  </div>

                  <div class="hero-media-card">
                    <img
                      *ngIf="campaign.coverMediaUrl; else noCover"
                      class="cover-preview"
                      [src]="campaign.coverMediaUrl"
                      alt="Campaign cover"
                    />
                    <ng-template #noCover>
                      <div class="empty-media">
                        <span>Image</span>
                        <p>Add a cover image URL or use an image block as cover.</p>
                      </div>
                    </ng-template>
                    <input
                      class="media-input"
                      [(ngModel)]="campaign.coverMediaUrl"
                      [readonly]="!canEdit"
                      placeholder="Cover image URL"
                    />
                  </div>
                </section>

                <input
                  *ngSwitchCase="'header'"
                  class="header-input"
                  [(ngModel)]="block.content"
                  [readonly]="!canEdit"
                  placeholder="Large section header"
                />

                <input
                  *ngSwitchCase="'subheader'"
                  class="subheader-input"
                  [(ngModel)]="block.content"
                  [readonly]="!canEdit"
                  placeholder="Smaller subheading"
                />

                <textarea
                  *ngSwitchCase="'paragraph'"
                  class="paragraph-input"
                  [(ngModel)]="block.content"
                  [readonly]="!canEdit"
                  rows="5"
                  placeholder="Write your story here..."
                ></textarea>

                <section class="media-block" *ngSwitchCase="'image'">
                  <div class="media-fields">
                    <input
                      class="media-input"
                      [(ngModel)]="block.url"
                      [readonly]="!canEdit"
                      placeholder="Image URL"
                    />
                    <input
                      class="caption-input"
                      [(ngModel)]="block.label"
                      [readonly]="!canEdit"
                      placeholder="Image caption"
                    />
                    <button
                      class="mini-action"
                      type="button"
                      *ngIf="block.url && canEdit"
                      (click)="campaign.coverMediaUrl = block.url"
                    >
                      Use as cover image
                    </button>
                  </div>
                  <img *ngIf="block.url; else imagePlaceholder" [src]="block.url" [alt]="block.label || 'Campaign image'" />
                  <ng-template #imagePlaceholder>
                    <div class="empty-media compact-empty">
                      <span>Image block</span>
                      <p>Paste an image link and it will preview here.</p>
                    </div>
                  </ng-template>
                </section>

                <section class="media-block youtube-block" *ngSwitchCase="'youtube'">
                  <div class="media-fields">
                    <input
                      class="media-input"
                      [(ngModel)]="block.url"
                      [readonly]="!canEdit"
                      placeholder="Paste YouTube link"
                    />
                    <p class="helper">The video plays inside the studio and public campaign page.</p>
                  </div>

                  <div class="video-shell">
                    <ng-container *ngIf="youtubeEmbed(block.url) as videoUrl; else noVideo">
                      <iframe
                        [src]="videoUrl"
                        title="Campaign video"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        allowfullscreen
                      ></iframe>
                    </ng-container>
                    <ng-template #noVideo>
                      <div class="empty-media compact-empty">
                        <span>Video player</span>
                        <p>Paste a YouTube URL to preview the media player here.</p>
                      </div>
                    </ng-template>
                  </div>
                </section>

                <section class="document-block" *ngSwitchCase="'document'">
                  <label>
                    <span>Public document</span>
                    <select
                      [ngModel]="block.documentId ?? null"
                      [disabled]="!canEdit"
                      (ngModelChange)="setBlockDocument(block, $event)"
                    >
                      <option [ngValue]="null">Choose an approved document</option>
                      <option *ngFor="let doc of availableDocuments" [ngValue]="doc.id">
                        {{ formatDocumentType(doc.docType) }} · {{ doc.fileName }}
                      </option>
                    </select>
                  </label>
                  <input
                    class="caption-input"
                    [(ngModel)]="block.label"
                    [readonly]="!canEdit"
                    placeholder="Button label, e.g. View pitch deck"
                  />
                </section>

                <section class="funding-block" *ngSwitchCase="'funding_cta'">
                  <span>Funding goal</span>
                  <strong>{{ formatMoney(campaign.fundingGoal, campaign.currency) }}</strong>
                  <p>
                    {{ formatMoney(campaign.investorsPledgedAmount, campaign.currency) }} already pledged.
                  </p>
                </section>

                <hr *ngSwitchCase="'divider'" />
              </ng-container>
            </article>
          </section>
        </main>

        <aside class="right-panel">
          <section class="panel-card compact-official">
            <span class="panel-label">RNE / official information</span>
            <h2>{{ campaign.businessName || 'Official application' }}</h2>
            <dl>
              <div>
                <dt>Legal name</dt>
                <dd>{{ campaign.equityDetail?.companyLegalName || campaign.businessName || '—' }}</dd>
              </div>
              <div>
                <dt>RNE</dt>
                <dd>{{ campaign.equityDetail?.companyRegistrationNumber || 'Not provided' }}</dd>
              </div>
              <div>
                <dt>Type</dt>
                <dd>{{ formatLabel(campaign.applicationType) }}</dd>
              </div>
              <div>
                <dt>Location</dt>
                <dd>{{ formatLocation(campaign) }}</dd>
              </div>
            </dl>
          </section>

          <section class="panel-card">
            <span class="panel-label">Campaign controls</span>
            <label>
              <span>Title</span>
              <input [(ngModel)]="campaign.title" [readonly]="!canEdit" />
            </label>
            <label>
              <span>Subtitle</span>
              <textarea [(ngModel)]="campaign.subtitle" [readonly]="!canEdit" rows="3"></textarea>
            </label>
            <label>
              <span>Public slug</span>
              <input [(ngModel)]="campaign.slug" [readonly]="!canEdit" />
            </label>
            <div class="public-link">/campaigns/{{ campaign.slug }}</div>
            <div class="status-row">
              <span>Status</span>
              <strong [ngClass]="statusClass(campaign.status)">{{ formatLabel(campaign.status) }}</strong>
            </div>
            <p class="review-note" *ngIf="campaign.reviewNote">{{ campaign.reviewNote }}</p>
            <button class="btn btn-primary full" type="button" [disabled]="!canEdit || saving" (click)="save()">
              Save draft
            </button>
            <button
              class="btn btn-secondary full"
              type="button"
              [disabled]="!canEdit || saving"
              (click)="submit()"
            >
              Submit for review
            </button>
          </section>

          <section class="panel-card">
            <span class="panel-label">Style</span>
            <label>
              <span>Font</span>
              <select [(ngModel)]="style.fontFamily" [disabled]="!canEdit">
                <option *ngFor="let font of fonts" [value]="font">{{ font }}</option>
              </select>
            </label>
            <label>
              <span>Primary color</span>
              <input type="color" [(ngModel)]="style.primaryColor" [disabled]="!canEdit" />
            </label>
            <label>
              <span>Accent color</span>
              <input type="color" [(ngModel)]="style.accentColor" [disabled]="!canEdit" />
            </label>
            <label>
              <span>Hero layout</span>
              <select [(ngModel)]="style.heroLayout" [disabled]="!canEdit">
                <option value="centered">Centered</option>
                <option value="split">Split</option>
                <option value="editorial">Editorial</option>
              </select>
            </label>
          </section>

          <section class="panel-card">
            <span class="panel-label">Public documents</span>
            <p class="helper">Only safe application documents are available here. ID card and RIB stay private.</p>
            <label class="doc-choice" *ngFor="let doc of availableDocuments">
              <input
                type="checkbox"
                [disabled]="!canEdit"
                [checked]="isDocumentSelected(doc.id)"
                (change)="toggleDocument(doc.id)"
              />
              <span>
                <strong>{{ formatDocumentType(doc.docType) }}</strong>
                <small>{{ doc.fileName }}</small>
              </span>
            </label>
          </section>
        </aside>
      </div>

      <nav class="block-toolbar" *ngIf="!loading && campaign && canEdit">
        <button type="button" *ngFor="let item of blockTools" (click)="addBlock(item.type)">
          <span>{{ item.icon }}</span>
          {{ item.label }}
        </button>
      </nav>
    </section>
  `,
  styles: [
    `
      .builder-page {
        min-height: calc(100vh - 24px);
        display: grid;
        gap: 18px;
        padding-bottom: 96px;
      }

      .builder-topbar {
        position: sticky;
        top: 0;
        z-index: 12;
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 18px;
        flex-wrap: wrap;
        padding: 20px;
        border-radius: 28px;
        background: rgba(255, 255, 255, 0.92);
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
        backdrop-filter: blur(14px);
      }

      .eyebrow,
      .panel-label {
        display: inline-flex;
        color: #9a6a17;
        font-size: 0.72rem;
        font-weight: 950;
        letter-spacing: 0.08em;
        text-transform: uppercase;
      }

      .eyebrow {
        padding: 6px 11px;
        border-radius: 999px;
        background: #fff7ed;
      }

      h1 {
        margin: 8px 0 0;
        color: #1f2937;
        letter-spacing: -0.045em;
      }

      .builder-topbar p {
        margin: 6px 0 0;
        color: #667085;
        font-weight: 650;
      }

      .topbar-actions {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
      }

      .studio-grid {
        display: grid;
        grid-template-columns: minmax(0, 1fr) 360px;
        gap: 18px;
        align-items: start;
      }

      .canvas-shell {
        min-width: 0;
        display: grid;
        gap: 0;
        border-radius: 34px;
        overflow: hidden;
        background:
          radial-gradient(circle at top left, rgba(247, 215, 124, 0.32), transparent 30%),
          linear-gradient(135deg, #f8fafc 0%, #edf2f7 100%);
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: inset 0 1px 0 rgba(255,255,255,.8);
      }

      .browser-preview-bar {
        min-height: 46px;
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 0 18px;
        background: rgba(255, 255, 255, 0.8);
        border-bottom: 1px solid rgba(15, 23, 42, 0.08);
        backdrop-filter: blur(12px);
      }

      .browser-preview-bar span {
        width: 10px;
        height: 10px;
        border-radius: 999px;
        background: #d0d5dd;
      }

      .browser-preview-bar strong {
        margin-left: 8px;
        min-width: 0;
        color: #667085;
        font-size: .84rem;
        font-weight: 850;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .campaign-canvas {
        width: min(1240px, 100%);
        margin: 0 auto;
        display: grid;
        align-content: start;
        gap: 22px;
        padding: clamp(22px, 3.2vw, 46px);
      }

      .campaign-block {
        position: relative;
        width: 100%;
        justify-self: center;
        display: grid;
        gap: 12px;
        padding: clamp(18px, 2.3vw, 30px);
        border: 1px dashed rgba(15, 23, 42, 0.15);
        border-radius: 30px;
        background: rgba(255, 255, 255, 0.84);
        box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
        transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
      }

      .campaign-block:hover {
        border-color: rgba(201, 162, 39, 0.72);
        box-shadow: 0 22px 60px rgba(15, 23, 42, 0.11);
      }

      .campaign-block.drag-over {
        border-color: #d6a13d;
        box-shadow: 0 0 0 5px rgba(201, 162, 39, 0.16), 0 22px 60px rgba(15, 23, 42, 0.12);
        transform: translateY(-2px);
      }

      .campaign-block.is-dragging {
        opacity: 0.45;
      }

      .size-compact {
        max-width: 720px;
      }

      .size-normal {
        max-width: 940px;
      }

      .size-wide {
        max-width: 1120px;
      }

      .size-full {
        max-width: none;
      }

      .block-controls {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        color: #98a2b3;
        font-size: 0.72rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      .block-type {
        margin-right: auto;
      }

      .drag-handle,
      .icon-btn,
      .size-control button,
      .mini-action {
        border: 0;
        font: inherit;
        font-weight: 950;
        cursor: pointer;
      }

      .drag-handle,
      .icon-btn {
        width: 30px;
        height: 30px;
        display: grid;
        place-items: center;
        border-radius: 11px;
        background: #f2f4f7;
        color: #344054;
      }

      .drag-handle {
        cursor: grab;
        color: #9a6a17;
        background: #fff7ed;
      }

      .drag-handle:active {
        cursor: grabbing;
      }

      .icon-btn:disabled {
        opacity: 0.35;
        cursor: not-allowed;
      }

      .icon-btn.danger {
        color: #b42318;
        background: #fef2f2;
      }

      .size-control {
        display: inline-flex;
        align-items: center;
        padding: 3px;
        border-radius: 999px;
        background: #f2f4f7;
      }

      .size-control button {
        min-width: 34px;
        height: 26px;
        padding: 0 8px;
        border-radius: 999px;
        background: transparent;
        color: #667085;
        font-size: .68rem;
      }

      .size-control button.active {
        color: #271a08;
        background: #f7d77c;
        box-shadow: 0 4px 12px rgba(184, 130, 38, 0.18);
      }

      input,
      textarea,
      select {
        width: 100%;
        border: 1px solid rgba(15, 23, 42, 0.1);
        border-radius: 16px;
        padding: 12px 13px;
        background: #fff;
        color: #1f2937;
        font: inherit;
        font-weight: 700;
        outline: none;
        box-sizing: border-box;
      }

      input:focus,
      textarea:focus,
      select:focus {
        border-color: rgba(201, 162, 39, 0.65);
        box-shadow: 0 0 0 4px rgba(201, 162, 39, 0.14);
      }

      input[readonly],
      textarea[readonly] {
        background: #f8fafc;
      }

      .hero-block {
        display: grid;
        gap: 24px;
        align-items: center;
      }

      .hero-layout-centered,
      .hero-layout-editorial {
        justify-items: center;
        text-align: center;
      }

      .hero-layout-split {
        grid-template-columns: minmax(0, 0.94fr) minmax(320px, 1.06fr);
        text-align: left;
      }

      .hero-copy {
        display: grid;
        gap: 14px;
        width: 100%;
      }

      .type-pill {
        justify-self: inherit;
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

      .hero-title-input {
        border: 0;
        padding: 0;
        background: transparent;
        text-align: inherit;
        font-size: clamp(2.6rem, 7vw, 6.5rem);
        line-height: 0.88;
        font-weight: 950;
        letter-spacing: -0.08em;
        color: inherit;
      }

      .hero-subtitle-input {
        border: 0;
        padding: 0;
        resize: vertical;
        background: transparent;
        text-align: inherit;
        color: #667085;
        font-size: clamp(1rem, 2vw, 1.22rem);
        line-height: 1.75;
        font-weight: 700;
      }

      .hero-media-card {
        width: 100%;
        display: grid;
        gap: 10px;
      }

      .header-input {
        border: 0;
        padding: 0;
        background: transparent;
        font-size: clamp(2rem, 4vw, 3.4rem);
        font-weight: 950;
        line-height: 1;
        letter-spacing: -0.06em;
        color: inherit;
      }

      .subheader-input {
        border: 0;
        padding: 0;
        background: transparent;
        color: #475467;
        font-size: clamp(1.25rem, 2vw, 1.6rem);
        font-weight: 900;
        line-height: 1.35;
      }

      .paragraph-input {
        border: 0;
        padding: 0;
        resize: vertical;
        background: transparent;
        color: #475467;
        line-height: 1.9;
        font-size: 1.04rem;
        font-weight: 650;
      }

      .media-block {
        display: grid;
        gap: 16px;
      }

      .media-fields {
        display: grid;
        gap: 10px;
      }

      .media-input,
      .caption-input {
        background: #f8fafc;
      }

      .cover-preview,
      .media-block img {
        width: 100%;
        max-height: 620px;
        object-fit: cover;
        border-radius: 26px;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 44px rgba(15, 23, 42, 0.1);
      }

      .empty-media {
        min-height: 300px;
        display: grid;
        place-items: center;
        align-content: center;
        gap: 8px;
        padding: 24px;
        border-radius: 26px;
        border: 1px dashed rgba(15, 23, 42, 0.16);
        background:
          linear-gradient(135deg, rgba(255,255,255,.74), rgba(248,250,252,.88)),
          repeating-linear-gradient(45deg, rgba(15,23,42,.035) 0 8px, transparent 8px 16px);
        color: #98a2b3;
        text-align: center;
      }

      .compact-empty {
        min-height: 240px;
      }

      .empty-media span {
        color: #344054;
        font-weight: 950;
      }

      .empty-media p,
      .helper {
        margin: 0;
        color: #98a2b3;
        font-size: 0.82rem;
        line-height: 1.55;
        font-weight: 700;
      }

      .mini-action {
        width: fit-content;
        min-height: 36px;
        border-radius: 999px;
        padding: 0 13px;
        color: #271a08;
        background: #fff0c7;
      }

      .video-shell {
        position: relative;
        overflow: hidden;
        border-radius: 28px;
        background: #111827;
        aspect-ratio: 16 / 9;
        box-shadow: 0 18px 44px rgba(15, 23, 42, 0.12);
      }

      iframe {
        width: 100%;
        height: 100%;
        border: 0;
      }

      .document-block label,
      .panel-card label {
        display: grid;
        gap: 7px;
      }

      .document-block label span,
      .panel-card label span {
        color: #667085;
        font-size: 0.76rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .funding-block {
        justify-items: center;
        text-align: center;
        gap: 8px;
        background: linear-gradient(135deg, #fff7ed, #fff0c7);
        border-color: rgba(201, 162, 39, 0.25);
      }

      .funding-block span {
        color: #9a6a17;
        font-weight: 950;
        text-transform: uppercase;
        font-size: 0.78rem;
        letter-spacing: .08em;
      }

      .funding-block strong {
        color: #1f2937;
        font-size: clamp(2rem, 5vw, 3.4rem);
        letter-spacing: -.05em;
      }

      .funding-block p {
        margin: 0;
        color: #667085;
        font-weight: 800;
      }

      .right-panel {
        display: grid;
        gap: 14px;
        position: sticky;
        top: 118px;
      }

      .panel-card {
        display: grid;
        gap: 14px;
        padding: 18px;
        border-radius: 24px;
        background: #fff;
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 44px rgba(15, 23, 42, 0.07);
      }

      .compact-official h2 {
        margin: 0;
        color: #1f2937;
        letter-spacing: -0.03em;
      }

      dl {
        display: grid;
        gap: 10px;
        margin: 0;
      }

      dl div {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        padding-bottom: 10px;
        border-bottom: 1px solid rgba(15, 23, 42, 0.06);
      }

      dt {
        color: #98a2b3;
        font-weight: 900;
      }

      dd {
        margin: 0;
        color: #344054;
        font-weight: 900;
        text-align: right;
      }

      .public-link {
        padding: 11px 12px;
        border-radius: 14px;
        background: #f8fafc;
        color: #475467;
        font-weight: 850;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .status-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
      }

      .status-row span {
        color: #98a2b3;
        font-weight: 900;
      }

      .status-pill {
        padding: 7px 10px;
        border-radius: 999px;
        font-size: 0.72rem;
        font-weight: 950;
        text-transform: uppercase;
      }

      .status-draft { background: #fff7ed; color: #b45309; }
      .status-review { background: #eff6ff; color: #1d4ed8; }
      .status-published { background: #ecfdf5; color: #047857; }
      .status-changes { background: #fef2f2; color: #b42318; }

      .review-note {
        margin: 0;
        padding: 12px;
        border-radius: 16px;
        background: #fff7ed;
        color: #92400e;
        line-height: 1.6;
        font-weight: 750;
      }

      .doc-choice {
        display: flex !important;
        grid-template-columns: none !important;
        align-items: flex-start;
        gap: 10px;
        padding: 10px;
        border-radius: 16px;
        background: #f8fafc;
      }

      .doc-choice input {
        width: auto;
        margin-top: 4px;
      }

      .doc-choice span {
        display: grid;
        gap: 2px;
        text-transform: none !important;
        letter-spacing: 0 !important;
      }

      .doc-choice strong {
        color: #344054;
        font-size: 0.86rem;
      }

      .doc-choice small {
        color: #98a2b3;
        font-weight: 800;
      }

      .btn {
        min-height: 42px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        border: 0;
        border-radius: 999px;
        padding: 0 16px;
        font: inherit;
        font-weight: 950;
        text-decoration: none;
        cursor: pointer;
      }

      .btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .btn.full { width: 100%; }
      .btn-primary { color: #271a08; background: linear-gradient(135deg, #f7d77c, #d6a13d); box-shadow: 0 14px 30px rgba(184, 130, 38, 0.22); }
      .btn-secondary { color: #271a08; background: #fff0c7; border: 1px solid rgba(184, 130, 38, 0.18); }
      .btn-ghost { color: #344054; background: #f2f4f7; }

      .block-toolbar {
        position: fixed;
        left: 50%;
        bottom: 18px;
        z-index: 20;
        transform: translateX(-50%);
        display: flex;
        gap: 8px;
        max-width: min(980px, calc(100vw - 28px));
        overflow: auto;
        padding: 10px;
        border-radius: 999px;
        background: rgba(17, 24, 39, 0.92);
        box-shadow: 0 22px 70px rgba(15, 23, 42, 0.28);
        backdrop-filter: blur(16px);
      }

      .block-toolbar button {
        white-space: nowrap;
        display: inline-flex;
        align-items: center;
        gap: 7px;
        border: 0;
        border-radius: 999px;
        padding: 11px 14px;
        color: #fff;
        background: rgba(255, 255, 255, 0.1);
        font: inherit;
        font-weight: 900;
        cursor: pointer;
      }

      .block-toolbar button:hover {
        background: rgba(247, 215, 124, 0.22);
      }

      .loading-card {
        display: grid;
        justify-items: center;
        gap: 14px;
        padding: 38px 24px;
        border-radius: 28px;
        background: #fff;
        color: #475467;
        font-weight: 850;
      }

      .loader {
        width: 34px;
        height: 34px;
        border-radius: 50%;
        border: 4px solid rgba(184, 130, 38, 0.16);
        border-top-color: #d6a13d;
        animation: spin 0.75s linear infinite;
      }

      .error,
      .success {
        margin: 0;
        padding: 14px 16px;
        border-radius: 18px;
        font-weight: 900;
        border: 1px solid transparent;
      }

      .error { background: #fef2f2; color: #b42318; border-color: rgba(239, 68, 68, 0.16); }
      .success { background: #ecfdf5; color: #047857; border-color: rgba(16, 185, 129, 0.16); }

      @keyframes spin { to { transform: rotate(360deg); } }

      @media (max-width: 1240px) {
        .studio-grid { grid-template-columns: 1fr; }
        .right-panel { position: static; grid-template-columns: repeat(2, minmax(0, 1fr)); }
      }

      @media (max-width: 860px) {
        .campaign-canvas { padding: 18px; }
        .right-panel { grid-template-columns: 1fr; }
        .hero-layout-split { grid-template-columns: 1fr; text-align: center; }
        .hero-layout-split .type-pill { justify-self: center; }
      }
    `,
  ],
})
export class YouthCampaignBuilderPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly crowdfundingService = inject(CrowdfundingService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly blockTools: { type: CampaignBlockType; label: string; icon: string }[] = [
    { type: 'header', label: 'Header', icon: 'H1' },
    { type: 'subheader', label: 'Subheader', icon: 'H2' },
    { type: 'paragraph', label: 'Paragraph', icon: '¶' },
    { type: 'image', label: 'Image', icon: '▧' },
    { type: 'youtube', label: 'YouTube', icon: '▶' },
    { type: 'document', label: 'Document', icon: 'PDF' },
    { type: 'divider', label: 'Divider', icon: '—' },
    { type: 'funding_cta', label: 'Funding CTA', icon: 'TND' },
  ];

  readonly blockSizes: { value: CampaignBlockSize; label: string }[] = [
    { value: 'compact', label: 'S' },
    { value: 'normal', label: 'M' },
    { value: 'wide', label: 'L' },
    { value: 'full', label: 'Full' },
  ];

  readonly fonts = ['Inter', 'Poppins', 'Montserrat', 'Roboto', 'Lora', 'Playfair Display'];

  campaign: CampaignPageResponse | null = null;
  content: CampaignContentJson = { blocks: [] };
  style: CampaignStyleJson = { ...DEFAULT_STYLE };
  availableDocuments: ApplicationDocumentResponse[] = [];
  selectedPublicDocumentIds = new Set<number>();

  loading = false;
  saving = false;
  error: string | null = null;
  success: string | null = null;
  draggingIndex: number | null = null;
  dragOverIndex: number | null = null;

  get canEdit(): boolean {
    return (
      this.campaign?.status === CampaignPageStatus.DRAFT ||
      this.campaign?.status === CampaignPageStatus.CHANGES_REQUESTED
    );
  }

  get canvasStyles(): Record<string, string> {
    return {
      fontFamily: this.style.fontFamily,
      color: this.style.primaryColor,
      '--accent': this.style.accentColor,
    };
  }

  ngOnInit(): void {
    const campaignId = Number(this.route.snapshot.paramMap.get('id'));
    const applicationId = Number(this.route.snapshot.paramMap.get('applicationId'));

    if (Number.isFinite(campaignId) && campaignId > 0) {
      this.loadCampaign(campaignId);
      return;
    }

    if (Number.isFinite(applicationId) && applicationId > 0) {
      this.createThenOpen(applicationId);
      return;
    }

    this.error = 'Campaign route is missing an id.';
  }

  loadCampaign(id: number): void {
    this.loading = true;
    this.error = null;

    this.crowdfundingService
      .getCampaignPageBuilder(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (campaign) => this.hydrate(campaign),
        error: () => {
          this.error = 'Unable to load this campaign builder.';
        },
      });
  }

  createThenOpen(applicationId: number): void {
    this.loading = true;
    this.error = null;

    this.crowdfundingService
      .createCampaignPage(applicationId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (campaign) => {
          this.hydrate(campaign);
          this.router.navigate(['/youth/campaigns', campaign.id, 'builder'], { replaceUrl: true });
        },
        error: () => {
          this.error = 'Unable to create a campaign. The application must be approved first.';
        },
      });
  }

  hydrate(campaign: CampaignPageResponse): void {
    this.campaign = campaign;
    this.content = this.parseContent(campaign.contentJson, campaign);
    this.style = this.parseStyle(campaign.styleJson);
    this.availableDocuments = campaign.availableDocuments ?? [];
    this.selectedPublicDocumentIds = new Set(
      (campaign.publicDocuments ?? []).map((doc) => doc.applicationDocumentId),
    );
  }

  save(showSuccess = true): void {
    if (!this.campaign || !this.canEdit) return;

    this.saving = true;
    this.error = null;
    this.success = null;

    this.crowdfundingService
      .updateCampaignPage(this.campaign.id, this.buildPayload())
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (campaign) => {
          this.hydrate(campaign);
          if (showSuccess) this.success = 'Campaign draft saved.';
        },
        error: () => {
          this.error = 'Unable to save this campaign. Check the slug and content, then try again.';
        },
      });
  }

  submit(): void {
    if (!this.campaign || !this.canEdit) return;

    this.saving = true;
    this.error = null;
    this.success = null;

    const campaignId = this.campaign.id;

    this.crowdfundingService
      .updateCampaignPage(campaignId, this.buildPayload())
      .pipe(
        switchMap(() => this.crowdfundingService.submitCampaignPage(campaignId)),
        finalize(() => (this.saving = false)),
      )
      .subscribe({
        next: (campaign) => {
          this.hydrate(campaign);
          this.success = 'Campaign submitted for compliance review.';
        },
        error: () => {
          this.error = 'Unable to submit yet. Make sure the campaign has a title, slug, and enough content.';
        },
      });
  }

  addBlock(type: CampaignBlockType): void {
    this.content.blocks.push(this.createBlock(type));
  }

  removeBlock(index: number): void {
    if (this.content.blocks.length <= 1) return;
    this.content.blocks.splice(index, 1);
  }

  moveBlock(index: number, direction: -1 | 1): void {
    const nextIndex = index + direction;
    if (nextIndex < 0 || nextIndex >= this.content.blocks.length) return;

    const [block] = this.content.blocks.splice(index, 1);
    if (block) this.content.blocks.splice(nextIndex, 0, block);
  }

  onDragStart(index: number, event: DragEvent): void {
    if (!this.canEdit) return;
    this.draggingIndex = index;
    event.dataTransfer?.setData('text/plain', String(index));
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
  }

  onDragOver(index: number, event: DragEvent): void {
    if (!this.canEdit || this.draggingIndex === null || this.draggingIndex === index) return;
    event.preventDefault();
    this.dragOverIndex = index;
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
  }

  onDragLeave(index: number): void {
    if (this.dragOverIndex === index) this.dragOverIndex = null;
  }

  onDrop(index: number, event: DragEvent): void {
    event.preventDefault();
    if (!this.canEdit || this.draggingIndex === null || this.draggingIndex === index) {
      this.onDragEnd();
      return;
    }

    const [block] = this.content.blocks.splice(this.draggingIndex, 1);
    if (block) this.content.blocks.splice(index, 0, block);
    this.onDragEnd();
  }

  onDragEnd(): void {
    this.draggingIndex = null;
    this.dragOverIndex = null;
  }

  blockClasses(block: CampaignBuilderBlock, index: number): string[] {
    return [
      `block-${block.type}`,
      `size-${this.blockSize(block)}`,
      this.draggingIndex === index ? 'is-dragging' : '',
      this.dragOverIndex === index ? 'drag-over' : '',
    ].filter(Boolean);
  }

  blockSize(block: CampaignBuilderBlock): CampaignBlockSize {
    return block.size ?? this.defaultSizeFor(block.type);
  }

  setBlockSize(block: CampaignBuilderBlock, size: CampaignBlockSize): void {
    block.size = size;
  }

  youtubeEmbed(url: string | null | undefined): SafeResourceUrl | null {
    const id = this.extractYouTubeId(url);
    if (!id) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube.com/embed/${id}`);
  }

  setBlockDocument(block: CampaignBuilderBlock, documentId: number | null): void {
    block.documentId = documentId;
    if (documentId !== null) {
      this.selectedPublicDocumentIds.add(documentId);
      const doc = this.availableDocuments.find((item) => item.id === documentId);
      if (doc && !block.label) block.label = `View ${this.formatDocumentType(doc.docType)}`;
    }
  }

  toggleDocument(documentId: number): void {
    if (this.selectedPublicDocumentIds.has(documentId)) {
      this.selectedPublicDocumentIds.delete(documentId);
      return;
    }
    this.selectedPublicDocumentIds.add(documentId);
  }

  isDocumentSelected(documentId: number): boolean {
    return this.selectedPublicDocumentIds.has(documentId);
  }

  statusClass(status: CampaignPageStatus): string {
    switch (status) {
      case CampaignPageStatus.DRAFT:
        return 'status-pill status-draft';
      case CampaignPageStatus.PENDING_REVIEW:
        return 'status-pill status-review';
      case CampaignPageStatus.PUBLISHED:
        return 'status-pill status-published';
      case CampaignPageStatus.CHANGES_REQUESTED:
        return 'status-pill status-changes';
      case CampaignPageStatus.ARCHIVED:
        return 'status-pill status-draft';
      default:
        return 'status-pill status-draft';
    }
  }

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') return 'Not selected';
    return String(value)
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  formatDocumentType(type: DocumentType | null | undefined): string {
    return this.formatLabel(type);
  }

  formatLocation(campaign: CampaignPageResponse): string {
    const parts = [campaign.governorate, campaign.city].filter(Boolean);
    return parts.length ? parts.join(', ') : 'Not selected';
  }

  formatMoney(value: number | null | undefined, currency: string | null | undefined): string {
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

  private buildPayload() {
    const selectedFromBlocks = this.content.blocks
      .map((block) => block.documentId)
      .filter((id): id is number => typeof id === 'number');

    const publicDocumentIds = Array.from(
      new Set([...Array.from(this.selectedPublicDocumentIds), ...selectedFromBlocks]),
    );

    return {
      title: this.campaign?.title ?? null,
      subtitle: this.campaign?.subtitle ?? null,
      slug: this.campaign?.slug ?? null,
      coverMediaUrl: this.campaign?.coverMediaUrl ?? null,
      contentJson: JSON.stringify(this.content),
      styleJson: JSON.stringify(this.style),
      publicDocumentIds,
    };
  }

  private parseContent(value: string | null, campaign: CampaignPageResponse): CampaignContentJson {
    try {
      const parsed = JSON.parse(value || '') as Partial<CampaignContentJson>;
      if (Array.isArray(parsed.blocks) && parsed.blocks.length > 0) {
        return {
          blocks: parsed.blocks.map((block) => ({
            ...block,
            id: block.id || this.createId(),
            size: block.size ?? this.defaultSizeFor(block.type),
          })),
        };
      }
    } catch {
      // use default below
    }

    return {
      blocks: [
        {
          id: 'hero',
          type: 'hero',
          size: 'full',
          title: campaign.title || campaign.businessName || 'Your campaign headline',
          subtitle: campaign.subtitle || campaign.summary || 'Tell people why this campaign matters.',
        },
        {
          id: this.createId(),
          type: 'paragraph',
          size: 'normal',
          content: campaign.summary || 'Introduce your project, your mission, and the impact you want to create.',
        },
        { id: this.createId(), type: 'funding_cta', size: 'wide' },
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

  private createBlock(type: CampaignBlockType): CampaignBuilderBlock {
    const id = this.createId();
    switch (type) {
      case 'hero':
        return { id, type, size: 'full', title: 'New campaign headline', subtitle: 'Add a short promise or impact statement.' };
      case 'header':
        return { id, type, size: 'wide', content: 'New section title' };
      case 'subheader':
        return { id, type, size: 'normal', content: 'Supportive subheading' };
      case 'paragraph':
        return { id, type, size: 'normal', content: 'Write a clear paragraph about your project, team, traction, or impact.' };
      case 'image':
        return { id, type, size: 'wide', url: '', label: '' };
      case 'youtube':
        return { id, type, size: 'wide', url: '' };
      case 'document':
        return { id, type, size: 'compact', documentId: null, label: 'View document' };
      case 'funding_cta':
        return { id, type, size: 'wide' };
      case 'divider':
        return { id, type, size: 'wide' };
      default:
        return { id, type: 'paragraph', size: 'normal', content: '' };
    }
  }

  private defaultSizeFor(type: CampaignBlockType): CampaignBlockSize {
    switch (type) {
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

  private createId(): string {
    return `block-${Date.now()}-${Math.round(Math.random() * 100000)}`;
  }
}
