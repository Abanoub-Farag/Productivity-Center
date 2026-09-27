package app.virtual_workspace.tasks.dtos.analytics;

public record TaskSummaryDto(
        Long totalTasks,
        Long completedTasks,
        Long pendingTasks,
        Double completionRate
) {}
