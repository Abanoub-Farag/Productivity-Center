package app.virtual_workspace.tasks.dtos.analytics;

import java.sql.Date;

/**
 * Native SQL projection for the daily task breakdown.
 * "date" is the truncated day; "created" uses created_at; "completed" uses completed_at.
 */
public interface DailyTaskCountProjection {
    Date getDate();
    Long getCreated();
    Long getCompleted();
}
