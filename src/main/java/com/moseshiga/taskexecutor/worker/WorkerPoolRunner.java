package com.moseshiga.taskexecutor.worker;

import com.moseshiga.taskexecutor.service.TaskExecutionService;
import com.moseshiga.taskexecutor.service.TaskWorkerService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "worker.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WorkerPoolRunner implements ApplicationRunner {
    private final ExecutorService workerExecutorService;
    private final TaskWorkerService taskWorkerService;
    private final TaskExecutionService taskExecutionService;

    @Value("${worker.pool-size}")
    private int poolSize;

    @Value("${worker.poll-delay-ms}")
    private long pollDelayMs;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting task worker pool: poolSize={}, pollDelayMs={}", poolSize, pollDelayMs);

        for (int workerId = 1; workerId <= poolSize; workerId++) {
            TaskWorker worker = new TaskWorker(
                    workerId,
                    pollDelayMs,
                    taskWorkerService,
                    taskExecutionService
            );

            workerExecutorService.submit(worker);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping task worker pool");

        workerExecutorService.shutdown();

        try {
            if (!workerExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Task worker pool did not stop within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while stopping task worker pool");
        }
    }
}