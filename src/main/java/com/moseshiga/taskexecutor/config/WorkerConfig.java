package com.moseshiga.taskexecutor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class WorkerConfig {
    @Bean
    public ExecutorService workerExecutorService(
            @Value("${worker.pool-size}") int poolSize
    ) {
        return Executors.newFixedThreadPool(poolSize);
    }
}
