package com.moseshiga.taskexecutor.dto;

import com.moseshiga.taskexecutor.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record TaskResponseDto(
        Long id,
        String name,
        Long durationMs,
        TaskStatus status,
        @Schema(description = "Last saved execution progress. Failed tasks keep this value for diagnostics and possible resume logic.")
        Integer progress,
        @Schema(description = "Current readable task state or progress message.")
        String statusMessage,
        @Schema(description = "Final successful execution result. Intermediate progress messages are stored in statusMessage.")
        String result,
        String errorMessage,
        Integer attemptCount,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt
) {
}
