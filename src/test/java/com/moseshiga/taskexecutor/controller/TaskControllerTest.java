package com.moseshiga.taskexecutor.controller;

import com.moseshiga.taskexecutor.dto.TaskResponseDto;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.exception.GlobalExceptionHandler;
import com.moseshiga.taskexecutor.exception.TaskNotFoundException;
import com.moseshiga.taskexecutor.service.TaskQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskQueryService taskQueryService;

    @Test
    void getTaskByIdShouldReturnTaskWhenTaskExists() throws Exception {
        Instant now = Instant.parse("2026-07-06T10:00:00Z");

        TaskResponseDto responseDto = new TaskResponseDto(
                1L,
                "Test task",
                5000L,
                TaskStatus.COMPLETED,
                100,
                "Task completed successfully",
                "Task completed successfully",
                null,
                1,
                now,
                now,
                now,
                now
        );

        when(taskQueryService.getById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/tasks/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test task")))
                .andExpect(jsonPath("$.durationMs", is(5000)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.progress", is(100)))
                .andExpect(jsonPath("$.statusMessage", is("Task completed successfully")))
                .andExpect(jsonPath("$.result", is("Task completed successfully")))
                .andExpect(jsonPath("$.errorMessage").doesNotExist())
                .andExpect(jsonPath("$.attemptCount", is(1)));
    }

    @Test
    void getTaskByIdShouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        when(taskQueryService.getById(999L))
                .thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(get("/api/tasks/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Task with id 999 was not found")))
                .andExpect(jsonPath("$.path", is("/api/tasks/999")));
    }

    @Test
    void getTaskByIdShouldReturnBadRequestWhenIdIsNegative() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.path", is("/api/tasks/-1")));
    }

    @Test
    void getTaskByIdShouldReturnBadRequestWhenIdIsNotNumber() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid parameter value: id")))
                .andExpect(jsonPath("$.path", is("/api/tasks/abc")));
    }
}
