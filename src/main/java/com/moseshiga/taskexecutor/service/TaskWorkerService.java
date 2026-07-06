package com.moseshiga.taskexecutor.service;

import java.util.Optional;

public interface TaskWorkerService {
    Optional<TaskExecutionLease> pickNextTask();
}