package com.moseshiga.taskexecutor.controller;

import com.moseshiga.taskexecutor.dto.TaskResponseDto;
import com.moseshiga.taskexecutor.exception.ErrorResponseDto;
import com.moseshiga.taskexecutor.service.TaskQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
@Tag(name = "Task API", description = "API for reading asynchronous task execution results")
public class TaskController {

    private final TaskQueryService taskQueryService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Get task by id",
            description = "Returns current task status, last saved progress, status message, final result or error details."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task found",
                    content = @Content(schema = @Schema(implementation = TaskResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid task id",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    public ResponseEntity<TaskResponseDto> getTaskById(
            @PathVariable @Positive(message = "Task id must be positive") Long id
    ) {
        return ResponseEntity.ok(taskQueryService.getById(id));
    }
}
