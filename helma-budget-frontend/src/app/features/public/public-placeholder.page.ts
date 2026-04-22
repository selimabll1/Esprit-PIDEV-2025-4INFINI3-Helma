import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthStorageService } from '../../core/services/auth-storage.service';

@Component({
  selector: 'app-public-placeholder-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="placeholder-page">
      <div class="container">
        <div class="card placeholder-card">
          <span class="eyebrow">{{ eyebrow }}</span>
          <h1>{{ title }}</h1>
          <p>{{ description }}</p>

          <div class="actions">
            <!-- Youth Tools: logged in → go to budget, logged out → login -->
            <ng-container *ngIf="isYouthTools">
              <a *ngIf="isLoggedIn" class="btn btn-primary" routerLink="/budget/dashboard">
                Go to My Budget
              </a>
              <a *ngIf="!isLoggedIn" class="btn btn-primary" routerLink="/auth/login">
                Get Started
              </a>
            </ng-container>

            <!-- All other placeholder pages -->
            <ng-container *ngIf="!isYouthTools">
              <a class="btn btn-primary" routerLink="/auth/login">Get Started</a>
            </ng-container>

            <a class="btn btn-secondary" routerLink="/">Back Home</a>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .placeholder-page {
      padding: 48px 0;
      min-height: calc(100vh - var(--navbar-height));
      display: grid;
      align-items: start;
      background: linear-gradient(180deg, var(--helma-ivory), #fff 55%);
    }

    .placeholder-card {
      max-width: 780px;
      margin: 0 auto;
      padding: 40px;
      border-radius: 24px;
    }

    .eyebrow {
      display: inline-flex;
      padding: 8px 12px;
      border-radius: 999px;
      background: var(--helma-mint);
      color: var(--helma-teal);
      font-size: 12px;
      font-weight: 800;
      margin-bottom: 16px;
    }

    .actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      margin-top: 24px;
    }
  `]
})
export class PublicPlaceholderPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthStorageService);

  readonly title = (this.route.snapshot.data['title'] as string) ?? 'Page';
  readonly description =
    (this.route.snapshot.data['description'] as string) ??
    'This page is the next public section to design.';
  readonly eyebrow =
    (this.route.snapshot.data['eyebrow'] as string) ?? 'Helma';

  readonly isYouthTools = this.eyebrow === 'Youth Tools';
  readonly isLoggedIn = this.auth.isAuthenticated();
}
