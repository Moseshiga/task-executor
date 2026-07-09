package com.moseshiga.taskexecutor.kafka;

import com.moseshiga.taskexecutor.dto.TaskRequestDto;
import com.moseshiga.taskexecutor.service.TaskRegistrationService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void consumeShouldRethrowAndNotAcknowledgeWhenNameIsBlank() {
        TaskRequestDto requestDto = new TaskRequestDto(" ", 5000L);

        assertThatThrownBy(() -> taskKafkaConsumer.consume(requestDto, acknowledgment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid task message");

        verify(taskRegistrationService, never()).register(requestDto);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consumeShouldRethrowAndNotAcknowledgeWhenDurationIsNull() {
        TaskRequestDto requestDto = new TaskRequestDto("Invalid task", null);

        assertThatThrownBy(() -> taskKafkaConsumer.consume(requestDto, acknowledgment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid task message");

        verify(taskRegistrationService, never()).register(requestDto);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consumeShouldRethrowAndNotAcknowledgeWhenDurationIsNegative() {
        TaskRequestDto requestDto = new TaskRequestDto("Invalid task", -100L);

        assertThatThrownBy(() -> taskKafkaConsumer.consume(requestDto, acknowledgment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid task message");

        verify(taskRegistrationService, never()).register(requestDto);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consumeShouldRethrowAndNotAcknowledgeWhenRegistrationFails() {
        TaskRequestDto requestDto = new TaskRequestDto("Valid task", 5000L);

        RuntimeException databaseException = new RuntimeException("Database error");
        doThrow(databaseException)
                .when(taskRegistrationService)
                .register(requestDto);

        assertThatThrownBy(() -> taskKafkaConsumer.consume(requestDto, acknowledgment))
                .isSameAs(databaseException);

        verify(taskRegistrationService).register(requestDto);
        verify(acknowledgment, never()).acknowledge();
    }
}
