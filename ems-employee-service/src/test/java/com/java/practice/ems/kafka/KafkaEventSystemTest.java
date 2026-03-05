package com.java.practice.ems.kafka;

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
@SuppressWarnings("null")
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
                                        "uuid-1", "42", "John Doe", "john@company.com",
                                        "Engineering", "Senior Dev", new BigDecimal("5000.00"));

                        // ── THEN ──────────────────────────────────────────────────────────
                        assertThat(event.eventId()).isEqualTo("uuid-1");
                        assertThat(event.eventType()).isEqualTo("EMPLOYEE_CREATED");
                        assertThat(event.timestamp()).isNotNull();
                        assertThat(event.timestamp()).isBefore(Instant.now().plusSeconds(1));
                        assertThat(event.employeeId()).isEqualTo("42");
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
                                        "uuid-2", "42", "John Doe", "john@company.com",
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
                                        "uuid-3", "42", "John Doe", "john@company.com");

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
                                        "id", "TYPE", Instant.EPOCH, "1", "Name", "email",
                                        "dept", "title", BigDecimal.ONE, "ACTIVE");
                        EmployeeEvent event2 = new EmployeeEvent(
                                        "id", "TYPE", Instant.EPOCH, "1", "Name", "email",
                                        "dept", "title", BigDecimal.ONE, "ACTIVE");
                        assertThat(event1).isEqualTo(event2);
                        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // CONSUMER TESTS
        // ═══════════════════════════════════════════════════════════════════════════

        // Removing EmployeeEventConsumerTest as it tests a component which relies
        // on NotificationLogRepository located in the ems-notification-service.

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
                                        "uuid-1", "42", "John Doe", "john@company.com",
                                        "Engineering", "Dev", new BigDecimal("5000.00"));
                        var future = new java.util.concurrent.CompletableFuture<org.springframework.kafka.support.SendResult<String, EmployeeEvent>>();
                        given(kafkaTemplate.send(anyString(), anyString(), any()))
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
