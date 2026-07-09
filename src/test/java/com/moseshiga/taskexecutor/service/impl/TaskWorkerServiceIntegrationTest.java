package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.TaskExecutionLease;
import com.moseshiga.taskexecutor.service.TaskWorkerService;
import com.moseshiga.taskexecutor.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TaskWorkerServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskWorkerService taskWorkerService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void pickNextTaskShouldMoveOldestNewTaskToInProgress() {
        TaskEntity firstTask = saveTask("First task", TaskStatus.NEW);
        TaskEntity secondTask = saveTask("Second task", TaskStatus.NEW);

        Optional<TaskExecutionLease> pickedLease = taskWorkerService.pickNextTask();

        assertThat(pickedLease).hasValueSatisfying(lease -> assertThat(lease.taskId()).isEqualTo(firstTask.getId()));
        assertThat(pickedLease).hasValueSatisfying(lease -> assertThat(lease.attemptCount()).isEqualTo(1));

        TaskEntity pickedTask = taskRepository.findById(firstTask.getId()).orElseThrow();
        TaskEntity untouchedTask = taskRepository.findById(secondTask.getId()).orElseThrow();

        assertThat(pickedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(pickedTask.getAttemptCount()).isEqualTo(1);
        assertThat(pickedTask.getStartedAt()).isNotNull();
        assertThat(pickedTask.getUpdatedAt()).isNotNull();
        assertThat(pickedTask.getStatusMessage()).isEqualTo("Task execution started");
        assertThat(pickedTask.getResult()).isNull();
        assertThat(pickedTask.getErrorMessage()).isNull();

        assertThat(untouchedTask.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(untouchedTask.getAttemptCount()).isZero();
        assertThat(untouchedTask.getStartedAt()).isNull();
    }

    @Test
    void pickNextTaskShouldReturnEmptyWhenNoNewTasksExist() {
        saveTask("In progress task", TaskStatus.IN_PROGRESS);
        saveTask("Completed task", TaskStatus.COMPLETED);
        saveTask("Failed task", TaskStatus.FAILED);

        Optional<TaskExecutionLease> pickedTaskId = taskWorkerService.pickNextTask();

        assertThat(pickedTaskId).isEmpty();
    }

    @Test
    void pickNextTaskShouldNotPickAlreadyInProgressTask() {
        TaskEntity inProgressTask = saveTask("In progress task", TaskStatus.IN_PROGRESS);

        Optional<TaskExecutionLease> pickedTaskId = taskWorkerService.pickNextTask();

        assertThat(pickedTaskId).isEmpty();

        TaskEntity unchangedTask = taskRepository.findById(inProgressTask.getId()).orElseThrow();

        assertThat(unchangedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(unchangedTask.getAttemptCount()).isZero();
    }

    @Test
    void pickNextTaskShouldIncrementAttemptCountFromPreviousValue() {
        TaskEntity task = saveTask("Retry task", TaskStatus.NEW, 2);

        Optional<TaskExecutionLease> pickedLease = taskWorkerService.pickNextTask();

        assertThat(pickedLease).hasValueSatisfying(lease -> assertThat(lease.taskId()).isEqualTo(task.getId()));
        assertThat(pickedLease).hasValueSatisfying(lease -> assertThat(lease.attemptCount()).isEqualTo(3));

        TaskEntity pickedTask = taskRepository.findById(task.getId()).orElseThrow();

        assertThat(pickedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(pickedTask.getAttemptCount()).isEqualTo(3);
    }

    @Test
    void findNextNewTaskForUpdateShouldSkipLockedTaskInAnotherTransaction() {
        TaskEntity firstTask = saveTask("Locked task", TaskStatus.NEW);
        TaskEntity secondTask = saveTask("Available task", TaskStatus.NEW);

        transactionTemplate.executeWithoutResult(outerStatus -> {
            Optional<TaskEntity> lockedTask = taskRepository.findNextNewTaskForUpdate();

            assertThat(lockedTask).isPresent();
            assertThat(lockedTask.get().getId()).isEqualTo(firstTask.getId());

            TransactionTemplate requiresNewTransaction = new TransactionTemplate(transactionManager);
            requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            Optional<TaskExecutionLease> pickedTask = requiresNewTransaction.execute(innerStatus ->
                    taskWorkerService.pickNextTask()
            );

            assertThat(pickedTask).hasValueSatisfying(lease -> assertThat(lease.taskId()).isEqualTo(secondTask.getId()));
        });
    }

    private TaskEntity saveTask(String name, TaskStatus status) {
        return saveTask(name, status, 0);
    }

    private TaskEntity saveTask(String name, TaskStatus status, int attemptCount) {
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
                .startedAt(status == TaskStatus.IN_PROGRESS ? now : null)
                .completedAt(status == TaskStatus.COMPLETED || status == TaskStatus.FAILED ? now : null)
                .updatedAt(now)
                .build();

        return taskRepository.saveAndFlush(task);
    }
}
