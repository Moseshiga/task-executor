package com.moseshiga.taskexecutor.kafka;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.service.TaskRegistrationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskKafkaConsumer {
    private final TaskRegistrationService taskRegistrationService;
    private final Validator validator;

    @KafkaListener(
            topics = "${task.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(TaskRequestDto requestDto, Acknowledgment acknowledgment) {
        try {
            validate(requestDto);
            taskRegistrationService.register(requestDto);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to consume task message: {}", requestDto, e);

            /*
             * We acknowledge invalid or failed messages here to avoid endless re-reading
             * of the same poison message.
             */
            acknowledgment.acknowledge();
        }
    }

    private void validate(TaskRequestDto requestDto) {
        Set<ConstraintViolation<TaskRequestDto>> violations = validator.validate(requestDto);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining("; "));

            throw new IllegalArgumentException("Invalid task message: " + message);
        }
    }
}