import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { timer, switchMap, retry, catchError, of, Observable } from 'rxjs';
import { Router } from '@angular/router';
import { RoomsDataService } from './rooms-data.service';
import { TaskService } from './task.service';
import { AuthService } from '../../../core/services/auth.service';
import { RoomData, UpdateRoomDto, TaskData, UpdateTaskRequest } from '../models/rooms.models';

/** Discriminated-union type for heartbeat stream results. */
type HeartbeatResult =
  | { isError: false }
  | { isError: true; error: HttpErrorResponse };

/** Scoped per room-detail route; provided in RoomDetailComponent's providers array. */
@Injectable()
export class RoomDetailFacade {
  private readonly roomsData = inject(RoomsDataService);
  private readonly taskService = inject(TaskService);
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

  // ── Edit / Delete modal state ──────────────────────────────────────────────
  readonly isEditRoomModalOpen = signal<boolean>(false);
  readonly editRoomTitle = signal<string>('');
  readonly editRoomDescription = signal<string>('');
  readonly editRoomVisibility = signal<'PUBLIC' | 'PRIVATE'>('PUBLIC');
  readonly isUpdatingRoom = signal<boolean>(false);
  readonly roomUpdateError = signal<string | null>(null);

  readonly isDeleteRoomModalOpen = signal<boolean>(false);
  readonly isDeletingRoom = signal<boolean>(false);
  readonly roomDeleteError = signal<string | null>(null);

  // ── Heartbeat state ────────────────────────────────────────────────────────
  readonly heartbeatStatus = signal<'active' | 'retrying' | 'failed'>('active');
  readonly heartbeatErrorMessage = signal<string | null>(null);

  // ── Task state ────────────────────────────────────────────────────────────
  readonly tasks = signal<TaskData[]>([]);
  readonly isTasksLoading = signal<boolean>(true);
  readonly tasksError = signal<string | null>(null);
  readonly newTaskText = signal<string>('');
  readonly editingTaskId = signal<number | null>(null);
  readonly editingTaskTitle = signal<string>('');
  readonly updatingTaskId = signal<number | null>(null);
  readonly taskUpdateError = signal<string | null>(null);

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  initialize(id: number): void {
    this.roomId.set(id);
    this.joinRoom(id);
    this.fetchRoom(id);
    this.startHeartbeat(id);
    this.checkIfFavorite(id);
    this.fetchTasks();
  }

  // ── Room actions ───────────────────────────────────────────────────────────

  checkIfFavorite(roomId: number): void {
    this.roomsData.getFavorites(0, 100)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const items = res.data?.content ?? [];
          this.isFavorite.set(items.some((item) => item.roomId === roomId));
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

    const request$ = targetState
      ? this.roomsData.addToFavorites(roomId)
      : this.roomsData.removeFromFavorites(roomId);

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

  openEditRoomModal(): void {
    const r = this.room();
    if (!r) return;
    this.editRoomTitle.set(r.title ?? '');
    this.editRoomDescription.set(r.description ?? '');
    this.editRoomVisibility.set(r.visibility ?? 'PUBLIC');
    this.roomUpdateError.set(null);
    this.isEditRoomModalOpen.set(true);
  }

  closeEditRoomModal(): void {
    this.isEditRoomModalOpen.set(false);
    this.roomUpdateError.set(null);
  }

  setEditRoomTitle(title: string): void { this.editRoomTitle.set(title); }
  setEditRoomDescription(desc: string): void { this.editRoomDescription.set(desc); }
  setEditRoomVisibility(v: 'PUBLIC' | 'PRIVATE'): void { this.editRoomVisibility.set(v); }

  submitUpdateRoom(): void {
    const currentRoom = this.room();
    if (!currentRoom) return;

    const title = this.editRoomTitle().trim();
    if (!title) { this.roomUpdateError.set('Room title is required.'); return; }

    const description = this.editRoomDescription().trim();
    const visibility = this.editRoomVisibility();
    const dto: UpdateRoomDto = { title, description, visibility };

    this.isUpdatingRoom.set(true);
    this.roomUpdateError.set(null);

    this.roomsData.updateRoom(currentRoom.id, dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.isUpdatingRoom.set(false);
          this.isEditRoomModalOpen.set(false);
          const updated = res.data ?? { ...currentRoom, title, description, visibility };
          this.room.update((r) => (r ? { ...r, ...updated } : null));
        },
        error: (err: HttpErrorResponse) => {
          this.isUpdatingRoom.set(false);
          const msg =
            err.status === 403
              ? 'You do not have permission to perform this action'
              : ((err.error?.message as string | undefined) ?? 'Failed to update room. Please try again.');
          this.roomUpdateError.set(msg);
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

  // ── Task actions ───────────────────────────────────────────────────────────

  fetchTasks(): void {
    this.isTasksLoading.set(true);
    this.tasksError.set(null);

    this.taskService.getTasks()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const list = (response.data?.content ?? []).map((t) => ({
            ...t,
            isCompleted: t.isCompleted ?? t.completed ?? false,
          }));
          this.tasks.set(list);
          this.isTasksLoading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          console.error(err);
          this.tasksError.set('Failed to load tasks.');
          this.isTasksLoading.set(false);
        },
      });
  }

  toggleTask(task: TaskData): void {
    const currentCompleted = task.isCompleted ?? task.completed ?? false;
    const updatedStatus = !currentCompleted;
    this.updatingTaskId.set(task.id);
    this.taskUpdateError.set(null);
    this.tasks.update((list) =>
      list.map((t) =>
        t.id === task.id ? { ...t, isCompleted: updatedStatus, completed: updatedStatus } : t
      ),
    );

    const payload: UpdateTaskRequest = {
      title: task.title,
      isCompleted: updatedStatus,
    };
    this.taskService.updateTask(task.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.updatingTaskId.set(null),
        error: (err: HttpErrorResponse) => {
          console.error('Failed to update task completion', err);
          this.taskUpdateError.set('Failed to update task status.');
          this.updatingTaskId.set(null);
          this.tasks.update((list) =>
            list.map((t) =>
              t.id === task.id ? { ...t, isCompleted: currentCompleted, completed: currentCompleted } : t
            ),
          );
        },
      });
  }

  startEditTask(task: TaskData): void {
    this.editingTaskId.set(task.id);
    this.editingTaskTitle.set(task.title);
    this.taskUpdateError.set(null);
  }

  cancelEditTask(): void {
    this.editingTaskId.set(null);
    this.editingTaskTitle.set('');
  }

  setEditingTaskTitle(title: string): void { this.editingTaskTitle.set(title); }

  saveTaskTitle(task: TaskData): void {
    const newTitle = this.editingTaskTitle().trim();
    if (!newTitle) return;
    if (newTitle === task.title) { this.cancelEditTask(); return; }

    const previousTitle = task.title;
    this.updatingTaskId.set(task.id);
    this.taskUpdateError.set(null);
    this.tasks.update((list) =>
      list.map((t) => (t.id === task.id ? { ...t, title: newTitle } : t)),
    );
    this.editingTaskId.set(null);

    const payload: UpdateTaskRequest = {
      title: newTitle,
      isCompleted: task.isCompleted ?? task.completed ?? false,
    };
    this.taskService.updateTask(task.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.updatingTaskId.set(null),
        error: (err: HttpErrorResponse) => {
          console.error('Failed to update task title', err);
          this.taskUpdateError.set('Failed to update task title.');
          this.updatingTaskId.set(null);
          this.tasks.update((list) =>
            list.map((t) => (t.id === task.id ? { ...t, title: previousTitle } : t)),
          );
        },
      });
  }

  addTask(): void {
    const text = this.newTaskText().trim();
    if (!text) return;
    this.newTaskText.set('');
    this.taskService.createTask({ title: text, isCompleted: false })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.fetchTasks(),
        error: (err: HttpErrorResponse) =>
          console.error('Failed to create task', err),
      });
  }

  setNewTaskText(text: string): void { this.newTaskText.set(text); }

  deleteTask(taskId: number): void {
    this.updatingTaskId.set(taskId);
    this.taskUpdateError.set(null);
    this.taskService.deleteTask(taskId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.tasks.update((list) => list.filter((t) => t.id !== taskId));
          this.updatingTaskId.set(null);
        },
        error: (err: HttpErrorResponse) => {
          console.error('Failed to delete task', err);
          this.taskUpdateError.set('Failed to delete task.');
          this.updatingTaskId.set(null);
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
            this.heartbeatStatus.set('failed');
            this.heartbeatErrorMessage.set(this.extractHeartbeatError(err));
            if (err.status === 404)
              this.error.set('Room has been closed or no longer exists.');
            else if (err.status === 401 || err.status === 403)
              this.error.set('Session expired or access to this room was revoked.');
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
