package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.TaskExecutionLease;
import com.moseshiga.taskexecutor.service.TaskExecutionService;
import com.moseshiga.taskexecutor.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TaskExecutionServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void executeShouldCompleteTaskSuccessfully() {
        TaskEntity task = saveTask(
                "Execution test task",
                TaskStatus.IN_PROGRESS,
                20L
        );

        taskExecutionService.execute(new TaskExecutionLease(task.getId(), 1));

        TaskEntity completedTask = taskRepository.findById(task.getId()).orElseThrow();

        assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completedTask.getProgress()).isEqualTo(100);
        assertThat(completedTask.getResult()).isEqualTo("Task completed successfully");
        assertThat(completedTask.getErrorMessage()).isNull();
        assertThat(completedTask.getCompletedAt()).isNotNull();
        assertThat(completedTask.getUpdatedAt()).isNotNull();
    }

    @Test
    void executeShouldReturnTaskToNewWhenExecutionThreadIsInterrupted() throws InterruptedException {
        TaskEntity task = saveTask(
                "Interrupted execution task",
                TaskStatus.IN_PROGRESS,
                5000L
        );

        Thread executionThread = new Thread(() -> taskExecutionService.execute(new TaskExecutionLease(task.getId(), 1)));

        executionThread.start();
        Thread.sleep(100);
        executionThread.interrupt();
        executionThread.join(2000);

        assertThat(executionThread.isAlive()).isFalse();

        TaskEntity returnedTask = taskRepository.findById(task.getId()).orElseThrow();

        assertThat(returnedTask.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(returnedTask.getProgress()).isZero();
        assertThat(returnedTask.getErrorMessage()).isNull();
        assertThat(returnedTask.getStartedAt()).isNull();
        assertThat(returnedTask.getCompletedAt()).isNull();
        assertThat(returnedTask.getUpdatedAt()).isNotNull();
    }

    private TaskEntity saveTask(String name, TaskStatus status, Long durationMs) {
        Instant now = Instant.now();

        TaskEntity task = TaskEntity.builder()
                .name(name)
                .durationMs(durationMs)
                .status(status)
                .progress(0)
                .result("Task execution started")
                .errorMessage(null)
                .attemptCount(1)
                .createdAt(now)
                .startedAt(now)
                .completedAt(null)
                .updatedAt(now)
                .build();

        return taskRepository.saveAndFlush(task);
    }
}