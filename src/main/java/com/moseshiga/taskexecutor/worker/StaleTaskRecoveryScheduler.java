package com.moseshiga.taskexecutor.worker;

import com.moseshiga.taskexecutor.service.StaleTaskRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "task-execution.recovery-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class StaleTaskRecoveryScheduler {
    private final StaleTaskRecoveryService staleTaskRecoveryService;

    @Scheduled(fixedDelayString = "${task-execution.recovery-delay-ms}")
    public void recoverStaleTasks() {
        int recoveredCount = staleTaskRecoveryService.recoverStaleTasks();

        if (recoveredCount > 0) {
            log.warn("Stale task recovery finished: recoveredCount={}", recoveredCount);
        }
    }
}