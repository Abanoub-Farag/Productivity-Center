package app.virtual_workspace.timer.dtos.analytics;

import java.time.LocalDate;

/** Week-start date (Monday) plus aggregated focus time for that ISO week. */
public record WeeklyFocusDto(LocalDate weekStart, Long focusSeconds) {}
