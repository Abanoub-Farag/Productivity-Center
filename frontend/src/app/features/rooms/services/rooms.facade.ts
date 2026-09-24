import { Injectable, inject, signal, DestroyRef, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { RoomsDataService } from './rooms-data.service';
import { FavoriteRoomService } from './favorite-room.service';
import { AuthService } from '../../../core/services/auth.service';
import { Room, FavoriteRoomItem, RoomData, CreateRoomDto, UpdateRoomDto } from '../models/rooms.models';

type ActiveTab = 'All Rooms' | 'My Teams' | 'Favorites';

@Injectable()
export class RoomsFacade {
  private readonly data = inject(RoomsDataService);
  private readonly favService = inject(FavoriteRoomService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ──────────────────────────────────────────────────────────────────

  readonly rooms = signal<Room[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);

  readonly favoriteRoomIds = signal<Set<string>>(new Set());
  readonly favPage = signal<number>(0);
  readonly favTotalPages = signal<number>(1);
  readonly favIsFirst = signal<boolean>(true);
  readonly favIsLast = signal<boolean>(true);

  /** Delegates to RoomsDataService — single source of truth for the user's active room. */
  readonly userRoomId = this.data.userRoomId;

  /** Current authenticated user's numeric ID — used for ownership checks. */
  readonly currentUserId = computed<number | null>(() => this.authService.currentUser()?.id ?? null);

  private readonly _activeTab = signal<ActiveTab>('All Rooms');
  /** Read-only view of the active tab for template consumers. */
  readonly activeTab = computed(() => this._activeTab());

  // ── Actions ────────────────────────────────────────────────────────────────

  setActiveTab(tab: ActiveTab): void {
    this._activeTab.set(tab);
  }

  /**
   * Loads favorites first, then rooms — guarantees isFavorite is correctly
   * set on every room card without a race condition.
   */
  loadAll(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.favService.getFavorites(0, 100)
      .pipe(
        switchMap((res) => {
          const favContent: FavoriteRoomItem[] = res.data?.content ?? [];
          const set = new Set(favContent.map((f) => f.roomId.toString()));
          this.favoriteRoomIds.set(set);
          return this.data.getRooms(0, 50);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          const favSet = this.favoriteRoomIds();
          const fetchedRooms = response.data?.content ?? [];
          const mapped: Room[] = fetchedRooms
            .filter((r: RoomData) => r.id != null)
            .map((r: RoomData) => ({
              id: r.id.toString(),
              title: r.title ?? 'Untitled Room',
              description: r.description ?? 'No description provided.',
              tags: r.tags ?? [],
              actionType: (r.actionType as 'join' | 'view') ?? 'view',
              visibility: r.visibility ?? 'PUBLIC',
              isFavorite: favSet.has(r.id.toString()),
              ownerId: r.ownerId,
            }));
          this.rooms.set(mapped);
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Failed to load rooms.');
          this.isLoading.set(false);
        },
      });
  }

  loadRooms(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.data.getRooms(0, 50)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const favSet = this.favoriteRoomIds();
          const fetchedRooms = response.data?.content ?? [];
          const mapped: Room[] = fetchedRooms
            .filter((r: RoomData) => r.id != null)
            .map((r: RoomData) => ({
              id: r.id.toString(),
              title: r.title ?? 'Untitled Room',
              description: r.description ?? 'No description provided.',
              tags: r.tags ?? [],
              actionType: (r.actionType as 'join' | 'view') ?? 'view',
              visibility: r.visibility ?? 'PUBLIC',
              isFavorite: favSet.has(r.id.toString()),
              ownerId: r.ownerId,
            }));
          this.rooms.set(mapped);
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Failed to load rooms.');
          this.isLoading.set(false);
        },
      });
  }

  loadFavoriteSet(): void {
    this.favService.getFavorites(0, 100)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const favContent: FavoriteRoomItem[] = res.data?.content ?? [];
          const set = new Set(favContent.map((f) => f.roomId.toString()));
          this.favoriteRoomIds.set(set);
          this.rooms.update((list) =>
            list.map((r) => ({ ...r, isFavorite: set.has(r.id) })),
          );
        },
        error: (err: HttpErrorResponse) =>
          console.error('Error preloading favorites:', err),
      });
  }

  loadFavorites(page = 0): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.favPage.set(page);

    this.favService.getFavorites(page, 20)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const d = response.data;
          const items: FavoriteRoomItem[] = d?.content ?? [];
          this.favIsFirst.set(d?.first ?? true);
          this.favIsLast.set(d?.last ?? true);
          this.favTotalPages.set(d?.totalPages ?? 1);

          const mapped: Room[] = items.map((f) => ({
            id: f.roomId.toString(),
            title: f.title ?? 'Untitled Room',
            description: f.description ?? 'No description provided.',
            tags: ['favorite'],
            actionType: 'join',
            visibility: 'PUBLIC',
            isFavorite: true,
            addedAt: f.addedAt,
          }));
          this.rooms.set(mapped);
          this.isLoading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.extractErrorMessage(err, 'Failed to load favorite rooms.'));
          this.isLoading.set(false);
        },
      });
  }

  joinRoom(roomId: string): void {
    const numericId = parseInt(roomId, 10);
    if (isNaN(numericId) || numericId <= 0) {
      this.router.navigate(['/rooms', roomId]);
      return;
    }
    this.data.joinRoom(numericId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigate(['/rooms', roomId]),
        error: () => this.router.navigate(['/rooms', roomId]),
      });
  }

  toggleFavorite(room: Room): void {
    const isCurrentlyFav = !!room.isFavorite;
    const targetState = !isCurrentlyFav;

    this.rooms.update((list) =>
      list.map((r) =>
        r.id === room.id ? { ...r, isFavorite: targetState, isPendingFavorite: true } : r,
      ),
    );

    const request$: Observable<unknown> = targetState
      ? this.favService.addFavorite(parseInt(room.id, 10))
      : this.favService.removeFavorite(parseInt(room.id, 10));

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.handleFavoriteSuccess(room.id, targetState),
      error: (err: HttpErrorResponse) =>
        this.handleFavoriteError(room.id, isCurrentlyFav, err),
    });
  }

  createRoom(payload: CreateRoomDto): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.data.createRoom(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.isLoading.set(false);
          this.handleRoomCreated(response.data?.id);
        },
        error: (err: unknown) => {
          console.error('Error creating room', err);
          this.error.set('Failed to create the room. Please try again.');
          this.isLoading.set(false);
        },
      });
  }

  updateRoom(roomId: string, dto: UpdateRoomDto): void {
    this.data.updateRoom(roomId, dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const updated = response.data;
          if (!updated) return;
          this.rooms.update((list) =>
            list.map((r) =>
              r.id === roomId
                ? {
                    ...r,
                    title: updated.title ?? r.title,
                    description: updated.description ?? r.description,
                    visibility: updated.visibility ?? r.visibility,
                  }
                : r,
            ),
          );
        },
        error: (err: HttpErrorResponse) => {
          console.error('Failed to update room:', err);
          alert(this.extractErrorMessage(err, 'Could not update the room.'));
        },
      });
  }

  /** Called after a successful room creation to update auth user state + navigate. */
  handleRoomCreated(newRoomId: number | undefined): void {
    if (newRoomId) {
      this.authService.addRoomId(newRoomId);
      this.router.navigate(['/rooms', newRoomId]);
    } else {
      this.router.navigate(['/rooms']);
    }
  }

  // ── Private ────────────────────────────────────────────────────────────────

  private handleFavoriteSuccess(roomId: string, targetState: boolean): void {
    const activeTab = this._activeTab();
    this.rooms.update((list) => {
      if (!targetState && activeTab === 'Favorites') {
        return list.filter((r) => r.id !== roomId);
      }
      return list.map((r) =>
        r.id === roomId ? { ...r, isFavorite: targetState, isPendingFavorite: false } : r,
      );
    });
    this.favoriteRoomIds.update((set) => {
      const next = new Set(set);
      if (targetState) next.add(roomId);
      else next.delete(roomId);
      return next;
    });
  }

  private handleFavoriteError(
    roomId: string,
    isCurrentlyFav: boolean,
    err: HttpErrorResponse,
  ): void {
    console.error('Failed to toggle favorite:', err);
    this.rooms.update((list) =>
      list.map((r) =>
        r.id === roomId ? { ...r, isFavorite: isCurrentlyFav, isPendingFavorite: false } : r,
      ),
    );
    alert(this.extractErrorMessage(err, 'Could not update favorite status.'));
  }

  private extractErrorMessage(err: HttpErrorResponse, defaultMsg: string): string {
    if (err?.status === 0) return 'Network Error: Unable to connect to server.';
    const errs = err?.error?.errors;
    if (errs && typeof errs === 'object' && Object.keys(errs).length > 0) {
      return Object.entries(errs as Record<string, string>)
        .map(([k, v]) => `${k}: ${v}`)
        .join('; ');
    }
    return (err?.error?.message as string | undefined) ?? defaultMsg;
  }
}
