import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { getHomeRouteByRole } from '../../../core/utils/role-home.util';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="login-page">
      <div class="login-card">
        <h1>HELMA</h1>
        <p class="subtitle">Sign in to continue</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" />
            <small *ngIf="form.controls.email.touched && form.controls.email.invalid">
              Please enter a valid email.
            </small>
          </div>

          <div class="field">
            <label for="password">Password</label>
            <input id="password" type="password" formControlName="password" />
            <small *ngIf="form.controls.password.touched && form.controls.password.invalid">
              Password is required.
            </small>
          </div>

          <p class="error" *ngIf="error()">{{ error() }}</p>

          <button type="submit" [disabled]="loading()">
            {{ loading() ? 'Signing in...' : 'Login' }}
          </button>
        </form>
      </div>
    </section>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
      background:
        linear-gradient(135deg, rgba(6,42,43,0.08), rgba(42,157,143,0.08)),
        #f7f7f7;
    }

    .login-card {
      width: min(420px, 100%);
      background: #fff;
      border-radius: 18px;
      padding: 32px;
      box-shadow: 0 16px 40px rgba(0,0,0,0.08);
    }

    h1 {
      margin: 0;
      font-size: 2rem;
      letter-spacing: 0.08em;
      color: #0b3b3c;
    }

    .subtitle {
      margin: 8px 0 24px;
      color: #5c6b73;
    }

    form {
      display: grid;
      gap: 16px;
    }

    .field {
      display: grid;
      gap: 8px;
    }

    label {
      font-weight: 600;
      color: #062a2b;
    }

    input {
      height: 44px;
      border: 1px solid #d9dfe3;
      border-radius: 10px;
      padding: 0 12px;
      font-size: 0.95rem;
    }

    input:focus {
      outline: none;
      border-color: #2a9d8f;
      box-shadow: 0 0 0 3px rgba(42,157,143,0.12);
    }

    small {
      color: #c0392b;
    }

    .error {
      margin: 0;
      color: #c0392b;
      font-size: 0.95rem;
    }

    button {
      height: 46px;
      border: 0;
      border-radius: 10px;
      background: #062a2b;
      color: white;
      font-weight: 600;
      cursor: pointer;
    }

    button:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
  `]
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.authService
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (session) => {
          void this.router.navigateByUrl(getHomeRouteByRole(session.role));
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(err.error?.message || 'Login failed. Please try again.');
        }
      });
  }
}