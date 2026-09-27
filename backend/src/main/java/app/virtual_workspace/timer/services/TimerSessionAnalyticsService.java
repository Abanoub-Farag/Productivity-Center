package app.virtual_workspace.timer.services;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.timer.dtos.analytics.DailyFocusDto;
import app.virtual_workspace.timer.dtos.analytics.MonthlyFocusDto;
import app.virtual_workspace.timer.dtos.analytics.RoomFocusDto;
import app.virtual_workspace.timer.dtos.analytics.TimerAnalyticsResponseDto;
import app.virtual_workspace.timer.dtos.analytics.TimerSummaryDto;
import app.virtual_workspace.timer.dtos.analytics.TimerSummaryProjection;
import app.virtual_workspace.timer.dtos.analytics.WeeklyFocusDto;
import app.virtual_workspace.timer.repositories.TimerSessionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimerSessionAnalyticsService {

    private final TimerSessionRepository timerSessionRepository;

    @Transactional(readOnly = true)
    public TimerAnalyticsResponseDto getAnalytics(Long userId, Instant from, Instant to) {

        // ── Scalar summary ────────────────────────────────────────────────────────
        TimerSummaryProjection proj = timerSessionRepository.findTimerSummary(userId, from, to);
        TimerSummaryDto summary = new TimerSummaryDto(
                proj.getTotalSessions(),
                proj.getTotalFocusSeconds(),
                proj.getLongestSessionSeconds(),
                proj.getAvgSessionSeconds()
        );

        // ── Daily breakdown ───────────────────────────────────────────────────────
        List<DailyFocusDto> daily = timerSessionRepository
                .findDailyFocusBreakdown(userId, from, to)
                .stream()
                .map(p -> new DailyFocusDto(p.getDate().toLocalDate(), p.getFocusSeconds()))
                .toList();

        // ── Weekly breakdown ──────────────────────────────────────────────────────
        List<WeeklyFocusDto> weekly = timerSessionRepository
                .findWeeklyFocusBreakdown(userId, from, to)
                .stream()
                .map(p -> new WeeklyFocusDto(p.getDate().toLocalDate(), p.getFocusSeconds()))
                .toList();

        // ── Monthly breakdown ─────────────────────────────────────────────────────
        List<MonthlyFocusDto> monthly = timerSessionRepository
                .findMonthlyFocusBreakdown(userId, from, to)
                .stream()
                .map(p -> new MonthlyFocusDto(p.getDate().toLocalDate(), p.getFocusSeconds()))
                .toList();

        // ── Per-room breakdown ────────────────────────────────────────────────────
        List<RoomFocusDto> perRoom = timerSessionRepository
                .findPerRoomFocusBreakdown(userId, from, to)
                .stream()
                .map(p -> new RoomFocusDto(
                        p.getRoomId(),
                        p.getRoomTitle() != null ? p.getRoomTitle() : "Deleted Room",
                        p.getFocusSeconds(),
                        p.getSessionCount()))
                .toList();

        return new TimerAnalyticsResponseDto(summary, daily, weekly, monthly, perRoom);
    }
}
