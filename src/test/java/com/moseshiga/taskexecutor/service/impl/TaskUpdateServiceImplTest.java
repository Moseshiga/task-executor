package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.exception.TaskNotFoundException;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskUpdateServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Test
    void updateProgressShouldUpdateProgressResultAndUpdatedAt() {
        TaskUpdateServiceImpl taskUpdateService = new TaskUpdateServiceImpl(taskRepository);

        TaskEntity task = baseTask();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskUpdateService.updateProgress(1L, 50, "Task execution progress: 50%");

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(captor.capture());

        TaskEntity savedTask = captor.getValue();

        assertThat(savedTask.getProgress()).isEqualTo(50);
        assertThat(savedTask.getResult()).isEqualTo("Task execution progress: 50%");
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(savedTask.getUpdatedAt()).isNotNull();
    }

    @Test
    void completeShouldMarkTaskAsCompleted() {
        TaskUpdateServiceImpl taskUpdateService = new TaskUpdateServiceImpl(taskRepository);

        TaskEntity task = baseTask();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskUpdateService.complete(1L, "Task completed successfully");

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(captor.capture());

        TaskEntity savedTask = captor.getValue();

        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(savedTask.getProgress()).isEqualTo(100);
        assertThat(savedTask.getResult()).isEqualTo("Task completed successfully");
        assertThat(savedTask.getErrorMessage()).isNull();
        assertThat(savedTask.getCompletedAt()).isNotNull();
        assertThat(savedTask.getUpdatedAt()).isNotNull();
    }

    @Test
    void failShouldMarkTaskAsFailed() {
        TaskUpdateServiceImpl taskUpdateService = new TaskUpdateServiceImpl(taskRepository);

        TaskEntity task = baseTask();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskUpdateService.fail(1L, "Something went wrong");

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(captor.capture());

        TaskEntity savedTask = captor.getValue();

        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(savedTask.getErrorMessage()).isEqualTo("Something went wrong");
        assertThat(savedTask.getCompletedAt()).isNotNull();
        assertThat(savedTask.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateProgressShouldThrowTaskNotFoundExceptionWhenTaskDoesNotExist() {
        TaskUpdateServiceImpl taskUpdateService = new TaskUpdateServiceImpl(taskRepository);

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                taskUpdateService.updateProgress(999L, 50, "Progress")
        )
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task with id 999 was not found");
    }

    private TaskEntity baseTask() {
        Instant now = Instant.now();

        return TaskEntity.builder()
                .id(1L)
                .name("Test task")
                .durationMs(5000L)
                .status(TaskStatus.IN_PROGRESS)
                .progress(0)
                .result("Task execution started")
                .errorMessage(null)
                .attemptCount(1)
                .version(0L)
                .createdAt(now)
                .startedAt(now)
                .completedAt(null)
                .updatedAt(now)
                .build();
    }
}