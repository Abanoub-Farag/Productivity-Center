-- V10__add_completed_at_to_tasks.sql
-- Adds a nullable completed_at column to track when a task was marked complete.
-- Backfills existing completed rows with updated_at as a best-effort approximation.

ALTER TABLE tasks
    ADD COLUMN completed_at TIMESTAMPTZ NULL;

-- Backfill: treat updated_at as completed_at for rows already marked complete
UPDATE tasks
SET completed_at = updated_at
WHERE is_completed = true AND updated_at IS NOT NULL;

CREATE INDEX idx_task_completed_at
    ON tasks (user_id, completed_at)
    WHERE completed_at IS NOT NULL;
