import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SidebarComponent } from '../components/sidebar/sidebar.component';
import { TopNavComponent } from '../components/top-nav/top-nav.component';
import { RoomService } from '../services/room.service';
import { AuthService } from '../../../core/services/auth.service';
import { LucideAngularModule, Plus, Globe, Shield } from 'lucide-angular';

@Component({
  selector: 'app-create-room',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, SidebarComponent, TopNavComponent, LucideAngularModule],
  templateUrl: './create-room.component.html',
  styleUrls: ['./create-room.component.scss']
})
export class CreateRoomComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly roomService = inject(RoomService);
  private readonly authService = inject(AuthService);

  readonly PlusIcon = Plus;
  readonly GlobeIcon = Globe;
  readonly ShieldIcon = Shield;
  
  createRoomForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    visibility: ['PUBLIC', [Validators.required]]
  });

  isSubmitting = signal<boolean>(false);
  error = signal<string | null>(null);

  onSubmit() {
    if (this.createRoomForm.invalid) {
      this.createRoomForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.error.set(null);

    const payload = this.createRoomForm.value;

    this.roomService.createRoom(payload).subscribe({
      next: (response) => this.handleSuccess(response.data?.id),
      error: (err) => this.handleError(err)
    });
  }

  private handleSuccess(newRoomId?: number) {
    this.isSubmitting.set(false);
    if (newRoomId) {
      this.authService.addRoomId(newRoomId);
      this.router.navigate(['/rooms', newRoomId]);
      return;
    }
    this.router.navigate(['/rooms']);
  }

  private handleError(err: any) {
    console.error('Error creating room', err);
    this.error.set('Failed to create the room. Please try again.');
    this.isSubmitting.set(false);
  }
}
