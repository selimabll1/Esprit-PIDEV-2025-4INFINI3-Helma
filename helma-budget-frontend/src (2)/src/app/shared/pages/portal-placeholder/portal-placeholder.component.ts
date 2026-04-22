import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthStorageService } from '../../../core/services/auth-storage.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-portal-placeholder',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="portal">
      <div class="card">
        <h1>{{ title }}</h1>
        <p>This is the temporary Phase 1 portal page.</p>

        <div class="meta" *ngIf="session">
          <p><strong>Email:</strong> {{ session.email }}</p>
          <p><strong>Role:</strong> {{ session.role }}</p>
          <p><strong>User ID:</strong> {{ session.userId }}</p>
        </div>

        <button type="button" (click)="logout()">Logout</button>
      </div>
    </section>
  `,
  styles: [`
    .portal {
      min-height: 100vh;
      display: grid;
      place-items: center;
      background: #f7f7f7;
      padding: 24px;
    }

    .card {
      width: min(560px, 100%);
      background: white;
      border-radius: 16px;
      padding: 32px;
      box-shadow: 0 12px 30px rgba(0,0,0,0.08);
    }

    h1 {
      margin: 0 0 12px;
      color: #0b3b3c;
    }

    .meta {
      margin: 20px 0;
      padding: 16px;
      border-radius: 12px;
      background: #f5fbfb;
    }

    button {
      border: 0;
      border-radius: 10px;
      padding: 12px 16px;
      cursor: pointer;
      background: #062a2b;
      color: white;
    }
  `]
})
export class PortalPlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly authStorage = inject(AuthStorageService);

  readonly title = (this.route.snapshot.data['title'] as string) ?? 'Portal';
  readonly session = this.authStorage.getUser();

  logout(): void {
    this.authService.logout();
  }
}