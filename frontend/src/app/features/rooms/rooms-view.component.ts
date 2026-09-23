import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { TopNavComponent } from './components/top-nav/top-nav.component';
import { RoomCardComponent } from './components/room-card/room-card.component';
import { EditRoomModalComponent } from './components/edit-room-modal/edit-room-modal.component';
import { RoomsFacade } from './services/rooms.facade';
import { Room, UpdateRoomDto } from './models/rooms.models';

type Tab = 'All Rooms' | 'My Teams' | 'Favorites';

@Component({
  selector: 'app-rooms-view',
  standalone: true,
  imports: [SidebarComponent, TopNavComponent, RoomCardComponent, EditRoomModalComponent],
  providers: [RoomsFacade],
  templateUrl: './rooms-view.component.html',
  styleUrls: ['./rooms-view.component.scss'],
})
export class RoomsViewComponent implements OnInit {
  readonly facade = inject(RoomsFacade);

  readonly tabs: Tab[] = ['All Rooms', 'My Teams', 'Favorites'];
  readonly activeTab = signal<Tab>('All Rooms');
  readonly searchQuery = signal<string>('');

  /** Room currently being edited — opens the modal when non-null. */
  readonly editingRoom = signal<Room | null>(null);

  /** Client-side filter over facade.rooms() — no extra HTTP calls needed. */
  readonly filteredRooms = computed(() => {
    const query = this.searchQuery().toLowerCase();
    const tab = this.activeTab();
    return this.facade.rooms().filter((room) => this.matchesSearchAndTab(room, query, tab));
  });

  ngOnInit(): void {
    // loadAll() chains favorites → rooms sequentially, fixing the isFavorite race condition.
    this.facade.loadAll();
  }

  setActiveTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.facade.setActiveTab(tab);
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

  onEditRoom(room: Room): void {
    this.editingRoom.set(room);
  }

  onSaveRoom(event: { roomId: string; dto: UpdateRoomDto }): void {
    this.facade.updateRoom(event.roomId, event.dto);
    this.editingRoom.set(null);
  }

  onCloseModal(): void {
    this.editingRoom.set(null);
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
