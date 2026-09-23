import {
  Component,
  input,
  output,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, X, Save, Globe, Lock } from 'lucide-angular';
import { Room, UpdateRoomDto, RoomVisibility } from '../../models/rooms.models';

@Component({
  selector: 'app-edit-room-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './edit-room-modal.component.html',
  styleUrls: ['./edit-room-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditRoomModalComponent implements OnInit {
  readonly room = input.required<Room>();

  /** Emits the updated DTO when the user submits. */
  readonly onSave = output<{ roomId: string; dto: UpdateRoomDto }>();
  /** Emits when the user dismisses the modal. */
  readonly onClose = output<void>();

  readonly XIcon = X;
  readonly SaveIcon = Save;
  readonly GlobeIcon = Globe;
  readonly LockIcon = Lock;

  readonly title = signal('');
  readonly description = signal('');
  readonly visibility = signal<RoomVisibility>('PUBLIC');
  readonly isSaving = signal(false);

  ngOnInit(): void {
    const r = this.room();
    this.title.set(r.title);
    this.description.set(r.description);
    this.visibility.set(r.visibility ?? 'PUBLIC');
  }

  toggleVisibility(): void {
    this.visibility.set(this.visibility() === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC');
  }

  submit(): void {
    const t = this.title().trim();
    const d = this.description().trim();
    if (!t || !d) return;

    this.isSaving.set(true);
    this.onSave.emit({
      roomId: this.room().id,
      dto: { title: t, description: d, visibility: this.visibility() },
    });
  }

  close(): void {
    this.onClose.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close();
    }
  }
}
