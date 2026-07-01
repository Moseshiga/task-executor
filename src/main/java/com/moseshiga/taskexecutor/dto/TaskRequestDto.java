package com.moseshiga.taskexecutor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TaskRequestDto(
        @NotBlank(message = "Task name must not be blank")
        String name,

        @NotNull(message = "Task duration must not be null")
        @Positive(message = "Task duration must be positive")
        Long durationMs
) {
}
