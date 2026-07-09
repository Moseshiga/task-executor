package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.mapper.TaskMapper;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskRegistrationServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    private final TaskMapper taskMapper = new TaskMapper();

    @InjectMocks
    private TaskRegistrationServiceImpl taskRegistrationService;

    @Test
    void registerShouldSaveTaskWithNewStatus() {
        taskRegistrationService = new TaskRegistrationServiceImpl(taskRepository, taskMapper);

        TaskRequestDto requestDto = new TaskRequestDto("Test task", 5000L);

        when(taskRepository.save(org.mockito.ArgumentMatchers.any(TaskEntity.class)))
                .thenAnswer(invocation -> {
                    TaskEntity task = invocation.getArgument(0);
                    task.setId(1L);
                    return task;
                });

        TaskEntity savedTask = taskRegistrationService.register(requestDto);

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(captor.capture());

        TaskEntity capturedTask = captor.getValue();

        assertThat(capturedTask.getName()).isEqualTo("Test task");
        assertThat(capturedTask.getDurationMs()).isEqualTo(5000L);
        assertThat(capturedTask.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(capturedTask.getProgress()).isZero();
        assertThat(capturedTask.getAttemptCount()).isZero();

        assertThat(savedTask.getId()).isEqualTo(1L);
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.NEW);
    }
}
