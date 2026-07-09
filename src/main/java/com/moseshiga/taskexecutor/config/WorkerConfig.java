package com.moseshiga.taskexecutor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class WorkerConfig {
    @Bean
    public ExecutorService workerExecutorService(
            @Value("${worker.pool-size}") int poolSize
    ) {
        AtomicInteger counter = new AtomicInteger(1);

        return Executors.newFixedThreadPool(poolSize, runnable ->
                new Thread(runnable, "task-worker-" + counter.getAndIncrement())
        );
    }
}
