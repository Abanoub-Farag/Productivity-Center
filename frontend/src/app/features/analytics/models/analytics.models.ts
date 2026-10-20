// ── Timer Analytics ───────────────────────────────────────────────────────────

export interface TimerSummaryDto {
  totalSessions: number;
  totalFocusSeconds: number;
  longestSessionSeconds: number;
  avgSessionSeconds: number;
}

export interface DailyFocusDto {
  date: string; // LocalDate serialised as 'YYYY-MM-DD'
  focusSeconds: number;
}

export interface WeeklyFocusDto {
  weekStart: string;
  focusSeconds: number;
}

export interface MonthlyFocusDto {
  monthStart: string;
  focusSeconds: number;
}

export interface RoomFocusDto {
  roomId: number;
  roomName: string;
  focusSeconds: number;
  sessionCount: number;
}

export interface TimerAnalyticsResponseDto {
  summary: TimerSummaryDto;
  dailyBreakdown: DailyFocusDto[];
  weeklyBreakdown: WeeklyFocusDto[];
  monthlyBreakdown: MonthlyFocusDto[];
  perRoomBreakdown: RoomFocusDto[];
}

// ── Task Analytics ────────────────────────────────────────────────────────────

export interface TaskSummaryDto {
  totalTasks: number;
  completedTasks: number;
  pendingTasks: number;
  completionRate: number;
}

export interface DailyTaskCountDto {
  date: string;
  created: number;
  completed: number;
}

export interface OldestPendingTaskDto {
  id: number;
  title: string;
  createdAt: string; // Instant serialised as ISO string
}

export interface TaskAnalyticsResponseDto {
  summary: TaskSummaryDto;
  dailyBreakdown: DailyTaskCountDto[];
  oldestPendingTasks: OldestPendingTaskDto[];
}

// ── Top-level Dashboard ───────────────────────────────────────────────────────

export interface DashboardResponseDto {
  timerAnalytics: TimerAnalyticsResponseDto;
  taskAnalytics: TaskAnalyticsResponseDto;
}

// ── Date range helper ────────────────────────────────────────────────────────

export type DateRangeKey = '7D' | '30D' | '90D';
