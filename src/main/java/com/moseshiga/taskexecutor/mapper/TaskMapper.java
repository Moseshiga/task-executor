package com.moseshiga.taskexecutor.mapper;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.dto.TaskResponseDto;
import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TaskMapper {
    public TaskEntity toEntity(TaskRequestDto dto) {
        Instant now = Instant.now();

        return TaskEntity.builder()
                .name(dto.name())
                .durationMs(dto.durationMs())
                .status(TaskStatus.NEW)
                .progress(0)
                .result(null)
                .errorMessage(null)
                .attemptCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public TaskResponseDto toResponseDto(TaskEntity entity) {
        return new TaskResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getDurationMs(),
                entity.getStatus(),
                entity.getProgress(),
                entity.getResult(),
                entity.getErrorMessage(),
                entity.getAttemptCount(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getUpdatedAt()
        );
    }
}
