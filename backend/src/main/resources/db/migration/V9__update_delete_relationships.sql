-- V9__update_delete_relationships.sql

-- Required for ON DELETE SET NULL
ALTER TABLE rooms
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE timer_sessions
    ALTER COLUMN room_id DROP NOT NULL;


-- users -> profiles
ALTER TABLE profiles
    DROP CONSTRAINT fk410q61iev7klncmpqfuo85ivh;

ALTER TABLE profiles
    ADD CONSTRAINT fk410q61iev7klncmpqfuo85ivh
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;


-- users -> favorite_rooms
ALTER TABLE favorite_rooms
    DROP CONSTRAINT fkjr59t0hlxrpkqqxcpl588ykx0;

ALTER TABLE favorite_rooms
    ADD CONSTRAINT fkjr59t0hlxrpkqqxcpl588ykx0
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;


-- users -> room_members
ALTER TABLE room_members
    DROP CONSTRAINT fkmcymqhedxe30d98p07eeqo3my;

ALTER TABLE room_members
    ADD CONSTRAINT fkmcymqhedxe30d98p07eeqo3my
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;


-- users -> rooms
ALTER TABLE rooms
    DROP CONSTRAINT fka84ab0lpjkgd9beja545d9ysd;

ALTER TABLE rooms
    ADD CONSTRAINT fka84ab0lpjkgd9beja545d9ysd
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE SET NULL;


-- users -> tasks
ALTER TABLE tasks
    DROP CONSTRAINT fk6s1ob9k4ihi75xbxe2w0ylsdh;

ALTER TABLE tasks
    ADD CONSTRAINT fk6s1ob9k4ihi75xbxe2w0ylsdh
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;


-- users -> timer_sessions
ALTER TABLE timer_sessions
    DROP CONSTRAINT fk_timer_sessions_user;

ALTER TABLE timer_sessions
    ADD CONSTRAINT fk_timer_sessions_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;


-- rooms -> favorite_rooms
ALTER TABLE favorite_rooms
    DROP CONSTRAINT fkn89cahplj3qfwqlmmalnq0r3v;

ALTER TABLE favorite_rooms
    ADD CONSTRAINT fkn89cahplj3qfwqlmmalnq0r3v
    FOREIGN KEY (room_id)
    REFERENCES rooms(id)
    ON DELETE CASCADE;


-- rooms -> room_members
ALTER TABLE room_members
    DROP CONSTRAINT fk1bbl9rh6ae8v6mebaoq2ilg9g;

ALTER TABLE room_members
    ADD CONSTRAINT fk1bbl9rh6ae8v6mebaoq2ilg9g
    FOREIGN KEY (room_id)
    REFERENCES rooms(id)
    ON DELETE CASCADE;


-- rooms -> timer_sessions
ALTER TABLE timer_sessions
    DROP CONSTRAINT fk_timer_sessions_room;

ALTER TABLE timer_sessions
    ADD CONSTRAINT fk_timer_sessions_room
    FOREIGN KEY (room_id)
    REFERENCES rooms(id)
    ON DELETE SET NULL;