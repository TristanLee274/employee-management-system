package com.java.practice.ems.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ EmployeeEventProducer — Kafka Producer (The "Subject" in Observer Pattern)║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>DEPENDENCY INVERSION & OBSERVER PATTERN</h2>
 *
 * <p>
 * This class implements the {@code EventPublisher} interface defined in the domain layer.
 * By doing so, the domain service only depends on the abstraction (EventPublisher) and
 * remains entirely decoupled from Spring Kafka and infrastructure concerns.
 * </p>
 *
 * <h2>OBSERVER PATTERN — KAFKA AS A DISTRIBUTED IMPLEMENTATION</h2>
 *
 * <p>
 * The classic Observer Pattern (Gang of Four) has a tight coupling problem:
 * </p>
 * 
 * <pre>
 *   ┌──────────────────┐      direct call     ┌──────────────────┐
 *   │  Subject         │────────────────────→  │  Observer         │
 *   │  (EmployeeService)│   observer.update()  │  (NotifService)   │
 *   └──────────────────┘                      └──────────────────┘
 *
 *   Problems:
 *   • Subject KNOWS about every Observer (tight coupling)
 *   • If Observer is slow/crashes, Subject is blocked/affected
 *   • Adding a new Observer requires modifying the Subject
 *   • All components must run in the SAME JVM process
 * </pre>
 *
 * <p>
 * <strong>Kafka solves ALL of these problems:</strong>
 * </p>
 * 
 * <pre>
 *   ┌──────────────────┐                     ┌──────────────────┐
 *   │  Producer         │   publish event     │  Kafka Broker     │
 *   │  (THIS CLASS)     │───────────────────→ │  (persistent log) │
 *   └──────────────────┘                     └────────┬─────────┘
 *                                                     │
 *                         ┌───────────────────────────┼──────────────────────┐
 *                         │                           │                      │
 *                    ┌────▼─────┐              ┌──────▼───────┐       ┌──────▼───────┐
 *                    │ Consumer │              │ Consumer      │       │ Consumer      │
 *                    │ Notif    │              │ Audit Log     │       │ Analytics     │
 *                    │ Service  │              │ Service       │       │ Service       │
 *                    └──────────┘              └──────────────┘       └──────────────┘
 *
 *   Benefits:
 *   • Producer does NOT know about consumers (fully decoupled)
 *   • If a consumer crashes, events are retained on Kafka — replay on recovery
 *   • Adding a new consumer requires ZERO changes to the producer (OCP!)
 *   • Producer and consumers can run in DIFFERENT JVMs, pods, or even data centers
 * </pre>
 *
 * <h2>MICROSERVICES COMMUNICATION — WHY ASYNC MESSAGING IMPROVES
 * RESILIENCE</h2>
 *
 * <p>
 * In a synchronous (REST) architecture:
 * </p>
 * 
 * <pre>
 *   EmployeeService → POST /notifications → NotificationService
 *                                           │
 *                                   (if NotifService is DOWN)
 *                                           │
 *                                   EmployeeService FAILS too!
 *                                   → cascading failure
 * </pre>
 *
 * <p>
 * With Kafka (asynchronous messaging):
 * </p>
 * 
 * <pre>
 *   EmployeeService → publish to Kafka → DONE (returns immediately)
 *                                        │
 *                               (if NotifService is DOWN)
 *                                        │
 *                               Events are RETAINED on Kafka disk
 *                               → when NotifService recovers,
 *                                 it replays from last offset
 *                               → ZERO data loss, ZERO cascading failure
 * </pre>
 *
 * <p>
 * <strong>This is why Kafka is the backbone of resilient
 * microservices.</strong>
 * The producer's availability is independent of the consumer's availability.
 * </p>
 *
 * <h2>EVENTUAL CONSISTENCY</h2>
 *
 * <p>
 * When the employee is created in PostgreSQL and the event is published to
 * Kafka,
 * there is a brief window where the employee exists in the database but the
 * notification log in MongoDB has NOT yet been created. This is
 * <strong>eventual consistency</strong> — the system will converge to a
 * consistent
 * state, but not instantaneously.
 * </p>
 *
 * <p>
 * This trade-off is acceptable because:
 * </p>
 * <ul>
 * <li>The employee data (PostgreSQL) is the <em>source of truth</em></li>
 * <li>The notification log (MongoDB) is a <em>derived view</em> — eventually
 * consistent</li>
 * <li>Kafka guarantees at-least-once delivery — the event WILL be
 * processed</li>
 * <li>The alternative (distributed 2-phase commit) is far more complex and
 * slower</li>
 * </ul>
 */
import com.java.practice.ems.employee.event.EventPublisher;

@Service
@Slf4j
public class EmployeeEventProducer implements EventPublisher {

    // ── Kafka topic name as a constant ──────────────────────────────────────────
    // Keeping it as a constant ensures producer and consumer always reference
    // the same topic. In production, consider externalizing this to
    // application.yml.
    public static final String TOPIC_EMPLOYEE_EVENTS = "employee.events";

    // ── KafkaTemplate: Spring's abstraction over the Kafka Producer API ──────
    // Dependency Inversion: we depend on KafkaTemplate (abstraction), not on the
    // raw KafkaProducer class. Spring auto-configures this bean based on
    // application.yml settings (serializers, acks, retries).
    private final KafkaTemplate<String, EmployeeEvent> kafkaTemplate;

    public EmployeeEventProducer(KafkaTemplate<String, EmployeeEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes an employee event to the Kafka topic.
     *
     * <p>
     * <strong>This is the "notifyObservers()" of the distributed Observer
     * Pattern.</strong>
     * Instead of calling each observer directly, we publish once to Kafka, and ALL
     * subscribed consumers receive the event independently.
     * </p>
     *
     * <p>
     * The event key is the employee ID, ensuring all events for the same employee
     * land on the SAME Kafka partition → guaranteed ordering per employee.
     * </p>
     *
     * @param event the employee event to publish
     */
    @Override
    public void publishEvent(EmployeeEvent event) {
        log.info("Publishing event: type={}, employeeId={}, eventId={}",
                event.eventType(), event.employeeId(), event.eventId());

        // ── Asynchronous send with callback ─────────────────────────────────────
        // KafkaTemplate.send() returns a CompletableFuture.
        //
        // The message key (employee ID as String) determines the partition:
        // → All events for employee #42 go to the SAME partition
        // → This guarantees ORDERED processing per employee
        // (CREATED before UPDATED before DEACTIVATED)
        CompletableFuture<SendResult<String, EmployeeEvent>> future = kafkaTemplate.send(
                TOPIC_EMPLOYEE_EVENTS,
                java.util.Objects.requireNonNull(String.valueOf(event.employeeId())), // key → partition routing
                event // value → serialized to JSON
        );

        // ── Handle success/failure asynchronously ───────────────────────────────
        // We don't block the calling thread (EmployeeService) waiting for Kafka ACK.
        // With Java 21 Virtual Threads, this callback runs on a lightweight thread.
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                // Log the failure but DON'T throw — the employee was already saved
                // in PostgreSQL. This is the eventual consistency trade-off:
                // the event will be retried (Kafka producer retries: 3 configured in YAML)
                log.error("Failed to publish event {}: {}", event.eventId(), ex.getMessage(), ex);
            } else {
                log.debug("Event published successfully: topic={}, partition={}, offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
