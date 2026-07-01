package com.moseshiga.taskexecutor.entity;

import com.moseshiga.taskexecutor.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tasks")
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable task name received from Kafka.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Simulated task execution duration in milliseconds.
     */
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    /**
     * Current task execution status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;

    /**
     * Execution progress from 0 to 100.
     */
    @Column(name = "progress", nullable = false)
    private Integer progress;

    /**
     * Intermediate or final execution result.
     */
    @Column(name = "result", length = 1000)
    private String result;

    /**
     * Error details if task execution failed.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Number of times workers tried to execute this task.
     */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    /**
     * Additional optimistic version field.
     * Main worker distribution will still use FOR UPDATE SKIP LOCKED.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
