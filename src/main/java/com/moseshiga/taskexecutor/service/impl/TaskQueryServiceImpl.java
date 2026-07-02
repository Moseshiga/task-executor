package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.dto.TaskResponseDto;
import com.moseshiga.taskexecutor.exception.TaskNotFoundException;
import com.moseshiga.taskexecutor.mapper.TaskMapper;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskQueryServiceImpl implements TaskQueryService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDto getById(Long id) {
        return taskRepository.findById(id)
                .map(taskMapper::toResponseDto)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
