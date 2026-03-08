package com.ems.employee.event;



/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ EventPublisher — Domain Event Abstraction (DIP + Observer Pattern) ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>DEPENDENCY INVERSION PRINCIPLE (SOLID)</h2>
 *
 * <p>
 * This interface is the key architectural boundary between the <strong>domain
 * layer</strong> (EmployeeService) and the <strong>infrastructure
 * layer</strong>
 * (Kafka). The EmployeeService depends on THIS abstraction — not on
 * {@code KafkaTemplate} or any other messaging library.
 * </p>
 *
 * <pre>
 *   EmployeeService  ──depends on──►  EventPublisher (this interface)
 *                                           ▲
 *                                           │ implements
 *                                  EmployeeEventProducer (Kafka)
 *                                  (infrastructure detail)
 * </pre>
 *
 * <h2>OBSERVER PATTERN (Distributed via Kafka)</h2>
 *
 * <p>
 * In the classic Observer Pattern, a Subject notifies Observers of state
 * changes. Here, Kafka acts as the message broker between Subject
 * (EmployeeService) and Observers (NotificationService, AuditService, etc.).
 * This interface is the "notify" channel.
 * </p>
 *
 * <h3>Benefits of this abstraction:</h3>
 * <ul>
 * <li><strong>Testability:</strong> In unit tests, inject a mock
 * EventPublisher — no Kafka broker needed</li>
 * <li><strong>Portability:</strong> Switch from Kafka to RabbitMQ or
 * AWS SNS by changing only the implementation class</li>
 * <li><strong>SRP:</strong> EmployeeService handles business logic;
 * EmployeeEventProducer handles messaging plumbing</li>
 * </ul>
 */
public interface EventPublisher {

    /**
     * Publishes a domain event to the messaging infrastructure.
     *
     * <p>
     * Implementations must be idempotent-safe: if the same event is published
     * twice (e.g., due to retry), consumers should handle deduplication via
     * the event's {@code eventId}.
     * </p>
     *
     * @param event the domain event to publish (never null)
     */
    void publishEvent(EmployeeEvent event);
}
