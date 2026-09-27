-- Convert application timestamps from Cairo local time
-- to timestamptz.
--
-- Assumption:
-- Existing timestamp WITHOUT TIME ZONE values represent
-- Africa/Cairo local time.

ALTER TABLE users
    ALTER COLUMN created_at TYPE timestamptz
        USING created_at AT TIME ZONE 'Africa/Cairo',
    ALTER COLUMN updated_at TYPE timestamptz
        USING updated_at AT TIME ZONE 'Africa/Cairo';

ALTER TABLE rooms
    ALTER COLUMN created_at TYPE timestamptz
        USING created_at AT TIME ZONE 'Africa/Cairo',
    ALTER COLUMN updated_at TYPE timestamptz
        USING updated_at AT TIME ZONE 'Africa/Cairo';

ALTER TABLE room_members
    ALTER COLUMN created_at TYPE timestamptz
        USING created_at AT TIME ZONE 'Africa/Cairo',
    ALTER COLUMN last_active_at TYPE timestamptz
        USING last_active_at AT TIME ZONE 'Africa/Cairo';

ALTER TABLE tasks
    ALTER COLUMN created_at TYPE timestamptz
        USING created_at AT TIME ZONE 'Africa/Cairo',
    ALTER COLUMN updated_at TYPE timestamptz
        USING updated_at AT TIME ZONE 'Africa/Cairo';

ALTER TABLE timer_sessions
    ALTER COLUMN started_at TYPE timestamptz
        USING started_at AT TIME ZONE 'Africa/Cairo',
    ALTER COLUMN ended_at TYPE timestamptz
        USING ended_at AT TIME ZONE 'Africa/Cairo';