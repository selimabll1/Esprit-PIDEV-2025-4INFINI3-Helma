import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../../../core/services/auth-api.service';
import { SessionService } from '../../../core/services/session.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-shell">
      <div class="auth-leaves" aria-hidden="true">
        <span class="falling-leaf fl-1"></span>
        <span class="falling-leaf fl-2"></span>
        <span class="falling-leaf fl-3"></span>
        <span class="falling-leaf fl-4"></span>
        <span class="falling-leaf fl-5"></span>
      </div>

      <div class="auth-layout">
        <div class="auth-visual">
          <div class="visual-inner">
            <img src="logo/helma-logo.png" alt="Helma logo" class="visual-logo" />

            <div class="brand-copy">
              <p class="eyebrow">Welcome back to Helma</p>
              <h1>Empower your next step</h1>
              <p class="description">
                Sign in to continue your journey with youth financial tools,
                responsible financing, and trusted crowdfunding opportunities.
              </p>

              <div class="pill-list">
                <span>Empower</span>
                <span>Innovate</span>
                <span>Growth</span>
              </div>
            </div>

            <div class="info-card">
              <h3>Inside your Helma account</h3>
              <ul>
                <li>Track your financial progress</li>
                <li>Manage campaigns and opportunities</li>
                <li>Follow investments and funding activity</li>
                <li>Access a platform built for trust and growth</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="auth-panel">
          <div class="form-card">
            <div class="form-head">
              <h2>Sign in</h2>
              <p>Access your Helma account.</p>
            </div>

            <form [formGroup]="form" (ngSubmit)="submit()">
              <div class="field">
                <label for="email">Email</label>
                <input id="email" type="email" formControlName="email" />
                <small *ngIf="invalid('email')">Enter a valid email.</small>
              </div>

              <div class="field">
                <label for="password">Password</label>
                <input id="password" type="password" formControlName="password" />
                <small *ngIf="invalid('password')">Password is required.</small>
              </div>

              <div class="form-row">
                <label class="check-row">
                  <input type="checkbox" formControlName="rememberMe" />
                  <span>Remember me</span>
                </label>

                <a class="forgot-link" routerLink="/auth/forgot-password">Forgot password?</a>
              </div>

              <p class="error" *ngIf="error()">{{ error() }}</p>

              <button class="submit-btn" type="submit" [disabled]="loading()">
                {{ loading() ? 'Signing in...' : 'Sign in' }}
              </button>
            </form>

            <p class="switch-auth">
              Don’t have an account?
              <a routerLink="/auth/signup">Create one</a>
            </p>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    :host {
      display: block;
      position: relative;
      z-index: 1;
    }

    .auth-shell {
      position: relative;
      min-height: 100vh;
      background:
        radial-gradient(circle at top left, rgba(212,166,42,0.12), transparent 30%),
        radial-gradient(circle at bottom right, rgba(15,107,104,0.10), transparent 35%),
        linear-gradient(180deg, var(--helma-ivory), #ffffff 60%);
      overflow: hidden;
    }

    .auth-layout {
      min-height: 100vh;
      display: grid;
      grid-template-columns: 1fr 1fr;
      position: relative;
      z-index: 1;
    }

    .auth-visual,
    .auth-panel {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 48px;
    }

    .auth-visual {
      animation: riseIn 0.9s ease;
    }

    .auth-panel {
      animation: riseIn 1s ease;
    }

    .visual-inner {
      max-width: 540px;
    }

    .visual-logo {
      width: 180px;
      height: auto;
      object-fit: contain;
      margin-bottom: 24px;
      animation: floatTree 4.2s ease-in-out infinite;
      filter: drop-shadow(0 14px 32px rgba(212,166,42,0.16));
    }

    .eyebrow {
      display: inline-flex;
      padding: 8px 12px;
      border-radius: 999px;
      background: rgba(221,244,236,0.9);
      color: var(--helma-teal);
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 0.02em;
      margin-bottom: 16px;
    }

    h1 {
      font-size: clamp(2.4rem, 3vw, 3.5rem);
      line-height: 1.1;
      margin-bottom: 16px;
    }

    .description {
      font-size: 16px;
      max-width: 520px;
      margin-bottom: 24px;
    }

    .pill-list {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      margin-bottom: 28px;
    }

    .pill-list span {
      padding: 10px 14px;
      border-radius: 999px;
      background: rgba(255,255,255,0.82);
      border: 1px solid rgba(229,231,235,0.9);
      font-weight: 700;
      color: var(--helma-teal);
    }

    .info-card {
      background: rgba(255,255,255,0.84);
      border: 1px solid rgba(229,231,235,0.9);
      box-shadow: var(--shadow-md);
      border-radius: 24px;
      padding: 24px;
      backdrop-filter: blur(10px);
    }

    .info-card h3 {
      margin-bottom: 12px;
    }

    .info-card ul {
      margin: 0;
      padding-left: 18px;
      color: var(--color-text-muted);
      line-height: 1.8;
    }

    .form-card {
      width: min(500px, 100%);
      background: rgba(255,255,255,0.92);
      border: 1px solid rgba(229,231,235,0.9);
      box-shadow: 0 24px 60px rgba(15,23,42,0.10);
      border-radius: 28px;
      padding: 32px;
      backdrop-filter: blur(12px);
    }

    .form-head h2 {
      margin-bottom: 8px;
    }

    .form-head p {
      margin-bottom: 24px;
    }

    form {
      display: grid;
      gap: 18px;
    }

    .field {
      display: grid;
      gap: 8px;
    }

    label {
      font-weight: 700;
      color: var(--color-text);
      font-size: 14px;
    }

    input {
      width: 100%;
      height: 48px;
      border: 1px solid var(--color-border);
      border-radius: 14px;
      padding: 0 14px;
      font: inherit;
      background: #fff;
      color: var(--color-text);
      transition: 0.2s ease;
    }

    input:focus {
      outline: none;
      border-color: var(--helma-teal);
      box-shadow: 0 0 0 4px rgba(15,107,104,0.10);
    }

    .form-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }

    .check-row {
      display: flex;
      gap: 10px;
      align-items: center;
      color: var(--color-text-muted);
      font-weight: 600;
    }

    .check-row input {
      width: 18px;
      height: 18px;
      margin: 0;
    }

    .forgot-link {
      color: var(--helma-teal);
      font-weight: 700;
      text-decoration: none;
    }

    small {
      color: #c65454;
      font-weight: 600;
    }

    .error {
      margin: 0;
      color: #c65454;
      font-weight: 600;
    }

    .submit-btn {
      height: 52px;
      border: 0;
      border-radius: 16px;
      background: linear-gradient(135deg, var(--helma-gold), #e5b93a);
      color: #1f2937;
      font-weight: 800;
      font-size: 15px;
      cursor: pointer;
      transition: 0.2s ease;
      box-shadow: 0 12px 26px rgba(212,166,42,0.24);
    }

    .submit-btn:hover {
      transform: translateY(-1px);
    }

    .submit-btn:disabled {
      opacity: 0.7;
      cursor: not-allowed;
      transform: none;
    }

    .switch-auth {
      margin-top: 20px;
      text-align: center;
      color: var(--color-text-muted);
    }

    .switch-auth a {
      color: var(--helma-teal);
      font-weight: 800;
    }

    .auth-leaves {
      position: absolute;
      inset: 0;
      z-index: 0;
      pointer-events: none;
      overflow: hidden;
    }

    .falling-leaf {
      position: absolute;
      top: -100px;
      width: 38px;
      height: 38px;
      opacity: 0.18;
      background: linear-gradient(180deg, #f8dc7a, var(--helma-gold));
      clip-path: path("M19 2 C27 5, 36 13, 35 22 C34 30, 26 36, 18 37 C11 38, 4 33, 3 25 C2 17, 7 8, 19 2 Z");
      animation: fallingLeaf linear infinite;
    }

    .falling-leaf::after {
      content: '';
      position: absolute;
      left: 50%;
      top: 18%;
      width: 1.5px;
      height: 58%;
      background: rgba(143,107,18,0.35);
      transform: translateX(-50%);
    }

    .fl-1 { left: 8%; animation-duration: 15s; animation-delay: 0s; }
    .fl-2 { left: 26%; animation-duration: 12s; animation-delay: 2s; }
    .fl-3 { left: 48%; animation-duration: 16s; animation-delay: 1s; }
    .fl-4 { left: 72%; animation-duration: 13s; animation-delay: 3s; }
    .fl-5 { left: 90%; animation-duration: 18s; animation-delay: 4s; }

    @keyframes fallingLeaf {
      0% { transform: translate3d(0, -100px, 0) rotate(0deg); }
      25% { transform: translate3d(-20px, 25vh, 0) rotate(60deg); }
      50% { transform: translate3d(18px, 50vh, 0) rotate(130deg); }
      75% { transform: translate3d(-16px, 75vh, 0) rotate(220deg); }
      100% { transform: translate3d(10px, 110vh, 0) rotate(300deg); }
    }

    @keyframes floatTree {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-10px); }
    }

    @keyframes riseIn {
      from { opacity: 0; transform: translateY(30px); }
      to { opacity: 1; transform: translateY(0); }
    }

    @media (max-width: 1080px) {
      .auth-layout {
        grid-template-columns: 1fr;
      }

      .auth-visual {
        padding-bottom: 12px;
      }

      .auth-panel {
        padding-top: 0;
      }
    }

    @media (max-width: 640px) {
      .auth-visual,
      .auth-panel {
        padding: 24px;
      }

      .form-card {
        padding: 24px;
        border-radius: 22px;
      }

      .visual-logo {
        width: 140px;
      }
    }
  `]
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authApi = inject(AuthApiService);
  private readonly sessionService = inject(SessionService);

  readonly loading = signal(false);
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    rememberMe: [true]
  });

  invalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return !!(control.touched && control.invalid);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.authApi.login({
      email: this.form.getRawValue().email,
      password: this.form.getRawValue().password
    }).subscribe({
      next: (res) => {
        this.sessionService.setSession(
          {
            userId: res.userId,
            email: res.email,
            role: res.role,
            token: res.accessToken
          },
          this.form.getRawValue().rememberMe
        );

        this.loading.set(false);
        void this.router.navigateByUrl(this.sessionService.getPortalRoute());
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(
          err?.error?.message ||
          err?.error?.error ||
          'Login failed. Please check your credentials.'
        );
      }
    });
  }
}