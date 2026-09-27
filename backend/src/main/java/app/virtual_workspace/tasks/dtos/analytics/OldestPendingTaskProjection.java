package app.virtual_workspace.tasks.dtos.analytics;

import java.time.Instant;

public interface OldestPendingTaskProjection {
    Long getId();
    String getTitle();
    Instant getCreatedAt();
}
