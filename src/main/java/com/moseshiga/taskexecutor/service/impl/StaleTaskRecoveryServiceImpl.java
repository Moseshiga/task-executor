package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.StaleTaskRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaleTaskRecoveryServiceImpl implements StaleTaskRecoveryService {

    private static final int RECOVERY_BATCH_SIZE = 100;

    private final TaskRepository taskRepository;

    @Value("${task-execution.max-attempts}")
    private int maxAttempts;

    @Value("${task-execution.stale-timeout-ms}")
    private long staleTimeoutMs;

    @Override
    @Transactional
    public int recoverStaleTasks() {
        Instant staleBefore = Instant.now().minusMillis(staleTimeoutMs);

        int recoveredCount = taskRepository.recoverStaleTasks(
                staleBefore,
                maxAttempts,
                RECOVERY_BATCH_SIZE
        );

        if (recoveredCount > 0) {
            log.warn(
                    "Stale task recovery completed: recoveredCount={}, staleBefore={}, maxAttempts={}",
                    recoveredCount,
                    staleBefore,
                    maxAttempts
            );
        }

        return recoveredCount;
    }
}
