package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.TaskExecutionLease;
import com.moseshiga.taskexecutor.service.TaskWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskWorkerServiceImpl implements TaskWorkerService {

    private final TaskRepository taskRepository;

    @Override
    @Transactional
    public Optional<TaskExecutionLease> pickNextTask() {
        return taskRepository.findNextNewTaskForUpdate()
                .map(this::markAsInProgress);
    }

    private TaskExecutionLease markAsInProgress(TaskEntity task) {
        Instant now = Instant.now();
        int nextAttemptCount = task.getAttemptCount() + 1;

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartedAt(now);
        task.setUpdatedAt(now);
        task.setAttemptCount(nextAttemptCount);
        task.setResult("Task execution started");
        task.setErrorMessage(null);

        TaskEntity savedTask = taskRepository.save(task);

        log.info(
                "Task picked for execution: id={}, attemptCount={}",
                savedTask.getId(),
                savedTask.getAttemptCount()
        );

        return new TaskExecutionLease(savedTask.getId(), savedTask.getAttemptCount());
    }
}