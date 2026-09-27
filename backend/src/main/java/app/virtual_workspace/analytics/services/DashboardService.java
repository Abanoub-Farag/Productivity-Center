package app.virtual_workspace.analytics.services;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.analytics.dtos.DashboardResponseDto;
import app.virtual_workspace.tasks.services.TaskAnalyticsService;
import app.virtual_workspace.timer.services.TimerSessionAnalyticsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TimerSessionAnalyticsService timerSessionAnalyticsService;
    private final TaskAnalyticsService taskAnalyticsService;

    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboard(Long userId, Instant from, Instant to) {
        return new DashboardResponseDto(
                timerSessionAnalyticsService.getAnalytics(userId, from, to),
                taskAnalyticsService.getAnalytics(userId, from, to)
        );
    }
}
