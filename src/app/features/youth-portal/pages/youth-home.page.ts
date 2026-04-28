import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-youth-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <header class="hero card">
        <div>
          <span class="eyebrow">Home</span>
          <h1>Youth Beneficiary Portal</h1>
          <p>
            Start a new fundraising application, review existing drafts, and manage your crowdfunding journey.
          </p>
        </div>

        <div class="hero__actions">
          <a class="btn btn-primary" routerLink="/youth/crowdfunding">Open Crowdfunding</a>
          <a class="btn btn-secondary" routerLink="/youth/applications">View Applications</a>
        </div>
      </header>

      <section class="grid">
        <article class="card">
          <h2>Crowdfunding</h2>
          <p>Explore your funding actions and create a new raise request.</p>
          <a class="btn btn-primary" routerLink="/youth/crowdfunding">Go to Crowdfunding</a>
        </article>

        <article class="card">
          <h2>Applications</h2>
          <p>See your drafts, submitted applications, and decisions.</p>
          <a class="btn btn-secondary" routerLink="/youth/applications">Open Applications</a>
        </article>
      </section>
    </section>
  `,
  styles: [`
    .page {
      display: grid;
      gap: 24px;
    }

    .hero {
      display: grid;
      gap: 16px;
    }

    .eyebrow {
      display: inline-flex;
      width: fit-content;
      padding: 6px 12px;
      border-radius: 999px;
      background: var(--helma-gold-soft);
      color: #7a5a00;
      font-size: var(--fs-caption);
      font-weight: 800;
      text-transform: uppercase;
    }

    .hero__actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 20px;
    }

    @media (max-width: 900px) {
      .grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class YouthHomePageComponent {}