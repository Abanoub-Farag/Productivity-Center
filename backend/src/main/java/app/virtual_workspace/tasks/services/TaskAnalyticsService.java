package app.virtual_workspace.tasks.services;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.virtual_workspace.tasks.dtos.analytics.DailyTaskCountDto;
import app.virtual_workspace.tasks.dtos.analytics.OldestPendingTaskDto;
import app.virtual_workspace.tasks.dtos.analytics.TaskAnalyticsResponseDto;
import app.virtual_workspace.tasks.dtos.analytics.TaskSummaryDto;
import app.virtual_workspace.tasks.dtos.analytics.TaskSummaryProjection;
import app.virtual_workspace.tasks.repositories.TaskRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskAnalyticsService {

    private static final int MAX_OLDEST_PENDING = 5;

    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public TaskAnalyticsResponseDto getAnalytics(Long userId, Instant from, Instant to) {

        // ── Scalar summary ────────────────────────────────────────────────────────
        TaskSummaryProjection proj = taskRepository.findTaskSummary(userId, from, to);
        long total = proj.getTotalTasks() != null ? proj.getTotalTasks() : 0L;
        long completed = proj.getCompletedTasks() != null ? proj.getCompletedTasks() : 0L;
        long pending = proj.getPendingTasks() != null ? proj.getPendingTasks() : 0L;
        double completionRate = total == 0 ? 0.0 : (completed * 100.0) / total;

        TaskSummaryDto summary = new TaskSummaryDto(total, completed, pending, completionRate);

        // ── Daily breakdown ───────────────────────────────────────────────────────
        List<DailyTaskCountDto> daily = taskRepository
                .findDailyTaskBreakdown(userId, from, to)
                .stream()
                .map(p -> new DailyTaskCountDto(p.getDate().toLocalDate(), p.getCreated(), p.getCompleted()))
                .toList();

        // ── Top-5 oldest pending tasks ────────────────────────────────────────────
        List<OldestPendingTaskDto> oldest = taskRepository
                .findOldestPendingTasks(userId, PageRequest.of(0, MAX_OLDEST_PENDING))
                .stream()
                .map(p -> new OldestPendingTaskDto(p.getId(), p.getTitle(), p.getCreatedAt()))
                .toList();

        return new TaskAnalyticsResponseDto(summary, daily, oldest);
    }
}
