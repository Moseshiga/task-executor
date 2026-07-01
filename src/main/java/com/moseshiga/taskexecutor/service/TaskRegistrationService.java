package com.moseshiga.taskexecutor.service;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.entity.TaskEntity;

public interface TaskRegistrationService {
    TaskEntity register(TaskRequestDto requestDto);
}
