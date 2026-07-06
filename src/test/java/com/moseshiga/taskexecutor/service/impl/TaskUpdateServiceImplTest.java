package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.exception.TaskNotFoundException;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskUpdateServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Test
    void updateProgressShouldUpdateProgressResultAndUpdatedAt() {
        TaskUpdateServiceImpl taskUpdateService = new TaskUpdateServiceImpl(taskRepository);

        when(taskRepository.updateProgressIfCurrent(eq(1L), eq(1), eq(50), eq("Task execution progress: 50%"), any(Instant.class)))
                .thenReturn(1);

        boolean updated = taskUpdateService.updateProgress(1L, 1, 50, "Task execution progress: 50%");

        assertThat(updated).isTrue();
        verify(taskRepository).updateProgressIfCurrent(eq(1L), eq(1), eq(50), eq("Task execution progress: 50%"), any(Instant.class));
    }

    @Test
    void completeShouldMarkTaskAsCompleted() {
        TaskUpdateServiceImpl taskUpdateService = new TaskUpdateServiceImpl(taskRepository);

        when(taskRepository.completeIfCurrent(eq(1L), eq(1), eq("Task completed successfully"), any(Instant.class)))
                .thenReturn(1);

        boolean updated = taskUpdateService.complete(1L, 1, "Task completed successfully");

        assertThat(updated).isTrue();
        verify(taskRepository).completeIfCurrent(eq(1L), eq(1), eq("Task completed successfully"), any(Instant.class));
    }

    @Test
    void failShouldMarkTaskAsFailed() {
        TaskUpdateServiceImpl taskUpdateService = new TaskUpdateServiceImpl(taskRepository);

        when(taskRepository.failIfCurrent(eq(1L), eq(1), eq("Something went wrong"), any(Instant.class)))
                .thenReturn(1);

        boolean updated = taskUpdateService.fail(1L, 1, "Something went wrong");

        assertThat(updated).isTrue();
        verify(taskRepository).failIfCurrent(eq(1L), eq(1), eq("Something went wrong"), any(Instant.class));
    }

    @Test
    void returnToNewShouldReleaseCurrentAttempt() {
        TaskUpdateServiceImpl taskUpdateService = new TaskUpdateServiceImpl(taskRepository);

        when(taskRepository.returnToNewIfCurrent(eq(1L), eq(1), eq("Interrupted"), any(Instant.class)))
                .thenReturn(1);

        boolean updated = taskUpdateService.returnToNew(1L, 1, "Interrupted");

        assertThat(updated).isTrue();
        verify(taskRepository).returnToNewIfCurrent(eq(1L), eq(1), eq("Interrupted"), any(Instant.class));
    }

    @Test
    void updateProgressShouldThrowTaskNotFoundExceptionWhenTaskDoesNotExist() {
        TaskUpdateServiceImpl taskUpdateService = new TaskUpdateServiceImpl(taskRepository);

        when(taskRepository.updateProgressIfCurrent(eq(999L), eq(1), eq(50), eq("Progress"), any(Instant.class))).thenReturn(0);
        when(taskRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() ->
                taskUpdateService.updateProgress(999L, 1, 50, "Progress")
        )
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task with id 999 was not found");
    }
}