# Task Executor Service

Service for asynchronous distributed task execution.

The service receives task messages from Kafka, stores them in PostgreSQL, processes them asynchronously using a configurable worker pool, and exposes task status/result through a REST API.

## Features

* Register tasks from Kafka messages
* Store tasks in PostgreSQL
* Execute tasks asynchronously using worker threads
* Persist intermediate progress, status messages, and final result
* Query task status by id through REST API
* Support multiple application instances sharing the same database
* Prevent duplicate task execution using PostgreSQL `FOR UPDATE SKIP LOCKED`
* Guard task updates with an execution lease based on `taskId` and `attemptCount`
* Recover stale `IN_PROGRESS` tasks using `updated_at` as a heartbeat timestamp
* Retry Kafka processing failures and publish exhausted records to `tasks.DLT`
* Manage JPA entity timestamps with Spring Data Auditing
* Validate incoming task data
* Document REST API with Swagger/OpenAPI
* Run PostgreSQL, Kafka, Kafka UI, and the application through Docker Compose
* Cover core logic with unit and integration tests

## Tech Stack

* Java 21
* Spring Boot 3.5.x
* Spring Web
* Spring Data JPA
* PostgreSQL
* Kafka
* Flyway
* Bean Validation
* Swagger/OpenAPI via springdoc-openapi
* Docker / Docker Compose
* JUnit 5
* Mockito
* Testcontainers

## Architecture Overview

```text
Kafka topic: tasks
        |
        v
TaskKafkaConsumer
        |
        | valid message saved successfully
        v
TaskRegistrationService
        |
        v
PostgreSQL: tasks table, status = NEW
        |
        v
Worker pool
        |
        v
TaskWorkerService
        |
        v
SELECT ... FOR UPDATE SKIP LOCKED
        |
        v
status = IN_PROGRESS, attemptCount incremented
        |
        v
TaskExecutionLease(taskId, attemptCount)
        |
        v
TaskExecutionService
        |
        v
conditional progress / complete / fail updates
        |
        v
COMPLETED / FAILED
        |
        v
REST API: GET /api/tasks/{id}

Invalid Kafka messages or records that still fail after retry:

tasks -> retry -> tasks.DLT
```

## Task Lifecycle

```text
NEW
 |
 | worker picked task
 v
IN_PROGRESS
 |
 | success
 v
COMPLETED

NEW
 |
 | worker picked task
 v
IN_PROGRESS
 |
 | execution error
 v
FAILED

IN_PROGRESS
 |
 | stale heartbeat timeout / app crash
 v
NEW or FAILED
```

A task is first saved with status `NEW`.

A worker picks the next available task using PostgreSQL row locking:

```sql
SELECT *
FROM tasks
WHERE status = 'NEW'
ORDER BY id
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

This allows multiple application instances to safely work with the same database without processing the same task twice.

When a worker claims a task, it increments `attemptCount` and receives a `TaskExecutionLease`.

## Stale Task Recovery

If an application instance crashes after setting a task to `IN_PROGRESS`, the task could otherwise stay stuck forever.

The recovery scheduler periodically finds stale `IN_PROGRESS` tasks. A task is considered stale when its `updated_at` timestamp is older than the configured timeout.

`updated_at` is used as a heartbeat timestamp. Progress updates refresh it, so a long-running healthy task is not recovered only because its `started_at` value is old.

If the task has not reached the maximum attempt count, it is returned to `NEW`.

If the maximum attempt count is reached, it is marked as `FAILED`.

```text
IN_PROGRESS with old updated_at
    |
    | attemptCount < maxAttempts
    v
NEW

IN_PROGRESS with old updated_at
    |
    | attemptCount >= maxAttempts
    v
FAILED
```

Recovery also uses `FOR UPDATE SKIP LOCKED`, so multiple instances can run recovery safely.

`created_at` and JPA-managed `updated_at` values are populated by Spring Data Auditing. Native conditional updates still set `updated_at` explicitly because this timestamp is part of the worker heartbeat and stale recovery algorithm.

## Execution Lease

When a worker picks a task, it receives an execution lease:

```java
TaskExecutionLease(taskId, attemptCount)
```

The `attemptCount` acts as a lightweight execution token.

All progress, completion, failure, and retry-release updates are conditional on:

```text
id = taskId
attempt_count = attemptCount
status = IN_PROGRESS
```

This prevents an old worker from overwriting a newer retry attempt after stale recovery has returned a task to `NEW` and another worker has picked it again.

## Project Structure

```text
src/main/java/com/moseshiga/taskexecutor
 ├── config
 │   └── WorkerConfig.java
 ├── controller
 │   └── TaskController.java
 ├── dto
 │   ├── ErrorResponseDto.java
 │   ├── TaskRequestDto.java
 │   └── TaskResponseDto.java
 ├── entity
 │   └── TaskEntity.java
 ├── enums
 │   └── TaskStatus.java
 ├── exception
 │   ├── GlobalExceptionHandler.java
 │   └── TaskNotFoundException.java
 ├── kafka
 │   └── TaskKafkaConsumer.java
 ├── mapper
 │   └── TaskMapper.java
 ├── repository
 │   └── TaskRepository.java
 ├── service
 │   ├── StaleTaskRecoveryService.java
 │   ├── TaskExecutionService.java
 │   ├── TaskQueryService.java
 │   ├── TaskRegistrationService.java
 │   ├── TaskUpdateService.java
 │   ├── TaskWorkerService.java
 │   └── impl
 │       ├── StaleTaskRecoveryServiceImpl.java
 │       ├── TaskExecutionServiceImpl.java
 │       ├── TaskQueryServiceImpl.java
 │       ├── TaskRegistrationServiceImpl.java
 │       ├── TaskUpdateServiceImpl.java
 │       └── TaskWorkerServiceImpl.java
 └── worker
     ├── StaleTaskRecoveryScheduler.java
     ├── TaskWorker.java
     └── WorkerPoolRunner.java
```

## Configuration

Main configuration is located in:

```text
src/main/resources/application.properties
```

Important properties:

```properties
server.port=8080

spring.datasource.url=jdbc:postgresql://localhost:5432/taskdb
spring.datasource.username=user
spring.datasource.password=password

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=task-executor-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.listener.ack-mode=manual
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

task.kafka.topic=tasks

worker.enabled=true
worker.pool-size=4
worker.poll-delay-ms=1000

task-execution.max-attempts=3
task-execution.stale-timeout-ms=600000
task-execution.recovery-delay-ms=60000
task-execution.recovery-enabled=true
```

In Docker Compose, datasource and Kafka bootstrap server values are overridden through environment variables.

## Database Migration

Database schema is created by Flyway.

Migration location:

```text
src/main/resources/db/migration
```

Migrations:

```text
V1__create_tasks_table.sql
V2__add_status_message_to_tasks.sql
V3__drop_task_version.sql
```

Hibernate is configured with:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This means Hibernate validates the schema, but Flyway owns schema creation.

## Running Locally with Docker Compose

Build and start all services:

```bash
docker compose up --build
```

Detached mode:

```bash
docker compose up --build -d
```

Stop services:

```bash
docker compose down
```

Stop services and remove volumes:

```bash
docker compose down -v
```

Use `down -v` only if you want to delete PostgreSQL data.

## Services

| Service     | URL / Port                            |
| ----------- | ------------------------------------- |
| Application | http://localhost:8080                 |
| Swagger UI  | http://localhost:8080/swagger-ui.html |
| Kafka UI    | http://localhost:8090                 |
| PostgreSQL  | localhost:5432                        |
| Kafka       | localhost:9092                        |

## Kafka Topics

Default input topic:

```text
tasks
```

Dead-letter topic:

```text
tasks.DLT
```

Both topics are created by the `kafka-init` service in Docker Compose.

## Kafka Error Handling

The consumer uses manual acknowledgment.

Valid messages are acknowledged only after the task is successfully saved to PostgreSQL.

Invalid messages and infrastructure failures are rethrown to Spring Kafka. `DefaultErrorHandler` applies the configured retry policy and delegates exhausted records to `DeadLetterPublishingRecoverer`.

The dead-letter topic is derived from the original topic name:

```text
<source-topic>.DLT
```

For the default input topic, failed records are published to:

```text
tasks.DLT
```

Current retry policy:

```text
3 retry attempts
1 second fixed backoff between attempts
```

If all retry attempts are exhausted, the original Kafka record is published to `tasks.DLT`. The consumer does not publish to DLT manually.

## Producing a Task Message

### Option 1: Kafka UI

Open:

```text
http://localhost:8090
```

Go to:

```text
Topics → tasks → Produce Message
```

Message value:

```json
{
  "name": "Test task from Kafka UI",
  "durationMs": 10000
}
```

Key can be empty.

### Option 2: Console Producer

Start producer:

```bash
docker exec -i task-kafka kafka-console-producer \
  --bootstrap-server kafka:29092 \
  --topic tasks
```

Send message:

```json
{"name":"Test task from console","durationMs":10000}
```

Press Enter.

Exit producer:

```text
Ctrl + C
```

## REST API

### Get task by id

```http
GET /api/tasks/{id}
```

Example:

```bash
curl http://localhost:8080/api/tasks/1
```

Example response:

```json
{
  "id": 1,
  "name": "Test task from Kafka UI",
  "durationMs": 10000,
  "status": "COMPLETED",
  "progress": 100,
  "statusMessage": "Task completed successfully",
  "result": "Task completed successfully",
  "errorMessage": null,
  "attemptCount": 1,
  "createdAt": "2026-07-06T10:00:00Z",
  "startedAt": "2026-07-06T10:00:01Z",
  "completedAt": "2026-07-06T10:00:11Z",
  "updatedAt": "2026-07-06T10:00:11Z"
}
```

Possible statuses:

```text
NEW
IN_PROGRESS
COMPLETED
FAILED
```

## Swagger

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

## Horizontal Scaling

The application can be scaled to multiple instances.

Before scaling, the `app` service in `docker-compose.yml` should not use a fixed `container_name`, and the application port should not be bound as `8080:8080` for every replica.

Run 3 application instances:

```bash
docker compose up --build --scale app=3
```

Check running containers:

```bash
docker compose ps
```

Follow logs from all application instances:

```bash
docker compose logs -f app
```

Expected behavior:

```text
- Kafka messages are registered as tasks.
- Workers from multiple app instances pick different tasks.
- The same task id is not processed by two workers.
- PostgreSQL SKIP LOCKED prevents duplicate execution.
```

## Kafka Key and Task Name

Task `name` is not unique.

Multiple tasks can have the same name:

```json
{"name":"Report generation","durationMs":5000}
{"name":"Report generation","durationMs":10000}
```

They are still different tasks because the database `id` is the real task identifier.

Kafka key is not required for the current project.

Current flow:

```text
Kafka key/partition
    ↓
message registration in DB
    ↓
worker execution from PostgreSQL
```

Task execution order is controlled by PostgreSQL workers, not by Kafka partition order.

## Delivery and Idempotency Notes

The consumer commits Kafka offset manually after successful processing:

```text
message received
    |
    v
validation
    |
    v
task saved to DB
    |
    v
acknowledge offset
```

Invalid messages are handled separately:

```text
invalid message
    |
    v
retry with fixed backoff
    |
    v
publish to tasks.DLT if retries are exhausted
```

Infrastructure failures are not acknowledged immediately:

```text
database / transaction error
    |
    v
retry with fixed backoff
    |
    v
publish to tasks.DLT if retries are exhausted
```

If the application crashes after saving the task but before acknowledging the Kafka message, the same Kafka message may be consumed again after restart.

A production-grade version should add idempotency, for example:

```json
{
  "externalTaskId": "a1778e3c-12a1-4a1f-a3e5-9282d5f57db5",
  "name": "Test task",
  "durationMs": 10000
}
```

And a database unique constraint:

```sql
external_task_id UUID UNIQUE
```

This is intentionally not implemented in the current version because the task specification only requires name and duration as task input.

## Running Tests

Run all tests:

```bash
mvn clean test
```

Run a specific test:

```bash
mvn -Dtest=TaskRepositoryIntegrationTest test
```

Integration tests use Testcontainers PostgreSQL.

Docker must be running before running integration tests.

## Test Coverage

The project contains tests for:

```text
TaskMapper
TaskRegistrationService
TaskQueryService
TaskUpdateService
TaskController
TaskKafkaConsumer
TaskRepository with PostgreSQL
StaleTaskRecoveryService
TaskWorkerService
TaskExecutionService
Full task lifecycle
```

Important PostgreSQL-specific behavior is tested with Testcontainers:

```text
FOR UPDATE SKIP LOCKED
stale task recovery through native SQL
```

## Common Commands

Build project:

```bash
mvn clean package -DskipTests
```

Run tests:

```bash
mvn clean test
```

Start Docker environment:

```bash
docker compose up --build
```

Stop Docker environment:

```bash
docker compose down
```

View application logs:

```bash
docker compose logs -f app
```

View Kafka logs:

```bash
docker logs -f task-kafka
```

View PostgreSQL logs:

```bash
docker logs -f task-postgres
```

## Common Issues

### Port is already in use

Check whether one of these ports is already occupied:

```text
8080
5432
9092
8090
```

Stop conflicting services or change ports in `docker-compose.yml`.

### PostgreSQL connection refused

Check that PostgreSQL is healthy:

```bash
docker compose ps
```

Application inside Docker must use:

```text
jdbc:postgresql://postgres:5432/taskdb
```

not:

```text
jdbc:postgresql://localhost:5432/taskdb
```

### Kafka connection refused

Application inside Docker must use:

```text
kafka:29092
```

Local tools can use:

```text
localhost:9092
```

### Testcontainers cannot start

Make sure Docker Desktop is running.

Error example:

```text
Could not find a valid Docker environment
```

Start Docker Desktop and run tests again.

## Current Limitations

The current version intentionally does not include:

```text
Security
Redis
Admin panel
Prometheus/Grafana
CI/CD
User accounts
Task cancellation
Task priority
External task id idempotency
```

These can be added later as production improvements.

## Production Improvement Ideas

Possible next steps:

```text
Add idempotency with externalTaskId
Add task cancellation endpoint
Add task priority
Add metrics for worker pool and task execution time
Add Prometheus/Grafana
Add Spring Security
Add CI pipeline
Add more concurrency tests
```
