package app.virtual_workspace.timer.dtos.analytics;

public record TimerSummaryDto(
        Long totalSessions,
        Long totalFocusSeconds,
        Long longestSessionSeconds,
        Double avgSessionSeconds
) {}
