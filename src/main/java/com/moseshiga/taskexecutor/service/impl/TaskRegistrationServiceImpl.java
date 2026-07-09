package com.moseshiga.taskexecutor.service.impl;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.entity.TaskEntity;
import com.moseshiga.taskexecutor.mapper.TaskMapper;
import com.moseshiga.taskexecutor.repository.TaskRepository;
import com.moseshiga.taskexecutor.service.TaskRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskRegistrationServiceImpl implements TaskRegistrationService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Transactional
    @Override
    public TaskEntity register(TaskRequestDto requestDto) {
        TaskEntity task = taskMapper.toEntity(requestDto);
        TaskEntity savedTask = taskRepository.save(task);

        log.info(
                "Task registered: id={}, name={}, durationMs={}",
                savedTask.getId(),
                savedTask.getName(),
                savedTask.getDurationMs()
        );

        return savedTask;
    }
}
