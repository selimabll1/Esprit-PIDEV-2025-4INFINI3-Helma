import { CommonModule } from "@angular/common";
import {
  Component,
  HostListener,
  OnDestroy,
  OnInit,
  inject,
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { DomSanitizer, SafeResourceUrl } from "@angular/platform-browser";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { finalize } from "rxjs/operators";
import {
  ApplicationDocumentResponse,
  CampaignPageResponse,
  CampaignPageStatus,
  CampaignStyleJson,
  CrowdfundingType,
  DocumentType,
} from "../../../core/models/crowdfunding.models";
import { CrowdfundingService } from "../../../core/services/crowdfunding.service";

type StudioSectionType =
  | "hero"
  | "story"
  | "problem_solution"
  | "video"
  | "impact"
  | "use_of_funds"
  | "documents"
  | "faq"
  | "cta"
  | "empty";

type StudioElementType =
  | "title"
  | "text"
  | "image"
  | "video"
  | "button"
  | "box"
  | "document"
  | "funding";

type TextAlign = "left" | "center" | "right";
type ObjectFit = "cover" | "contain";

type InteractionMode = "move" | "resize";

interface StudioSection {
  id: string;
  type: StudioSectionType;
  name: string;
  height: number;
  background: string;
  elements: StudioElement[];
}

interface StudioElement {
  id: string;
  type: StudioElementType;
  x: number;
  y: number;
  width: number;
  height: number;
  zIndex: number;
  text?: string | null;
  url?: string | null;
  href?: string | null;
  documentId?: number | null;
  color: string;
  background: string;
  borderColor: string;
  borderWidth: number;
  radius: number;
  opacity: number;
  fontSize: number;
  fontWeight: number;
  align: TextAlign;
  objectFit?: ObjectFit;
}

interface StudioInteraction {
  mode: InteractionMode;
  sectionId: string;
  elementId: string;
  startX: number;
  startY: number;
  startElement: StudioElement;
  rect: DOMRect;
}

const DEFAULT_STYLE: CampaignStyleJson = {
  fontFamily: "Inter",
  primaryColor: "#111827",
  accentColor: "#C9A227",
  radius: "large",
  heroLayout: "split",
  buttonStyle: "pill",
};

const SAFE_PUBLIC_DOCUMENT_TYPES = new Set<DocumentType>([
  DocumentType.PROJECT_PITCH_DECK,
  DocumentType.CNRE_EXTRACT,
  DocumentType.FINANCIAL_STATEMENTS,
]);

@Component({
  selector: "app-youth-campaign-builder-page",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="studio-page">
      <header class="studio-topbar">
        <div class="topbar-copy">
          <a class="back-link" routerLink="/youth/campaigns">← Campaigns</a>
          <span class="eyebrow">Campaign Studio</span>
          <h1>
            {{ campaign?.title || campaign?.businessName || "Campaign page" }}
          </h1>
          <p>
            Start from a professional campaign structure, then move, resize,
            align, recolor, and customize every object like a PowerPoint slide.
          </p>
        </div>

        <div class="topbar-actions" *ngIf="campaign">
          <a
            class="btn btn-ghost"
            [routerLink]="['/youth/campaigns', campaign.id, 'preview']"
          >
            Preview
          </a>
          <button
            class="btn btn-primary"
            type="button"
            [disabled]="!canEdit || saving"
            (click)="save()"
          >
            {{ saving ? "Saving..." : "Save draft" }}
          </button>
          <button
            class="btn btn-dark"
            type="button"
            [disabled]="!canSubmit || submitting"
            (click)="submitForReview()"
          >
            {{ submitting ? "Submitting..." : "Submit for review" }}
          </button>
        </div>
      </header>

      <nav class="power-ribbon" *ngIf="!loading && campaign">
        <div class="ribbon-group">
          <span>Arrange</span>
          <button
            type="button"
            [disabled]="!selectedElement"
            (click)="alignSelected('left')"
          >
            ⬅ Left
          </button>
          <button
            type="button"
            [disabled]="!selectedElement"
            (click)="alignSelected('center')"
          >
            ↔ Center
          </button>
          <button
            type="button"
            [disabled]="!selectedElement"
            (click)="alignSelected('right')"
          >
            Right ➡
          </button>
          <button
            type="button"
            [disabled]="!selectedElement"
            (click)="alignSelected('top')"
          >
            ⬆ Top
          </button>
          <button
            type="button"
            [disabled]="!selectedElement"
            (click)="alignSelected('middle')"
          >
            ↕ Middle
          </button>
          <button
            type="button"
            [disabled]="!selectedElement"
            (click)="alignSelected('bottom')"
          >
            Bottom ⬇
          </button>
        </div>

        <div class="ribbon-group">
          <span>Layer</span>
          <button
            type="button"
            [disabled]="!selectedElement"
            (click)="sendBackward()"
          >
            Send back
          </button>
          <button
            type="button"
            [disabled]="!selectedElement"
            (click)="bringForward()"
          >
            Bring front
          </button>
          <button
            type="button"
            [disabled]="!selectedElement"
            (click)="duplicateSelectedElement()"
          >
            Duplicate
          </button>
          <button
            type="button"
            [disabled]="!selectedElement"
            class="danger"
            (click)="deleteSelectedElement()"
          >
            Delete
          </button>
        </div>

        <div class="ribbon-group" *ngIf="selectedElement">
          <span>Text</span>
          <button
            type="button"
            [class.active]="selectedElement.align === 'left'"
            (click)="selectedElement.align = 'left'"
          >
            L
          </button>
          <button
            type="button"
            [class.active]="selectedElement.align === 'center'"
            (click)="selectedElement.align = 'center'"
          >
            C
          </button>
          <button
            type="button"
            [class.active]="selectedElement.align === 'right'"
            (click)="selectedElement.align = 'right'"
          >
            R
          </button>
          <input
            class="mini-number"
            type="number"
            min="10"
            max="96"
            [(ngModel)]="selectedElement.fontSize"
          />
          <input
            class="mini-number"
            type="number"
            min="300"
            max="950"
            step="50"
            [(ngModel)]="selectedElement.fontWeight"
          />
        </div>

        <div class="ribbon-group" *ngIf="selectedElement">
          <span>Color</span>
          <label class="color-chip"
            >Text <input type="color" [(ngModel)]="selectedElement.color"
          /></label>
          <label class="color-chip"
            >Fill <input type="color" [(ngModel)]="selectedElement.background"
          /></label>
          <label class="color-chip"
            >Line <input type="color" [(ngModel)]="selectedElement.borderColor"
          /></label>
        </div>
      </nav>

      <p class="error" *ngIf="error">{{ error }}</p>
      <p class="success" *ngIf="success">{{ success }}</p>

      <div class="loading-card" *ngIf="loading">
        <span class="loader"></span>
        Loading campaign studio...
      </div>

      <div class="studio-grid" *ngIf="!loading && campaign">
        <main class="workspace">
          <div class="browser-bar">
            <span></span><span></span><span></span>
            <strong>/campaigns/{{ campaign.slug || "your-campaign" }}</strong>
          </div>

          <section class="campaign-canvas" [ngStyle]="canvasStyles">
            <article
              class="slide-section"
              *ngFor="
                let section of sections;
                let sectionIndex = index;
                trackBy: trackBySectionId
              "
              [class.selected]="section.id === selectedSectionId"
              [class.drop-target]="sectionIndex === dropTargetIndex"
              [style.height.px]="section.height"
              [style.background]="section.background"
              (click)="selectSection(section.id)"
              (dragover)="onSectionDragOver(sectionIndex, $event)"
              (dragleave)="dropTargetIndex = null"
              (drop)="onSectionDrop(sectionIndex, $event)"
            >
              <div class="section-mini-toolbar" *ngIf="canEdit">
                <button
                  type="button"
                  class="drag-section"
                  title="Move section"
                  draggable="true"
                  (click)="$event.stopPropagation()"
                  (dragstart)="onSectionDragStart(sectionIndex, $event)"
                  (dragend)="onSectionDragEnd()"
                >
                  ⋮⋮
                </button>
                <span>{{ section.name }}</span>
                <button
                  type="button"
                  (click)="
                    duplicateSection(sectionIndex); $event.stopPropagation()
                  "
                >
                  Duplicate
                </button>
                <button
                  type="button"
                  class="danger"
                  (click)="
                    removeSection(sectionIndex); $event.stopPropagation()
                  "
                >
                  Delete
                </button>
              </div>

              <div
                class="canvas-element"
                *ngFor="
                  let element of section.elements;
                  trackBy: trackByElementId
                "
                [class.selected]="
                  element.id === selectedElementId &&
                  section.id === selectedSectionId
                "
                [class.textual]="isTextual(element)"
                [ngClass]="'element-' + element.type"
                [ngStyle]="elementStyles(element)"
                (mousedown)="startMove($event, section, element)"
                (click)="selectElement(section.id, element.id, $event)"
              >
                <ng-container [ngSwitch]="element.type">
                  <textarea
                    *ngSwitchCase="'title'"
                    class="editable-text title-object"
                    [readonly]="!canEdit"
                    [(ngModel)]="element.text"
                    [style.text-align]="element.align"
                    placeholder="Title"
                    spellcheck="true"
                    (mousedown)="selectElement(section.id, element.id, $event)"
                  ></textarea>

                  <textarea
                    *ngSwitchCase="'text'"
                    class="editable-text body-object"
                    [readonly]="!canEdit"
                    [(ngModel)]="element.text"
                    [style.text-align]="element.align"
                    placeholder="Write your text here..."
                    spellcheck="true"
                    (mousedown)="selectElement(section.id, element.id, $event)"
                  ></textarea>

                  <div *ngSwitchCase="'image'" class="image-object">
                    <img
                      *ngIf="element.url; else emptyImage"
                      [src]="element.url"
                      [style.object-fit]="element.objectFit || 'cover'"
                      alt="Campaign image"
                    />
                    <ng-template #emptyImage>
                      <div class="empty-object">🖼️ Image</div>
                    </ng-template>
                  </div>

                  <div *ngSwitchCase="'video'" class="video-object">
                    <iframe
                      *ngIf="
                        youtubeEmbed(element.url) as videoUrl;
                        else emptyVideo
                      "
                      [src]="videoUrl"
                      title="Campaign video"
                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                      allowfullscreen
                    ></iframe>
                    <ng-template #emptyVideo>
                      <div class="empty-object">▶️ Video</div>
                    </ng-template>
                  </div>

                  <div
                    *ngSwitchCase="'button'"
                    class="button-object"
                    [style.text-align]="element.align"
                    [style.justify-content]="justifyContent(element.align)"
                  >
                    <span>{{ element.text || "Button" }}</span>
                  </div>

                  <div *ngSwitchCase="'box'" class="box-object"></div>

                  <div *ngSwitchCase="'document'" class="document-object">
                    <span>PDF</span>
                    <strong>{{ documentName(element.documentId) }}</strong>
                    <button
                      type="button"
                      class="pdf-preview-chip"
                      [disabled]="!element.documentId"
                      (mousedown)="$event.stopPropagation()"
                      (click)="
                        openPdfPreview(element); $event.stopPropagation()
                      "
                    >
                      Preview
                    </button>
                  </div>

                  <div *ngSwitchCase="'funding'" class="funding-object">
                    <span>Funding goal</span>
                    <strong>{{
                      formatMoney(campaign.fundingGoal, campaign.currency)
                    }}</strong>
                    <small
                      >{{
                        formatMoney(
                          campaign.investorsPledgedAmount,
                          campaign.currency
                        )
                      }}
                      pledged</small
                    >
                  </div>
                </ng-container>

                <button
                  *ngIf="canEdit"
                  type="button"
                  class="object-move-handle"
                  title="Move object"
                  aria-label="Move object"
                  (mousedown)="startMoveFromHandle($event, section, element)"
                >
                  ⠿
                </button>

                <button
                  *ngIf="
                    canEdit &&
                    selectedElementId === element.id &&
                    selectedSectionId === section.id
                  "
                  type="button"
                  class="resize-handle"
                  aria-label="Resize object"
                  (mousedown)="startResize($event, section, element)"
                ></button>
              </div>
            </article>
          </section>
        </main>

        <aside class="inspector">
          <section class="panel-card" *ngIf="campaign">
            <span class="panel-title">Campaign</span>
            <label>
              <span>Title</span>
              <input [(ngModel)]="campaign.title" [readonly]="!canEdit" />
            </label>
            <label>
              <span>Subtitle</span>
              <input [(ngModel)]="campaign.subtitle" [readonly]="!canEdit" />
            </label>
            <label>
              <span>Slug</span>
              <input [(ngModel)]="campaign.slug" [readonly]="!canEdit" />
            </label>
          </section>

          <section class="panel-card" *ngIf="selectedSection">
            <span class="panel-title">Section</span>
            <label>
              <span>Name</span>
              <input [(ngModel)]="selectedSection.name" [readonly]="!canEdit" />
            </label>
            <label>
              <span>Height</span>
              <input
                type="range"
                min="360"
                max="980"
                step="20"
                [(ngModel)]="selectedSection.height"
                [disabled]="!canEdit"
              />
              <em>{{ selectedSection.height }}px</em>
            </label>
            <label>
              <span>Background</span>
              <input
                type="color"
                [(ngModel)]="selectedSection.background"
                [disabled]="!canEdit"
              />
            </label>
          </section>

          <section class="panel-card" *ngIf="selectedElement; else noElement">
            <span class="panel-title">Selected object</span>

            <div class="object-kind">
              {{ objectLabel(selectedElement.type) }}
            </div>

            <div class="inspector-grid">
              <label>
                <span>X</span>
                <input
                  type="number"
                  min="0"
                  max="100"
                  [(ngModel)]="selectedElement.x"
                />
              </label>
              <label>
                <span>Y</span>
                <input
                  type="number"
                  min="0"
                  max="100"
                  [(ngModel)]="selectedElement.y"
                />
              </label>
              <label>
                <span>W</span>
                <input
                  type="number"
                  min="2"
                  max="100"
                  [(ngModel)]="selectedElement.width"
                />
              </label>
              <label>
                <span>H</span>
                <input
                  type="number"
                  min="2"
                  max="100"
                  [(ngModel)]="selectedElement.height"
                />
              </label>
            </div>

            <label
              *ngIf="
                selectedElement.type === 'image' ||
                selectedElement.type === 'video'
              "
            >
              <span>{{
                selectedElement.type === "video" ? "YouTube link" : "Image URL"
              }}</span>
              <input
                [(ngModel)]="selectedElement.url"
                placeholder="Paste media URL"
              />
            </label>

            <label *ngIf="selectedElement.type === 'button'">
              <span>Button text</span>
              <input [(ngModel)]="selectedElement.text" />
            </label>

            <label *ngIf="selectedElement.type === 'button'">
              <span>Button link/action</span>
              <input
                [(ngModel)]="selectedElement.href"
                placeholder="/campaigns/... or #pledge"
              />
            </label>

            <label *ngIf="selectedElement.type === 'document'">
              <span>Document</span>
              <select [(ngModel)]="selectedElement.documentId">
                <option [ngValue]="null">Choose document</option>
                <option *ngFor="let doc of safeDocuments" [ngValue]="doc.id">
                  {{ formatLabel(doc.docType) }} · {{ doc.fileName }}
                </option>
              </select>
            </label>

            <button
              *ngIf="selectedElement.type === 'document'"
              class="inspector-action"
              type="button"
              [disabled]="!selectedElement.documentId"
              (click)="openPdfPreview(selectedElement)"
            >
              Open PDF preview
            </button>

            <label *ngIf="selectedElement.type === 'image'">
              <span>Image fit</span>
              <select [(ngModel)]="selectedElement.objectFit">
                <option value="cover">Cover</option>
                <option value="contain">Contain</option>
              </select>
            </label>

            <label
              *ngIf="
                isTextual(selectedElement) || selectedElement.type === 'button'
              "
            >
              <span>Text size</span>
              <input
                type="range"
                min="10"
                max="96"
                [(ngModel)]="selectedElement.fontSize"
              />
              <em>{{ selectedElement.fontSize }}px</em>
            </label>

            <label
              *ngIf="
                isTextual(selectedElement) || selectedElement.type === 'button'
              "
            >
              <span>Text weight</span>
              <input
                type="range"
                min="300"
                max="950"
                step="50"
                [(ngModel)]="selectedElement.fontWeight"
              />
              <em>{{ selectedElement.fontWeight }}</em>
            </label>

            <div class="inspector-grid">
              <label>
                <span>Text</span>
                <input type="color" [(ngModel)]="selectedElement.color" />
              </label>
              <label>
                <span>Fill</span>
                <input type="color" [(ngModel)]="selectedElement.background" />
              </label>
              <label>
                <span>Line</span>
                <input type="color" [(ngModel)]="selectedElement.borderColor" />
              </label>
              <label>
                <span>Line px</span>
                <input
                  type="number"
                  min="0"
                  max="8"
                  [(ngModel)]="selectedElement.borderWidth"
                />
              </label>
            </div>

            <label>
              <span>Corner radius</span>
              <input
                type="range"
                min="0"
                max="60"
                [(ngModel)]="selectedElement.radius"
              />
              <em>{{ selectedElement.radius }}px</em>
            </label>

            <label>
              <span>Opacity</span>
              <input
                type="range"
                min="0.1"
                max="1"
                step="0.05"
                [(ngModel)]="selectedElement.opacity"
              />
              <em>{{ selectedElement.opacity }}</em>
            </label>
          </section>

          <ng-template #noElement>
            <section class="panel-card empty-inspector">
              <span class="panel-title">Object inspector</span>
              <p>
                Click any object on the canvas to edit its size, color, text,
                position, and layer.
              </p>
            </section>
          </ng-template>

          <section class="panel-card official-card" *ngIf="campaign">
            <span class="panel-title">Official information</span>
            <dl>
              <div>
                <dt>Business</dt>
                <dd>{{ campaign.businessName || "—" }}</dd>
              </div>
              <div>
                <dt>Type</dt>
                <dd>{{ formatLabel(campaign.applicationType) }}</dd>
              </div>
              <div>
                <dt>Location</dt>
                <dd>{{ locationLabel }}</dd>
              </div>
              <div>
                <dt>Goal</dt>
                <dd>
                  {{ formatMoney(campaign.fundingGoal, campaign.currency) }}
                </dd>
              </div>
            </dl>
          </section>
        </aside>
      </div>

      <footer
        class="insert-dock"
        *ngIf="!loading && campaign && canEdit"
        [class.collapsed]="dockCollapsed"
        [style.width.px]="dockCollapsed ? 380 : dockWidth"
      >
        <div class="dock-header">
          <button
            class="dock-grip"
            type="button"
            (click)="dockCollapsed = !dockCollapsed"
            [title]="
              dockCollapsed ? 'Expand insert menu' : 'Collapse insert menu'
            "
          >
            {{ dockCollapsed ? "＋" : "−" }}
          </button>

          <div class="dock-title">
            <strong>Insert</strong>
            <small>{{
              selectedSection ? selectedSection.name : "Select a section first"
            }}</small>
          </div>

          <div class="dock-tabs" *ngIf="!dockCollapsed">
            <button
              type="button"
              [class.active]="dockTab === 'sections'"
              (click)="dockTab = 'sections'"
            >
              Sections
            </button>
            <button
              type="button"
              [class.active]="dockTab === 'objects'"
              (click)="dockTab = 'objects'"
            >
              Components
            </button>
          </div>

          <label class="dock-width" *ngIf="!dockCollapsed">
            <span>Width</span>
            <input
              type="range"
              min="760"
              max="1320"
              step="20"
              [(ngModel)]="dockWidth"
            />
          </label>
        </div>

        <div class="dock-body" *ngIf="!dockCollapsed">
          <div class="dock-group" *ngIf="dockTab === 'sections'">
            <button
              type="button"
              *ngFor="let item of sectionDock"
              (click)="addSection(item.type)"
            >
              <b>{{ item.icon }}</b>
              <span>{{ item.label }}</span>
            </button>
          </div>

          <div class="dock-group" *ngIf="dockTab === 'objects'">
            <button
              type="button"
              *ngFor="let item of objectDock"
              [disabled]="!selectedSection"
              (click)="addElement(item.type)"
            >
              <b>{{ item.icon }}</b>
              <span>{{ item.label }}</span>
            </button>
          </div>
        </div>
      </footer>

      <section
        class="pdf-preview-backdrop"
        *ngIf="pdfPreviewOpen"
        (click)="closePdfPreview()"
      >
        <article class="pdf-preview-modal" (click)="$event.stopPropagation()">
          <header>
            <div>
              <span class="eyebrow">Document preview</span>
              <h2>{{ pdfPreviewTitle || "Campaign document" }}</h2>
            </div>
            <button type="button" class="pdf-close" (click)="closePdfPreview()">
              ×
            </button>
          </header>

          <div class="pdf-loading" *ngIf="pdfLoading">
            <span class="loader"></span>
            Loading PDF preview...
          </div>

          <div class="pdf-error" *ngIf="pdfPreviewError && !pdfLoading">
            <strong>PDF preview could not be displayed here.</strong>
            <p>{{ pdfPreviewError }}</p>
            <a
              *ngIf="pdfPreviewObjectUrl"
              [href]="pdfPreviewObjectUrl"
              target="_blank"
              rel="noopener"
            >
              Open PDF in a new tab
            </a>
          </div>

          <object
            *ngIf="pdfPreviewUrl && !pdfLoading && !pdfPreviewError"
            [data]="pdfPreviewUrl"
            type="application/pdf"
            class="pdf-object"
          >
            <iframe [src]="pdfPreviewUrl" title="PDF preview"></iframe>
            <div class="pdf-error">
              <strong>Your browser could not render the PDF inline.</strong>
              <a
                *ngIf="pdfPreviewObjectUrl"
                [href]="pdfPreviewObjectUrl"
                target="_blank"
                rel="noopener"
              >
                Open PDF in a new tab
              </a>
            </div>
          </object>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .studio-page {
        display: grid;
        gap: 18px;
        padding-bottom: 112px;
      }

      .studio-topbar,
      .power-ribbon,
      .panel-card,
      .workspace {
        border: 1px solid rgba(15, 23, 42, 0.08);
        box-shadow: 0 18px 44px rgba(15, 23, 42, 0.07);
      }

      .studio-topbar {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 22px;
        flex-wrap: wrap;
        padding: 26px;
        border-radius: 32px;
        background:
          radial-gradient(
            circle at top left,
            rgba(201, 162, 39, 0.18),
            transparent 35%
          ),
          #ffffff;
      }

      .back-link {
        display: inline-flex;
        margin-bottom: 10px;
        color: #475467;
        font-weight: 900;
        text-decoration: none;
      }

      .eyebrow,
      .panel-title {
        color: #9a6a17;
        font-size: 0.73rem;
        font-weight: 950;
        letter-spacing: 0.09em;
        text-transform: uppercase;
      }

      h1 {
        margin: 8px 0;
        color: #111827;
        font-size: clamp(2rem, 4vw, 3.35rem);
        letter-spacing: -0.065em;
        line-height: 0.95;
      }

      .topbar-copy p {
        max-width: 760px;
        margin: 0;
        color: #667085;
        line-height: 1.7;
        font-weight: 650;
      }

      .topbar-actions,
      .power-ribbon,
      .ribbon-group,
      .insert-dock,
      .dock-group {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;
      }

      .btn,
      .power-ribbon button,
      .section-mini-toolbar button,
      .insert-dock button {
        min-height: 38px;
        border: 0;
        border-radius: 999px;
        padding: 0 14px;
        font: inherit;
        font-weight: 950;
        cursor: pointer;
      }

      .btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-height: 44px;
        text-decoration: none;
      }

      button:disabled,
      .btn:disabled {
        opacity: 0.52;
        cursor: not-allowed;
      }

      .btn-primary {
        color: #fff;
        background: linear-gradient(135deg, #c9a227, #9a6a17);
        box-shadow: 0 16px 34px rgba(154, 106, 23, 0.24);
      }

      .btn-dark {
        color: #fff;
        background: #111827;
        box-shadow: 0 16px 34px rgba(17, 24, 39, 0.22);
      }

      .btn-ghost {
        color: #344054;
        background: #f2f4f7;
      }

      .power-ribbon {
        position: sticky;
        top: 10px;
        z-index: 30;
        padding: 12px;
        border-radius: 24px;
        background: rgba(255, 255, 255, 0.92);
        backdrop-filter: blur(18px);
      }

      .ribbon-group {
        padding: 8px;
        border-radius: 18px;
        background: #f8fafc;
      }

      .ribbon-group span {
        color: #98a2b3;
        font-size: 0.68rem;
        font-weight: 950;
        letter-spacing: 0.08em;
        text-transform: uppercase;
      }

      .power-ribbon button {
        min-height: 32px;
        background: #fff;
        color: #344054;
        border: 1px solid rgba(15, 23, 42, 0.08);
      }

      .power-ribbon button.active {
        color: #9a6a17;
        background: #fff7ed;
        border-color: rgba(201, 162, 39, 0.38);
      }

      .power-ribbon button.danger,
      .section-mini-toolbar button.danger {
        color: #b42318;
        background: #fef2f2;
      }

      .mini-number {
        width: 70px;
        min-height: 32px;
        border: 1px solid rgba(15, 23, 42, 0.1);
        border-radius: 12px;
        padding: 0 8px;
        font: inherit;
        font-weight: 850;
      }

      .color-chip {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        color: #475467;
        font-size: 0.8rem;
        font-weight: 900;
      }

      .color-chip input {
        width: 28px;
        height: 28px;
        padding: 0;
        border: 0;
        border-radius: 9px;
        background: transparent;
      }

      .error,
      .success,
      .loading-card {
        padding: 14px 16px;
        border-radius: 18px;
        font-weight: 900;
      }

      .error {
        background: #fef2f2;
        color: #b42318;
        border: 1px solid rgba(239, 68, 68, 0.18);
      }

      .success {
        background: #ecfdf5;
        color: #047857;
        border: 1px solid rgba(16, 185, 129, 0.18);
      }

      .loading-card {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10px;
        background: #fff;
        color: #475467;
        border: 1px solid rgba(15, 23, 42, 0.08);
      }

      .loader {
        width: 18px;
        height: 18px;
        border-radius: 999px;
        border: 3px solid #e5e7eb;
        border-top-color: #c9a227;
        animation: spin 0.8s linear infinite;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      .studio-grid {
        display: grid;
        grid-template-columns: minmax(0, 1fr) 360px;
        gap: 18px;
        align-items: start;
      }

      .workspace {
        min-width: 0;
        overflow: hidden;
        border-radius: 34px;
        background: #e9edf3;
      }

      .browser-bar {
        display: flex;
        align-items: center;
        gap: 8px;
        min-height: 50px;
        padding: 0 18px;
        background: #111827;
        color: #e5e7eb;
      }

      .browser-bar span {
        width: 11px;
        height: 11px;
        border-radius: 999px;
        background: #f87171;
      }
      .browser-bar span:nth-child(2) {
        background: #fbbf24;
      }
      .browser-bar span:nth-child(3) {
        background: #34d399;
      }
      .browser-bar strong {
        margin-left: 10px;
        font-size: 0.86rem;
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
        overflow: hidden;
        border-bottom: 1px solid rgba(15, 23, 42, 0.08);
        background-image:
          linear-gradient(rgba(15, 23, 42, 0.035) 1px, transparent 1px),
          linear-gradient(90deg, rgba(15, 23, 42, 0.035) 1px, transparent 1px);
        background-size: 24px 24px;
      }

      .slide-section.selected {
        outline: 3px solid rgba(201, 162, 39, 0.44);
        outline-offset: -3px;
      }

      .slide-section.drop-target {
        box-shadow: inset 0 0 0 4px rgba(5, 150, 105, 0.32);
      }

      .section-mini-toolbar {
        position: absolute;
        top: 12px;
        left: 12px;
        z-index: 100;
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px;
        border-radius: 999px;
        background: rgba(17, 24, 39, 0.86);
        color: #ffffff;
        backdrop-filter: blur(12px);
        opacity: 0;
        pointer-events: none;
        transition: opacity 0.16s ease;
      }

      .slide-section:hover .section-mini-toolbar,
      .slide-section.selected .section-mini-toolbar {
        opacity: 1;
        pointer-events: auto;
      }

      .section-mini-toolbar span {
        padding: 0 6px;
        font-size: 0.78rem;
        font-weight: 950;
      }

      .section-mini-toolbar button {
        min-height: 30px;
        background: rgba(255, 255, 255, 0.14);
        color: #ffffff;
      }

      .drag-section {
        width: 34px;
        padding: 0 !important;
        cursor: grab;
      }

      .canvas-element {
        position: absolute;
        box-sizing: border-box;
        display: flex;
        overflow: hidden;
        user-select: none;
        cursor: move;
      }

      .canvas-element.selected {
        outline: 2px solid #2563eb;
        outline-offset: 3px;
        box-shadow: 0 0 0 8px rgba(37, 99, 235, 0.11);
      }

      .canvas-element.textual {
        overflow: visible;
      }

      .editable-text {
        width: 100%;
        height: 100%;
        border: 0;
        padding: 0;
        margin: 0;
        outline: none;
        color: inherit;
        background: transparent;
        font: inherit;
        font-size: inherit;
        font-weight: inherit;
        resize: none;
        overflow: hidden;
        white-space: pre-wrap;
        word-break: break-word;
        cursor: text;
        direction: ltr;
        unicode-bidi: plaintext;
        writing-mode: horizontal-tb;
      }

      .editable-text:read-only {
        cursor: default;
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
        pointer-events: none;
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
      }

      .button-object {
        border: 0;
        font: inherit;
        font-weight: inherit;
        color: inherit;
        background: transparent;
        cursor: move;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 12px;
        box-sizing: border-box;
        line-height: 1.1;
        white-space: normal;
        overflow-wrap: anywhere;
        word-break: break-word;
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
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        box-sizing: border-box;
      }

      .document-object span {
        width: 42px;
        height: 42px;
        display: grid;
        place-items: center;
        border-radius: 14px;
        background: #fff7ed;
        color: #9a6a17;
        font-weight: 950;
      }

      .document-object strong {
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
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

      .object-move-handle {
        position: absolute;
        top: 6px;
        left: 6px;
        z-index: 20;
        width: 28px;
        height: 28px;
        display: grid;
        place-items: center;
        border: 2px solid #ffffff;
        border-radius: 999px;
        background: #111827;
        color: #ffffff;
        box-shadow: 0 8px 20px rgba(17, 24, 39, 0.28);
        cursor: grab;
        opacity: 0;
        pointer-events: none;
        transition:
          opacity 0.14s ease,
          transform 0.14s ease;
      }

      .canvas-element:hover .object-move-handle,
      .canvas-element.selected .object-move-handle {
        opacity: 1;
        pointer-events: auto;
      }

      .object-move-handle:active {
        cursor: grabbing;
        transform: scale(0.96);
      }

      .resize-handle {
        position: absolute;
        right: -8px;
        bottom: -8px;
        width: 18px;
        height: 18px;
        border: 2px solid #ffffff;
        border-radius: 999px;
        background: #2563eb;
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.38);
        cursor: nwse-resize;
      }

      .inspector {
        position: sticky;
        top: 92px;
        display: grid;
        gap: 14px;
      }

      .panel-card {
        display: grid;
        gap: 13px;
        padding: 18px;
        border-radius: 24px;
        background: #ffffff;
      }

      .panel-card label {
        display: grid;
        gap: 7px;
      }

      .panel-card label span,
      .object-kind {
        color: #667085;
        font-size: 0.76rem;
        font-weight: 950;
        letter-spacing: 0.04em;
        text-transform: uppercase;
      }

      .panel-card input,
      .panel-card select {
        min-height: 40px;
        border: 1px solid rgba(15, 23, 42, 0.1);
        border-radius: 14px;
        padding: 0 11px;
        background: #fff;
        color: #111827;
        font: inherit;
        font-weight: 750;
        outline: none;
        box-sizing: border-box;
      }

      .panel-card input[type="color"] {
        padding: 4px;
      }

      .panel-card input[type="range"] {
        padding: 0;
      }

      .panel-card em {
        color: #98a2b3;
        font-style: normal;
        font-weight: 900;
      }

      .inspector-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
      }

      .empty-inspector p {
        margin: 0;
        color: #667085;
        line-height: 1.6;
        font-weight: 700;
      }

      dl {
        display: grid;
        gap: 8px;
        margin: 0;
      }

      dl div {
        display: flex;
        justify-content: space-between;
        gap: 10px;
        padding: 10px 12px;
        border-radius: 14px;
        background: #f8fafc;
      }

      dt {
        color: #98a2b3;
        font-weight: 950;
      }

      dd {
        margin: 0;
        min-width: 0;
        color: #344054;
        font-weight: 900;
        text-align: right;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .insert-dock {
        position: fixed;
        left: 50%;
        bottom: 18px;
        z-index: 50;
        transform: translateX(-50%);
        max-width: calc(100vw - 32px);
        display: grid;
        gap: 10px;
        padding: 10px;
        border-radius: 30px;
        background: linear-gradient(
          135deg,
          rgba(11, 18, 32, 0.98),
          rgba(25, 33, 49, 0.96)
        );
        color: #ffffff;
        box-shadow: 0 24px 80px rgba(15, 23, 42, 0.42);
        backdrop-filter: blur(20px);
        border: 1px solid rgba(255, 255, 255, 0.12);
      }

      .insert-dock.collapsed {
        border-radius: 999px;
      }

      .dock-header {
        display: flex;
        align-items: center;
        gap: 10px;
        min-width: 0;
      }

      .dock-grip {
        width: 40px;
        min-width: 40px;
        min-height: 40px !important;
        padding: 0 !important;
        display: grid !important;
        place-items: center;
        color: #111827 !important;
        background: #ffffff !important;
        border: 0 !important;
        font-size: 1.1rem !important;
      }

      .dock-title {
        display: grid;
        min-width: 150px;
        line-height: 1.1;
      }

      .dock-title strong {
        color: #ffffff;
        font-size: 0.95rem;
        font-weight: 950;
      }

      .dock-title small {
        color: rgba(255, 255, 255, 0.52);
        font-weight: 800;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .dock-tabs {
        display: inline-flex;
        gap: 6px;
        padding: 5px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.08);
      }

      .dock-tabs button,
      .dock-width,
      .dock-group button {
        display: inline-flex;
        align-items: center;
      }

      .dock-tabs button {
        min-height: 34px !important;
        color: rgba(255, 255, 255, 0.7) !important;
        background: transparent !important;
        border: 0 !important;
      }

      .dock-tabs button.active {
        color: #111827 !important;
        background: #ffffff !important;
      }

      .dock-width {
        margin-left: auto;
        gap: 8px;
        color: rgba(255, 255, 255, 0.58);
        font-size: 0.72rem;
        font-weight: 950;
        text-transform: uppercase;
        letter-spacing: 0.07em;
      }

      .dock-width input {
        width: 120px;
      }

      .dock-body {
        overflow-x: auto;
        scrollbar-width: thin;
        padding-bottom: 2px;
      }

      .dock-group {
        display: flex;
        align-items: center;
        gap: 9px;
        flex-wrap: nowrap;
      }

      .dock-group button {
        flex: 0 0 auto;
        min-width: 92px;
        min-height: 66px;
        flex-direction: column;
        justify-content: center;
        gap: 5px;
        color: #f9fafb;
        background: rgba(255, 255, 255, 0.085);
        border: 1px solid rgba(255, 255, 255, 0.11);
        border-radius: 20px;
      }

      .dock-group button:hover {
        background: rgba(255, 255, 255, 0.16);
        transform: translateY(-1px);
      }

      .dock-group button b {
        width: 30px;
        height: 30px;
        display: grid;
        place-items: center;
        border-radius: 12px;
        background: rgba(255, 255, 255, 0.12);
        color: #ffffff;
        font-size: 0.95rem;
        font-weight: 950;
        font-family:
          ui-sans-serif,
          system-ui,
          -apple-system,
          BlinkMacSystemFont,
          "Segoe UI",
          sans-serif;
      }

      .dock-group button span {
        color: rgba(255, 255, 255, 0.72);
        font-size: 0.72rem;
        font-weight: 950;
        letter-spacing: 0.04em;
        text-transform: uppercase;
      }

      .pdf-preview-chip {
        margin-left: auto;
        min-height: 34px;
        border: 0;
        border-radius: 999px;
        padding: 0 12px;
        background: #111827;
        color: #ffffff;
        font: inherit;
        font-size: 0.78rem;
        font-weight: 950;
        cursor: pointer;
      }

      .pdf-preview-chip:disabled {
        opacity: 0.45;
        cursor: not-allowed;
      }

      .inspector-action {
        min-height: 42px;
        border: 0;
        border-radius: 999px;
        background: #111827;
        color: #ffffff;
        font: inherit;
        font-weight: 950;
        cursor: pointer;
      }

      .inspector-action:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .pdf-preview-backdrop {
        position: fixed;
        inset: 0;
        z-index: 100;
        display: grid;
        place-items: center;
        padding: 24px;
        background: rgba(15, 23, 42, 0.68);
        backdrop-filter: blur(10px);
      }

      .pdf-preview-modal {
        width: min(1100px, 96vw);
        height: min(860px, 92vh);
        display: grid;
        grid-template-rows: auto 1fr;
        overflow: hidden;
        border-radius: 30px;
        background: #ffffff;
        box-shadow: 0 30px 100px rgba(0, 0, 0, 0.35);
      }

      .pdf-preview-modal header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 16px;
        padding: 18px 20px;
        border-bottom: 1px solid rgba(15, 23, 42, 0.08);
      }

      .pdf-preview-modal h2 {
        margin: 4px 0 0;
        color: #111827;
        font-size: 1.25rem;
        letter-spacing: -0.035em;
      }

      .pdf-close {
        width: 42px;
        height: 42px;
        border: 0;
        border-radius: 999px;
        background: #f2f4f7;
        color: #111827;
        font-size: 1.6rem;
        line-height: 1;
        cursor: pointer;
      }

      .pdf-preview-modal iframe,
      .pdf-object {
        width: 100%;
        height: 100%;
        border: 0;
        background: #f8fafc;
      }

      .pdf-object {
        display: block;
      }

      .pdf-error {
        display: grid;
        place-items: center;
        align-content: center;
        gap: 10px;
        padding: 30px;
        color: #475467;
        text-align: center;
        font-weight: 800;
      }

      .pdf-error strong {
        color: #111827;
        font-size: 1.05rem;
      }

      .pdf-error p {
        margin: 0;
      }

      .pdf-error a {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-height: 40px;
        padding: 0 14px;
        border-radius: 999px;
        background: #111827;
        color: #ffffff;
        text-decoration: none;
        font-weight: 950;
      }

      .pdf-loading {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10px;
        color: #475467;
        font-weight: 950;
      }

      @media (max-width: 1180px) {
        .studio-grid {
          grid-template-columns: 1fr;
        }
        .inspector {
          position: static;
        }
      }

      @media (max-width: 760px) {
        .power-ribbon {
          position: static;
        }
        .slide-section {
          height: auto !important;
          min-height: 560px;
        }
        .insert-dock {
          position: static;
          transform: none;
          max-width: 100%;
          width: 100% !important;
          box-sizing: border-box;
        }
        .dock-header {
          flex-wrap: wrap;
        }
        .dock-width {
          width: 100%;
          margin-left: 0;
        }
      }
    `,
  ],
})
export class YouthCampaignBuilderPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly crowdfundingService = inject(CrowdfundingService);

  readonly sectionDock: Array<{
    type: StudioSectionType;
    label: string;
    icon: string;
  }> = [
    { type: "hero", label: "Hero", icon: "H" },
    { type: "story", label: "Story", icon: "✎" },
    { type: "problem_solution", label: "Problem", icon: "⚙" },
    { type: "video", label: "Video", icon: "▶" },
    { type: "impact", label: "Impact", icon: "▥" },
    { type: "use_of_funds", label: "Funds", icon: "$" },
    { type: "documents", label: "Docs", icon: "PDF" },
    { type: "cta", label: "CTA", icon: "↗" },
    { type: "empty", label: "Empty", icon: "+" },
  ];

  readonly objectDock: Array<{
    type: StudioElementType;
    label: string;
    icon: string;
  }> = [
    { type: "title", label: "Title", icon: "Tt" },
    { type: "text", label: "Text", icon: "¶" },
    { type: "image", label: "Image", icon: "▧" },
    { type: "video", label: "Video", icon: "▶" },
    { type: "button", label: "Button", icon: "◉" },
    { type: "box", label: "Box", icon: "□" },
    { type: "document", label: "PDF", icon: "PDF" },
    { type: "funding", label: "Funding", icon: "$" },
  ];

  campaign: CampaignPageResponse | null = null;
  style: CampaignStyleJson = { ...DEFAULT_STYLE };
  sections: StudioSection[] = [];
  selectedSectionId: string | null = null;
  selectedElementId: string | null = null;
  draggingSectionIndex: number | null = null;
  dropTargetIndex: number | null = null;
  interaction: StudioInteraction | null = null;
  dockTab: "sections" | "objects" = "sections";
  dockCollapsed = false;
  dockWidth = 1120;

  pdfPreviewOpen = false;
  pdfPreviewUrl: SafeResourceUrl | null = null;
  pdfPreviewObjectUrl: string | null = null;
  pdfPreviewTitle = "";
  pdfPreviewError = "";
  pdfLoading = false;

  loading = false;
  saving = false;
  submitting = false;
  error: string | null = null;
  success: string | null = null;

  get canEdit(): boolean {
    return (
      !!this.campaign &&
      [CampaignPageStatus.DRAFT, CampaignPageStatus.CHANGES_REQUESTED].includes(
        this.campaign.status,
      )
    );
  }

  get canSubmit(): boolean {
    return this.canEdit && this.sections.length > 0 && !this.saving;
  }

  get selectedSection(): StudioSection | null {
    return (
      this.sections.find((section) => section.id === this.selectedSectionId) ??
      null
    );
  }

  get selectedElement(): StudioElement | null {
    const section = this.selectedSection;
    if (!section || !this.selectedElementId) return null;
    return (
      section.elements.find(
        (element) => element.id === this.selectedElementId,
      ) ?? null
    );
  }

  get safeDocuments(): ApplicationDocumentResponse[] {
    const docs = this.campaign?.availableDocuments ?? [];
    return docs.filter((doc) => SAFE_PUBLIC_DOCUMENT_TYPES.has(doc.docType));
  }

  get locationLabel(): string {
    if (!this.campaign) return "—";
    return (
      [this.campaign.city, this.campaign.governorate]
        .filter(Boolean)
        .join(", ") || "—"
    );
  }

  get canvasStyles(): Record<string, string> {
    return {
      fontFamily: this.style.fontFamily || "Inter",
      color: this.style.primaryColor || "#111827",
    };
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get("id"));
    if (!Number.isFinite(id) || id <= 0) {
      this.error = "Campaign id is missing.";
      return;
    }
    this.load(id);
  }

  ngOnDestroy(): void {
    this.revokePdfPreviewUrl();
  }

  load(id: number): void {
    this.loading = true;
    this.error = null;
    this.success = null;

    this.crowdfundingService
      .getCampaignPageBuilder(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (campaign) => {
          this.campaign = campaign;
          this.style = this.parseStyle(campaign.styleJson);
          this.sections = this.parseSections(campaign);
          this.selectedSectionId = this.sections[0]?.id ?? null;
          this.selectedElementId = null;
        },
        error: (err) => {
          console.error("[CampaignStudio] load failed:", err);
          this.error = err?.error?.message || "Unable to load campaign studio.";
        },
      });
  }

  save(): void {
    if (!this.campaign || !this.canEdit) return;

    this.saving = true;
    this.error = null;
    this.success = null;

    const heroTitle =
      this.findFirstElementText("hero", "title") || this.campaign.title;
    const heroSubtitle =
      this.findFirstElementText("hero", "text") || this.campaign.subtitle;
    const coverMediaUrl =
      this.findFirstElementUrl("hero", "image") || this.campaign.coverMediaUrl;
    const publicDocumentIds = this.collectPublicDocumentIds();

    this.crowdfundingService
      .updateCampaignPage(this.campaign.id, {
        title: heroTitle,
        subtitle: heroSubtitle,
        slug: this.campaign.slug,
        coverMediaUrl,
        contentJson: JSON.stringify({
          version: 4,
          editor: "helma-presentation-studio",
          sections: this.sections,
        }),
        styleJson: JSON.stringify(this.style),
        publicDocumentIds,
      })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (updated) => {
          this.campaign = updated;
          this.success = "Campaign saved.";
        },
        error: (err) => {
          console.error("[CampaignStudio] save failed:", err);
          this.error = err?.error?.message || "Unable to save campaign.";
        },
      });
  }

  submitForReview(): void {
    if (!this.campaign || !this.canSubmit) return;

    this.saving = true;
    this.error = null;
    this.success = null;

    const heroTitle =
      this.findFirstElementText("hero", "title") || this.campaign.title;
    const heroSubtitle =
      this.findFirstElementText("hero", "text") || this.campaign.subtitle;
    const coverMediaUrl =
      this.findFirstElementUrl("hero", "image") || this.campaign.coverMediaUrl;
    const publicDocumentIds = this.collectPublicDocumentIds();

    this.crowdfundingService
      .updateCampaignPage(this.campaign.id, {
        title: heroTitle,
        subtitle: heroSubtitle,
        slug: this.campaign.slug,
        coverMediaUrl,
        contentJson: JSON.stringify({
          version: 4,
          editor: "helma-presentation-studio",
          sections: this.sections,
        }),
        styleJson: JSON.stringify(this.style),
        publicDocumentIds,
      })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (updated) => {
          this.campaign = updated;
          this.doSubmit(updated.id);
        },
        error: (err) => {
          console.error("[CampaignStudio] save before submit failed:", err);
          this.error =
            err?.error?.message || "Unable to save campaign before submission.";
        },
      });
  }

  doSubmit(id: number): void {
    this.submitting = true;
    this.crowdfundingService
      .submitCampaignPage(id)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (updated) => {
          this.campaign = updated;
          this.success = "Campaign submitted for compliance review.";
          void this.router.navigate(["/youth/campaigns"]);
        },
        error: (err) => {
          console.error("[CampaignStudio] submit failed:", err);
          this.error = err?.error?.message || "Unable to submit campaign.";
        },
      });
  }

  addSection(type: StudioSectionType): void {
    if (!this.canEdit) return;
    const section = this.createSection(type);
    const selectedIndex = this.sections.findIndex(
      (item) => item.id === this.selectedSectionId,
    );
    const insertAt =
      selectedIndex >= 0 ? selectedIndex + 1 : this.sections.length;
    this.sections.splice(insertAt, 0, section);
    this.selectedSectionId = section.id;
    this.selectedElementId = section.elements[0]?.id ?? null;
  }

  duplicateSection(index: number): void {
    if (!this.canEdit) return;
    const clone = this.deepClone(this.sections[index]);
    clone.id = this.uid("section");
    clone.name = `${clone.name} copy`;
    clone.elements = clone.elements.map((element) => ({
      ...element,
      id: this.uid("element"),
    }));
    this.sections.splice(index + 1, 0, clone);
    this.selectedSectionId = clone.id;
    this.selectedElementId = clone.elements[0]?.id ?? null;
  }

  removeSection(index: number): void {
    if (!this.canEdit) return;
    if (this.sections.length === 1) {
      this.error = "Keep at least one section in your campaign.";
      return;
    }
    const removed = this.sections.splice(index, 1)[0];
    if (removed?.id === this.selectedSectionId) {
      this.selectedSectionId =
        this.sections[Math.max(0, index - 1)]?.id ?? null;
      this.selectedElementId = null;
    }
  }

  addElement(type: StudioElementType): void {
    const section = this.selectedSection;
    if (!this.canEdit || !section) return;
    const element = this.createElement(
      type,
      12,
      16,
      section.elements.length + 1,
    );
    section.elements.push(element);
    this.selectedElementId = element.id;
  }

  duplicateSelectedElement(): void {
    const section = this.selectedSection;
    const element = this.selectedElement;
    if (!this.canEdit || !section || !element) return;
    const clone = {
      ...this.deepClone(element),
      id: this.uid("element"),
      x: this.clamp(element.x + 4, 0, 92),
      y: this.clamp(element.y + 4, 0, 92),
      zIndex: this.nextZ(section),
    };
    section.elements.push(clone);
    this.selectedElementId = clone.id;
  }

  deleteSelectedElement(): void {
    const section = this.selectedSection;
    if (!this.canEdit || !section || !this.selectedElementId) return;
    section.elements = section.elements.filter(
      (element) => element.id !== this.selectedElementId,
    );
    this.selectedElementId = null;
  }

  bringForward(): void {
    const section = this.selectedSection;
    const element = this.selectedElement;
    if (!section || !element) return;
    element.zIndex = this.nextZ(section);
  }

  sendBackward(): void {
    const element = this.selectedElement;
    if (!element) return;
    element.zIndex = Math.max(1, element.zIndex - 1);
  }

  alignSelected(
    mode: "left" | "center" | "right" | "top" | "middle" | "bottom",
  ): void {
    const element = this.selectedElement;
    if (!element) return;

    switch (mode) {
      case "left":
        element.x = 6;
        break;
      case "center":
        element.x = this.round((100 - element.width) / 2);
        break;
      case "right":
        element.x = this.round(94 - element.width);
        break;
      case "top":
        element.y = 8;
        break;
      case "middle":
        element.y = this.round((100 - element.height) / 2);
        break;
      case "bottom":
        element.y = this.round(92 - element.height);
        break;
    }
  }

  selectSection(sectionId: string): void {
    this.selectedSectionId = sectionId;
    if (
      !this.selectedSection?.elements.some(
        (element) => element.id === this.selectedElementId,
      )
    ) {
      this.selectedElementId = null;
    }
  }

  selectElement(sectionId: string, elementId: string, event?: Event): void {
    event?.stopPropagation();
    this.selectedSectionId = sectionId;
    this.selectedElementId = elementId;
  }

  startMoveFromHandle(
    event: MouseEvent,
    section: StudioSection,
    element: StudioElement,
  ): void {
    if (!this.canEdit) return;
    event.preventDefault();
    event.stopPropagation();
    this.beginInteraction("move", event, section, element);
  }

  startMove(
    event: MouseEvent,
    section: StudioSection,
    element: StudioElement,
  ): void {
    if (!this.canEdit) return;
    const target = event.target as HTMLElement;

    // Text objects use a textarea so typing stays normal. Use the black ⠿ handle
    // to move them instead of stealing normal text selection/cursor behavior.
    if (target.closest(".resize-handle") || target.closest("textarea")) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    this.beginInteraction("move", event, section, element);
  }

  private beginInteraction(
    mode: InteractionMode,
    event: MouseEvent,
    section: StudioSection,
    element: StudioElement,
  ): void {
    this.selectElement(section.id, element.id);
    const rect = (event.currentTarget as HTMLElement)
      .closest(".slide-section")
      ?.getBoundingClientRect();
    if (!rect) return;
    this.interaction = {
      mode,
      sectionId: section.id,
      elementId: element.id,
      startX: event.clientX,
      startY: event.clientY,
      startElement: this.deepClone(element),
      rect,
    };
  }

  startResize(
    event: MouseEvent,
    section: StudioSection,
    element: StudioElement,
  ): void {
    if (!this.canEdit) return;
    event.preventDefault();
    event.stopPropagation();
    const rect = (event.currentTarget as HTMLElement)
      .closest(".slide-section")
      ?.getBoundingClientRect();
    if (!rect) return;
    this.interaction = {
      mode: "resize",
      sectionId: section.id,
      elementId: element.id,
      startX: event.clientX,
      startY: event.clientY,
      startElement: this.deepClone(element),
      rect,
    };
  }

  @HostListener("document:mousemove", ["$event"])
  onDocumentMouseMove(event: MouseEvent): void {
    if (!this.interaction) return;
    const section = this.sections.find(
      (item) => item.id === this.interaction?.sectionId,
    );
    const element = section?.elements.find(
      (item) => item.id === this.interaction?.elementId,
    );
    if (!section || !element) return;

    const dx =
      ((event.clientX - this.interaction.startX) /
        this.interaction.rect.width) *
      100;
    const dy =
      ((event.clientY - this.interaction.startY) /
        this.interaction.rect.height) *
      100;

    if (this.interaction.mode === "move") {
      element.x = this.snap(
        this.clamp(
          this.interaction.startElement.x + dx,
          0,
          100 - element.width,
        ),
      );
      element.y = this.snap(
        this.clamp(
          this.interaction.startElement.y + dy,
          0,
          100 - element.height,
        ),
      );
      return;
    }

    element.width = this.snap(
      this.clamp(this.interaction.startElement.width + dx, 4, 100 - element.x),
    );
    element.height = this.snap(
      this.clamp(this.interaction.startElement.height + dy, 4, 100 - element.y),
    );
  }

  @HostListener("document:mouseup")
  onDocumentMouseUp(): void {
    this.interaction = null;
  }

  onSectionDragStart(index: number, event: DragEvent): void {
    this.draggingSectionIndex = index;
    event.dataTransfer?.setData("text/plain", String(index));
    if (event.dataTransfer) event.dataTransfer.effectAllowed = "move";
  }

  onSectionDragOver(index: number, event: DragEvent): void {
    if (this.draggingSectionIndex === null) return;
    event.preventDefault();
    this.dropTargetIndex = index;
  }

  onSectionDrop(index: number, event: DragEvent): void {
    event.preventDefault();
    if (
      this.draggingSectionIndex === null ||
      this.draggingSectionIndex === index
    ) {
      this.onSectionDragEnd();
      return;
    }
    const [moved] = this.sections.splice(this.draggingSectionIndex, 1);
    this.sections.splice(index, 0, moved);
    this.selectedSectionId = moved.id;
    this.onSectionDragEnd();
  }

  onSectionDragEnd(): void {
    this.draggingSectionIndex = null;
    this.dropTargetIndex = null;
  }

  updateElementText(element: StudioElement, event: Event): void {
    const target = event.target as HTMLElement;
    element.text = target.innerText;
  }

  justifyContent(align: TextAlign | null | undefined): string {
    if (align === "left") return "flex-start";
    if (align === "right") return "flex-end";
    return "center";
  }

  elementStyles(element: StudioElement): Record<string, string> {
    const isText = this.isTextual(element);
    return {
      left: `${element.x}%`,
      top: `${element.y}%`,
      width: `${element.width}%`,
      height: `${element.height}%`,
      zIndex: String(element.zIndex),
      color: element.color,
      background:
        isText && element.background === "#ffffff"
          ? "transparent"
          : element.background,
      border: `${element.borderWidth}px solid ${element.borderColor}`,
      borderRadius: `${element.radius}px`,
      opacity: String(element.opacity),
      fontSize: `${element.fontSize}px`,
      fontWeight: String(element.fontWeight),
      textAlign: element.align,
      alignItems: this.isTextual(element) ? "flex-start" : "stretch",
      justifyContent: "stretch",
    };
  }

  youtubeEmbed(url: string | null | undefined): SafeResourceUrl | null {
    const id = this.extractYouTubeId(url);
    if (!id) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.youtube.com/embed/${id}`,
    );
  }

  isTextual(element: StudioElement): boolean {
    return element.type === "title" || element.type === "text";
  }

  objectLabel(type: StudioElementType): string {
    return this.objectDock.find((item) => item.type === type)?.label ?? type;
  }

  documentName(documentId: number | null | undefined): string {
    if (!documentId) return "Choose document";
    const doc = this.safeDocuments.find((item) => item.id === documentId);
    return doc
      ? `${this.formatLabel(doc.docType)} · ${doc.fileName}`
      : "Campaign document";
  }

  openPdfPreview(element: StudioElement): void {
    if (!this.campaign || !element.documentId) {
      this.error = "Choose a document before opening the preview.";
      return;
    }

    const doc = this.safeDocuments.find(
      (item) => item.id === element.documentId,
    );
    if (!doc) {
      this.error = "This document cannot be previewed publicly.";
      return;
    }

    this.pdfPreviewOpen = true;
    this.pdfLoading = true;
    this.pdfPreviewTitle = `${this.formatLabel(doc.docType)} · ${doc.fileName}`;
    this.pdfPreviewError = "";
    this.revokePdfPreviewUrl();

    this.crowdfundingService
      .fetchDocumentBlob(this.campaign.applicationRaiseId, doc.docType)
      .subscribe({
        next: (blob) => {
          if (!blob || blob.size === 0) {
            this.pdfLoading = false;
            this.pdfPreviewError = "The document response was empty.";
            return;
          }

          const pdfBlob =
            blob.type === "application/pdf"
              ? blob
              : new Blob([blob], { type: "application/pdf" });

          this.pdfPreviewObjectUrl = URL.createObjectURL(pdfBlob);
          this.pdfPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
            this.pdfPreviewObjectUrl,
          );
          this.pdfLoading = false;
        },
        error: (err) => {
          console.error("[CampaignStudio] PDF preview failed:", err);
          this.pdfLoading = false;
          this.pdfPreviewError = err?.error?.message || "Unable to open the PDF preview.";
          this.error = this.pdfPreviewError;
        },
      });
  }

  closePdfPreview(): void {
    this.pdfPreviewOpen = false;
    this.pdfLoading = false;
    this.pdfPreviewTitle = "";
    this.pdfPreviewError = "";
    this.revokePdfPreviewUrl();
  }

  private revokePdfPreviewUrl(): void {
    if (this.pdfPreviewObjectUrl) {
      URL.revokeObjectURL(this.pdfPreviewObjectUrl);
    }
    this.pdfPreviewObjectUrl = null;
    this.pdfPreviewUrl = null;
  }

  formatLabel(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === "") return "—";
    return String(value)
      .toLowerCase()
      .split("_")
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(" ");
  }

  formatMoney(
    value: number | null | undefined,
    currency: string | null | undefined,
  ): string {
    if (value === null || value === undefined) return "—";
    return new Intl.NumberFormat("en-TN", {
      style: "currency",
      currency: currency || "TND",
      maximumFractionDigits: 0,
    }).format(value);
  }

  trackBySectionId(_index: number, section: StudioSection): string {
    return section.id;
  }

  trackByElementId(_index: number, element: StudioElement): string {
    return element.id;
  }

  private parseStyle(value: string | null): CampaignStyleJson {
    try {
      return {
        ...DEFAULT_STYLE,
        ...(JSON.parse(value || "{}") as Partial<CampaignStyleJson>),
      };
    } catch {
      return { ...DEFAULT_STYLE };
    }
  }

  private parseSections(campaign: CampaignPageResponse): StudioSection[] {
    try {
      const parsed = JSON.parse(campaign.contentJson || "{}") as {
        sections?: StudioSection[];
      };
      if (Array.isArray(parsed.sections) && parsed.sections.length > 0) {
        return parsed.sections.map((section) => this.normalizeSection(section));
      }
    } catch {
      // template fallback below
    }

    return this.defaultCampaignSections(campaign);
  }

  private normalizeSection(
    section: Partial<StudioSection> & { type?: string },
  ): StudioSection {
    const normalized: StudioSection = {
      id: section.id || this.uid("section"),
      type: this.normalizeSectionType(section.type),
      name:
        section.name ||
        this.defaultSectionName(this.normalizeSectionType(section.type)),
      height: Number(section.height) || 640,
      background: section.background || "#ffffff",
      elements: Array.isArray(section.elements)
        ? section.elements.map((element, index) =>
            this.normalizeElement(element, index),
          )
        : [],
    };

    // Backward compatibility: if an older guided section had fields but no canvas objects,
    // convert those fields into editable objects.
    if (normalized.elements.length === 0) {
      const fallback = this.createSection(normalized.type);
      normalized.elements = fallback.elements;
    }

    return normalized;
  }

  private normalizeSectionType(value: string | undefined): StudioSectionType {
    const allowed: StudioSectionType[] = [
      "hero",
      "story",
      "problem_solution",
      "video",
      "impact",
      "use_of_funds",
      "documents",
      "faq",
      "cta",
      "empty",
    ];
    return allowed.includes(value as StudioSectionType)
      ? (value as StudioSectionType)
      : "empty";
  }

  private normalizeElement(
    raw: Partial<StudioElement> & { type?: string },
    index: number,
  ): StudioElement {
    const type = this.normalizeElementType(raw.type);
    const base = this.createElement(
      type,
      Number(raw.x) || 10,
      Number(raw.y) || 12,
      index + 1,
    );
    return {
      ...base,
      ...raw,
      id: raw.id || base.id,
      type,
      x: this.clamp(Number(raw.x ?? base.x), 0, 100),
      y: this.clamp(Number(raw.y ?? base.y), 0, 100),
      width: this.clamp(Number(raw.width ?? base.width), 4, 100),
      height: this.clamp(Number(raw.height ?? base.height), 4, 100),
      zIndex: Number(raw.zIndex ?? base.zIndex),
      borderWidth: Number(raw.borderWidth ?? base.borderWidth),
      radius: Number(raw.radius ?? base.radius),
      opacity: Number(raw.opacity ?? base.opacity),
      fontSize: Number(raw.fontSize ?? base.fontSize),
      fontWeight: Number(raw.fontWeight ?? base.fontWeight),
      align: (raw.align as TextAlign) || base.align,
      objectFit: (raw.objectFit as ObjectFit) || base.objectFit,
    };
  }

  private normalizeElementType(value: string | undefined): StudioElementType {
    if (value === "text_box") return "text";
    if (value === "image_box") return "image";
    if (value === "shape") return "box";
    const allowed: StudioElementType[] = [
      "title",
      "text",
      "image",
      "video",
      "button",
      "box",
      "document",
      "funding",
    ];
    return allowed.includes(value as StudioElementType)
      ? (value as StudioElementType)
      : "text";
  }

  private defaultCampaignSections(
    campaign: CampaignPageResponse,
  ): StudioSection[] {
    const sections = [
      this.createSection("hero", campaign),
      this.createSection("story", campaign),
      this.createSection("problem_solution", campaign),
      campaign.applicationType === CrowdfundingType.EQUITY
        ? this.createSection("impact", campaign)
        : this.createSection("use_of_funds", campaign),
      this.createSection("documents", campaign),
      this.createSection("cta", campaign),
    ];
    return sections;
  }

  private createSection(
    type: StudioSectionType,
    campaign: CampaignPageResponse | null = this.campaign,
  ): StudioSection {
    const id = this.uid("section");
    const goal = this.formatMoney(campaign?.fundingGoal, campaign?.currency);
    const title = campaign?.title || campaign?.businessName || "Your campaign";
    const subtitle =
      campaign?.subtitle ||
      campaign?.summary ||
      "Tell people why this campaign matters.";
    const image = campaign?.coverMediaUrl || "";
    const primary = this.style.accentColor || "#C9A227";
    const dark = "#111827";
    const muted = "#667085";

    switch (type) {
      case "hero":
        return {
          id,
          type,
          name: "Hero section",
          height: 720,
          background: "#fffaf0",
          elements: [
            this.element(
              "title",
              6,
              18,
              46,
              22,
              1,
              title,
              null,
              "#111827",
              "transparent",
              "transparent",
              0,
              0,
              68,
              900,
              "left",
            ),
            this.element(
              "text",
              7,
              44,
              40,
              15,
              2,
              subtitle,
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              22,
              650,
              "left",
            ),
            this.element(
              "button",
              7,
              64,
              18,
              8,
              3,
              campaign?.applicationType === CrowdfundingType.EQUITY
                ? "Invest now"
                : "Donate now",
              null,
              "#ffffff",
              primary,
              primary,
              0,
              999,
              18,
              900,
              "center",
            ),
            this.element(
              "funding",
              28,
              62,
              22,
              15,
              4,
              goal,
              null,
              "#111827",
              "#ffffff",
              "rgba(15,23,42,0.08)",
              1,
              26,
              17,
              850,
              "center",
            ),
            this.element(
              "image",
              55,
              14,
              38,
              66,
              5,
              null,
              image,
              "#111827",
              "#ffffff",
              "rgba(15,23,42,0.08)",
              1,
              34,
              16,
              700,
              "center",
            ),
          ],
        };

      case "story":
        return {
          id,
          type,
          name: "Story section",
          height: 620,
          background: "#ffffff",
          elements: [
            this.element(
              "title",
              8,
              14,
              34,
              14,
              1,
              "Our story",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              48,
              900,
              "left",
            ),
            this.element(
              "text",
              8,
              32,
              42,
              42,
              2,
              campaign?.summary ||
                "Share the human story behind the project. Explain why you started, who you serve, and what makes this mission important.",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              20,
              600,
              "left",
            ),
            this.element(
              "image",
              58,
              16,
              34,
              58,
              3,
              null,
              image,
              dark,
              "#f8fafc",
              "rgba(15,23,42,0.08)",
              1,
              30,
              16,
              700,
              "center",
            ),
          ],
        };

      case "problem_solution":
        return {
          id,
          type,
          name: "Problem / solution",
          height: 620,
          background: "#f8fafc",
          elements: [
            this.element(
              "title",
              8,
              10,
              50,
              12,
              1,
              "Problem & solution",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              46,
              900,
              "left",
            ),
            this.element(
              "box",
              8,
              30,
              38,
              42,
              2,
              null,
              null,
              dark,
              "#ffffff",
              "rgba(15,23,42,0.08)",
              1,
              28,
              16,
              700,
              "left",
            ),
            this.element(
              "title",
              12,
              36,
              28,
              9,
              3,
              "The problem",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              30,
              900,
              "left",
            ),
            this.element(
              "text",
              12,
              48,
              28,
              18,
              4,
              campaign?.problemStatement ||
                "Explain the pain point clearly. What is difficult, expensive, slow, risky, or missing today?",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              18,
              600,
              "left",
            ),
            this.element(
              "box",
              54,
              30,
              38,
              42,
              5,
              null,
              null,
              dark,
              "#fff7ed",
              "rgba(201,162,39,0.24)",
              1,
              28,
              16,
              700,
              "left",
            ),
            this.element(
              "title",
              58,
              36,
              28,
              9,
              6,
              "Our solution",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              30,
              900,
              "left",
            ),
            this.element(
              "text",
              58,
              48,
              28,
              18,
              7,
              campaign?.solution ||
                "Explain what you built and why it is better than the current alternative.",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              18,
              600,
              "left",
            ),
          ],
        };

      case "video":
        return {
          id,
          type,
          name: "Video section",
          height: 660,
          background: "#111827",
          elements: [
            this.element(
              "title",
              8,
              10,
              60,
              11,
              1,
              "Watch our pitch",
              null,
              "#ffffff",
              "transparent",
              "transparent",
              0,
              0,
              46,
              900,
              "left",
            ),
            this.element(
              "text",
              8,
              24,
              48,
              10,
              2,
              "Add a YouTube link to make the campaign more personal and convincing.",
              null,
              "#d1d5db",
              "transparent",
              "transparent",
              0,
              0,
              19,
              600,
              "left",
            ),
            this.element(
              "video",
              16,
              38,
              68,
              46,
              3,
              null,
              null,
              "#ffffff",
              "#020617",
              "rgba(255,255,255,0.1)",
              1,
              28,
              16,
              700,
              "center",
            ),
          ],
        };

      case "impact":
        return {
          id,
          type,
          name: "Impact / traction",
          height: 600,
          background: "#ecfdf5",
          elements: [
            this.element(
              "title",
              8,
              12,
              50,
              12,
              1,
              campaign?.applicationType === CrowdfundingType.EQUITY
                ? "Traction so far"
                : "Expected impact",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              48,
              900,
              "left",
            ),
            this.element(
              "box",
              8,
              34,
              24,
              32,
              2,
              null,
              null,
              dark,
              "#ffffff",
              "rgba(15,23,42,0.08)",
              1,
              28,
              16,
              700,
              "center",
            ),
            this.element(
              "title",
              11,
              40,
              18,
              9,
              3,
              "300+",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              42,
              900,
              "center",
            ),
            this.element(
              "text",
              11,
              52,
              18,
              8,
              4,
              "Customers or people reached",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              15,
              700,
              "center",
            ),
            this.element(
              "box",
              38,
              34,
              24,
              32,
              5,
              null,
              null,
              dark,
              "#ffffff",
              "rgba(15,23,42,0.08)",
              1,
              28,
              16,
              700,
              "center",
            ),
            this.element(
              "title",
              41,
              40,
              18,
              9,
              6,
              "25%",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              42,
              900,
              "center",
            ),
            this.element(
              "text",
              41,
              52,
              18,
              8,
              7,
              "Growth / savings / improvement",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              15,
              700,
              "center",
            ),
            this.element(
              "box",
              68,
              34,
              24,
              32,
              8,
              null,
              null,
              dark,
              "#ffffff",
              "rgba(15,23,42,0.08)",
              1,
              28,
              16,
              700,
              "center",
            ),
            this.element(
              "title",
              71,
              40,
              18,
              9,
              9,
              goal,
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              34,
              900,
              "center",
            ),
            this.element(
              "text",
              71,
              52,
              18,
              8,
              10,
              "Funding goal",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              15,
              700,
              "center",
            ),
          ],
        };

      case "use_of_funds":
        return {
          id,
          type,
          name: "Use of funds",
          height: 620,
          background: "#ffffff",
          elements: [
            this.element(
              "title",
              8,
              13,
              44,
              12,
              1,
              "How the money will be used",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              46,
              900,
              "left",
            ),
            this.element(
              "text",
              8,
              29,
              38,
              38,
              2,
              campaign?.useOfFunds ||
                "Explain exactly where the money goes: product, equipment, hiring, marketing, operations, or impact delivery.",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              19,
              600,
              "left",
            ),
            this.element(
              "box",
              58,
              18,
              32,
              50,
              3,
              null,
              null,
              dark,
              "#fff7ed",
              "rgba(201,162,39,0.24)",
              1,
              34,
              16,
              700,
              "center",
            ),
            this.element(
              "funding",
              62,
              30,
              24,
              25,
              4,
              goal,
              null,
              dark,
              "#ffffff",
              "rgba(15,23,42,0.08)",
              1,
              28,
              18,
              900,
              "center",
            ),
          ],
        };

      case "documents":
        return {
          id,
          type,
          name: "Documents",
          height: 520,
          background: "#f8fafc",
          elements: [
            this.element(
              "title",
              8,
              14,
              52,
              12,
              1,
              "Documents & transparency",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              44,
              900,
              "left",
            ),
            this.element(
              "text",
              8,
              30,
              42,
              12,
              2,
              "Public-safe documents help visitors understand the opportunity. Sensitive files like ID card and bank RIB stay private.",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              18,
              600,
              "left",
            ),
            this.documentElement(8, 52, 38, 15, 3),
            this.documentElement(52, 52, 38, 15, 4),
          ],
        };

      case "cta":
        return {
          id,
          type,
          name: "Final CTA",
          height: 520,
          background: "#111827",
          elements: [
            this.element(
              "title",
              16,
              18,
              68,
              18,
              1,
              campaign?.applicationType === CrowdfundingType.EQUITY
                ? "Ready to invest in this campaign?"
                : "Ready to support this campaign?",
              null,
              "#ffffff",
              "transparent",
              "transparent",
              0,
              0,
              54,
              900,
              "center",
            ),
            this.element(
              "text",
              24,
              42,
              52,
              12,
              2,
              "Join the people backing this project and help it reach the next milestone.",
              null,
              "#d1d5db",
              "transparent",
              "transparent",
              0,
              0,
              20,
              600,
              "center",
            ),
            this.element(
              "button",
              40,
              62,
              20,
              10,
              3,
              campaign?.applicationType === CrowdfundingType.EQUITY
                ? "Invest now"
                : "Donate now",
              null,
              "#111827",
              "#ffffff",
              "#ffffff",
              0,
              999,
              18,
              950,
              "center",
            ),
          ],
        };

      case "faq":
        return {
          id,
          type,
          name: "FAQ",
          height: 620,
          background: "#ffffff",
          elements: [
            this.element(
              "title",
              8,
              12,
              40,
              10,
              1,
              "Questions investors may ask",
              null,
              dark,
              "transparent",
              "transparent",
              0,
              0,
              42,
              900,
              "left",
            ),
            this.element(
              "box",
              8,
              30,
              84,
              16,
              2,
              null,
              null,
              dark,
              "#f8fafc",
              "rgba(15,23,42,0.08)",
              1,
              22,
              16,
              700,
              "left",
            ),
            this.element(
              "text",
              11,
              35,
              78,
              8,
              3,
              "What makes this project different? Add your answer here.",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              18,
              650,
              "left",
            ),
            this.element(
              "box",
              8,
              52,
              84,
              16,
              4,
              null,
              null,
              dark,
              "#f8fafc",
              "rgba(15,23,42,0.08)",
              1,
              22,
              16,
              700,
              "left",
            ),
            this.element(
              "text",
              11,
              57,
              78,
              8,
              5,
              "What are the risks? Add a transparent answer here.",
              null,
              muted,
              "transparent",
              "transparent",
              0,
              0,
              18,
              650,
              "left",
            ),
          ],
        };

      case "empty":
      default:
        return {
          id,
          type: "empty",
          name: "Empty section",
          height: 620,
          background: "#ffffff",
          elements: [],
        };
    }
  }

  private createElement(
    type: StudioElementType,
    x: number,
    y: number,
    zIndex: number,
  ): StudioElement {
    switch (type) {
      case "title":
        return this.element(
          "title",
          x,
          y,
          42,
          14,
          zIndex,
          "New title",
          null,
          "#111827",
          "transparent",
          "transparent",
          0,
          0,
          48,
          900,
          "left",
        );
      case "text":
        return this.element(
          "text",
          x,
          y,
          36,
          20,
          zIndex,
          "Write your text here...",
          null,
          "#475467",
          "transparent",
          "transparent",
          0,
          0,
          19,
          600,
          "left",
        );
      case "image":
        return this.element(
          "image",
          x,
          y,
          34,
          28,
          zIndex,
          null,
          null,
          "#111827",
          "#f8fafc",
          "rgba(15,23,42,0.08)",
          1,
          24,
          16,
          700,
          "center",
        );
      case "video":
        return this.element(
          "video",
          x,
          y,
          42,
          30,
          zIndex,
          null,
          null,
          "#ffffff",
          "#111827",
          "rgba(15,23,42,0.1)",
          1,
          24,
          16,
          700,
          "center",
        );
      case "button":
        return this.element(
          "button",
          x,
          y,
          18,
          8,
          zIndex,
          "Click here",
          null,
          "#ffffff",
          this.style.accentColor || "#C9A227",
          this.style.accentColor || "#C9A227",
          0,
          999,
          17,
          900,
          "center",
        );
      case "box":
        return this.element(
          "box",
          x,
          y,
          30,
          22,
          zIndex,
          null,
          null,
          "#111827",
          "#fff7ed",
          "rgba(201,162,39,0.28)",
          1,
          24,
          16,
          700,
          "center",
        );
      case "document":
        return this.documentElement(x, y, 32, 12, zIndex);
      case "funding":
        return this.element(
          "funding",
          x,
          y,
          28,
          18,
          zIndex,
          null,
          null,
          "#111827",
          "#ffffff",
          "rgba(15,23,42,0.08)",
          1,
          24,
          16,
          850,
          "center",
        );
    }
  }

  private element(
    type: StudioElementType,
    x: number,
    y: number,
    width: number,
    height: number,
    zIndex: number,
    text: string | null,
    url: string | null,
    color: string,
    background: string,
    borderColor: string,
    borderWidth: number,
    radius: number,
    fontSize: number,
    fontWeight: number,
    align: TextAlign,
  ): StudioElement {
    return {
      id: this.uid("element"),
      type,
      x,
      y,
      width,
      height,
      zIndex,
      text,
      url,
      href: null,
      documentId: null,
      color,
      background,
      borderColor,
      borderWidth,
      radius,
      opacity: 1,
      fontSize,
      fontWeight,
      align,
      objectFit: "cover",
    };
  }

  private documentElement(
    x: number,
    y: number,
    width: number,
    height: number,
    zIndex: number,
  ): StudioElement {
    const element = this.element(
      "document",
      x,
      y,
      width,
      height,
      zIndex,
      "Campaign document",
      null,
      "#111827",
      "#ffffff",
      "rgba(15,23,42,0.08)",
      1,
      22,
      16,
      850,
      "left",
    );
    element.documentId = this.safeDocuments[0]?.id ?? null;
    return element;
  }

  private defaultSectionName(type: StudioSectionType): string {
    return (
      this.sectionDock.find((item) => item.type === type)?.label || "Section"
    );
  }

  private findFirstElementText(
    sectionType: StudioSectionType,
    elementType: StudioElementType,
  ): string | null {
    return (
      this.sections
        .find((section) => section.type === sectionType)
        ?.elements.find((element) => element.type === elementType)?.text || null
    );
  }

  private findFirstElementUrl(
    sectionType: StudioSectionType,
    elementType: StudioElementType,
  ): string | null {
    return (
      this.sections
        .find((section) => section.type === sectionType)
        ?.elements.find((element) => element.type === elementType)?.url || null
    );
  }

  private collectPublicDocumentIds(): number[] {
    const ids = new Set<number>();
    this.sections.forEach((section) => {
      section.elements.forEach((element) => {
        if (
          element.type === "document" &&
          typeof element.documentId === "number"
        ) {
          ids.add(element.documentId);
        }
      });
    });
    return [...ids];
  }

  private nextZ(section: StudioSection): number {
    return (
      Math.max(0, ...section.elements.map((element) => element.zIndex || 0)) + 1
    );
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

  private clamp(value: number, min: number, max: number): number {
    return Math.min(max, Math.max(min, value));
  }

  private snap(value: number): number {
    return this.round(Math.round(value / 1) * 1);
  }

  private round(value: number): number {
    return Math.round(value * 10) / 10;
  }

  private deepClone<T>(value: T): T {
    return JSON.parse(JSON.stringify(value)) as T;
  }

  private uid(prefix: string): string {
    return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
}
