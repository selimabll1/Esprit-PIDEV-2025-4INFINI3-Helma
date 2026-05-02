import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { InvestorSidebarComponent } from '../components/investor-sidebar.component';

@Component({
  selector: 'app-investor-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, InvestorSidebarComponent],
  template: `
    <div class="investor-portal">
      <app-investor-sidebar />

      <main class="investor-portal__content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .investor-portal {
      min-height: 100vh;
      height: 100vh;
      display: flex;
      align-items: stretch;
      background:
        radial-gradient(circle at 82% 4%, rgba(74, 163, 255, 0.11), transparent 32%),
        linear-gradient(180deg, #f5f9ff 0%, #f7f3ea 100%);
      overflow: hidden;
    }

    .investor-portal__content {
      flex: 1 1 auto;
      min-width: 0;
      height: 100vh;
      overflow-y: auto;
      padding: 22px 28px 32px;
    }

    @media (max-width: 1100px) {
      .investor-portal__content {
        padding: 18px 22px 28px;
      }
    }

    @media (max-width: 960px) {
      .investor-portal {
        min-height: 100vh;
        height: auto;
        display: block;
        overflow: visible;
      }

      .investor-portal__content {
        height: auto;
        overflow: visible;
        padding: 16px;
      }
    }
  `],
})
export class InvestorShellComponent {}