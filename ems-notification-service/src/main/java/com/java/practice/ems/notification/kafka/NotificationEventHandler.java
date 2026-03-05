package com.java.practice.ems.notification.kafka;

import com.java.practice.ems.notification.NotificationLog;
import com.java.practice.ems.notification.NotificationLogRepository;
import com.java.practice.ems.notification.dto.EmployeeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ NotificationEventHandler — Kafka Consumer (Observer Pattern) ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>EVENTUAL CONSISTENCY — THE TRADE-OFF</h2>
 *
 * <p>
 * When the producer publishes an event, this consumer processes it
 * asynchronously. There is a latency gap (typically milliseconds, worst case
 * seconds)
 * between Employee creation in PostgreSQL (immediate) and reading the Event
 * here in the Notification service.
 * </p>
 * <p>
 * This is <strong>eventual consistency</strong> — the MongoDB notification log
 * will <em>eventually</em> reflect every employee creation, but not
 * instantaneously.
 * </p>
 */
@Service
@Slf4j
@SuppressWarnings("null")
public class NotificationEventHandler {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationEventHandler(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @KafkaListener(topics = "employee.events", groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void handleEmployeeEvent(EmployeeEvent event) {
        log.info("Received event: type={}, employeeId={}, eventId={}",
                event.eventType(), event.employeeId(), event.eventId());

        try {
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
                    .notificationSent(false)
                    .build();

            notificationLogRepository.save(logEntry);

            log.info("Notification log saved to MongoDB: eventId={}, type={}",
                    event.eventId(), event.eventType());

        } catch (Exception e) {
            log.error("Failed to process event {}: {}", event.eventId(), e.getMessage(), e);
            throw e;
        }
    }
}
