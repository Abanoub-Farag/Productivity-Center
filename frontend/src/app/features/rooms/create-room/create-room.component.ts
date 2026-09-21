import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SidebarComponent } from '../components/sidebar/sidebar.component';
import { TopNavComponent } from '../components/top-nav/top-nav.component';
import { RoomsDataService } from '../services/rooms-data.service';
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
  private readonly roomsData = inject(RoomsDataService);
  private readonly facade = inject(RoomsFacade);

  readonly PlusIcon = Plus;
  readonly GlobeIcon = Globe;
  readonly ShieldIcon = Shield;

  createRoomForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    visibility: ['PUBLIC', [Validators.required]],
  });

  isSubmitting = signal<boolean>(false);
  error = signal<string | null>(null);

  onSubmit(): void {
    if (this.createRoomForm.invalid) {
      this.createRoomForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.error.set(null);

    const payload = this.createRoomForm.value as CreateRoomDto;

    this.roomsData.createRoom(payload).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.facade.handleRoomCreated(response.data?.id);
      },
      error: (err: unknown) => {
        console.error('Error creating room', err);
        this.error.set('Failed to create the room. Please try again.');
        this.isSubmitting.set(false);
      },
    });
  }
}
