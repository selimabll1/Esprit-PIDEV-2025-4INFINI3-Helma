import { Component, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';
import { UserRole } from '../../../../core/models/auth.model';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent implements OnDestroy {
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
      firstName:       ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50), Validators.pattern(/^[A-Za-zÀ-ÿ' -]+$/)]],
      lastName:        ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50), Validators.pattern(/^[A-Za-zÀ-ÿ' -]+$/)]],
      email:           ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
      password:        ['', [Validators.required, Validators.minLength(8), Validators.maxLength(64), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).+$/)]],
      confirmPassword: ['', [Validators.required]],
      role:            ['', [Validators.required]],
      acceptTerms:     [false, [Validators.requiredTrue]]
    });
  }

  invalid(name: string): boolean {
    const ctrl = this.form.get(name);
    return !!(ctrl?.touched && ctrl?.invalid);
  }

  passwordMismatch(): boolean {
    const p  = this.form.get('password')?.value as string;
    const cp = this.form.get('confirmPassword');
    return !!(cp?.touched && cp.value && p !== cp.value);
  }

  submit(): void {
    if (this.form.invalid || this.passwordMismatch()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    const raw = this.form.getRawValue() as {
      firstName: string; lastName: string; email: string;
      password: string; role: string; confirmPassword: string; acceptTerms: boolean;
    };

    this.authService.register({
      firstName: raw.firstName.trim(),
      lastName:  raw.lastName.trim(),
      email:     raw.email.trim().toLowerCase(),
      password:  raw.password,
      role:      raw.role as UserRole
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: () => {
        this.loading = false;
        void this.router.navigateByUrl(this.authService.getPortalRoute());
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = err?.error?.message || err?.error?.error || 'Registration failed. Please try again.';
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
