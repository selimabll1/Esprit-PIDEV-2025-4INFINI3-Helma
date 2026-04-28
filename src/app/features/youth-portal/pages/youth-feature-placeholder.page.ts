import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-youth-feature-placeholder-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="feature-page">
      <div class="feature-card">
        <span class="feature-page__eyebrow">{{ eyebrow }}</span>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>

        <div class="feature-page__actions">
          <a class="feature-page__btn feature-page__btn--primary" routerLink="/youth">
            Back to Dashboard
          </a>

          <a class="feature-page__btn feature-page__btn--secondary" routerLink="/youth/applications/new">
            Start New Raise
          </a>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .feature-page {
      padding: 8px 0 24px;
    }

    .feature-card {
      max-width: 860px;
      padding: 36px;
      border: 1px solid rgba(229, 231, 235, 0.92);
      border-radius: 28px;
      background:
        radial-gradient(circle at top right, rgba(212, 166, 42, 0.10), transparent 26%),
        linear-gradient(180deg, rgba(255,255,255,0.98), rgba(252,250,245,0.96));
      box-shadow: var(--shadow-md);
    }

    .feature-page__eyebrow {
      display: inline-flex;
      padding: 8px 12px;
      margin-bottom: 16px;
      border-radius: 999px;
      background: var(--helma-teal-soft);
      color: var(--helma-teal);
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 0.05em;
      text-transform: uppercase;
    }

    h1 {
      margin-bottom: 12px;
      font-size: 2rem;
    }

    p {
      max-width: 700px;
      margin-bottom: 24px;
      font-size: 15px;
      line-height: 1.8;
    }

    .feature-page__actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .feature-page__btn {
      min-height: 46px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0 16px;
      border-radius: 999px;
      font-weight: 800;
      text-decoration: none;
    }

    .feature-page__btn--primary {
      background: var(--helma-teal);
      color: #ffffff;
    }

    .feature-page__btn--secondary {
      background: var(--helma-gold);
      color: #1f2937;
    }

    @media (max-width: 640px) {
      .feature-card {
        padding: 24px;
        border-radius: 22px;
      }

      h1 {
        font-size: 1.7rem;
      }
    }
  `]
})
export class YouthFeaturePlaceholderPageComponent {
  private readonly route = inject(ActivatedRoute);

  readonly title = (this.route.snapshot.data['title'] as string) ?? 'Coming next';
  readonly description =
    (this.route.snapshot.data['description'] as string) ??
    'This section is routed and ready for its real feature implementation.';
  readonly eyebrow = (this.route.snapshot.data['eyebrow'] as string) ?? 'Youth Portal';
}