package com.moseshiga.taskexecutor.service;

public interface TaskUpdateService {
    void updateProgress(Long taskId, int progress, String result);

    void complete(Long taskId, String result);

    void fail(Long taskId, String errorMessage);
}