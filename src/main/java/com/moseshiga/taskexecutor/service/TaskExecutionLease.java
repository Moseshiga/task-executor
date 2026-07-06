package com.moseshiga.taskexecutor.service;

public record TaskExecutionLease(
        Long taskId,
        int attemptCount
) {
}