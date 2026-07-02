package com.moseshiga.taskexecutor.repository;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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

    List<TaskEntity> findByStatusAndStartedAtBefore(TaskStatus status, Instant startedAt);
}
