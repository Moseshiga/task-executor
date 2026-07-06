package com.moseshiga.taskexecutor.service.impl;

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
    public boolean updateProgress(Long taskId, int attemptCount, int progress, String result) {
        int updatedRows = taskRepository.updateProgressIfCurrent(
                taskId,
                attemptCount,
                progress,
                result,
                Instant.now()
        );

        return handleConditionalUpdateResult(taskId, attemptCount, updatedRows, "progress update");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(Long taskId, int attemptCount, String result) {
        Instant now = Instant.now();

        int updatedRows = taskRepository.completeIfCurrent(
                taskId,
                attemptCount,
                result,
                now
        );

        boolean updated = handleConditionalUpdateResult(taskId, attemptCount, updatedRows, "completion");
        if (updated) {
            log.info("Task completed: id={}, attemptCount={}", taskId, attemptCount);
        }
        return updated;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean fail(Long taskId, int attemptCount, String errorMessage) {
        Instant now = Instant.now();

        int updatedRows = taskRepository.failIfCurrent(
                taskId,
                attemptCount,
                errorMessage,
                now
        );

        boolean updated = handleConditionalUpdateResult(taskId, attemptCount, updatedRows, "failure");
        if (updated) {
            log.warn("Task failed: id={}, attemptCount={}, error={}", taskId, attemptCount, errorMessage);
        }
        return updated;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean returnToNew(Long taskId, int attemptCount, String result) {

        int updatedRows = taskRepository.returnToNewIfCurrent(
                taskId,
                attemptCount,
                result,
                Instant.now()
        );

        boolean updated = handleConditionalUpdateResult(taskId, attemptCount, updatedRows, "return to NEW");
        if (updated) {
            log.info("Task returned to NEW: id={}, attemptCount={}", taskId, attemptCount);
        }
        return updated;
    }

    private boolean handleConditionalUpdateResult(Long taskId, int attemptCount, int updatedRows, String operation) {
        if (updatedRows == 1) {
            log.debug("Task {} accepted: id={}, attemptCount={}", operation, taskId, attemptCount);
            return true;
        }

        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }

        log.warn("Task {} skipped because lease is stale: id={}, attemptCount={}", operation, taskId, attemptCount);
        return false;
    }
}