package com.moseshiga.taskexecutor.repository;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    @Query(
            value = """
                    SELECT *
                    FROM tasks
                    WHERE status = 'NEW'
                    ORDER BY id
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<TaskEntity> findNextNewTaskForUpdate();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                WITH locked_tasks AS (
                    SELECT id
                    FROM tasks
                    WHERE status = 'IN_PROGRESS'
                      AND started_at < :staleBefore
                    ORDER BY started_at, id
                    FOR UPDATE SKIP LOCKED
                    LIMIT :batchSize
                )
                UPDATE tasks t
                SET status = CASE
                        WHEN t.attempt_count >= :maxAttempts THEN 'FAILED'
                        ELSE 'NEW'
                    END,
                    progress = CASE
                        WHEN t.attempt_count >= :maxAttempts THEN t.progress
                        ELSE 0
                    END,
                    result = CASE
                        WHEN t.attempt_count >= :maxAttempts THEN 'Task failed by stale task recovery'
                        ELSE 'Task returned to NEW after stale IN_PROGRESS timeout'
                    END,
                    error_message = CASE
                        WHEN t.attempt_count >= :maxAttempts THEN 'Task failed after reaching max attempts'
                        ELSE NULL
                    END,
                    started_at = CASE
                        WHEN t.attempt_count >= :maxAttempts THEN t.started_at
                        ELSE NULL
                    END,
                    completed_at = CASE
                        WHEN t.attempt_count >= :maxAttempts THEN CURRENT_TIMESTAMP
                        ELSE NULL
                    END,
                    updated_at = CURRENT_TIMESTAMP,
                    version = t.version + 1
                FROM locked_tasks lt
                WHERE t.id = lt.id
                """,
            nativeQuery = true
    )
    int recoverStaleTasks(
            @Param("staleBefore") Instant staleBefore,
            @Param("maxAttempts") int maxAttempts,
            @Param("batchSize") int batchSize
    );
}
