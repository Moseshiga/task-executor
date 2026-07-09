package com.moseshiga.taskexecutor.mapper;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.dto.TaskResponseDto;
import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {
    public TaskEntity toEntity(TaskRequestDto dto) {
        return TaskEntity.builder()
                .name(dto.name())
                .durationMs(dto.durationMs())
                .status(TaskStatus.NEW)
                .progress(0)
                .statusMessage("Task registered")
                .attemptCount(0)
                .build();
    }

    public TaskResponseDto toResponseDto(TaskEntity entity) {
        return new TaskResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getDurationMs(),
                entity.getStatus(),
                entity.getProgress(),
                entity.getStatusMessage(),
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
