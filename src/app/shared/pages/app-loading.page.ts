import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  selector: 'app-loading-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main class="loading-page" aria-label="Loading">
      <div class="loading-page__center">
        <div class="loading-page__spinner">
          <img
            src="logo/helma-logo.png"
            alt="Helma logo"
            class="loading-page__logo"
          />
        </div>
      </div>
    </main>
  `,
  styles: [`
    :host {
      display: block;
    }

    .loading-page {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
      background:
        radial-gradient(circle at top right, rgba(212, 166, 42, 0.08), transparent 18%),
        radial-gradient(circle at bottom left, rgba(42, 157, 143, 0.08), transparent 20%),
        linear-gradient(180deg, #f8fafc 0%, #f6f7f9 100%);
    }

    .loading-page__center {
      display: grid;
      place-items: center;
    }

    .loading-page__spinner {
      position: relative;
      width: 170px;
      height: 170px;
      display: grid;
      place-items: center;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.92);
      box-shadow:
        0 18px 40px rgba(15, 23, 42, 0.08),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);
    }

    .loading-page__spinner::before {
      content: '';
      position: absolute;
      inset: -8px;
      border-radius: 999px;
      border: 8px solid rgba(212, 166, 42, 0.16);
      border-top-color: #d4a62a;
      border-right-color: #e6bf4e;
      animation: helma-spin 1s linear infinite;
    }

    .loading-page__spinner::after {
      content: '';
      position: absolute;
      inset: 10px;
      border-radius: 999px;
      background: radial-gradient(circle, rgba(212, 166, 42, 0.08), transparent 70%);
      pointer-events: none;
    }

    .loading-page__logo {
      position: relative;
      z-index: 1;
      width: 96px;
      height: 96px;
      object-fit: contain;
      user-select: none;
      -webkit-user-drag: none;
    }

    @keyframes helma-spin {
      to {
        transform: rotate(360deg);
      }
    }
  `]
})
export class AppLoadingPageComponent {}