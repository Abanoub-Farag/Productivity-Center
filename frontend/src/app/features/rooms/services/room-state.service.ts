import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { timer, switchMap, retry, catchError, of, Observable } from 'rxjs';
import { Router } from '@angular/router';
import { RoomsDataService } from './rooms-data.service';
import { FavoriteRoomService } from './favorite-room.service';
import { TimerDataService } from './timer-data.service';
import { TaskService } from '../../../features/tasks/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { RoomData, UpdateRoomDto } from '../models/rooms.models';
import { TaskData, UpdateTaskRequest } from '../../../features/tasks/models/task.models';
import { TimerMode, TimerLifecycle } from '../models/timer.models';

/** Discriminated-union type for heartbeat stream results. */
type HeartbeatResult =
  | { isError: false }
  | { isError: true; error: HttpErrorResponse };

/** Default duration of the Pomodoro session in seconds (25 min). */
const DEFAULT_POMODORO_DURATION = 25 * 60;

/** Full-ring cap for the stopwatch SVG ring (60 min). */
const STOPWATCH_RING_CAP = 60 * 60;

/** Scoped per room-detail route; provided in RoomDetailComponent's providers array. */
@Injectable()
export class RoomDetailFacade {
  private readonly roomsData = inject(RoomsDataService);
  private readonly favService = inject(FavoriteRoomService);
  private readonly timerDataService = inject(TimerDataService);
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

  /** Wired by the host component to refresh the members list on each heartbeat success. */
  onHeartbeatSuccess: (() => void) | null = null;

  // ── Task state ────────────────────────────────────────────────────────────
  readonly tasks = signal<TaskData[]>([]);
  readonly isTasksLoading = signal<boolean>(true);
  readonly tasksError = signal<string | null>(null);
  readonly newTaskText = signal<string>('');
  readonly editingTaskId = signal<number | null>(null);
  readonly editingTaskTitle = signal<string>('');
  readonly updatingTaskId = signal<number | null>(null);
  readonly taskUpdateError = signal<string | null>(null);

  // ── Timer state ────────────────────────────────────────────────────────────

  readonly timerMode          = signal<TimerMode>(
    (localStorage.getItem('preferred_timer_mode') as TimerMode) || 'pomodoro'
  );
  readonly timerStatus        = signal<TimerLifecycle>('idle');
  readonly timerSessionId     = signal<number | null>(null);
  /** ISO-8601 instant from the server — used for drift-corrected elapsed calculation. */
  private readonly timerStartedAt = signal<string | null>(null);
  readonly timerElapsed       = signal<number>(0);    // seconds
  readonly timerError         = signal<string | null>(null);
  readonly isTimerLoading     = signal<boolean>(false);
  readonly isStopConfirmOpen  = signal<boolean>(false);
  readonly lastSessionDuration = signal<number | null>(null); // seconds
  readonly pomodoroDuration   = signal<number>(
    Number(localStorage.getItem('preferred_pomodoro_duration')) || DEFAULT_POMODORO_DURATION
  );

  /** Seconds left (Pomodoro) or elapsed (Stopwatch) — drives the display and ring. */
  readonly timerDisplaySeconds = computed(() => {
    const elapsed = this.timerElapsed();
    return this.timerMode() === 'pomodoro'
      ? Math.max(0, this.pomodoroDuration() - elapsed)
      : Math.min(elapsed, STOPWATCH_RING_CAP);
  });

  /** 0–1 ring fill fraction — 0 = empty, 1 = full. */
  readonly timerRingFraction = computed(() => {
    const elapsed = this.timerElapsed();
    return this.timerMode() === 'pomodoro'
      ? Math.max(0, (this.pomodoroDuration() - elapsed) / this.pomodoroDuration())
      : Math.min(elapsed, STOPWATCH_RING_CAP) / STOPWATCH_RING_CAP;
  });

  private tickIntervalId: ReturnType<typeof setInterval> | null = null;

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  initialize(id: number): void {
    this.roomId.set(id);
    this.joinRoom(id);
    this.fetchRoom(id);
    this.startHeartbeat(id);
    this.checkIfFavorite(id);
    this.fetchTasks();
    this.recoverTimerSession();
    this.registerLeaveRoomCleanup();
  }

  // ── Room actions ───────────────────────────────────────────────────────────

  checkIfFavorite(roomId: number): void {
    this.favService.getFavorites(0, 100)
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
          // TaskService already normalises `completed` → `isCompleted`.
          this.tasks.set(response.data?.content ?? []);
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
    const updatedStatus = !task.isCompleted;
    this.updatingTaskId.set(task.id);
    this.taskUpdateError.set(null);
    this.tasks.update((list) =>
      list.map((t) =>
        t.id === task.id ? { ...t, isCompleted: updatedStatus } : t
      ),
    );

    const payload: UpdateTaskRequest = { title: task.title, isCompleted: updatedStatus };
    this.taskService.updateTask(task.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.updatingTaskId.set(null),
        error: (err: HttpErrorResponse) => {
          console.error('Failed to update task completion', err);
          this.taskUpdateError.set('Failed to update task status.');
          this.updatingTaskId.set(null);
          // Roll back optimistic update.
          this.tasks.update((list) =>
            list.map((t) =>
              t.id === task.id ? { ...t, isCompleted: task.isCompleted } : t
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
      isCompleted: task.isCompleted,
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

  // ── Timer actions ──────────────────────────────────────────────────────────

  /** Toggle between Pomodoro and Stopwatch. Locked while a session is running. */
  setTimerMode(mode: TimerMode): void {
    if (this.timerStatus() === 'running') return;
    this.timerMode.set(mode);
    localStorage.setItem('preferred_timer_mode', mode);
    this.timerError.set(null);
  }

  /** Set the Pomodoro duration in seconds */
  setPomodoroDuration(durationSeconds: number): void {
    if (this.timerStatus() === 'running') return;
    this.pomodoroDuration.set(durationSeconds);
    localStorage.setItem('preferred_pomodoro_duration', String(durationSeconds));
  }

  /** Start a new timer session on the backend, then begin local tick. */
  startTimer(): void {
    const roomId = this.roomId();
    if (!roomId || this.timerStatus() === 'running' || this.timerStatus() === 'completing') return;

    this.isTimerLoading.set(true);
    this.timerError.set(null);
    this.lastSessionDuration.set(null);

    this.timerDataService.startTimer(roomId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const session = res.data!;
          this.timerSessionId.set(session.id);
          this.timerStartedAt.set(session.startedAt);
          this.timerElapsed.set(0);
          this.timerStatus.set('running');
          this.timerError.set(null);
          this.saveSessionId(session.id);
          this.isTimerLoading.set(false);
          this.startLocalTick();
        },
        error: (err: HttpErrorResponse) => {
          this.isTimerLoading.set(false);
          const msg = (err.error?.message as string | undefined)
            ?? 'Failed to start timer. Please try again.';
          this.timerError.set(msg);
        },
      });
  }

  // ── Stop: manual (shows confirmation dialog) ────────────────────────────────

  requestStopTimer(): void  { this.isStopConfirmOpen.set(true); }
  cancelStopTimer(): void   { this.isStopConfirmOpen.set(false); }

  confirmStopTimer(): void {
    this.isStopConfirmOpen.set(false);
    this._doCompleteTimer(true);
  }

  // ── Stop: automatic (Pomodoro 0:00 — no dialog, show summary) ─────────────

  private autoCompleteTimer(): void {
    // Guard: only proceed if still running (interval can tick again before PATCH returns)
    if (this.timerStatus() !== 'running') return;
    this._doCompleteTimer(true);
  }

  // ── Stop: leave-room (component destroy — fire-and-forget, no summary) ─────

  private completeTimerOnLeave(): void {
    const sessionId = this.timerSessionId();
    if (!sessionId) return;
    this.stopLocalTick();
    this.clearSessionId();
    // Fire-and-forget — the facade is being destroyed; we just need the HTTP request to fly
    this.timerDataService.completeTimer(sessionId).subscribe({ error: () => {} });
  }

  // ── Shared complete implementation ─────────────────────────────────────────

  private _doCompleteTimer(showSummary: boolean): void {
    const sessionId = this.timerSessionId();
    if (!sessionId) return;

    this.timerStatus.set('completing');
    this.stopLocalTick();

    this.timerDataService.completeTimer(sessionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const session = res.data!;
          this.clearSessionId();
          this.timerSessionId.set(null);
          this.timerStartedAt.set(null);
          this.timerElapsed.set(0);
          this.timerError.set(null);

          if (showSummary) {
            this.lastSessionDuration.set(session.duration);
            this.timerStatus.set('done');
            // Auto-reset to idle after 4 seconds
            setTimeout(() => {
              if (this.timerStatus() === 'done') this.timerStatus.set('idle');
            }, 4000);
          } else {
            this.timerStatus.set('idle');
          }
        },
        error: (err: HttpErrorResponse) => {
          // Roll back to running so the user can retry
          this.timerStatus.set('running');
          this.startLocalTick();
          const msg = (err.error?.message as string | undefined)
            ?? 'Failed to complete timer. Please try again.';
          this.timerError.set(msg);
        },
      });
  }

  // ── Session recovery (after page reload) ───────────────────────────────────

  private recoverTimerSession(): void {
    const sessionId = this.loadSessionId();
    if (!sessionId) return;

    this.isTimerLoading.set(true);

    this.timerDataService.getSession(sessionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const session = res.data;
          if (session?.status === 'RUNNING') {
            const elapsed = Math.floor(
              (Date.now() - new Date(session.startedAt).getTime()) / 1000,
            );
            this.timerSessionId.set(session.id);
            this.timerStartedAt.set(session.startedAt);
            this.timerElapsed.set(elapsed);
            this.timerStatus.set('running');
            this.startLocalTick();
          } else {
            // Session is already DONE or not owned by this user — clear stale entry
            this.clearSessionId();
          }
          this.isTimerLoading.set(false);
        },
        error: () => {
          // Session not found or unauthorized — clear stale entry
          this.clearSessionId();
          this.isTimerLoading.set(false);
        },
      });
  }

  // ── Leave-room cleanup (registered once in initialize) ─────────────────────

  private registerLeaveRoomCleanup(): void {
    this.destroyRef.onDestroy(() => {
      if (this.timerStatus() === 'running') {
        this.completeTimerOnLeave();
      }
    });
  }

  // ── Local tick (drift-corrected) ───────────────────────────────────────────

  private startLocalTick(): void {
    this.stopLocalTick();
    this.tickIntervalId = setInterval(() => {
      const startedAt = this.timerStartedAt();
      if (!startedAt) return;

      // Re-derive elapsed from server timestamp to avoid drift
      const elapsed = Math.floor((Date.now() - new Date(startedAt).getTime()) / 1000);
      this.timerElapsed.set(elapsed);

      // Pomodoro auto-complete — no confirmation dialog
      if (this.timerMode() === 'pomodoro' && elapsed >= this.pomodoroDuration()) {
        this.autoCompleteTimer();
      }
    }, 1000);
  }

  private stopLocalTick(): void {
    if (!this.tickIntervalId) return;
    clearInterval(this.tickIntervalId);
    this.tickIntervalId = null;
  }

  // ── localStorage helpers (room-scoped) ─────────────────────────────────────

  private storageKey(): string {
    return `timer_session_id_${this.roomId()}`;
  }

  private saveSessionId(id: number): void {
    localStorage.setItem(this.storageKey(), String(id));
  }

  private loadSessionId(): number | null {
    const raw = localStorage.getItem(this.storageKey());
    if (!raw) return null;
    const n = Number(raw);
    return Number.isInteger(n) && n > 0 ? n : null;
  }

  private clearSessionId(): void {
    localStorage.removeItem(this.storageKey());
  }

  // ── Dismiss timer error ────────────────────────────────────────────────────

  dismissTimerError(): void { this.timerError.set(null); }

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
          this.onHeartbeatSuccess?.();
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
