import { Component, ChangeDetectionStrategy, signal, computed, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Clock, RotateCcw, Play, Pause, Settings } from 'lucide-angular';

const TIMER_DURATION = 25 * 60; // 25 minutes in seconds
const RING_RADIUS = 90;
const CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS; // ≈ 565.49

@Component({
  selector: 'app-room-timer',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="timer-panel" aria-label="Pomodoro Timer">
      <div class="timer-badge">
        <lucide-icon [img]="ClockIcon" class="timer-badge-icon"></lucide-icon>
        <span>POMODORO FOCUS</span>
      </div>

      <div class="ring-wrapper" aria-live="polite" [attr.aria-label]="minutesDisplay() + ':' + secondsDisplay() + ' remaining'">
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
            cx="100" cy="100" r="90"
            fill="none"
            stroke="url(#ring-gradient)"
            stroke-width="6"
            stroke-linecap="round"
            filter="url(#ring-glow)"
            [style.strokeDasharray]="circumference"
            [style.strokeDashoffset]="ringProgress()"
            transform="rotate(-90 100 100)"
          />
        </svg>

        <div class="ring-content">
          <span class="timer-display">{{ minutesDisplay() }}:{{ secondsDisplay() }}</span>
          <span class="timer-label">REMAINING</span>
        </div>
      </div>

      <div class="timer-controls">
        <button class="ctrl-btn ctrl-btn--secondary" aria-label="Reset timer" (click)="resetTimer()">
          <lucide-icon [img]="RotateCcwIcon" class="ctrl-icon"></lucide-icon>
        </button>

        <button
          class="ctrl-btn ctrl-btn--primary"
          [attr.aria-label]="isRunning() ? 'Pause timer' : 'Start timer'"
          (click)="toggleTimer()"
        >
          <lucide-icon [img]="isRunning() ? PauseIcon : PlayIcon" class="ctrl-icon ctrl-icon--lg"></lucide-icon>
        </button>

        <button class="ctrl-btn ctrl-btn--secondary" aria-label="Timer settings">
          <lucide-icon [img]="SettingsIcon" class="ctrl-icon"></lucide-icon>
        </button>
      </div>
    </section>
  `,
  styleUrls: ['./room-timer.component.scss']
})
export class RoomTimerComponent implements OnDestroy {
  readonly ClockIcon = Clock;
  readonly RotateCcwIcon = RotateCcw;
  readonly PlayIcon = Play;
  readonly PauseIcon = Pause;
  readonly SettingsIcon = Settings;

  timeLeft = signal<number>(TIMER_DURATION);
  isRunning = signal<boolean>(false);
  private intervalId: ReturnType<typeof setInterval> | null = null;
  readonly circumference = CIRCUMFERENCE;

  readonly minutesDisplay = computed(() => Math.floor(this.timeLeft() / 60).toString().padStart(2, '0'));
  readonly secondsDisplay = computed(() => (this.timeLeft() % 60).toString().padStart(2, '0'));
  readonly ringProgress = computed(() => CIRCUMFERENCE * (1 - (this.timeLeft() / TIMER_DURATION)));

  ngOnDestroy() {
    this.clearInterval();
  }

  toggleTimer() {
    if (this.isRunning()) {
      this.pauseTimer();
      return;
    }
    this.startTimer();
  }

  resetTimer() {
    this.pauseTimer();
    this.timeLeft.set(TIMER_DURATION);
  }

  private startTimer() {
    if (this.timeLeft() <= 0) return;

    this.isRunning.set(true);
    this.intervalId = setInterval(() => this.tick(), 1000);
  }

  private tick() {
    const current = this.timeLeft();
    if (current <= 1) {
      this.timeLeft.set(0);
      this.pauseTimer();
      return;
    }
    this.timeLeft.set(current - 1);
  }

  private pauseTimer() {
    this.isRunning.set(false);
    this.clearInterval();
  }

  private clearInterval() {
    if (!this.intervalId) return;
    clearInterval(this.intervalId);
    this.intervalId = null;
  }
}
