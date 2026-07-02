package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.exception.TaskNotFoundException;
import com.moseshiga.taskexecutor.repository.TaskRepository;
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
    public void execute(Long taskId) {
        try {
            TaskEntity task = findTask(taskId);

            log.info("Task execution started: id={}, durationMs={}", taskId, task.getDurationMs());

            executeWithProgress(task);

            taskUpdateService.complete(
                    taskId,
                    "Task completed successfully"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            taskUpdateService.fail(
                    taskId,
                    "Task execution was interrupted"
            );
        } catch (Exception e) {
            log.error("Task execution failed: id={}", taskId, e);

            taskUpdateService.fail(
                    taskId,
                    "Task execution failed: " + e.getMessage()
            );
        }
    }

    private TaskEntity findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private void executeWithProgress(TaskEntity task) throws InterruptedException {
        long durationMs = task.getDurationMs();
        long stepDurationMs = Math.max(durationMs / PROGRESS_STEPS, 1);

        for (int step = 1; step <= PROGRESS_STEPS; step++) {
            Thread.sleep(stepDurationMs);

            int progress = step * 100 / PROGRESS_STEPS;

            taskUpdateService.updateProgress(
                    task.getId(),
                    progress,
                    "Task execution progress: " + progress + "%"
            );
        }
    }
}
