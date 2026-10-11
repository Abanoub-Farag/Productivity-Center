import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { timer, switchMap, retry, catchError, of, Observable } from 'rxjs';
import { Router } from '@angular/router';
import { RoomsDataService } from './rooms-data.service';
import { FavoriteRoomService } from './favorite-room.service';
import { AuthService } from '../../../core/services/auth.service';
import { RoomData, UpdateRoomDto } from '../models/rooms.models';

/** Discriminated-union type for heartbeat stream results. */
type HeartbeatResult =
  | { isError: false }
  | { isError: true; error: HttpErrorResponse };

/**
 * Scoped per room-detail route; provided in RoomDetailComponent's providers array.
 *
 * Owns: room metadata fetch/update/delete, favourite toggle, and the heartbeat polling loop.
 * Timer state lives in TimerFacade; task state lives in TaskFacade.
 */
@Injectable()
export class RoomDetailFacade {
  private readonly roomsData = inject(RoomsDataService);
  private readonly favService = inject(FavoriteRoomService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // ── Room state ─────────────────────────────────────────────────────────────
  readonly roomId = signal<number | null>(null);
  readonly room = signal<RoomData | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);

  readonly isOwner = computed(() => {
    const r = this.room();
    const user = this.authService.currentUser();
    return !!r && !!user && r.ownerId === user.id;
  });

  readonly isFavorite = signal<boolean>(false);
  readonly isPendingFavorite = signal<boolean>(false);

  // ── Delete modal state ─────────────────────────────────────────────────────
  readonly isDeleteRoomModalOpen = signal<boolean>(false);
  readonly isDeletingRoom = signal<boolean>(false);
  readonly roomDeleteError = signal<string | null>(null);

  // ── Heartbeat state ────────────────────────────────────────────────────────
  /**
   * Incremented on every successful heartbeat.
   * RoomMembersListComponent reacts to this via an input binding to re-fetch members,
   * replacing the former mutable `onHeartbeatSuccess` callback property.
   */
  readonly heartbeatTick = signal<number>(0);

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  initialize(id: number): void {
    this.roomId.set(id);
    this.joinRoom(id);
    this.fetchRoom(id);
    this.startHeartbeat(id);
    this.checkIfFavorite(id);
  }

  // ── Room actions ───────────────────────────────────────────────────────────

  checkIfFavorite(roomId: number): void {
    this.favService.isFavorite(roomId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.isFavorite.set(Boolean(res.data));
        },
        error: (err: HttpErrorResponse) =>
          console.error('Failed checking favorite state:', err),
      });
  }

  toggleFavorite(): void {
    const currentRoom = this.room();
    if (!currentRoom || this.isPendingFavorite()) return;

    const { id: roomId } = currentRoom;
    const isCurrentlyFav = this.isFavorite();
    const targetState = !isCurrentlyFav;

    this.isFavorite.set(targetState);
    this.isPendingFavorite.set(true);

    const request$: Observable<unknown> = targetState
      ? this.favService.addFavorite(roomId)
      : this.favService.removeFavorite(roomId);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.isPendingFavorite.set(false),
      error: (err: HttpErrorResponse) => {
        this.isFavorite.set(isCurrentlyFav);
        this.isPendingFavorite.set(false);
        const msg = (err.error?.message as string | undefined) ?? 'Failed to update favorite status.';
        alert(msg);
      },
    });
  }

  joinRoom(id: number): void {
    this.roomsData.joinRoom(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => console.log(`Successfully joined room ${id}`),
        error: (err: HttpErrorResponse) =>
          console.error(`Error joining room ${id}:`, err),
      });
  }

  fetchRoom(id: number): void {
    this.roomsData.getRoomById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (response.data) {
            this.room.set(response.data);
            this.isLoading.set(false);
          } else {
            this.router.navigate(['/404'], { replaceUrl: true });
          }
        },
        error: (err: HttpErrorResponse) => {
          console.error(err);
          if (err?.status === 404) {
            this.router.navigate(['/404'], { replaceUrl: true });
          } else {
            this.error.set('Failed to load room details.');
            this.isLoading.set(false);
          }
        },
      });
  }

  updateRoom(roomId: number, dto: UpdateRoomDto): void {
    this.roomsData.updateRoom(roomId, dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.room.update((current) => {
            if (!current) return null;
            return res.data ? { ...current, ...res.data } : { ...current, ...dto };
          });
        },
        error: (err: HttpErrorResponse) => {
          const msg =
            err.status === 403
              ? 'You do not have permission to perform this action'
              : ((err.error?.message as string | undefined) ?? 'Failed to update room. Please try again.');
          alert(msg);
        },
      });
  }

  openDeleteRoomModal(): void {
    this.roomDeleteError.set(null);
    this.isDeleteRoomModalOpen.set(true);
  }

  closeDeleteRoomModal(): void {
    this.isDeleteRoomModalOpen.set(false);
    this.roomDeleteError.set(null);
  }

  confirmDeleteRoom(): void {
    const currentRoom = this.room();
    if (!currentRoom) return;

    this.isDeletingRoom.set(true);
    this.roomDeleteError.set(null);

    this.roomsData.deleteRoom(currentRoom.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isDeletingRoom.set(false);
          this.isDeleteRoomModalOpen.set(false);
          this.router.navigate(['/rooms']);
        },
        error: (err: HttpErrorResponse) => {
          this.isDeletingRoom.set(false);
          const msg =
            err.status === 403
              ? 'You do not have permission to perform this action'
              : ((err.error?.message as string | undefined) ?? 'Failed to delete room. Please try again.');
          this.roomDeleteError.set(msg);
        },
      });
  }

  // ── Heartbeat ──────────────────────────────────────────────────────────────

  private startHeartbeat(roomId: number): void {
    const INTERVAL_MS = 25_000;
    const MAX_FAILURES = 3;
    let consecutiveFailures = 0;

    timer(0, INTERVAL_MS)
      .pipe(
        switchMap(() =>
          this.roomsData.sendHeartbeat(roomId).pipe(
            retry({
              count: 2,
              delay: (error: HttpErrorResponse, retryCount: number): Observable<number> => {
                if ([401, 403, 404].includes(error.status)) throw error;
                return timer(retryCount * 2000);
              },
            }),
            catchError((err: HttpErrorResponse) =>
              of<HeartbeatResult>({ isError: true, error: err }),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((res: HeartbeatResult | object) => {
        const result = res as HeartbeatResult;
        if (result.isError) {
          const err = result.error;
          const isCritical = [401, 403, 404].includes(err.status);
          consecutiveFailures++;

          if (isCritical || consecutiveFailures >= MAX_FAILURES) {
            if (err.status === 404)
              this.error.set('Room has been closed or no longer exists.');
            else if (err.status === 401 || err.status === 403)
              this.error.set('Session expired or access to this room was revoked.');
            else
              this.error.set(this.extractHeartbeatError(err));
          }
        } else {
          consecutiveFailures = 0;
          // Signal-driven refresh: increment tick so RoomMembersListComponent re-fetches.
          this.heartbeatTick.update((v) => v + 1);
        }
      });
  }

  private extractHeartbeatError(err: HttpErrorResponse): string {
    if (err.status === 0 || err.error instanceof ErrorEvent)
      return 'Network connection lost. Retrying heartbeat connection...';

    const payloadMsg = err.error?.message as string | undefined;
    const payloadErrors = err.error?.errors as Record<string, string> | undefined;

    if (payloadErrors && typeof payloadErrors === 'object') {
      const formatted = Object.entries(payloadErrors)
        .map(([k, v]) => `${k}: ${v}`)
        .join(', ');
      if (formatted) return formatted;
    }

    switch (err.status) {
      case 401: return payloadMsg ?? 'Unauthorized room session.';
      case 403: return payloadMsg ?? 'Access denied to this room.';
      case 404: return payloadMsg ?? 'Room not found or session ended.';
      case 500: return payloadMsg ?? 'Internal server error while sending keep-alive heartbeat.';
      default:  return payloadMsg ?? `Heartbeat failed (Status ${err.status}).`;
    }
  }
}

/** @deprecated Use RoomDetailFacade. Kept as alias for incremental migration. */
export { RoomDetailFacade as RoomStateService };
