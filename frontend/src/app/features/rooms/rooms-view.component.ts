import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { TopNavComponent } from './components/top-nav/top-nav.component';
import { RoomCardComponent } from './components/room-card/room-card.component';
import { RoomsFacade } from './services/rooms.facade';
import { Room } from './models/rooms.models';

type Tab = 'All Rooms' | 'My Teams' | 'Favorites';

@Component({
  selector: 'app-rooms-view',
  standalone: true,
  imports: [CommonModule, SidebarComponent, TopNavComponent, RoomCardComponent],
  providers: [RoomsFacade],
  templateUrl: './rooms-view.component.html',
  styleUrls: ['./rooms-view.component.scss'],
})
export class RoomsViewComponent implements OnInit {
  readonly facade = inject(RoomsFacade);

  readonly tabs: Tab[] = ['All Rooms', 'My Teams', 'Favorites'];
  readonly activeTab = signal<Tab>('All Rooms');
  readonly searchQuery = signal<string>('');

  /** Client-side filter over facade.rooms() — no extra HTTP calls needed. */
  readonly filteredRooms = computed(() => {
    const query = this.searchQuery().toLowerCase();
    const tab = this.activeTab();
    return this.facade.rooms().filter((room) => this.matchesSearchAndTab(room, query, tab));
  });

  ngOnInit(): void {
    this.facade.loadFavoriteSet();
    this.facade.loadRooms();
  }

  setActiveTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.facade._activeTab = tab;
    if (tab === 'Favorites') {
      this.facade.loadFavorites(0);
      return;
    }
    this.facade.loadRooms();
  }

  onSearchChange(query: string): void {
    this.searchQuery.set(query);
  }

  onRoomAction(roomId: string): void {
    this.facade.joinRoom(roomId);
  }

  onToggleFavorite(room: Room): void {
    this.facade.toggleFavorite(room);
  }

  private matchesSearchAndTab(room: Room, query: string, tab: Tab): boolean {
    const matchesQuery =
      !query ||
      room.title.toLowerCase().includes(query) ||
      room.description.toLowerCase().includes(query) ||
      room.tags.some((tag) => tag.toLowerCase().includes(query));

    if (!matchesQuery) return false;
    if (tab === 'My Teams') {
      const teamTags = ['engineering', 'frontend', 'design'];
      return room.tags.some((t) => teamTags.includes(t.toLowerCase()));
    }
    return true;
  }
}
