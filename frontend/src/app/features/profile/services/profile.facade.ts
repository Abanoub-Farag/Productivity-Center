import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormGroup } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ProfileDataService } from './profile.service';
import {
  UserProfileData,
  UpdateProfileRequest,
  FormSnapshot,
  PageError,
  parseApiError,
} from '../models/profile.models';

@Injectable({ providedIn: 'root' })
export class ProfileFacade {
  private readonly data = inject(ProfileDataService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  // ─── State ──────────────────────────────────────────────────────────────────
  readonly profile = signal<UserProfileData | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly pageError = signal<PageError | null>(null);
  readonly isSaving = signal<boolean>(false);
  private readonly _snapshot = signal<FormSnapshot | null>(null);

  /** Derived user id from AuthService — no token parsing in component. */
  readonly userId = computed<number | null>(() => this.auth.currentUser()?.id ?? null);

  // ─── Actions ────────────────────────────────────────────────────────────────

  loadProfile(): void {
    const id = this.userId();

    if (!id) {
      this.pageError.set({
        title: 'Session error',
        hint: 'We could not determine your identity. Please log out and sign in again.',
      });
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.pageError.set(null);

    this.data.getProfile(id).subscribe({
      next: (res) => {
        if (!res.data) {
          this.pageError.set({
            title: 'No profile data',
            hint: 'The server returned an empty response. Please try again.',
          });
          this.isLoading.set(false);
          return;
        }
        this.profile.set(res.data);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const parsed = parseApiError(err);
        this.pageError.set({ title: this._loadErrorTitle(err.status), hint: parsed.message });
        this.isLoading.set(false);
      },
    });
  }

  patchForm(form: FormGroup, data: UserProfileData): void {
    form.patchValue({
      firstName:   data.firstName   ?? '',
      lastName:    data.lastName    ?? '',
      email:       data.email       ?? '',
      bio:         data.bio         ?? '',
      gender:      data.gender      ?? '',
      dateOfBirth: data.dateOfBirth ?? '',
    });
    this._snapshot.set(form.getRawValue() as FormSnapshot);
  }

  isFormDirty(form: FormGroup): boolean {
    const snap = this._snapshot();
    if (!snap) return false;
    const current = form.getRawValue() as FormSnapshot;
    return (Object.keys(current) as (keyof FormSnapshot)[]).some(
      (k) => current[k] !== snap[k],
    );
  }

  saveProfile(form: FormGroup): void {
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);

    const raw = form.getRawValue() as FormSnapshot;
    const payload: UpdateProfileRequest = {
      firstName:   raw.firstName,
      lastName:    raw.lastName,
      bio:         raw.bio ?? '',
      gender:      (raw.gender as 'MALE' | 'FEMALE') || undefined,
      dateOfBirth: raw.dateOfBirth || undefined,
    };

    this.data.updateProfile(payload).subscribe({
      next: (res) => {
        this.isSaving.set(false);
        if (res.data) {
          this.profile.set(res.data);
          this.patchForm(form, res.data);
        }
        this.toast.success(res.message ?? 'Profile updated successfully.');
      },
      error: (err: HttpErrorResponse) => {
        this.isSaving.set(false);
        const parsed = parseApiError(err);

        if (err.status === 400 && Object.keys(parsed.fieldErrors).length > 0) {
          this._applyFieldErrors(form, parsed.fieldErrors);
        }

        this.toast.error(parsed.message);
      },
    });
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private _applyFieldErrors(form: FormGroup, errors: Record<string, string>): void {
    for (const [field, message] of Object.entries(errors)) {
      const ctrl = form.get(field);
      if (ctrl) {
        ctrl.setErrors({ serverError: message });
        ctrl.markAsTouched();
      }
    }
  }

  private _loadErrorTitle(status: number): string {
    if (!navigator.onLine || status === 0) return 'No connection';
    if (status === 401 || status === 403) return 'Access denied';
    if (status === 404) return 'Profile not found';
    return 'Failed to load profile';
  }
}
