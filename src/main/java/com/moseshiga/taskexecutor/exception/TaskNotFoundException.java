package com.moseshiga.taskexecutor.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long taskId) {
        super("Task with id " + taskId + " was not found");
    }
}