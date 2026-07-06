package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.enums.TaskStatus;
import com.moseshiga.taskexecutor.exception.TaskNotFoundException;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.TaskExecutionLease;
import com.moseshiga.taskexecutor.service.TaskExecutionService;
import com.moseshiga.taskexecutor.service.TaskUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionServiceImpl implements TaskExecutionService {
    private static final int PROGRESS_STEPS = 4;

    private final TaskRepository taskRepository;
    private final TaskUpdateService taskUpdateService;

    @Override
    public void execute(TaskExecutionLease lease) {
        Long taskId = lease.taskId();
        int attemptCount = lease.attemptCount();

        try {
            TaskEntity task = findTask(taskId);

            if (!isCurrentLease(task, attemptCount)) {
                log.warn(
                        "Skipping stale task execution lease: id={}, expectedAttempt={}, actualAttempt={}, status={}",
                        taskId,
                        attemptCount,
                        task.getAttemptCount(),
                        task.getStatus()
                );
                return;
            }

            log.info("Task execution started: id={}, attemptCount={}, durationMs={}", taskId, attemptCount, task.getDurationMs());

            boolean leaseStillCurrent = executeWithProgress(task, attemptCount);

            if (leaseStillCurrent) {
                taskUpdateService.complete(
                        taskId,
                        attemptCount,
                        "Task completed successfully"
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskUpdateService.returnToNew(
                    taskId,
                    attemptCount,
                    "Task returned to NEW because execution was interrupted"
            );
        } catch (Exception e) {
            log.error("Task execution failed: id={}, attemptCount={}", taskId, attemptCount, e);

            taskUpdateService.fail(
                    taskId,
                    attemptCount,
                    "Task execution failed: " + e.getMessage()
            );
        }
    }

    private boolean isCurrentLease(TaskEntity task, int attemptCount) {
        return task.getStatus() == TaskStatus.IN_PROGRESS
                && task.getAttemptCount() != null
                && task.getAttemptCount() == attemptCount;
    }

    private TaskEntity findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private boolean executeWithProgress(TaskEntity task, int attemptCount) throws InterruptedException {
        long durationMs = task.getDurationMs();
        long stepDurationMs = Math.max(durationMs / PROGRESS_STEPS, 1);

        for (int step = 1; step <= PROGRESS_STEPS; step++) {
            Thread.sleep(stepDurationMs);

            int progress = step * 100 / PROGRESS_STEPS;

            boolean updated = taskUpdateService.updateProgress(
                    task.getId(),
                    attemptCount,
                    progress,
                    "Task execution progress: " + progress + "%"
            );

            if (!updated) {
                log.warn("Stopping execution because task lease is no longer current: id={}, attemptCount={}", task.getId(), attemptCount);
                return false;
            }
        }

        return true;
    }
}