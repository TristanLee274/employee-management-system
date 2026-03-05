package com.java.practice.ems.notification;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB Repository for notification log documents.
 *
 * <p>
 * Like Spring Data JPA's {@code JpaRepository}, Spring Data MongoDB's
 * {@link MongoRepository} follows the <strong>Repository Pattern</strong>:
 * it abstracts data access behind an interface, and Spring generates the
 * implementation at runtime.
 * </p>
 *
 * <p>
 * <strong>Dependency Inversion:</strong> The consumer depends on this
 * interface, not on MongoDB driver classes. We could swap MongoDB for
 * Elasticsearch or Cassandra with just a different repository interface.
 * </p>
 */
@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {

    /**
     * Find all notification logs for a specific employee.
     * Spring Data derives the query from the method name automatically.
     * MongoDB query: {@code db.notification_logs.find({employeeId: ?})}
     */
    List<NotificationLog> findByEmployeeId(String employeeId);

    /**
     * Find all notification logs of a specific event type.
     * MongoDB query: {@code db.notification_logs.find({eventType: ?})}
     */
    List<NotificationLog> findByEventType(String eventType);

    /**
     * Find a notification log by its unique event ID (for idempotency checks).
     * Spring Data uses the @Indexed(unique=true) field for efficient lookup.
     */
    Optional<NotificationLog> findByEventId(String eventId);

    /**
     * Find unprocessed notifications that haven't been sent yet.
     * MongoDB query: {@code db.notification_logs.find({notificationSent: false})}
     */
    List<NotificationLog> findByNotificationSentFalse();
}
