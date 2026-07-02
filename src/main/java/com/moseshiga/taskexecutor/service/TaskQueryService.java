package com.moseshiga.taskexecutor.service;

import com.moseshiga.taskexecutor.dto.TaskResponseDto;

public interface TaskQueryService {
    TaskResponseDto getById(Long id);
}