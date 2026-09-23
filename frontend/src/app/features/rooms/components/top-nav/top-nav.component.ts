import { Component, output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Search, Bell } from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { RoomActionButtonComponent } from '../room-action-button/room-action-button.component';

@Component({
  selector: 'app-top-nav',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, FormsModule, RoomActionButtonComponent],
  templateUrl: './top-nav.component.html',
  styleUrls: ['./top-nav.component.scss'],
})
export class TopNavComponent {
  /** Emits the current search query string on each keystroke. */
  readonly searchChange = output<string>();

  readonly SearchIcon = Search;
  readonly BellIcon = Bell;

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchChange.emit(value);
  }
}
