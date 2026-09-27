package app.virtual_workspace.timer.dtos.analytics;

import java.time.LocalDate;

public record DailyFocusDto(LocalDate date, Long focusSeconds) {}
