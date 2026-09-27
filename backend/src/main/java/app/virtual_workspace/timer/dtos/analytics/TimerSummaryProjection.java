package app.virtual_workspace.timer.dtos.analytics;

public interface TimerSummaryProjection {
    Long getTotalSessions();
    Long getTotalFocusSeconds();
    Long getLongestSessionSeconds();
    Double getAvgSessionSeconds();
}
