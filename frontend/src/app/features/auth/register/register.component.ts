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
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthFacade } from '../services/auth.facade';
import { ThemeService } from '../../../core/services/theme.service';

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*#?&]).{8,64}$/;

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  readonly facade = inject(AuthFacade);
  readonly themeService = inject(ThemeService);

  readonly registerForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
    lastName:  ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
    email:     ['', [Validators.required, Validators.email]],
    password:  ['', [Validators.required, Validators.minLength(8), Validators.maxLength(64), Validators.pattern(PASSWORD_PATTERN)]],
  });

  get firstName(): AbstractControl { return this.registerForm.get('firstName')!; }
  get lastName():  AbstractControl { return this.registerForm.get('lastName')!; }
  get email():     AbstractControl { return this.registerForm.get('email')!; }
  get password():  AbstractControl { return this.registerForm.get('password')!; }

  showError(ctrl: AbstractControl): boolean {
    return ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  togglePasswordVisibility(): void { this.facade.togglePassword(); }

  onSubmit(): void {
    this.facade.register(this.registerForm);
  }
}
