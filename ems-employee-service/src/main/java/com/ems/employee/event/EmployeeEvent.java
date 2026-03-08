package com.ems.employee.event;


import java.math.BigDecimal;
import java.time.Instant;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ EmployeeEvent — Immutable Event DTO for Kafka Messaging ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>OBSERVER PATTERN — THIS IS THE "EVENT" PASSED TO OBSERVERS</h2>
 *
 * <p>
 * In the classic Observer Pattern:
 * </p>
 * 
 * <pre>
 *   Subject (EmployeeService)          Observer (NotificationService)
 *       │                                      │
 *       │  notifyObservers(event)               │
 *       ├──────────────────────────────────────→│  onEvent(event) → store log
 *       │                                      │
 * </pre>
 *
 * <p>
 * Kafka transforms this into a <strong>distributed, durable</strong> version:
 * </p>
 * 
 * <pre>
 *   Producer (EmployeeService)   →   Kafka Topic   →   Consumer (NotificationService)
 *       │                             (broker)              │
 *       │  send("employee.created",event)                   │  @KafkaListener
 *       ├──────────────→  [persisted on disk]  ──────────→  │  onEmployeeEvent(event)
 *       │                                                   │  → store in MongoDB
 * </pre>
 *
 * <p>
 * <strong>This Record IS the event payload.</strong> It carries all information
 * needed by consumers without requiring them to query back to the producer.
 * </p>
 *
 * <h2>WHY JAVA 21 RECORD FOR EVENTS?</h2>
 * <ul>
 * <li><strong>Immutable:</strong> Once created, event data cannot be modified —
 * essential
 * for event sourcing integrity (events are facts, not mutable state)</li>
 * <li><strong>Serialization-friendly:</strong> Records serialize/deserialize
 * cleanly with
 * Jackson (Kafka's JsonSerializer uses Jackson under the hood)</li>
 * <li><strong>Self-documenting:</strong> The record components ARE the event
 * schema</li>
 * </ul>
 *
 * @param eventId      unique event identifier (UUID for idempotency)
 * @param eventType    the action that occurred: CREATED, UPDATED, DEACTIVATED
 * @param timestamp    when the event was produced (UTC)
 * @param employeeId   the affected employee's database ID
 * @param employeeName full name for consumer convenience (avoids back-query)
 * @param email        email address (for notification dispatch)
 * @param department   department name
 * @param jobTitle     job title
 * @param salary       base salary (nullable — excluded for non-salary events)
 * @param status       employee status after the event
 */
public record EmployeeEvent(
                String eventId,
                String eventType,
                Instant timestamp,
                String employeeId,
                String employeeName,
                String email,
                String department,
                String jobTitle,
                BigDecimal salary,
                String status) {
        /**
         * Factory for creating EMPLOYEE_CREATED events.
         */
        public static EmployeeEvent created(String eventId, String employeeId, String name,
                        String email, String department, String jobTitle,
                        BigDecimal salary) {
                return new EmployeeEvent(
                                eventId, "EMPLOYEE_CREATED", Instant.now(),
                                employeeId, name, email, department, jobTitle, salary, "ACTIVE");
        }

        /**
         * Factory for creating EMPLOYEE_UPDATED events.
         */
        public static EmployeeEvent updated(String eventId, String employeeId, String name,
                        String email, String department, String jobTitle,
                        BigDecimal salary, String status) {
                return new EmployeeEvent(
                                eventId, "EMPLOYEE_UPDATED", Instant.now(),
                                employeeId, name, email, department, jobTitle, salary, status);
        }

        /**
         * Factory for creating EMPLOYEE_DEACTIVATED events.
         */
        public static EmployeeEvent deactivated(String eventId, String employeeId, String name,
                        String email) {
                return new EmployeeEvent(
                                eventId, "EMPLOYEE_DEACTIVATED", Instant.now(),
                                employeeId, name, email, null, null, null, "INACTIVE");
        }
}
