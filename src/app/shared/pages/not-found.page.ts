import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <main class="not-found">
      <section class="not-found__card">
        <div class="not-found__logo-wrap">
          <img
            src="logo/helma-logo.png"
            alt="Helma logo"
            class="not-found__logo"
          />
        </div>

        <span class="not-found__eyebrow">404</span>
        <h1>Page not found</h1>
        <p>
          The page you are looking for does not exist or may have been moved.
        </p>

        <div class="not-found__actions">
          <a routerLink="/" class="not-found__btn not-found__btn--primary">
            Back home
          </a>

          <a routerLink="/youth" class="not-found__btn not-found__btn--secondary">
            Go to portal
          </a>
        </div>
      </section>
    </main>
  `,
  styles: [`
    :host {
      display: block;
    }

    .not-found {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
      background:
        radial-gradient(circle at top right, rgba(212, 166, 42, 0.10), transparent 18%),
        radial-gradient(circle at bottom left, rgba(42, 157, 143, 0.08), transparent 22%),
        linear-gradient(180deg, #f8fafc 0%, #f6f7f9 100%);
    }

    .not-found__card {
      width: min(100%, 620px);
      padding: 40px 32px;
      border-radius: 32px;
      border: 1px solid rgba(229, 231, 235, 0.95);
      background: rgba(255, 255, 255, 0.94);
      box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
      text-align: center;
    }

    .not-found__logo-wrap {
      width: 104px;
      height: 104px;
      margin: 0 auto 18px;
      display: grid;
      place-items: center;
      border-radius: 999px;
      background: radial-gradient(circle, rgba(212, 166, 42, 0.16), rgba(212, 166, 42, 0.05));
      border: 6px solid rgba(212, 166, 42, 0.22);
    }

    .not-found__logo {
      width: 60px;
      height: 60px;
      object-fit: contain;
    }

    .not-found__eyebrow {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 34px;
      padding: 0 14px;
      margin-bottom: 14px;
      border-radius: 999px;
      background: rgba(212, 166, 42, 0.14);
      color: #8d6a08;
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 0.08em;
    }

    .not-found h1 {
      margin: 0 0 10px;
      font-size: clamp(2rem, 4vw, 3rem);
      line-height: 1.05;
      color: #1f2937;
    }

    .not-found p {
      margin: 0 auto;
      max-width: 460px;
      font-size: 1rem;
      line-height: 1.7;
      color: #6b7280;
    }

    .not-found__actions {
      display: flex;
      justify-content: center;
      gap: 12px;
      flex-wrap: wrap;
      margin-top: 28px;
    }

    .not-found__btn {
      min-height: 48px;
      padding: 0 18px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 999px;
      text-decoration: none;
      font-weight: 800;
      transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
    }

    .not-found__btn:hover {
      transform: translateY(-1px);
      box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
    }

    .not-found__btn--primary {
      background: #0f6b68;
      color: #ffffff;
    }

    .not-found__btn--secondary {
      background: #d4a62a;
      color: #1f2937;
    }

    @media (max-width: 640px) {
      .not-found__card {
        padding: 28px 20px;
        border-radius: 24px;
      }
    }
  `]
})
export class NotFoundPageComponent {}