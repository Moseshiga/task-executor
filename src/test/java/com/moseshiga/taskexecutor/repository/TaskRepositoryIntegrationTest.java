package com.moseshiga.taskexecutor.repository;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void findNextNewTaskForUpdateShouldReturnOldestNewTask() {
        TaskEntity firstTask = saveTask("First task", TaskStatus.NEW);
        saveTask("Second task", TaskStatus.NEW);
        saveTask("In progress task", TaskStatus.IN_PROGRESS);

        Optional<TaskEntity> result = transactionTemplate.execute(status ->
                taskRepository.findNextNewTaskForUpdate()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(firstTask.getId());
        assertThat(result.get().getStatus()).isEqualTo(TaskStatus.NEW);
    }

    @Test
    void findNextNewTaskForUpdateShouldReturnEmptyWhenNoNewTasksExist() {
        saveTask("Completed task", TaskStatus.COMPLETED);
        saveTask("Failed task", TaskStatus.FAILED);
        saveTask("In progress task", TaskStatus.IN_PROGRESS);

        Optional<TaskEntity> result = transactionTemplate.execute(status ->
                taskRepository.findNextNewTaskForUpdate()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void recoverStaleTasksShouldReturnInProgressTaskToNewWhenAttemptsLimitNotReached() {
        Instant now = Instant.now();

        TaskEntity staleTask = saveTask(
                "Stale task",
                TaskStatus.IN_PROGRESS,
                1,
                now.minusSeconds(600)
        );

        int updatedRows = transactionTemplate.execute(status ->
                taskRepository.recoverStaleTasks(
                        now.minusSeconds(300),
                        3,
                        100
                )
        );

        assertThat(updatedRows).isEqualTo(1);

        TaskEntity recoveredTask = taskRepository.findById(staleTask.getId()).orElseThrow();

        assertThat(recoveredTask.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(recoveredTask.getProgress()).isZero();
        assertThat(recoveredTask.getStartedAt()).isNull();
        assertThat(recoveredTask.getCompletedAt()).isNull();
        assertThat(recoveredTask.getStatusMessage())
                .isEqualTo("Task returned to NEW after stale IN_PROGRESS timeout");
        assertThat(recoveredTask.getResult()).isNull();
        assertThat(recoveredTask.getErrorMessage()).isNull();
    }

    @Test
    void recoverStaleTasksShouldMarkTaskAsFailedWhenAttemptsLimitReached() {
        Instant now = Instant.now();

        TaskEntity staleTask = saveTask(
                "Stale task with max attempts",
                TaskStatus.IN_PROGRESS,
                3,
                now.minusSeconds(600)
        );

        int updatedRows = transactionTemplate.execute(status ->
                taskRepository.recoverStaleTasks(
                        now.minusSeconds(300),
                        3,
                        100
                )
        );

        assertThat(updatedRows).isEqualTo(1);

        TaskEntity recoveredTask = taskRepository.findById(staleTask.getId()).orElseThrow();

        assertThat(recoveredTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(recoveredTask.getStartedAt()).isNotNull();
        assertThat(recoveredTask.getCompletedAt()).isNotNull();
        assertThat(recoveredTask.getStatusMessage())
                .isEqualTo("Task failed by stale task recovery");
        assertThat(recoveredTask.getResult()).isNull();
        assertThat(recoveredTask.getErrorMessage())
                .isEqualTo("Task failed after reaching max attempts");
    }

    @Test
    void recoverStaleTasksShouldNotTouchFreshInProgressTask() {
        Instant now = Instant.now();

        TaskEntity freshTask = saveTask(
                "Fresh task",
                TaskStatus.IN_PROGRESS,
                1,
                now.minusSeconds(30)
        );

        int updatedRows = transactionTemplate.execute(status ->
                taskRepository.recoverStaleTasks(
                        now.minusSeconds(300),
                        3,
                        100
                )
        );

        assertThat(updatedRows).isZero();

        TaskEntity unchangedTask = taskRepository.findById(freshTask.getId()).orElseThrow();

        assertThat(unchangedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(unchangedTask.getStartedAt()).isNotNull();
    }

    private TaskEntity saveTask(String name, TaskStatus status) {
        return saveTask(name, status, 0, null);
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
                .progress(status == TaskStatus.COMPLETED ? 100 : 0)
                .result(null)
                .errorMessage(null)
                .attemptCount(attemptCount)
                .createdAt(now)
                .startedAt(startedAt)
                .completedAt(status == TaskStatus.COMPLETED ? now : null)
                .updatedAt(startedAt == null ? now : startedAt)
                .build();

        return taskRepository.saveAndFlush(task);
    }
}
