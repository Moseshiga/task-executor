package com.moseshiga.taskexecutor.service;

public interface TaskUpdateService {
    boolean updateProgress(Long taskId, int attemptCount, int progress, String result);

    boolean complete(Long taskId, int attemptCount, String result);

    boolean fail(Long taskId, int attemptCount, String errorMessage);

    boolean returnToNew(Long taskId, int attemptCount, String result);
}