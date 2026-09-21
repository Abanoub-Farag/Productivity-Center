import { Injectable, inject } from '@angular/core';
import { Router, ActivatedRouteSnapshot } from '@angular/router';
import { signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { AuthService, AuthError } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { LoginRequest, RegisterRequest } from '../../../core/models/auth.models';

export type { AuthError };

@Injectable({ providedIn: 'root' })
export class AuthFacade {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  // ─── Shared state ────────────────────────────────────────────────────────────
  readonly isLoading = signal<boolean>(false);
  readonly serverError = signal<string | null>(null);
  readonly showPassword = signal<boolean>(false);

  // ─── Register-only state ──────────────────────────────────────────────────────
  readonly successMessage = signal<string | null>(null);

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  // ─── Login ────────────────────────────────────────────────────────────────────

  login(form: FormGroup, redirectSnapshot?: ActivatedRouteSnapshot): void {
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.serverError.set(null);

    const { email, password } = form.getRawValue() as LoginRequest;

    this.auth.login({ email, password }).subscribe({
      next: () => {
        this.isLoading.set(false);
        const redirect = redirectSnapshot?.queryParamMap.get('redirect');
        const target = redirect && redirect !== '/login' ? redirect : '/rooms';
        this.router.navigateByUrl(target, { replaceUrl: true });
      },
      error: (err: AuthError) => {
        this.isLoading.set(false);
        this.serverError.set(err.message);
        this._applyFieldErrors(form, err.fieldErrors);
      },
    });
  }

  // ─── Register ─────────────────────────────────────────────────────────────────

  register(form: FormGroup): void {
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.serverError.set(null);
    this.successMessage.set(null);

    const payload = form.getRawValue() as RegisterRequest;

    this.auth.register(payload).subscribe({
      next: () => {
        this.isLoading.set(false);
        form.disable();

        const msg = 'Account created successfully! Redirecting to login…';
        this.successMessage.set(msg);
        this.toast.success(msg, 3500);

        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err: AuthError) => {
        this.isLoading.set(false);

        if (err.status === 409) {
          const conflictMsg =
            err.message || 'An account with this email already exists. Please log in or use a different address.';
          form.get('email')?.setErrors({ serverError: conflictMsg });
          this.serverError.set(conflictMsg);
          this.toast.error(conflictMsg);
          return;
        }

        this.serverError.set(err.message);
        this.toast.error(err.message);
        this._applyFieldErrors(form, err.fieldErrors);
      },
    });
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private _applyFieldErrors(form: FormGroup, errors: Record<string, string> | undefined): void {
    if (!errors) return;
    Object.entries(errors).forEach(([field, msg]) => {
      form.get(field)?.setErrors({ serverError: msg });
    });
  }
}
