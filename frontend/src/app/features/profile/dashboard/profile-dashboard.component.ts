import { Component, OnInit, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ProfileFacade } from '../services/profile.facade';
import { ToastService } from '../../../core/services/toast.service';
import { SidebarComponent } from '../../rooms/components/sidebar/sidebar.component';
import { TopNavComponent } from '../../rooms/components/top-nav/top-nav.component';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-profile-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    SidebarComponent,
    TopNavComponent,
    LucideAngularModule,
  ],
  templateUrl: './profile-dashboard.component.html',
  styleUrls: ['./profile-dashboard.component.scss'],
})
export class ProfileDashboardComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly facade = inject(ProfileFacade);
  private readonly toastService = inject(ToastService);

  readonly userInfoForm = this.fb.group({
    firstName:   ['', Validators.required],
    lastName:    ['', Validators.required],
    email:       [{ value: '', disabled: true }],
    bio:         [''],
    gender:      [''],
    dateOfBirth: [''],
  });

  constructor() {
    // Reactively patch form whenever profile data arrives or refreshes
    effect(() => {
      const profile = this.facade.profile();
      if (profile) {
        this.facade.patchForm(this.userInfoForm, profile);
      }
    });
  }

  get isFormDirty(): boolean {
    return this.facade.isFormDirty(this.userInfoForm);
  }

  ngOnInit(): void {
    this.facade.loadProfile();
  }

  fetchProfile(): void {
    this.facade.loadProfile();
  }

  onSaveUserInfo(): void {
    this.facade.saveProfile(this.userInfoForm);
  }

  get formattedCreatedAt(): string {
    const data = this.facade.profile();
    if (!data?.createdAt) return 'Unknown';
    return new Date(data.createdAt).toLocaleDateString('en-US', {
      year: 'numeric', month: 'long', day: 'numeric',
    });
  }

  getServerError(field: string): string | null {
    return this.userInfoForm.get(field)?.errors?.['serverError'] ?? null;
  }

  isInvalid(field: string): boolean {
    const ctrl = this.userInfoForm.get(field);
    return !!(ctrl?.invalid && ctrl.touched);
  }
}
