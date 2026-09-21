import {
  Component,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import {
  FormBuilder,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
} from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthFacade } from '../services/auth.facade';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  readonly facade = inject(AuthFacade);
  readonly themeService = inject(ThemeService);

  readonly loginForm = this.fb.group({
    email:      ['', [Validators.required, Validators.email]],
    password:   ['', [Validators.required]],
    rememberMe: [false],
  });

  get email(): AbstractControl { return this.loginForm.get('email')!; }
  get password(): AbstractControl { return this.loginForm.get('password')!; }

  showError(ctrl: AbstractControl): boolean {
    return ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  togglePasswordVisibility(): void { this.facade.togglePassword(); }

  onSubmit(): void {
    this.facade.login(this.loginForm, this.route.snapshot);
  }
}
