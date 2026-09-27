package app.virtual_workspace.tasks.dtos.analytics;

import java.time.LocalDate;

/**
 * Tasks created and completed on a given calendar day.
 * "created" uses created_at; "completed" uses completed_at.
 */
public record DailyTaskCountDto(LocalDate date, Long created, Long completed) {}
