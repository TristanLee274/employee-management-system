package com.java.practice.ems.employee.event;

import com.java.practice.ems.kafka.EmployeeEvent;

/**
 * EventPublisher — Abstraction for Dependency Inversion.
 * 
 * Defines the contract for publishing domain events.
 */
public interface EventPublisher {
    void publishEvent(EmployeeEvent event);
}
