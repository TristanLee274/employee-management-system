/**
 * Data Transfer Objects (DTOs) for the Employee module.
 *
 * <h2>DTO Architecture — Java 21 Records</h2>
 *
 * <p>
 * All DTOs in this package are implemented as Java 21 Records, providing:
 * </p>
 * <ul>
 * <li><strong>Immutability:</strong> Fields are final, preventing mutation
 * after creation</li>
 * <li><strong>Value semantics:</strong> Equals/hashCode based on all fields
 * (ideal for testing)</li>
 * <li><strong>Zero boilerplate:</strong> Compiler generates constructor,
 * getters, toString</li>
 * <li><strong>Thread safety:</strong> Safe for concurrent access with Virtual
 * Threads</li>
 * </ul>
 *
 * <h2>DTO Categories</h2>
 *
 * <table>
 * <tr>
 * <th>DTO</th>
 * <th>Direction</th>
 * <th>Purpose</th>
 * </tr>
 * <tr>
 * <td>{@link com.java.practice.ems.employee.dto.CreateEmployeeRequest}</td>
 * <td>Client → Server</td>
 * <td>Create a new employee (all required fields)</td>
 * </tr>
 * <tr>
 * <td>{@link com.java.practice.ems.employee.dto.UpdateEmployeeRequest}</td>
 * <td>Client → Server</td>
 * <td>Partial update (only changed fields)</td>
 * </tr>
 * <tr>
 * <td>{@link com.java.practice.ems.employee.dto.EmployeeResponse}</td>
 * <td>Server → Client</td>
 * <td>Full employee details (single resource)</td>
 * </tr>
 * <tr>
 * <td>{@link com.java.practice.ems.employee.dto.EmployeeListResponse}</td>
 * <td>Server → Client</td>
 * <td>Summary for list views (excludes sensitive data)</td>
 * </tr>
 * </table>
 *
 * <h2>Why Separate Packages for DTOs and Entities?</h2>
 *
 * <p>
 * This package ({@code dto}) is separate from {@code entity} to enforce a clear
 * architectural boundary. Import analysis can verify that:
 * </p>
 * <ul>
 * <li>Controllers import DTOs (never entities)</li>
 * <li>Repositories import entities (never DTOs)</li>
 * <li>Services bridge the two using factory methods like
 * {@code EmployeeResponse.from(entity)}</li>
 * </ul>
 */
package com.java.practice.ems.employee.dto;
