CREATE TABLE timer_sessions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    room_id     BIGINT NOT NULL,
    started_at  TIMESTAMP(6) NOT NULL,
    ended_at    TIMESTAMP(6) NULL,
    duration    BIGINT NULL,
    status      VARCHAR(20) NOT NULL,
    CONSTRAINT chk_timer_sessions_status CHECK (status IN ('RUNNING', 'DONE')),
    CONSTRAINT fk_timer_sessions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_timer_sessions_room FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE INDEX idx_timer_sessions_user_started_at
    ON timer_sessions(user_id, started_at);

CREATE INDEX idx_timer_sessions_user_room_started_at
    ON timer_sessions(user_id, room_id, started_at);

CREATE INDEX idx_timer_sessions_room_started_at
    ON timer_sessions(room_id, started_at);

CREATE UNIQUE INDEX idx_timer_sessions_user_running
    ON timer_sessions(user_id)
    WHERE status = 'RUNNING';
