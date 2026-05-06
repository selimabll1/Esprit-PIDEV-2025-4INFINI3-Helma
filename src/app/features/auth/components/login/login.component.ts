import { Component, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnDestroy {
  readonly form: FormGroup;
  loading = false;
  error = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.form = this.fb.nonNullable.group({
      email:      ['', [Validators.required, Validators.email]],
      password:   ['', [Validators.required]],
      rememberMe: [true]
    });
  }

  invalid(name: string): boolean {
    const ctrl = this.form.get(name);
    return !!(ctrl?.touched && ctrl?.invalid);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    const { email, password } = this.form.getRawValue() as { email: string; password: string; rememberMe: boolean };

    this.authService.login({ email, password })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          void this.router.navigateByUrl(this.authService.getPortalRoute());
        },
        error: (err: HttpErrorResponse) => {
          this.loading = false;
          this.error = err?.error?.message || err?.error?.error || 'Login failed. Please check your credentials.';
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
