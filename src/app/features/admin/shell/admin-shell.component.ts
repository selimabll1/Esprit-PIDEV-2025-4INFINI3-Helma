import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AdminSidebarComponent } from '../components/admin-sidebar.component';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AdminSidebarComponent],
  template: `
    <div class="portal">
      <app-admin-sidebar></app-admin-sidebar>

      <main class="portal__content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .portal {
      min-height: 100vh;
      display: grid;
      grid-template-columns: 310px minmax(0, 1fr);
      background: var(--color-bg, #f7f3ea);
    }

    .portal__content {
      min-width: 0;
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
      }

      .portal__content {
        padding: 16px;
      }
    }
  `]
})
export class AdminShellComponent {}