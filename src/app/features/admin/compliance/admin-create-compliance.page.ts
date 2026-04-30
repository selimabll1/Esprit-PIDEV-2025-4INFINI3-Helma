import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../../../core/services/auth-api.service';
import { SessionService } from '../../../core/services/session.service';

@Component({
  selector: 'app-admin-create-compliance-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="create-page">
      <div class="top-card">
        <div>
          <p class="eyebrow">Admin feature</p>
          <h2>Add compliance worker</h2>
          <p>
            Create a compliance account that can access the admin/compliance portal
            and review applications and regulated workflows.
          </p>
        </div>

        <a routerLink="/admin/dashboard">Back to dashboard</a>
      </div>

      <div class="blocked-card" *ngIf="!isAdmin()">
        <h3>Access denied</h3>
        <p>Only administrators can create compliance workers.</p>
      </div>

      <form
        *ngIf="isAdmin()"
        class="form-card"
        [formGroup]="form"
        (ngSubmit)="submit()"
      >
        <div class="form-grid">
          <label>
            <span>First name</span>
            <input type="text" formControlName="firstName" placeholder="First name" />
          </label>

          <label>
            <span>Last name</span>
            <input type="text" formControlName="lastName" placeholder="Last name" />
          </label>

          <label>
            <span>Email</span>
            <input type="email" formControlName="email" placeholder="compliance@helma.tn" />
          </label>

          <label>
            <span>Password</span>
            <input type="password" formControlName="password" placeholder="Minimum 6 characters" />
          </label>
        </div>

        <div class="role-box">
          <strong>Assigned role:</strong>
          <span>COMPLIANCE</span>
        </div>

        <p class="message error" *ngIf="error()">{{ error() }}</p>
        <p class="message success" *ngIf="success()">{{ success() }}</p>

        <div class="actions">
          <button type="button" class="ghost-btn" routerLink="/admin/dashboard">
            Cancel
          </button>

          <button
            type="submit"
            class="submit-btn"
            [disabled]="form.invalid || submitting()"
          >
            {{ submitting() ? 'Creating...' : 'Create compliance worker' }}
          </button>
        </div>
      </form>
    </section>
  `,
  styles: [`
    .create-page {
      display: grid;
      gap: 20px;
      max-width: 980px;
    }

    .top-card,
    .form-card,
    .blocked-card {
      background: rgba(255,255,255,0.92);
      border: 1px solid rgba(6,42,43,0.08);
      box-shadow: 0 18px 40px rgba(6,42,43,0.08);
      border-radius: 26px;
    }

    .top-card {
      display: flex;
      justify-content: space-between;
      gap: 18px;
      align-items: flex-start;
      padding: 24px;
    }

    .eyebrow {
      margin: 0 0 6px;
      color: #b9922f;
      font-size: 0.76rem;
      font-weight: 900;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .top-card h2 {
      margin: 0;
      color: #062a2b;
      font-size: clamp(1.6rem, 3vw, 2.2rem);
    }

    .top-card p:not(.eyebrow) {
      margin: 10px 0 0;
      max-width: 620px;
      color: #5f6f6b;
      line-height: 1.6;
    }

    .top-card a {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 42px;
      padding: 0 15px;
      border-radius: 999px;
      background: #f4eedc;
      color: #87671c;
      text-decoration: none;
      font-weight: 900;
      white-space: nowrap;
    }

    .form-card {
      padding: 24px;
    }

    .form-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 18px;
    }

    label {
      display: grid;
      gap: 8px;
    }

    label span {
      color: #223734;
      font-weight: 900;
      font-size: 0.9rem;
    }

    input {
      width: 100%;
      min-height: 48px;
      border: 1px solid rgba(6,42,43,0.13);
      border-radius: 15px;
      padding: 0 14px;
      outline: none;
      background: #fbfaf6;
      color: #062a2b;
      font: inherit;
      transition: 0.18s ease;
    }

    input:focus {
      border-color: #d0a94a;
      background: white;
      box-shadow: 0 0 0 4px rgba(208,169,74,0.15);
    }

    .role-box {
      margin-top: 20px;
      padding: 15px 16px;
      border-radius: 16px;
      background: #f8f1db;
      color: #5a4712;
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      align-items: center;
    }

    .role-box span {
      font-weight: 900;
      color: #062a2b;
    }

    .message {
      margin: 18px 0 0;
      padding: 13px 15px;
      border-radius: 14px;
      font-weight: 800;
    }

    .error {
      background: #fff0f0;
      color: #a43a3a;
    }

    .success {
      background: #e3f7ed;
      color: #176546;
    }

    .actions {
      margin-top: 24px;
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }

    button {
      min-height: 46px;
      border: 0;
      border-radius: 999px;
      padding: 0 18px;
      cursor: pointer;
      font-weight: 900;
    }

    button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .ghost-btn {
      background: #edf0ef;
      color: #43534f;
    }

    .submit-btn {
      background: #062a2b;
      color: white;
      box-shadow: 0 16px 32px rgba(6,42,43,0.16);
    }

    .blocked-card {
      padding: 24px;
    }

    .blocked-card h3 {
      margin: 0 0 8px;
      color: #9a3333;
    }

    .blocked-card p {
      margin: 0;
      color: #677470;
    }

    @media (max-width: 720px) {
      .top-card {
        flex-direction: column;
      }

      .form-grid {
        grid-template-columns: 1fr;
      }

      .actions {
        flex-direction: column-reverse;
      }

      button {
        width: 100%;
      }
    }
  `]
})
export class AdminCreateCompliancePageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authApiService = inject(AuthApiService);
  private readonly sessionService = inject(SessionService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly isAdmin = computed(() => this.sessionService.role() === 'ADMIN');

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]]
  });

  submit(): void {
    if (!this.isAdmin()) {
      this.error.set('Only admins can create compliance workers.');
      return;
    }

    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.success.set(null);

    const raw = this.form.getRawValue();

    this.authApiService.register({
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      email: raw.email.trim(),
      password: raw.password,
      role: 'COMPLIANCE'
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.success.set('Compliance worker created successfully.');
        this.form.reset();

        setTimeout(() => {
          this.router.navigate(['/admin/dashboard']);
        }, 700);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(
          err?.error?.message ||
          err?.error?.error ||
          'Could not create compliance worker.'
        );
      }
    });
  }
}