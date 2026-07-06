package com.moseshiga.taskexecutor.kafka;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.service.TaskRegistrationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskKafkaConsumer {
    private final TaskRegistrationService taskRegistrationService;
    private final Validator validator;
    private final KafkaTemplate<String, TaskRequestDto> kafkaTemplate;
    private final String deadLetterTopic;

    public TaskKafkaConsumer(
            TaskRegistrationService taskRegistrationService,
            Validator validator,
            KafkaTemplate<String, TaskRequestDto> kafkaTemplate,
            @Value("${task.kafka.dlt-topic}") String deadLetterTopic
    ) {
        this.taskRegistrationService = taskRegistrationService;
        this.validator = validator;
        this.kafkaTemplate = kafkaTemplate;
        this.deadLetterTopic = deadLetterTopic;
    }

    @KafkaListener(
            topics = "${task.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(TaskRequestDto requestDto, Acknowledgment acknowledgment) {
        try {
            validate(requestDto);
            taskRegistrationService.register(requestDto);
            acknowledgment.acknowledge();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid task message received, sending to DLT: {}", requestDto, e);
            kafkaTemplate.send(deadLetterTopic, requestDto);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to consume task message: {}", requestDto, e);
            throw e;
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