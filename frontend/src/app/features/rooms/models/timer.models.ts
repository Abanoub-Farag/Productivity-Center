// ── Timer constants ───────────────────────────────────────────────────────────

/** Default duration of the Pomodoro session in seconds (25 min). */
export const DEFAULT_POMODORO_DURATION = 25 * 60;

/** Full-ring cap for the stopwatch SVG ring (60 min). */
export const STOPWATCH_RING_CAP = 60 * 60;

// ── Timer enums ───────────────────────────────────────────────────────────────

/** Mirrors the backend `TimerStatus` enum. */
export type TimerStatus = 'RUNNING' | 'DONE';

/** Frontend-only timer mode selection. */
export type TimerMode = 'pomodoro' | 'stopwatch';

/** Frontend-only timer lifecycle states. */
export type TimerLifecycle = 'idle' | 'running' | 'completing' | 'done';

// ── DTOs ──────────────────────────────────────────────────────────────────────

/** Mirrors `TimerSessionResponseDto` from the backend. */
export interface TimerSessionDto {
  id: number;
  userId: number;
  roomId: number;
  /** ISO-8601 instant string. */
  startedAt: string;
  /** ISO-8601 instant string, null when session is still RUNNING. */
  endedAt: string | null;
  /** Duration in seconds, null when session is still RUNNING. */
  duration: number | null;
  status: TimerStatus;
}
