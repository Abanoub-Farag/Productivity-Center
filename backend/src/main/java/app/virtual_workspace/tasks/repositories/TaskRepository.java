package app.virtual_workspace.tasks.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.virtual_workspace.tasks.dtos.analytics.DailyTaskCountProjection;
import app.virtual_workspace.tasks.dtos.analytics.OldestPendingTaskProjection;
import app.virtual_workspace.tasks.dtos.analytics.TaskSummaryProjection;
import app.virtual_workspace.tasks.models.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId")
    Slice<Task> findTasksByUserId(@Param("userId") Long userId, Pageable pageable);

    Task findTasksById(Long id);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    long deleteByIdAndUserId(Long id, Long userId);

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Query("""
            SELECT
              COUNT(t)                                                      AS totalTasks,
              SUM(CASE WHEN t.isCompleted = true  THEN 1 ELSE 0 END)      AS completedTasks,
              SUM(CASE WHEN t.isCompleted = false THEN 1 ELSE 0 END)      AS pendingTasks
            FROM Task t
            WHERE t.userId    = :userId
              AND t.createdAt >= :from
              AND t.createdAt  < :to
            """)
    TaskSummaryProjection findTaskSummary(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /**
     * Daily task creation vs completion counts.
     * - "created"   groups by the day the task was created  (created_at).
     * - "completed" groups by the day the task was completed (completed_at).
     * A FULL OUTER JOIN merges both sets so every date with activity appears once.
     */
    @Query(value = """
            SELECT
              COALESCE(c.date, d.date)  AS date,
              COALESCE(c.created, 0)   AS created,
              COALESCE(d.completed, 0) AS completed
            FROM (
                SELECT
                  date_trunc('day', created_at AT TIME ZONE 'UTC')::date AS date,
                  COUNT(*) AS created
                FROM tasks
                WHERE user_id   = :userId
                  AND created_at >= :from
                  AND created_at  < :to
                GROUP BY 1
            ) c
            FULL OUTER JOIN (
                SELECT
                  date_trunc('day', completed_at AT TIME ZONE 'UTC')::date AS date,
                  COUNT(*) AS completed
                FROM tasks
                WHERE user_id      = :userId
                  AND completed_at IS NOT NULL
                  AND completed_at >= :from
                  AND completed_at  < :to
                GROUP BY 1
            ) d ON c.date = d.date
            ORDER BY date
            """, nativeQuery = true)
    List<DailyTaskCountProjection> findDailyTaskBreakdown(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT t.id AS id, t.title AS title, t.createdAt AS createdAt
            FROM Task t
            WHERE t.userId      = :userId
              AND t.isCompleted = false
            ORDER BY t.createdAt ASC
            """)
    List<OldestPendingTaskProjection> findOldestPendingTasks(
            @Param("userId") Long userId,
            Pageable pageable);
}
