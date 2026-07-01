CREATE TABLE tasks
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    duration_ms     BIGINT       NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    progress        INTEGER      NOT NULL DEFAULT 0,
    result          VARCHAR(1000),
    error_message   VARCHAR(1000),
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_tasks_duration_positive
        CHECK (duration_ms > 0),

    CONSTRAINT chk_tasks_progress_range
        CHECK (progress >= 0 AND progress <= 100),

    CONSTRAINT chk_tasks_attempt_count_non_negative
        CHECK (attempt_count >= 0),

    CONSTRAINT chk_tasks_status
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_tasks_status_id
    ON tasks (status, id);

CREATE INDEX idx_tasks_status_started_at
    ON tasks (status, started_at);