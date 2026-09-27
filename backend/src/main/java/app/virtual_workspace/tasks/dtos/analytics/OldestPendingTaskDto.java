package app.virtual_workspace.tasks.dtos.analytics;

import java.time.Instant;

public record OldestPendingTaskDto(Long id, String title, Instant createdAt) {}
