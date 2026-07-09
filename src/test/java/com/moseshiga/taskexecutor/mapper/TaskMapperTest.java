package com.moseshiga.taskexecutor.mapper;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.dto.TaskResponseDto;
import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMapperTest {

    private final TaskMapper taskMapper = new TaskMapper();

    @Test
    void toEntityShouldCreateNewTaskWithDefaultFields() {
        TaskRequestDto requestDto = new TaskRequestDto("Test task", 5000L);

        TaskEntity entity = taskMapper.toEntity(requestDto);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("Test task");
        assertThat(entity.getDurationMs()).isEqualTo(5000L);
        assertThat(entity.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(entity.getProgress()).isZero();
        assertThat(entity.getStatusMessage()).isEqualTo("Task registered");
        assertThat(entity.getResult()).isNull();
        assertThat(entity.getErrorMessage()).isNull();
        assertThat(entity.getAttemptCount()).isZero();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toResponseDtoShouldMapAllFields() {
        Instant now = Instant.now();

        TaskEntity entity = TaskEntity.builder()
                .id(1L)
                .name("Test task")
                .durationMs(5000L)
                .status(TaskStatus.COMPLETED)
                .progress(100)
                .statusMessage("Task completed successfully")
                .result("Task completed successfully")
                .errorMessage(null)
                .attemptCount(1)
                .createdAt(now)
                .startedAt(now)
                .completedAt(now)
                .updatedAt(now)
                .build();

        TaskResponseDto responseDto = taskMapper.toResponseDto(entity);

        assertThat(responseDto.id()).isEqualTo(1L);
        assertThat(responseDto.name()).isEqualTo("Test task");
        assertThat(responseDto.durationMs()).isEqualTo(5000L);
        assertThat(responseDto.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(responseDto.progress()).isEqualTo(100);
        assertThat(responseDto.statusMessage()).isEqualTo("Task completed successfully");
        assertThat(responseDto.result()).isEqualTo("Task completed successfully");
        assertThat(responseDto.errorMessage()).isNull();
        assertThat(responseDto.attemptCount()).isEqualTo(1);
        assertThat(responseDto.createdAt()).isEqualTo(now);
        assertThat(responseDto.startedAt()).isEqualTo(now);
        assertThat(responseDto.completedAt()).isEqualTo(now);
        assertThat(responseDto.updatedAt()).isEqualTo(now);
    }
}
