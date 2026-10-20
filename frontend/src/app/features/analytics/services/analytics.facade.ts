import { Injectable, inject, signal, computed } from '@angular/core';
import { AnalyticsDataService } from './analytics-data.service';
import {
  DashboardResponseDto,
  DateRangeKey,
} from '../models/analytics.models';

function toLocalDateString(d: Date): string {
  return d.toISOString().split('T')[0];
}

function dateRangeDates(key: DateRangeKey): { start: string; end: string } {
  const end = new Date();
  const start = new Date();
  const days = key === '7D' ? 6 : key === '30D' ? 29 : 89;
  start.setDate(end.getDate() - days);
  return { start: toLocalDateString(start), end: toLocalDateString(end) };
}

@Injectable({ providedIn: 'root' })
export class AnalyticsFacade {
  private readonly data = inject(AnalyticsDataService);

  readonly range = signal<DateRangeKey>('30D');
  readonly dashboard = signal<DashboardResponseDto | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);

  // ─── Computed helpers ────────────────────────────────────────────────────────

  readonly timerSummary = computed(() => this.dashboard()?.timerAnalytics.summary ?? null);
  readonly taskSummary  = computed(() => this.dashboard()?.taskAnalytics.summary  ?? null);
  readonly perRoom      = computed(() => this.dashboard()?.timerAnalytics.perRoomBreakdown ?? []);
  readonly dailyFocus   = computed(() => this.dashboard()?.timerAnalytics.dailyBreakdown ?? []);
  readonly oldestTasks  = computed(() => this.dashboard()?.taskAnalytics.oldestPendingTasks ?? []);
  readonly dailyTasks   = computed(() => this.dashboard()?.taskAnalytics.dailyBreakdown ?? []);

  /** Total focus across all rooms — used for "where your time goes" percentages. */
  readonly totalRoomSeconds = computed(() =>
    this.perRoom().reduce((s, r) => s + r.focusSeconds, 0),
  );

  // ─── Actions ─────────────────────────────────────────────────────────────────

  load(): void {
    const { start, end } = dateRangeDates(this.range());
    this.isLoading.set(true);
    this.error.set(null);

    this.data.getDashboard(start, end).subscribe({
      next: (res) => {
        this.dashboard.set(res);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load analytics. Please try again.');
        this.isLoading.set(false);
      },
    });
  }

  setRange(r: DateRangeKey): void {
    this.range.set(r);
    this.load();
  }

  // ─── Formatting helpers used in templates ────────────────────────────────────

  formatSeconds(s: number): string {
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    if (h > 0) return `${h}h ${m.toString().padStart(2, '0')}m`;
    return `${m}m`;
  }

  formatHours(s: number): string {
    return (s / 3600).toFixed(1) + 'h';
  }

  roomColor(index: number): string {
    const palette = [
      '#7C3AED', '#10B981', '#F97316', '#3B82F6', '#EC4899',
      '#EAB308', '#06B6D4', '#8B5CF6', '#14B8A6', '#F43F5E',
    ];
    return palette[index % palette.length];
  }

  roomPercent(focusSeconds: number): number {
    const total = this.totalRoomSeconds();
    if (!total) return 0;
    return Math.round((focusSeconds / total) * 100);
  }

  ageLabel(createdAt: string): string {
    const days = Math.floor(
      (Date.now() - new Date(createdAt).getTime()) / 86_400_000,
    );
    if (days === 0) return 'Today';
    if (days === 1) return '1 day ago';
    return `${days} days ago`;
  }
}
