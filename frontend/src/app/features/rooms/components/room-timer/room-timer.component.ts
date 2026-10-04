import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Clock, Play, Square, AlertTriangle, Check, Timer } from 'lucide-angular';
import { TimerMode, TimerLifecycle } from '../../models/timer.models';

const POMODORO_DURATION = 25 * 60;
const STOPWATCH_RING_CAP = 60 * 60;
const RING_RADIUS = 90;
const CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS; // ≈ 565.49

@Component({
  selector: 'app-room-timer',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="timer-panel" aria-label="Focus Timer">

      <!-- ── Mode toggle ──────────────────────────────────────────────────── -->
      <div class="mode-toggle" role="group" aria-label="Timer mode">
        <button
          id="timer-mode-pomodoro"
          class="mode-btn"
          [class.mode-btn--active]="mode === 'pomodoro'"
          [disabled]="isRunning"
          (click)="modeChange.emit('pomodoro')"
          aria-label="Pomodoro mode (25 minutes)"
        >
          <lucide-icon [img]="ClockIcon" class="mode-icon"></lucide-icon>
          Pomodoro
        </button>
        <button
          id="timer-mode-stopwatch"
          class="mode-btn"
          [class.mode-btn--active]="mode === 'stopwatch'"
          [disabled]="isRunning"
          (click)="modeChange.emit('stopwatch')"
          aria-label="Stopwatch mode"
        >
          <lucide-icon [img]="TimerIcon" class="mode-icon"></lucide-icon>
          Stopwatch
        </button>
      </div>

      <!-- ── SVG ring + display ───────────────────────────────────────────── -->
      <div
        class="ring-wrapper"
        aria-live="polite"
        [attr.aria-label]="ariaLabel"
      >
        <!-- Loading overlay -->
        @if (isLoading) {
          <div class="ring-loading" aria-label="Loading timer state">
            <div class="loading-spinner"></div>
          </div>
        }

        <svg class="progress-ring" viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <filter id="ring-glow" x="-50%" y="-50%" width="200%" height="200%">
              <feGaussianBlur in="SourceGraphic" stdDeviation="4" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
            <linearGradient id="ring-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#7C3AED" />
              <stop offset="100%" style="stop-color:#8B5CF6" />
            </linearGradient>
          </defs>
          <circle class="ring-track" cx="100" cy="100" r="90" fill="none" stroke-width="6" />
          <circle
            class="ring-progress"
            [class.ring-progress--completing]="timerStatus === 'completing'"
            cx="100" cy="100" r="90"
            fill="none"
            stroke="url(#ring-gradient)"
            stroke-width="6"
            stroke-linecap="round"
            filter="url(#ring-glow)"
            [style.strokeDasharray]="circumference"
            [style.strokeDashoffset]="ringOffset"
            transform="rotate(-90 100 100)"
          />
        </svg>

        <!-- Inner content -->
        <div class="ring-content">
          @switch (timerStatus) {
            <!-- Done state: show completion summary -->
            @case ('done') {
              <lucide-icon [img]="CheckIcon" class="done-icon" aria-hidden="true"></lucide-icon>
              <span class="done-label">Session complete!</span>
              @if (lastSessionDuration !== null) {
                <span class="done-duration">{{ formatDuration(lastSessionDuration) }}</span>
              }
            }
            <!-- Completing: spinner + label -->
            @case ('completing') {
              <span class="timer-display">{{ minutesDisplay }}:{{ secondsDisplay }}</span>
              <span class="timer-label">SAVING...</span>
            }
            <!-- Running or idle: clock display -->
            @default {
              <span class="timer-display">{{ minutesDisplay }}:{{ secondsDisplay }}</span>
              <span class="timer-label">
                {{ timerStatus === 'running'
                    ? (mode === 'pomodoro' ? 'REMAINING' : 'ELAPSED')
                    : (mode === 'pomodoro' ? '25 MIN' : 'STOPWATCH') }}
              </span>
            }
          }
        </div>
      </div>

      <!-- ── Error banner ─────────────────────────────────────────────────── -->
      @if (timerError) {
        <div class="timer-error" role="alert">
          <lucide-icon [img]="AlertIcon" class="error-icon" aria-hidden="true"></lucide-icon>
          <span>{{ timerError }}</span>
          <button
            class="error-dismiss"
            aria-label="Dismiss error"
            (click)="dismissError.emit()"
          >✕</button>
        </div>
      }

      <!-- ── Stop confirmation dialog ─────────────────────────────────────── -->
      @if (isStopConfirmOpen) {
        <div class="confirm-dialog" role="alertdialog" aria-labelledby="confirm-title">
          <p id="confirm-title" class="confirm-title">Stop the session?</p>
          <p class="confirm-sub">Your focus time will be recorded.</p>
          <div class="confirm-actions">
            <button
              id="timer-cancel-stop-btn"
              class="ctrl-btn ctrl-btn--secondary ctrl-btn--sm"
              (click)="cancelStop.emit()"
            >
              Keep going
            </button>
            <button
              id="timer-confirm-stop-btn"
              class="ctrl-btn ctrl-btn--danger ctrl-btn--sm"
              (click)="confirmStop.emit()"
            >
              Stop & save
            </button>
          </div>
        </div>
      }

      <!-- ── Controls ─────────────────────────────────────────────────────── -->
      <div class="timer-controls">
        @if (timerStatus === 'idle' || timerStatus === 'done') {
          <!-- Start button -->
          <button
            id="timer-start-btn"
            class="ctrl-btn ctrl-btn--primary"
            aria-label="Start focus session"
            [disabled]="isLoading"
            (click)="startTimer.emit()"
          >
            <lucide-icon [img]="PlayIcon" class="ctrl-icon ctrl-icon--lg"></lucide-icon>
          </button>
        } @else if (timerStatus === 'running') {
          <!-- Stop button -->
          <button
            id="timer-stop-btn"
            class="ctrl-btn ctrl-btn--stop"
            aria-label="Stop focus session"
            (click)="requestStop.emit()"
          >
            <lucide-icon [img]="SquareIcon" class="ctrl-icon ctrl-icon--lg"></lucide-icon>
          </button>
        } @else {
          <!-- Completing: disabled spinner placeholder -->
          <button
            class="ctrl-btn ctrl-btn--primary"
            aria-label="Saving session..."
            disabled
          >
            <div class="btn-spinner"></div>
          </button>
        }
      </div>

    </section>
  `,
  styleUrls: ['./room-timer.component.scss'],
})
export class RoomTimerComponent {
  // ── Icons ──────────────────────────────────────────────────────────────────
  readonly ClockIcon     = Clock;
  readonly TimerIcon     = Timer;
  readonly PlayIcon      = Play;
  readonly SquareIcon    = Square;
  readonly AlertIcon     = AlertTriangle;
  readonly CheckIcon     = Check;

  readonly circumference = CIRCUMFERENCE;

  // ── Inputs ─────────────────────────────────────────────────────────────────
  @Input() mode: TimerMode                = 'pomodoro';
  @Input() timerStatus: TimerLifecycle    = 'idle';
  @Input() displaySeconds: number         = POMODORO_DURATION;
  @Input() ringFraction: number           = 1;  // 0–1
  @Input() isLoading: boolean             = false;
  @Input() timerError: string | null      = null;
  @Input() lastSessionDuration: number | null = null;
  @Input() isStopConfirmOpen: boolean     = false;

  // ── Outputs ────────────────────────────────────────────────────────────────
  @Output() startTimer   = new EventEmitter<void>();
  @Output() requestStop  = new EventEmitter<void>();
  @Output() confirmStop  = new EventEmitter<void>();
  @Output() cancelStop   = new EventEmitter<void>();
  @Output() modeChange   = new EventEmitter<TimerMode>();
  @Output() dismissError = new EventEmitter<void>();

  // ── Derived display values (pure getters, no local state) ──────────────────

  get isRunning(): boolean {
    return this.timerStatus === 'running' || this.timerStatus === 'completing';
  }

  get minutesDisplay(): string {
    return Math.floor(this.displaySeconds / 60).toString().padStart(2, '0');
  }

  get secondsDisplay(): string {
    return (this.displaySeconds % 60).toString().padStart(2, '0');
  }

  /** Stroke-dashoffset: full offset = empty ring, 0 = full ring. */
  get ringOffset(): number {
    return CIRCUMFERENCE * (1 - Math.max(0, Math.min(1, this.ringFraction)));
  }

  get ariaLabel(): string {
    if (this.timerStatus === 'done') return 'Session complete';
    const m = this.minutesDisplay;
    const s = this.secondsDisplay;
    return this.mode === 'pomodoro'
      ? `${m}:${s} remaining`
      : `${m}:${s} elapsed`;
  }

  /** Format seconds → "Xh Ym Zs" or "Xm Zs" or "Zs". */
  formatDuration(seconds: number): string {
    if (seconds <= 0) return '0s';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    const parts: string[] = [];
    if (h > 0) parts.push(`${h}h`);
    if (m > 0) parts.push(`${m}m`);
    if (s > 0 || parts.length === 0) parts.push(`${s}s`);
    return parts.join(' ');
  }
}
