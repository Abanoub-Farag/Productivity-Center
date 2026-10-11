import { Injectable, inject, signal, computed, DestroyRef, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { TimerDataService } from './timer-data.service';
import {
  TimerMode,
  TimerLifecycle,
  DEFAULT_POMODORO_DURATION,
  STOPWATCH_RING_CAP,
} from '../models/timer.models';

/**
 * Scoped per room-detail route; provided in RoomDetailComponent's providers array.
 *
 * Owns the complete timer lifecycle: session start/stop, local drift-corrected tick,
 * localStorage-based session recovery after page reload, and leave-room cleanup.
 * All HTTP I/O is delegated to TimerDataService.
 */
@Injectable()
export class TimerFacade {
  private readonly timerDataService = inject(TimerDataService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  // ── Room context ───────────────────────────────────────────────────────────
  private readonly _roomId = signal<number | null>(null);

  // ── Timer state ────────────────────────────────────────────────────────────

  readonly timerMode = signal<TimerMode>(
    this.isBrowser ? (localStorage.getItem('preferred_timer_mode') as TimerMode | null) ?? 'pomodoro' : 'pomodoro',
  );
  readonly timerStatus = signal<TimerLifecycle>('idle');
  readonly timerSessionId = signal<number | null>(null);
  /** ISO-8601 instant from the server — used for drift-corrected elapsed calculation. */
  private readonly timerStartedAt = signal<string | null>(null);
  readonly timerElapsed = signal<number>(0); // seconds
  readonly timerError = signal<string | null>(null);
  readonly isTimerLoading = signal<boolean>(false);
  readonly isStopConfirmOpen = signal<boolean>(false);
  readonly lastSessionDuration = signal<number | null>(null); // seconds
  readonly pomodoroDuration = signal<number>(
    this.isBrowser
      ? Number(localStorage.getItem('preferred_pomodoro_duration')) || DEFAULT_POMODORO_DURATION
      : DEFAULT_POMODORO_DURATION,
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

  initialize(roomId: number): void {
    this._roomId.set(roomId);
    this.recoverTimerSession();
    this.registerLeaveRoomCleanup();
  }

  // ── Timer actions ──────────────────────────────────────────────────────────

  /** Toggle between Pomodoro and Stopwatch. Locked while a session is running. */
  setTimerMode(mode: TimerMode): void {
    if (this.timerStatus() === 'running') return;
    this.timerMode.set(mode);
    if (this.isBrowser) {
      localStorage.setItem('preferred_timer_mode', mode);
    }
    this.timerError.set(null);
  }

  /** Set the Pomodoro duration in seconds. Locked while a session is running. */
  setPomodoroDuration(durationSeconds: number): void {
    if (this.timerStatus() === 'running') return;
    this.pomodoroDuration.set(durationSeconds);
    if (this.isBrowser) {
      localStorage.setItem('preferred_pomodoro_duration', String(durationSeconds));
    }
  }

  /** Start a new timer session on the backend, then begin local tick. */
  startTimer(): void {
    const roomId = this._roomId();
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

  // ── Stop: manual (shows confirmation dialog) ───────────────────────────────

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
    return `timer_session_id_${this._roomId()}`;
  }

  private saveSessionId(id: number): void {
    if (this.isBrowser) {
      localStorage.setItem(this.storageKey(), String(id));
    }
  }

  private loadSessionId(): number | null {
    if (!this.isBrowser) return null;
    const raw = localStorage.getItem(this.storageKey());
    if (!raw) return null;
    const n = Number(raw);
    return Number.isInteger(n) && n > 0 ? n : null;
  }

  private clearSessionId(): void {
    if (this.isBrowser) {
      localStorage.removeItem(this.storageKey());
    }
  }

  // ── Dismiss timer error ────────────────────────────────────────────────────

  dismissTimerError(): void { this.timerError.set(null); }
}
