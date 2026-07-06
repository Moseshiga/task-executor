package com.moseshiga.taskexecutor.integration;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.TaskExecutionLease;
import com.moseshiga.taskexecutor.service.TaskExecutionService;
import com.moseshiga.taskexecutor.service.TaskRegistrationService;
import com.moseshiga.taskexecutor.service.TaskWorkerService;
import com.moseshiga.taskexecutor.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TaskLifecycleIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskRegistrationService taskRegistrationService;

    @Autowired
    private TaskWorkerService taskWorkerService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void taskShouldPassFullLifecycleFromRegistrationToCompletion() {
        TaskRequestDto requestDto = new TaskRequestDto(
                "Full lifecycle task",
                20L
        );

        TaskEntity registeredTask = taskRegistrationService.register(requestDto);

        TaskEntity newTask = taskRepository.findById(registeredTask.getId()).orElseThrow();

        assertThat(newTask.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(newTask.getProgress()).isZero();
        assertThat(newTask.getAttemptCount()).isZero();
        assertThat(newTask.getStartedAt()).isNull();
        assertThat(newTask.getCompletedAt()).isNull();

        Optional<TaskExecutionLease> pickedTask = taskWorkerService.pickNextTask();

        assertThat(pickedTask).hasValueSatisfying(lease -> assertThat(lease.taskId()).isEqualTo(registeredTask.getId()));

        TaskEntity inProgressTask = taskRepository.findById(registeredTask.getId()).orElseThrow();

        assertThat(inProgressTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(inProgressTask.getAttemptCount()).isEqualTo(1);
        assertThat(inProgressTask.getStartedAt()).isNotNull();
        assertThat(inProgressTask.getResult()).isEqualTo("Task execution started");

        taskExecutionService.execute(pickedTask.orElseThrow());

        TaskEntity completedTask = taskRepository.findById(registeredTask.getId()).orElseThrow();

        assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completedTask.getProgress()).isEqualTo(100);
        assertThat(completedTask.getResult()).isEqualTo("Task completed successfully");
        assertThat(completedTask.getErrorMessage()).isNull();
        assertThat(completedTask.getAttemptCount()).isEqualTo(1);
        assertThat(completedTask.getStartedAt()).isNotNull();
        assertThat(completedTask.getCompletedAt()).isNotNull();
        assertThat(completedTask.getUpdatedAt()).isNotNull();
    }
}