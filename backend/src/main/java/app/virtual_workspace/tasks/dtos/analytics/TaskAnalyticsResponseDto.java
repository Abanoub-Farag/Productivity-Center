package app.virtual_workspace.tasks.dtos.analytics;

import java.util.List;

public record TaskAnalyticsResponseDto(
        TaskSummaryDto summary,
        List<DailyTaskCountDto> dailyBreakdown,
        List<OldestPendingTaskDto> oldestPendingTasks
) {}
