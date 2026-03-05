# src/test/resources/features/employee_management.feature

Feature: Employee Management
  As an HR manager
  I want to manage employee records
  So that I can maintain accurate employee information

  Background:
    Given the system has department "Engineering" with id "dept-1"
    And I am authenticated as an HR manager

  Scenario: Successfully create a new employee
    When I create an employee with the following details:
      | fullName | email              | type      | salary   |
      | John Doe | john@company.com   | FULL_TIME | 25000000 |
    Then the employee should be created successfully
    And the response status code should be 201
    And the employee should belong to department "Engineering"

  Scenario: Reject duplicate email
    Given an employee with email "existing@company.com" already exists
    When I create an employee with the following details:
      | fullName  | email                | type      | salary   |
      | Jane Doe  | existing@company.com | FULL_TIME | 22000000 |
    Then the response status code should be 409
    And the error message should contain "email"

  Scenario Outline: Search employees by status
    Given the system has <total> employees with status "<status>"
    When I search for employees with status "<status>"
    Then I should receive <expected> results

    Examples:
      | status     | total | expected |
      | ACTIVE     | 10    | 10       |
      | ON_LEAVE   | 3     | 3        |
      | RESIGNED   | 5     | 5        |
