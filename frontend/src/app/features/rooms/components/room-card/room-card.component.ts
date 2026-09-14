import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, ArrowRight, Eye, Heart, Shield } from 'lucide-angular';

export interface Room {
  id: string;
  title: string;
  description: string;
  tags: string[];
  actionType: 'join' | 'view';
  visibility?: 'PUBLIC' | 'PRIVATE';
  isFavorite?: boolean;
  addedAt?: string;
  isPendingFavorite?: boolean;
}

@Component({
  selector: 'app-room-card',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './room-card.component.html',
  styleUrls: ['./room-card.component.scss']
})
export class RoomCardComponent {
  @Input() room!: Room;
  @Output() onAction = new EventEmitter<string>();
  @Output() onToggleFavorite = new EventEmitter<Room>();
  
  // Expose icons for template
  readonly ArrowRightIcon = ArrowRight;
  readonly EyeIcon = Eye;
  readonly HeartIcon = Heart;
  readonly ShieldIcon = Shield;

  handleAction() {
    this.onAction.emit(this.room.id);
  }

  toggleFavorite(event: Event) {
    event.stopPropagation();
    if (this.room.isPendingFavorite) return;
    this.onToggleFavorite.emit(this.room);
  }
}
