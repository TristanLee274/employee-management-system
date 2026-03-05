package com.java.practice.ems.kafka;

import com.java.practice.ems.notification.NotificationLog;
import com.java.practice.ems.notification.NotificationLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ Kafka Event Consumer & Producer Tests ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>TESTING EVENT-DRIVEN COMPONENTS IN ISOLATION</h2>
 *
 * <p>
 * These tests verify the Kafka producer and consumer logic WITHOUT requiring
 * a real Kafka broker or MongoDB instance. We mock the infrastructure and focus
 * on the business logic: event creation, mapping, and persistence.
 * </p>
 *
 * <p>
 * <strong>What we test here (unit):</strong>
 * </p>
 * <ul>
 * <li>Event factory methods produce correct data structures</li>
 * <li>Consumer maps events to NotificationLog documents correctly</li>
 * <li>Consumer handles errors and re-throws for Kafka retry</li>
 * </ul>
 *
 * <p>
 * <strong>What we DON'T test here (integration):</strong>
 * </p>
 * <ul>
 * <li>Kafka broker connectivity and serialization</li>
 * <li>MongoDB document persistence</li>
 * <li>End-to-end event flow (producer → broker → consumer)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kafka Event System — Unit Tests")
class KafkaEventSystemTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // EVENT RECORD FACTORY TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EmployeeEvent Record Factories")
    class EmployeeEventTest {

        @Test
        @DisplayName("should create EMPLOYEE_CREATED event with correct fields")
        void should_CreateEmployeeCreatedEvent() {
            // ── GIVEN/WHEN ────────────────────────────────────────────────────
            EmployeeEvent event = EmployeeEvent.created(
                    "uuid-1", 42L, "John Doe", "john@company.com",
                    "Engineering", "Senior Dev", new BigDecimal("5000.00"));

            // ── THEN ──────────────────────────────────────────────────────────
            assertThat(event.eventId()).isEqualTo("uuid-1");
            assertThat(event.eventType()).isEqualTo("EMPLOYEE_CREATED");
            assertThat(event.timestamp()).isNotNull();
            assertThat(event.timestamp()).isBefore(Instant.now().plusSeconds(1));
            assertThat(event.employeeId()).isEqualTo(42L);
            assertThat(event.employeeName()).isEqualTo("John Doe");
            assertThat(event.email()).isEqualTo("john@company.com");
            assertThat(event.department()).isEqualTo("Engineering");
            assertThat(event.jobTitle()).isEqualTo("Senior Dev");
            assertThat(event.salary()).isEqualByComparingTo("5000.00");
            assertThat(event.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should create EMPLOYEE_UPDATED event with status")
        void should_CreateEmployeeUpdatedEvent() {
            // ── GIVEN/WHEN ────────────────────────────────────────────────────
            EmployeeEvent event = EmployeeEvent.updated(
                    "uuid-2", 42L, "John Doe", "john@company.com",
                    "Product", "Lead Dev", new BigDecimal("7000.00"), "ON_LEAVE");

            // ── THEN ──────────────────────────────────────────────────────────
            assertThat(event.eventType()).isEqualTo("EMPLOYEE_UPDATED");
            assertThat(event.department()).isEqualTo("Product");
            assertThat(event.salary()).isEqualByComparingTo("7000.00");
            assertThat(event.status()).isEqualTo("ON_LEAVE");
        }

        @Test
        @DisplayName("should create EMPLOYEE_DEACTIVATED event with null optional fields")
        void should_CreateEmployeeDeactivatedEvent() {
            // ── GIVEN/WHEN ────────────────────────────────────────────────────
            EmployeeEvent event = EmployeeEvent.deactivated(
                    "uuid-3", 42L, "John Doe", "john@company.com");

            // ── THEN ──────────────────────────────────────────────────────────
            assertThat(event.eventType()).isEqualTo("EMPLOYEE_DEACTIVATED");
            assertThat(event.status()).isEqualTo("INACTIVE");
            // Deactivated events don't carry salary/department/jobTitle
            assertThat(event.department()).isNull();
            assertThat(event.jobTitle()).isNull();
            assertThat(event.salary()).isNull();
        }

        @Test
        @DisplayName("should support Java 21 Record equality (value-based)")
        void should_SupportValueEquality() {
            // Java 21 Records have auto-generated equals() based on ALL fields.
            // Two events with the same data ARE equal — useful for test assertions.
            EmployeeEvent event1 = new EmployeeEvent(
                    "id", "TYPE", Instant.EPOCH, 1L, "Name", "email",
                    "dept", "title", BigDecimal.ONE, "ACTIVE");
            EmployeeEvent event2 = new EmployeeEvent(
                    "id", "TYPE", Instant.EPOCH, 1L, "Name", "email",
                    "dept", "title", BigDecimal.ONE, "ACTIVE");
            assertThat(event1).isEqualTo(event2);
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSUMER TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EmployeeEventConsumer")
    class EmployeeEventConsumerTest {

        @Mock
        private NotificationLogRepository notificationLogRepository;

        @InjectMocks
        private EmployeeEventConsumer consumer;

        @Captor
        private ArgumentCaptor<NotificationLog> logCaptor;

        @Test
        @DisplayName("should save NotificationLog to MongoDB when event is received")
        void should_SaveNotificationLog_When_EventReceived() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            EmployeeEvent event = EmployeeEvent.created(
                    "uuid-1", 42L, "John Doe", "john@company.com",
                    "Engineering", "Senior Dev", new BigDecimal("5000.00"));
            given(notificationLogRepository.save(any(NotificationLog.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // ── WHEN ──────────────────────────────────────────────────────────
            consumer.handleEmployeeEvent(event);

            // ── THEN ──────────────────────────────────────────────────────────
            then(notificationLogRepository).should().save(logCaptor.capture());
            NotificationLog savedLog = logCaptor.getValue();

            assertThat(savedLog.getEventId()).isEqualTo("uuid-1");
            assertThat(savedLog.getEventType()).isEqualTo("EMPLOYEE_CREATED");
            assertThat(savedLog.getEmployeeId()).isEqualTo(42L);
            assertThat(savedLog.getEmployeeName()).isEqualTo("John Doe");
            assertThat(savedLog.getEmail()).isEqualTo("john@company.com");
            assertThat(savedLog.getDepartment()).isEqualTo("Engineering");
            assertThat(savedLog.getSalary()).isEqualByComparingTo("5000.00");
            assertThat(savedLog.isNotificationSent()).isFalse();
            assertThat(savedLog.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("should re-throw exception when MongoDB save fails (for Kafka retry)")
        void should_RethrowException_When_MongoSaveFails() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            EmployeeEvent event = EmployeeEvent.created(
                    "uuid-fail", 42L, "John Doe", "john@company.com",
                    "Engineering", "Dev", new BigDecimal("5000.00"));
            given(notificationLogRepository.save(any()))
                    .willThrow(new RuntimeException("MongoDB connection refused"));

            // ── WHEN / THEN ───────────────────────────────────────────────────
            // The exception must propagate so that Kafka's error handler can
            // retry the message or route it to a Dead Letter Topic (DLT)
            assertThatThrownBy(() -> consumer.handleEmployeeEvent(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("MongoDB connection refused");
        }

        @Test
        @DisplayName("should map deactivated event with null optional fields")
        void should_MapDeactivatedEvent_WithNullFields() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            EmployeeEvent event = EmployeeEvent.deactivated(
                    "uuid-deact", 42L, "John Doe", "john@company.com");
            given(notificationLogRepository.save(any(NotificationLog.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // ── WHEN ──────────────────────────────────────────────────────────
            consumer.handleEmployeeEvent(event);

            // ── THEN ──────────────────────────────────────────────────────────
            then(notificationLogRepository).should().save(logCaptor.capture());
            NotificationLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getEventType()).isEqualTo("EMPLOYEE_DEACTIVATED");
            assertThat(savedLog.getDepartment()).isNull();
            assertThat(savedLog.getSalary()).isNull();
            assertThat(savedLog.getStatus()).isEqualTo("INACTIVE");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRODUCER TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EmployeeEventProducer")
    class EmployeeEventProducerTest {

        @Mock
        private org.springframework.kafka.core.KafkaTemplate<String, EmployeeEvent> kafkaTemplate;

        @InjectMocks
        private EmployeeEventProducer producer;

        @Test
        @DisplayName("should send event to correct topic with employee ID as key")
        void should_SendEventToCorrectTopic() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            EmployeeEvent event = EmployeeEvent.created(
                    "uuid-1", 42L, "John Doe", "john@company.com",
                    "Engineering", "Dev", new BigDecimal("5000.00"));
            var future = new java.util.concurrent.CompletableFuture<org.springframework.kafka.support.SendResult<String, EmployeeEvent>>();
            given(kafkaTemplate.send(anyString(), anyString(), any(EmployeeEvent.class)))
                    .willReturn(future);

            // ── WHEN ──────────────────────────────────────────────────────────
            producer.publishEvent(event);

            // ── THEN ──────────────────────────────────────────────────────────
            then(kafkaTemplate).should().send(
                    eq(EmployeeEventProducer.TOPIC_EMPLOYEE_EVENTS), // topic
                    eq("42"), // key = employeeId
                    eq(event) // value = event
            );
        }

        @Test
        @DisplayName("should verify topic name constant is 'employee.events'")
        void should_HaveCorrectTopicName() {
            assertThat(EmployeeEventProducer.TOPIC_EMPLOYEE_EVENTS)
                    .isEqualTo("employee.events");
        }
    }
}
