package com.moseshiga.taskexecutor.dto;

import com.moseshiga.taskexecutor.enums.TaskStatus;

import java.time.Instant;

public record TaskResponseDto(
        Long id,
        String name,
        Long durationMs,
        TaskStatus status,
        Integer progress,
        String result,
        String errorMessage,
        Integer attemptCount,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt
) {
}
