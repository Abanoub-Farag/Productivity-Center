-- V11__convert_favorite_rooms_to_timestamptz.sql
-- Convert favorite_rooms.added_at from timestamp without time zone to timestamptz
-- to align with Instant addedAt in FavoriteRoom entity.

UPDATE favorite_rooms
SET added_at = CURRENT_TIMESTAMP
WHERE added_at IS NULL;

ALTER TABLE favorite_rooms
    ALTER COLUMN added_at TYPE timestamptz
        USING added_at AT TIME ZONE 'Africa/Cairo',
    ALTER COLUMN added_at SET NOT NULL;
