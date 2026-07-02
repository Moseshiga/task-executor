package com.moseshiga.taskexecutor.worker;

import com.moseshiga.taskexecutor.service.TaskExecutionService;
import com.moseshiga.taskexecutor.service.TaskWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TaskWorker implements Runnable {
    private final int workerId;
    private final long pollDelayMs;
    private final TaskWorkerService taskWorkerService;
    private final TaskExecutionService taskExecutionService;

    @Override
    public void run() {
        log.info("Task worker started: workerId={}", workerId);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                processNextTaskOrWait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Task worker interrupted: workerId={}", workerId);
            } catch (Exception e) {
                log.error("Unexpected worker error: workerId={}", workerId, e);
            }
        }

        log.info("Task worker stopped: workerId={}", workerId);
    }

    private void processNextTaskOrWait() throws InterruptedException {
        taskWorkerService.pickNextTask()
                .ifPresentOrElse(
                        taskExecutionService::execute,
                        this::sleepBeforeNextPoll
                );
    }

    private void sleepBeforeNextPoll() {
        try {
            Thread.sleep(pollDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}