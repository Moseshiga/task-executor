package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.dto.TaskResponseDto;
import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.exception.TaskNotFoundException;
import com.moseshiga.taskexecutor.mapper.TaskMapper;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskQueryServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    private final TaskMapper taskMapper = new TaskMapper();

    @Test
    void getByIdShouldReturnTaskResponseDtoWhenTaskExists() {
        TaskQueryServiceImpl taskQueryService = new TaskQueryServiceImpl(taskRepository, taskMapper);

        Instant now = Instant.now();

        TaskEntity task = TaskEntity.builder()
                .id(1L)
                .name("Test task")
                .durationMs(5000L)
                .status(TaskStatus.COMPLETED)
                .progress(100)
                .statusMessage("Task completed successfully")
                .result("Task completed successfully")
                .errorMessage(null)
                .attemptCount(1)
                .version(0L)
                .createdAt(now)
                .startedAt(now)
                .completedAt(now)
                .updatedAt(now)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponseDto responseDto = taskQueryService.getById(1L);

        assertThat(responseDto.id()).isEqualTo(1L);
        assertThat(responseDto.name()).isEqualTo("Test task");
        assertThat(responseDto.durationMs()).isEqualTo(5000L);
        assertThat(responseDto.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(responseDto.progress()).isEqualTo(100);
        assertThat(responseDto.statusMessage()).isEqualTo("Task completed successfully");
        assertThat(responseDto.result()).isEqualTo("Task completed successfully");
        assertThat(responseDto.errorMessage()).isNull();
        assertThat(responseDto.attemptCount()).isEqualTo(1);
    }

    @Test
    void getByIdShouldThrowTaskNotFoundExceptionWhenTaskDoesNotExist() {
        TaskQueryServiceImpl taskQueryService = new TaskQueryServiceImpl(taskRepository, taskMapper);

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskQueryService.getById(999L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task with id 999 was not found");
    }
}
