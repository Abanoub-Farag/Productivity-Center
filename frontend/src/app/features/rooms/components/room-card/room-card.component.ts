import {
  Component,
  input,
  output,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, ArrowRight, Eye, Heart, Shield, Pencil } from 'lucide-angular';
import { Room } from '../../models/rooms.models';

export type { Room };

@Component({
  selector: 'app-room-card',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './room-card.component.html',
  styleUrls: ['./room-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoomCardComponent {
  readonly room = input.required<Room>();
  /** Current authenticated user's ID — used to show the edit button only to the owner. */
  readonly currentUserId = input<number | null>(null);

  readonly onAction = output<string>();
  readonly onToggleFavorite = output<Room>();
  readonly onEdit = output<Room>();

  readonly ArrowRightIcon = ArrowRight;
  readonly EyeIcon = Eye;
  readonly HeartIcon = Heart;
  readonly ShieldIcon = Shield;
  readonly PencilIcon = Pencil;

  get isOwner(): boolean {
    const uid = this.currentUserId();
    const oid = this.room().ownerId;
    return uid != null && oid != null && uid === oid;
  }

  handleAction(): void {
    this.onAction.emit(this.room().id);
  }

  toggleFavorite(event: Event): void {
    event.stopPropagation();
    if (this.room().isPendingFavorite) return;
    this.onToggleFavorite.emit(this.room());
  }

  editRoom(event: Event): void {
    event.stopPropagation();
    this.onEdit.emit(this.room());
  }
}
