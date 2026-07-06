package com.moseshiga.taskexecutor.service;

public interface TaskExecutionService {
    void execute(TaskExecutionLease lease);
}