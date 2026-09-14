import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  ArrowLeft,
  Bell,
  Heart,
  Pencil,
  Trash2,
  Lock,
} from 'lucide-angular';
import { RoomData } from '../../services/room.service';

@Component({
  selector: 'app-room-header',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './room-header.component.html',
  styleUrls: ['./room-header.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RoomHeaderComponent {
  readonly ArrowLeftIcon = ArrowLeft;
  readonly BellIcon = Bell;
  readonly HeartIcon = Heart;
  readonly PencilIcon = Pencil;
  readonly Trash2Icon = Trash2;
  readonly LockIcon = Lock;

  room = input<RoomData | null>(null);
  heartbeatStatus = input<'active' | 'retrying' | 'failed'>('active');
  isFavorite = input<boolean>(false);
  isPendingFavorite = input<boolean>(false);
  isOwner = input<boolean>(false);

  toggleFavorite = output<void>();
  editRoom = output<void>();
  deleteRoom = output<void>();
}
