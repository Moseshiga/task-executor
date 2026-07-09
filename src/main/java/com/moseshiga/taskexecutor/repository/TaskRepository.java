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
                    UPDATE tasks
                    SET progress = :progress,
                        status_message = :statusMessage,
                        updated_at = :updatedAt,
                        version = version + 1
                    WHERE id = :taskId
                      AND attempt_count = :attemptCount
                      AND status = 'IN_PROGRESS'
                    """,
            nativeQuery = true
    )
    int updateProgressIfCurrent(
            @Param("taskId") Long taskId,
            @Param("attemptCount") int attemptCount,
            @Param("progress") int progress,
            @Param("statusMessage") String statusMessage,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE tasks
                    SET status = 'COMPLETED',
                        progress = 100,
                        status_message = 'Task completed successfully',
                        result = :result,
                        error_message = NULL,
                        completed_at = :completedAt,
                        updated_at = :completedAt,
                        version = version + 1
                    WHERE id = :taskId
                      AND attempt_count = :attemptCount
                      AND status = 'IN_PROGRESS'
                    """,
            nativeQuery = true
    )
    int completeIfCurrent(
            @Param("taskId") Long taskId,
            @Param("attemptCount") int attemptCount,
            @Param("result") String result,
            @Param("completedAt") Instant completedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE tasks
                    SET status = 'FAILED',
                        status_message = :errorMessage,
                        error_message = :errorMessage,
                        completed_at = :completedAt,
                        updated_at = :completedAt,
                        version = version + 1
                    WHERE id = :taskId
                      AND attempt_count = :attemptCount
                      AND status = 'IN_PROGRESS'
                    """,
            nativeQuery = true
    )
    int failIfCurrent(
            @Param("taskId") Long taskId,
            @Param("attemptCount") int attemptCount,
            @Param("errorMessage") String errorMessage,
            @Param("completedAt") Instant completedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE tasks
                    SET status = 'NEW',
                        progress = 0,
                        status_message = :statusMessage,
                        result = NULL,
                        error_message = NULL,
                        started_at = NULL,
                        completed_at = NULL,
                        updated_at = :updatedAt,
                        version = version + 1
                    WHERE id = :taskId
                      AND attempt_count = :attemptCount
                      AND status = 'IN_PROGRESS'
                    """,
            nativeQuery = true
    )
    int returnToNewIfCurrent(
            @Param("taskId") Long taskId,
            @Param("attemptCount") int attemptCount,
            @Param("statusMessage") String statusMessage,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                WITH locked_tasks AS (
                    SELECT id
                    FROM tasks
                    WHERE status = 'IN_PROGRESS'
                      AND updated_at < :staleBefore
                    ORDER BY updated_at, id
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
                    status_message = CASE
                        WHEN t.attempt_count >= :maxAttempts THEN 'Task failed by stale task recovery'
                        ELSE 'Task returned to NEW after stale IN_PROGRESS timeout'
                    END,
                    result = CASE
                        WHEN t.attempt_count >= :maxAttempts THEN t.result
                        ELSE NULL
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
