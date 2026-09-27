package app.virtual_workspace.analytics.dtos;

import app.virtual_workspace.tasks.dtos.analytics.TaskAnalyticsResponseDto;
import app.virtual_workspace.timer.dtos.analytics.TimerAnalyticsResponseDto;

public record DashboardResponseDto(
        TimerAnalyticsResponseDto timerAnalytics,
        TaskAnalyticsResponseDto taskAnalytics
) {}
