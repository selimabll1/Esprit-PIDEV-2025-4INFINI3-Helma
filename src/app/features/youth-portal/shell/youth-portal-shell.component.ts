import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { YouthSidebarComponent } from '../components/youth-sidebar.component';

@Component({
  selector: 'app-youth-portal-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, YouthSidebarComponent],
  template: `
    <div class="portal">
      <app-youth-sidebar />
      <main class="portal__content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .portal {
      min-height: 100vh;
      height: 100vh;
      display: grid;
      grid-template-columns: 310px minmax(0, 1fr);
      gap: 0;
      align-items: stretch;
      background: var(--color-bg);
      overflow: hidden;
    }

    .portal__content {
      min-width: 0;
      height: 100vh;
      overflow-y: auto;
      padding: 22px 28px 32px;
    }

    @media (max-width: 1100px) {
      .portal {
        grid-template-columns: 290px minmax(0, 1fr);
      }

      .portal__content {
        padding: 18px 22px 28px;
      }
    }

    @media (max-width: 960px) {
      .portal {
        grid-template-columns: 1fr;
        height: auto;
        min-height: 100vh;
        overflow: visible;
      }

      .portal__content {
        height: auto;
        overflow: visible;
        padding: 16px;
      }
    }
  `]
})
export class YouthPortalShellComponent {}