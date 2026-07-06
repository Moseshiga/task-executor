package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.StaleTaskRecoveryService;
import com.moseshiga.taskexecutor.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "task-execution.max-attempts=3",
        "task-execution.stale-timeout-ms=300000"
})
class StaleTaskRecoveryServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private StaleTaskRecoveryService staleTaskRecoveryService;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void recoverStaleTasksShouldReturnOnlyStaleInProgressTasksToNew() {
        Instant now = Instant.now();

        TaskEntity staleTask = saveTask(
                "Stale task",
                TaskStatus.IN_PROGRESS,
                1,
                now.minusSeconds(600)
        );

        TaskEntity freshTask = saveTask(
                "Fresh task",
                TaskStatus.IN_PROGRESS,
                1,
                now.minusSeconds(60)
        );

        int recoveredCount = staleTaskRecoveryService.recoverStaleTasks();

        assertThat(recoveredCount).isEqualTo(1);

        TaskEntity recoveredStaleTask = taskRepository.findById(staleTask.getId()).orElseThrow();
        TaskEntity unchangedFreshTask = taskRepository.findById(freshTask.getId()).orElseThrow();

        assertThat(recoveredStaleTask.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(recoveredStaleTask.getStartedAt()).isNull();

        assertThat(unchangedFreshTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(unchangedFreshTask.getStartedAt()).isNotNull();
    }

    @Test
    void recoverStaleTasksShouldFailTaskWhenAttemptsLimitReached() {
        Instant now = Instant.now();

        TaskEntity staleTask = saveTask(
                "Stale task with max attempts",
                TaskStatus.IN_PROGRESS,
                3,
                now.minusSeconds(600)
        );

        int recoveredCount = staleTaskRecoveryService.recoverStaleTasks();

        assertThat(recoveredCount).isEqualTo(1);

        TaskEntity recoveredTask = taskRepository.findById(staleTask.getId()).orElseThrow();

        assertThat(recoveredTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(recoveredTask.getCompletedAt()).isNotNull();
        assertThat(recoveredTask.getErrorMessage())
                .isEqualTo("Task failed after reaching max attempts");
    }

    @Test
    void recoverStaleTasksShouldDoNothingWhenNoStaleTasksExist() {
        Instant now = Instant.now();

        saveTask(
                "Fresh task",
                TaskStatus.IN_PROGRESS,
                1,
                now.minusSeconds(60)
        );

        int recoveredCount = staleTaskRecoveryService.recoverStaleTasks();

        assertThat(recoveredCount).isZero();
    }

    private TaskEntity saveTask(
            String name,
            TaskStatus status,
            int attemptCount,
            Instant startedAt
    ) {
        Instant now = Instant.now();

        TaskEntity task = TaskEntity.builder()
                .name(name)
                .durationMs(5000L)
                .status(status)
                .progress(0)
                .result(null)
                .errorMessage(null)
                .attemptCount(attemptCount)
                .createdAt(now)
                .startedAt(startedAt)
                .completedAt(null)
                .updatedAt(startedAt == null ? now : startedAt)
                .build();

        return taskRepository.saveAndFlush(task);
    }
}