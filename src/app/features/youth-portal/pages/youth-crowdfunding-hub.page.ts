import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-youth-crowdfunding-hub-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <header class="page-header">
        <span class="eyebrow">Crowdfunding</span>
        <h1>Crowdfunding Workspace</h1>
        <p>Choose the action you want to take.</p>
      </header>

      <section class="options">
        <a class="option card" [routerLink]="['/youth/applications/new']" [queryParams]="{ type: 'DONATION' }">
          <h2>New Donation Raise</h2>
          <p>Create a donation-based application raise form.</p>
        </a>

        <a class="option card" [routerLink]="['/youth/applications/new']" [queryParams]="{ type: 'EQUITY' }">
          <h2>New Equity Raise</h2>
          <p>Create an equity-based application raise form.</p>
        </a>

        <a class="option card" routerLink="/youth/applications">
          <h2>All Applications</h2>
          <p>View every application, regardless of status.</p>
        </a>

        <a class="option card" [routerLink]="['/youth/applications']" [queryParams]="{ filter: 'draft' }">
          <h2>Continue Drafts</h2>
          <p>Open saved drafts and continue editing them.</p>
        </a>
      </section>
    </section>
  `,
  styles: [`
    .page {
      display: grid;
      gap: 24px;
    }

    .eyebrow {
      display: inline-flex;
      width: fit-content;
      padding: 6px 12px;
      border-radius: 999px;
      background: var(--helma-teal-soft);
      color: var(--helma-teal);
      font-size: var(--fs-caption);
      font-weight: 800;
      text-transform: uppercase;
    }

    .options {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 20px;
    }

    .option {
      display: grid;
      gap: 10px;
      text-decoration: none;
      color: inherit;
      min-height: 180px;
      align-content: start;
    }

    .option:hover {
      border-color: rgba(15, 107, 104, 0.2);
    }

    @media (max-width: 900px) {
      .options {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class YouthCrowdfundingHubPageComponent {}