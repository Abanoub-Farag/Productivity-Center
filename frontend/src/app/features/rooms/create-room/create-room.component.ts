import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SidebarComponent } from '../components/sidebar/sidebar.component';
import { TopNavComponent } from '../components/top-nav/top-nav.component';
import { RoomsFacade } from '../services/rooms.facade';
import { LucideAngularModule, Plus, Globe, Shield } from 'lucide-angular';
import { CreateRoomDto } from '../models/rooms.models';

@Component({
  selector: 'app-create-room',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    SidebarComponent,
    TopNavComponent,
    LucideAngularModule,
  ],
  providers: [RoomsFacade],
  templateUrl: './create-room.component.html',
  styleUrls: ['./create-room.component.scss'],
})
export class CreateRoomComponent {
  private readonly fb = inject(FormBuilder);
  private readonly facade = inject(RoomsFacade);

  readonly PlusIcon = Plus;
  readonly GlobeIcon = Globe;
  readonly ShieldIcon = Shield;

  readonly createRoomForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    visibility: ['PUBLIC', [Validators.required]],
  });

  /** Delegated to facade — avoids local duplicate signals for isLoading/error. */
  readonly isSubmitting = computed(() => this.facade.isLoading());
  readonly error = computed(() => this.facade.error());

  onSubmit(): void {
    if (this.createRoomForm.invalid) {
      this.createRoomForm.markAllAsTouched();
      return;
    }

    const payload = this.createRoomForm.value as CreateRoomDto;
    this.facade.createRoom(payload);
  }
}
