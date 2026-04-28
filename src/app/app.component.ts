import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
  RouterOutlet
} from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `
    <div class="app-shell">
      <div class="leaf-background" aria-hidden="true">
        <span class="leaf leaf-1"></span>
        <span class="leaf leaf-2"></span>
        <span class="leaf leaf-3"></span>
        <span class="leaf leaf-4"></span>
        <span class="leaf leaf-5"></span>
        <span class="leaf leaf-6"></span>
        <span class="leaf leaf-7"></span>
        <span class="leaf leaf-8"></span>
      </div>

      <router-outlet></router-outlet>

      <div class="route-loading" *ngIf="isRouteLoading()">
        <div class="route-loading__spinner">
          <img
            src="logo/helma-logo.png"
            alt="Helma logo"
            class="route-loading__logo"
          />
        </div>
      </div>
    </div>
  `,
  styles: [`
    .app-shell {
      position: relative;
      min-height: 100vh;
      overflow-x: clip;
      background: var(--color-bg);
    }

    .leaf-background {
      position: fixed;
      inset: 0;
      pointer-events: none;
      z-index: 0;
      overflow: hidden;
    }

    .leaf {
      position: absolute;
      width: 90px;
      height: 90px;
      opacity: 0.08;
      filter: blur(0.2px);
      background: linear-gradient(180deg, #f3d46b, #d4a62a);
      clip-path: path("M45 4 C62 10, 84 28, 82 48 C80 68, 60 84, 42 87 C26 89, 10 76, 7 57 C4 39, 16 18, 45 4 Z");
      transform: rotate(18deg);
      animation: floatLeaf 18s linear infinite;
    }

    .leaf::after {
      content: '';
      position: absolute;
      left: 50%;
      top: 14%;
      width: 2px;
      height: 60%;
      background: rgba(143, 107, 18, 0.35);
      transform: translateX(-50%) rotate(12deg);
      border-radius: 999px;
    }

    .leaf-1 { top: 8%; left: 4%; animation-duration: 24s; transform: rotate(-18deg) scale(0.8); }
    .leaf-2 { top: 16%; right: 10%; animation-duration: 22s; transform: rotate(24deg) scale(1); }
    .leaf-3 { top: 38%; left: 8%; animation-duration: 26s; transform: rotate(-32deg) scale(0.9); }
    .leaf-4 { top: 55%; right: 7%; animation-duration: 20s; transform: rotate(18deg) scale(1.1); }
    .leaf-5 { top: 74%; left: 14%; animation-duration: 28s; transform: rotate(-14deg) scale(0.75); }
    .leaf-6 { top: 82%; right: 18%; animation-duration: 25s; transform: rotate(33deg) scale(0.85); }
    .leaf-7 { top: 28%; left: 48%; animation-duration: 30s; transform: rotate(-10deg) scale(0.7); }
    .leaf-8 { top: 66%; right: 42%; animation-duration: 27s; transform: rotate(16deg) scale(0.95); }

    .route-loading {
      position: fixed;
      inset: 0;
      z-index: 2000;
      display: grid;
      place-items: center;
      background: rgba(248, 250, 252, 0.82);
      backdrop-filter: blur(6px);
    }

    .route-loading__spinner {
      position: relative;
      width: 164px;
      height: 164px;
      display: grid;
      place-items: center;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.94);
      box-shadow:
        0 20px 44px rgba(15, 23, 42, 0.08),
        inset 0 1px 0 rgba(255, 255, 255, 0.85);
    }

    .route-loading__spinner::before {
      content: '';
      position: absolute;
      inset: -8px;
      border-radius: 999px;
      border: 8px solid rgba(212, 166, 42, 0.18);
      border-top-color: #d4a62a;
      border-right-color: #e6bf4e;
      animation: helmaSpin 0.95s linear infinite;
    }

    .route-loading__logo {
      position: relative;
      z-index: 1;
      width: 92px;
      height: 92px;
      object-fit: contain;
      user-select: none;
      -webkit-user-drag: none;
    }

    @keyframes helmaSpin {
      to {
        transform: rotate(360deg);
      }
    }

    @keyframes floatLeaf {
      0%   { transform: translateY(0) rotate(0deg); }
      50%  { transform: translateY(14px) rotate(6deg); }
      100% { transform: translateY(0) rotate(0deg); }
    }
  `]
})
export class AppComponent {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly isRouteLoading = signal(false);

  private showTimer: ReturnType<typeof setTimeout> | null = null;
  private shownAt = 0;

  constructor() {
    this.router.events
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => {
        if (event instanceof NavigationStart) {
          this.startLoading();
        }

        if (
          event instanceof NavigationEnd ||
          event instanceof NavigationCancel ||
          event instanceof NavigationError
        ) {
          this.stopLoading();
        }
      });
  }

  private startLoading(): void {
    if (this.showTimer) {
      clearTimeout(this.showTimer);
    }

    this.showTimer = setTimeout(() => {
      this.isRouteLoading.set(true);
      this.shownAt = Date.now();
      this.showTimer = null;
    }, 120);
  }

  private stopLoading(): void {
    if (this.showTimer) {
      clearTimeout(this.showTimer);
      this.showTimer = null;
    }

    if (!this.isRouteLoading()) {
      return;
    }

    const visibleFor = Date.now() - this.shownAt;
    const remaining = Math.max(0, 220 - visibleFor);

    setTimeout(() => {
      this.isRouteLoading.set(false);
    }, remaining);
  }
  
}