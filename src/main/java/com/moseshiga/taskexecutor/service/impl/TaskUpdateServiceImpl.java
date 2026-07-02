package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.exception.TaskNotFoundException;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.TaskUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskUpdateServiceImpl implements TaskUpdateService {
    private final TaskRepository taskRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgress(Long taskId, int progress, String result) {
        TaskEntity task = findTask(taskId);

        task.setProgress(progress);
        task.setResult(result);
        task.setUpdatedAt(Instant.now());

        taskRepository.save(task);

        log.debug("Task progress updated: id={}, progress={}", taskId, progress);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long taskId, String result) {
        Instant now = Instant.now();

        TaskEntity task = findTask(taskId);

        task.setStatus(TaskStatus.COMPLETED);
        task.setProgress(100);
        task.setResult(result);
        task.setErrorMessage(null);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);

        taskRepository.save(task);

        log.info("Task completed: id={}", taskId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long taskId, String errorMessage) {
        Instant now = Instant.now();

        TaskEntity task = findTask(taskId);

        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);

        taskRepository.save(task);

        log.warn("Task failed: id={}, error={}", taskId, errorMessage);
    }

    private TaskEntity findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
