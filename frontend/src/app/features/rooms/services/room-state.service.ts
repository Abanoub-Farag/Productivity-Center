import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { timer, switchMap, retry, catchError, of } from 'rxjs';
import { Router } from '@angular/router';
import { RoomService, RoomData, UpdateRoomDto } from './room.service';
import { AuthService } from '../../../core/services/auth.service';

@Injectable()
export class RoomStateService {
  private readonly roomService = inject(RoomService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // ── Room data ─────────────────────────────────────────────────────────────
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

  // ── Room Edit & Delete States ──────────────────────────────────────────────
  readonly isEditRoomModalOpen = signal<boolean>(false);
  readonly editRoomTitle = signal<string>('');
  readonly editRoomDescription = signal<string>('');
  readonly editRoomVisibility = signal<'PUBLIC' | 'PRIVATE'>('PUBLIC');
  readonly isUpdatingRoom = signal<boolean>(false);
  readonly roomUpdateError = signal<string | null>(null);

  readonly isDeleteRoomModalOpen = signal<boolean>(false);
  readonly isDeletingRoom = signal<boolean>(false);
  readonly roomDeleteError = signal<string | null>(null);

  // ── Heartbeat State ───────────────────────────────────────────────────────
  readonly heartbeatStatus = signal<'active' | 'retrying' | 'failed'>('active');
  readonly heartbeatErrorMessage = signal<string | null>(null);

  initialize(id: number) {
    this.roomId.set(id);
    this.joinRoom(id);
    this.fetchRoom(id);
    this.startHeartbeat(id);
    this.checkIfFavorite(id);
  }

  checkIfFavorite(roomId: number) {
    this.roomService.getFavorites(0, 100).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        const items = res.data?.content || [];
        const isFav = items.some(item => item.roomId === roomId);
        this.isFavorite.set(isFav);
      },
      error: (err) => console.error('Failed checking favorite state:', err)
    });
  }

  toggleFavorite() {
    const currentRoom = this.room();
    if (!currentRoom || this.isPendingFavorite()) return;

    const roomId = currentRoom.id;
    const isCurrentlyFav = this.isFavorite();
    const targetState = !isCurrentlyFav;

    this.isFavorite.set(targetState);
    this.isPendingFavorite.set(true);

    const request$ = targetState 
      ? this.roomService.addToFavorites(roomId) 
      : this.roomService.removeFromFavorites(roomId);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.isPendingFavorite.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isFavorite.set(isCurrentlyFav);
        this.isPendingFavorite.set(false);
        const msg = err.error?.message || 'Failed to update favorite status.';
        alert(msg);
      }
    });
  }

  joinRoom(id: number) {
    this.roomService.joinRoom(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        console.log(`Successfully joined room ${id}:`, res);
      },
      error: (err) => {
        console.error(`Error joining room ${id}:`, err);
      }
    });
  }

  private startHeartbeat(roomId: number) {
    const INTERVAL_MS = 25000; // 25s keep-alive interval
    let consecutiveFailures = 0;
    const MAX_CONSECUTIVE_FAILURES = 3;

    timer(0, INTERVAL_MS)
      .pipe(
        switchMap(() =>
          this.roomService.sendHeartbeat(roomId).pipe(
            retry({
              count: 2,
              delay: (error: HttpErrorResponse, retryCount: number) => {
                // Critical security/membership errors should fail immediately without retry
                if ([401, 403, 404].includes(error.status)) {
                  throw error;
                }
                return timer(retryCount * 2000);
              }
            }),
            catchError((err: HttpErrorResponse) => {
              return of({ isError: true, error: err });
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((res: any) => {
        if (res?.isError) {
          const err: HttpErrorResponse = res.error;
          const isCritical = [401, 403, 404].includes(err.status);
          consecutiveFailures++;

          if (isCritical || consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            this.heartbeatStatus.set('failed');
            const parsedMsg = this.extractHeartbeatError(err);
            this.heartbeatErrorMessage.set(parsedMsg);
            if (err.status === 404) {
              this.error.set('Room has been closed or no longer exists.');
            } else if (err.status === 401 || err.status === 403) {
              this.error.set('Session expired or access to this room was revoked.');
            }
          } else {
            this.heartbeatStatus.set('retrying');
          }
        } else {
          consecutiveFailures = 0;
          this.heartbeatStatus.set('active');
          this.heartbeatErrorMessage.set(null);
        }
      });
  }

  private extractHeartbeatError(err: HttpErrorResponse | any): string {
    if (err.status === 0 || err.error instanceof ErrorEvent) {
      return 'Network connection lost. Retrying heartbeat connection...';
    }

    const payloadMsg = err.error?.message;
    const payloadErrors = err.error?.errors;

    if (payloadErrors && typeof payloadErrors === 'object') {
      const formatted = Object.entries(payloadErrors)
        .map(([k, v]) => `${k}: ${v}`)
        .join(', ');
      if (formatted) return formatted;
    }

    switch (err.status) {
      case 401: return payloadMsg || 'Unauthorized room session.';
      case 403: return payloadMsg || 'Access denied to this room.';
      case 404: return payloadMsg || 'Room not found or session ended.';
      case 500: return payloadMsg || 'Internal server error while sending keep-alive heartbeat.';
      default: return payloadMsg || `Heartbeat failed (Status ${err.status}).`;
    }
  }

  fetchRoom(id: number) {
    this.roomService.getRoomById(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
      }
    });
  }

  openEditRoomModal() {
    const currentRoom = this.room();
    if (!currentRoom) return;
    this.editRoomTitle.set(currentRoom.title || '');
    this.editRoomDescription.set(currentRoom.description || '');
    this.editRoomVisibility.set(currentRoom.visibility || 'PUBLIC');
    this.roomUpdateError.set(null);
    this.isEditRoomModalOpen.set(true);
  }

  closeEditRoomModal() {
    this.isEditRoomModalOpen.set(false);
    this.roomUpdateError.set(null);
  }

  setEditRoomTitle(title: string) {
    this.editRoomTitle.set(title);
  }

  setEditRoomDescription(description: string) {
    this.editRoomDescription.set(description);
  }

  submitUpdateRoom() {
    const currentRoom = this.room();
    if (!currentRoom) return;
    const title = this.editRoomTitle().trim();
    const description = this.editRoomDescription().trim();
    const visibility = this.editRoomVisibility();

    if (!title) {
      this.roomUpdateError.set('Room title is required.');
      return;
    }

    this.isUpdatingRoom.set(true);
    this.roomUpdateError.set(null);

    const dto: UpdateRoomDto = { title, description, visibility };

    this.roomService.updateRoom(currentRoom.id, dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.isUpdatingRoom.set(false);
          this.isEditRoomModalOpen.set(false);
          const updatedData = res.data || { ...currentRoom, title, description, visibility };
          this.room.update(r => r ? { ...r, ...updatedData } : null);
        },
        error: (err: HttpErrorResponse) => {
          console.error('Failed to update room:', err);
          this.isUpdatingRoom.set(false);
          const msg = err.status === 403 
            ? 'You do not have permission to perform this action' 
            : (err.error?.message || 'Failed to update room. Please try again.');
          this.roomUpdateError.set(msg);
        }
      });
  }

  openDeleteRoomModal() {
    this.roomDeleteError.set(null);
    this.isDeleteRoomModalOpen.set(true);
  }

  closeDeleteRoomModal() {
    this.isDeleteRoomModalOpen.set(false);
    this.roomDeleteError.set(null);
  }

  confirmDeleteRoom() {
    const currentRoom = this.room();
    if (!currentRoom) return;

    this.isDeletingRoom.set(true);
    this.roomDeleteError.set(null);

    this.roomService.deleteRoom(currentRoom.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isDeletingRoom.set(false);
          this.isDeleteRoomModalOpen.set(false);
          this.router.navigate(['/rooms']);
        },
        error: (err: HttpErrorResponse) => {
          console.error('Failed to delete room:', err);
          this.isDeletingRoom.set(false);
          const msg = err.status === 403 
            ? 'You do not have permission to perform this action' 
            : (err.error?.message || 'Failed to delete room. Please try again.');
          this.roomDeleteError.set(msg);
        }
      });
  }
}
