package app.virtual_workspace.timer.dtos.analytics;

import java.time.LocalDate;

/** Month-start date plus aggregated focus time for that calendar month. */
public record MonthlyFocusDto(LocalDate monthStart, Long focusSeconds) {}
