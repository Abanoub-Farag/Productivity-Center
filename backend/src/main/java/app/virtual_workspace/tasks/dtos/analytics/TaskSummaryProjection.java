package app.virtual_workspace.tasks.dtos.analytics;

public interface TaskSummaryProjection {
    Long getTotalTasks();
    Long getCompletedTasks();
    Long getPendingTasks();
}
