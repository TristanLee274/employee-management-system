package com.java.practice.ems.kafka;

import com.java.practice.ems.notification.NotificationLog;
import com.java.practice.ems.notification.NotificationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ EmployeeEventConsumer — Kafka Consumer (The "Observer" in Observer
 * Pattern)║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>OBSERVER PATTERN — THIS IS THE "OBSERVER" (SUBSCRIBER)</h2>
 *
 * <p>
 * In the distributed Observer Pattern via Kafka:
 * </p>
 * 
 * <pre>
 *   Producer (Subject)           Kafka Broker           Consumer (Observer)
 *   ──────────────────          ──────────────          ────────────────────
 *   EmployeeService             "employee.events"       THIS CLASS
 *       │                            │                       │
 *       │ publishEvent(event)        │                       │
 *       ├───────────────────────────→│                       │
 *       │                            │   @KafkaListener      │
 *       │                            ├──────────────────────→│
 *       │                            │                       │ handleEmployeeEvent()
 *       │                            │                       │ → save to MongoDB
 * </pre>
 *
 * <p>
 * <strong>This consumer is completely independent of the producer.</strong>
 * The producer (EmployeeService) has NO reference to this class. This consumer:
 * <ul>
 * <li>Can be deployed in a separate microservice (different pod/container)</li>
 * <li>Can be scaled independently (add more consumer instances for
 * parallelism)</li>
 * <li>Can be paused/restarted without affecting the producer</li>
 * <li>If it crashes, Kafka retains events — on restart, it replays from the
 * last
 * committed offset (at-least-once delivery)</li>
 * </ul>
 *
 * <h2>EVENTUAL CONSISTENCY — THE TRADE-OFF</h2>
 *
 * <p>
 * When the producer publishes an event, this consumer processes it
 * asynchronously.
 * There is a latency gap (typically milliseconds, worst case seconds) between:
 * </p>
 * <ol>
 * <li>Employee created in PostgreSQL (immediate, transactional)</li>
 * <li>Event published to Kafka (async, fire-and-forget from producer's
 * perspective)</li>
 * <li>Event consumed and notification log written to MongoDB (async)</li>
 * </ol>
 *
 * <p>
 * This is <strong>eventual consistency</strong> — the MongoDB notification log
 * will
 * <em>eventually</em> reflect every employee creation, but not instantaneously.
 * This pattern is ubiquitous in distributed systems because:
 * </p>
 * <ul>
 * <li>Strong consistency across distributed databases requires 2-phase commit
 * (2PC), which is slow, complex, and reduces system availability</li>
 * <li>The CAP theorem tells us that in a partitioned network, we must choose
 * between consistency and availability — most microservices choose
 * availability</li>
 * <li>For notification logs, eventual consistency is perfectly acceptable —
 * users
 * don't need real-time audit logs, they need reliable ones</li>
 * </ul>
 *
 * <h2>NoSQL (MongoDB) — WHY FOR NOTIFICATION LOGS?</h2>
 *
 * <p>
 * Notification logs stored in MongoDB instead of PostgreSQL because:
 * </p>
 * <ul>
 * <li><strong>Schema flexibility:</strong> Each event type (CREATED, UPDATED,
 * DEACTIVATED) carries different payload fields. MongoDB stores heterogeneous
 * documents in the same collection — no NULL columns, no schema
 * migrations.</li>
 * <li><strong>Write performance:</strong> Logging generates high write
 * throughput.
 * MongoDB's WiredTiger engine with journal writes is optimized for this
 * pattern, unlike PostgreSQL's WAL + MVCC which generates dead tuples.</li>
 * <li><strong>TTL indexes:</strong> Set {@code @Indexed(expireAfter = "90d")}
 * on
 * a timestamp field, and MongoDB automatically deletes logs older than 90 days.
 * PostgreSQL requires manual partition management or cron jobs.</li>
 * <li><strong>Separation of concerns:</strong> Employee data (structured,
 * relational,
 * ACID) belongs in PostgreSQL. Event logs (append-only, variable schema,
 * high volume) belong in MongoDB. Each database is used for its strengths.</li>
 * </ul>
 */
@Service
@Slf4j
public class EmployeeEventConsumer {

    private final NotificationLogRepository notificationLogRepository;

    // ── Constructor injection (Dependency Inversion Principle) ────────────────
    // We inject the MongoRepository interface, not a concrete MongoDB class.
    // This consumer is unaware of MongoDB implementation details — it just
    // calls repository.save().
    public EmployeeEventConsumer(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    /**
     * Handles incoming employee events from the Kafka topic.
     *
     * <p>
     * <strong>@KafkaListener</strong> is Spring Kafka's declarative consumer
     * annotation.
     * Under the hood, it:
     * </p>
     * <ol>
     * <li>Creates a Kafka consumer in the configured consumer group</li>
     * <li>Subscribes to the specified topic</li>
     * <li>Polls for new messages in a background thread</li>
     * <li>Deserializes the JSON payload into an {@link EmployeeEvent} object</li>
     * <li>Calls this method for each message</li>
     * <li>Commits the offset after successful processing (at-least-once
     * delivery)</li>
     * </ol>
     *
     * <p>
     * <strong>Consumer group:</strong> All instances of this service share the same
     * group ID. Kafka distributes topic partitions across group members
     * automatically.
     * If you scale to 3 pods and the topic has 6 partitions, each pod gets 2
     * partitions.
     * </p>
     *
     * @param event the deserialized employee event
     */
    @KafkaListener(topics = EmployeeEventProducer.TOPIC_EMPLOYEE_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void handleEmployeeEvent(EmployeeEvent event) {
        log.info("Received event: type={}, employeeId={}, eventId={}",
                event.eventType(), event.employeeId(), event.eventId());

        try {
            // ── Create notification log document for MongoDB ─────────────────────
            //
            // This demonstrates the power of MongoDB's schema flexibility:
            // The NotificationLog document can store ANY event fields without
            // requiring column-level schema changes. A CREATED event stores salary,
            // an UPDATE stores old/new values, a DEACTIVATED stores only the ID.
            //
            // In PostgreSQL, we'd need nullable columns or a separate table per
            // event type. In MongoDB, each document is self-describing.
            //
            NotificationLog logEntry = NotificationLog.builder()
                    .eventId(event.eventId())
                    .eventType(event.eventType())
                    .employeeId(event.employeeId())
                    .employeeName(event.employeeName())
                    .email(event.email())
                    .department(event.department())
                    .jobTitle(event.jobTitle())
                    .salary(event.salary())
                    .status(event.status())
                    .processedAt(Instant.now())
                    .notificationSent(false) // Will be updated when notification is dispatched
                    .build();

            notificationLogRepository.save(logEntry);

            log.info("Notification log saved to MongoDB: eventId={}, type={}",
                    event.eventId(), event.eventType());

        } catch (Exception e) {
            // ── Error handling strategy ──────────────────────────────────────────
            //
            // If MongoDB write fails, we LOG the error but DON'T suppress the exception.
            // Spring Kafka's error handler will:
            // 1. NOT commit the offset for this message
            // 2. Retry the message (configurable retry count)
            // 3. After max retries, send to a Dead Letter Topic (DLT) if configured
            //
            // This ensures at-least-once delivery: the event will be reprocessed
            // until it succeeds or is routed to the DLT for manual investigation.
            log.error("Failed to process event {}: {}", event.eventId(), e.getMessage(), e);
            throw e; // Re-throw to trigger Kafka retry mechanism
        }
    }
}
