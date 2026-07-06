package com.moseshiga.taskexecutor.kafka;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.service.TaskRegistrationService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TaskKafkaConsumerTest {

    private TaskRegistrationService taskRegistrationService;
    private Acknowledgment acknowledgment;
    private TaskKafkaConsumer taskKafkaConsumer;

    @BeforeEach
    void setUp() {
        taskRegistrationService = mock(TaskRegistrationService.class);
        acknowledgment = mock(Acknowledgment.class);

        Validator validator = Validation.buildDefaultValidatorFactory()
                .getValidator();

        taskKafkaConsumer = new TaskKafkaConsumer(
                taskRegistrationService,
                validator
        );
    }

    @Test
    void consumeShouldRegisterTaskAndAcknowledgeWhenMessageIsValid() {
        TaskRequestDto requestDto = new TaskRequestDto("Valid task", 5000L);

        taskKafkaConsumer.consume(requestDto, acknowledgment);

        verify(taskRegistrationService).register(requestDto);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeShouldAcknowledgeAndNotRegisterTaskWhenNameIsBlank() {
        TaskRequestDto requestDto = new TaskRequestDto(" ", 5000L);

        taskKafkaConsumer.consume(requestDto, acknowledgment);

        verify(taskRegistrationService, never()).register(requestDto);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeShouldAcknowledgeAndNotRegisterTaskWhenDurationIsNull() {
        TaskRequestDto requestDto = new TaskRequestDto("Invalid task", null);

        taskKafkaConsumer.consume(requestDto, acknowledgment);

        verify(taskRegistrationService, never()).register(requestDto);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeShouldAcknowledgeAndNotRegisterTaskWhenDurationIsNegative() {
        TaskRequestDto requestDto = new TaskRequestDto("Invalid task", -100L);

        taskKafkaConsumer.consume(requestDto, acknowledgment);

        verify(taskRegistrationService, never()).register(requestDto);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeShouldAcknowledgeWhenRegistrationFails() {
        TaskRequestDto requestDto = new TaskRequestDto("Valid task", 5000L);

        doThrow(new RuntimeException("Database error"))
                .when(taskRegistrationService)
                .register(requestDto);

        taskKafkaConsumer.consume(requestDto, acknowledgment);

        verify(taskRegistrationService).register(requestDto);
        verify(acknowledgment).acknowledge();
    }
}