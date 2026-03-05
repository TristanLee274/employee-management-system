package com.java.practice.ems.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ NotificationLog — MongoDB Document for Event Notifications ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>NoSQL (MongoDB) — WHY DOCUMENTS INSTEAD OF TABLES?</h2>
 *
 * <p>
 * This class is annotated with {@code @Document} (MongoDB) instead of
 * {@code @Entity} (JPA/PostgreSQL). Here's why this is the RIGHT choice
 * for notification logs:
 * </p>
 *
 * <h3>1. Schema Flexibility (the #1 reason)</h3>
 * 
 * <pre>
 *   PostgreSQL (rigid schema):                MongoDB (flexible schema):
 *   ┌────────────────────────────────┐       ┌────────────────────────────────┐
 *   │ notification_logs              │       │ notification_logs (collection) │
 *   ├────────────────────────────────┤       ├────────────────────────────────┤
 *   │ id          BIGSERIAL PK       │       │ { _id: ObjectId,              │
 *   │ event_id    VARCHAR NOT NULL   │       │   eventId: "uuid-1",          │
 *   │ event_type  VARCHAR NOT NULL   │       │   eventType: "CREATED",       │
 *   │ employee_id BIGINT             │       │   employeeId: 42,             │
 *   │ salary      DECIMAL  ← NULL   │       │   salary: 5000.00,            │
 *   │ old_status  VARCHAR  ← NULL   │       │   processedAt: ISODate(...),  │
 *   │ new_status  VARCHAR  ← NULL   │       │   customField: "anything"     │
 *   │ ...40 more nullable columns   │       │ }                             │
 *   └────────────────────────────────┘       └────────────────────────────────┘
 *
 *   Problem: Every new event type requires ALTER TABLE ADD COLUMN.
 *   MongoDB: Just add a field to the document — no migration needed.
 * </pre>
 *
 * <h3>2. Write Performance</h3>
 * <p>
 * Notification logging is a write-heavy workload. MongoDB's WiredTiger engine
 * with journaling is optimized for sequential writes (append-only), while
 * PostgreSQL's MVCC creates dead tuples on every UPDATE (requiring VACUUM).
 * </p>
 *
 * <h3>3. TTL Auto-Expiration</h3>
 * <p>
 * The {@code @Indexed(expireAfter = "90d")} annotation on {@code processedAt}
 * tells MongoDB to automatically delete documents older than 90 days.
 * PostgreSQL requires manual partition management or cron-based cleanup.
 * </p>
 *
 * <h3>4. Horizontal Scaling</h3>
 * <p>
 * MongoDB shards collections across nodes by any field (e.g., employeeId).
 * As event volume grows, add more shards — no application code changes needed.
 * </p>
 */
@Document(collection = "notification_logs")
// @Document: Maps this class to a MongoDB collection (like @Entity maps to a
// table)
// Unlike JPA, there's no rigid table structure — each document can have
// different fields.

@Data // Lombok: getters, setters, equals, hashCode, toString
@Builder // Lombok: fluent builder pattern for clean object construction
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    // ── MongoDB Document ID ─────────────────────────────────────────────────────
    // @Id maps to MongoDB's _id field. If left null, MongoDB auto-generates
    // an ObjectId (which is a 12-byte globally unique identifier).
    //
    // Unlike PostgreSQL's auto-increment BIGSERIAL, MongoDB ObjectIds are:
    // • Globally unique across all MongoDB instances (no central sequence needed)
    // • Timestamp-sortable (first 4 bytes = Unix epoch seconds)
    // • Generated client-side (no round-trip to the database for ID assignment)
    @Id
    private String id;

    // ── Event Metadata ──────────────────────────────────────────────────────────

    @Indexed(unique = true)
    // Indexed for fast lookups and duplicate detection (idempotency).
    // If the same event is consumed twice (at-least-once delivery), the unique
    // index prevents duplicate log entries.
    private String eventId;

    @Indexed
    // Index on eventType for queries like: "show me all CREATED events"
    private String eventType;

    @Indexed
    // Index on employeeId for queries like: "show me all events for employee #42"
    private String employeeId;

    // ── Employee Snapshot Data ───────────────────────────────────────────────────
    // We store a SNAPSHOT of the employee data at the time of the event.
    // This is important because the employee data in PostgreSQL may change later,
    // but the notification log should reflect what the data looked like WHEN the
    // event occurred (event sourcing principle).
    private String employeeName;
    private String email;
    private String department;
    private String jobTitle;
    private BigDecimal salary;
    private String status;

    // ── Processing Metadata ─────────────────────────────────────────────────────

    @Indexed(expireAfter = "90d")
    // TTL INDEX: MongoDB automatically deletes documents 90 days after processedAt.
    // This is equivalent to running:
    // DELETE FROM notification_logs WHERE processed_at < NOW() - INTERVAL '90 days'
    // but completely automated — no cron jobs, no manual cleanup scripts.
    private Instant processedAt;

    // Whether the notification (email/Slack/etc.) has been dispatched
    private boolean notificationSent;

    // Optional: reason for failure if notification dispatch fails
    private String failureReason;
}
