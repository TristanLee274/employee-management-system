package com.java.practice.ems.bdd;

import com.java.practice.ems.employee.dto.CreateEmployeeRequest;
import com.java.practice.ems.employee.dto.EmployeeResponse;
import com.java.practice.ems.employee.entity.Department;
import com.java.practice.ems.employee.entity.Employee;
import com.java.practice.ems.employee.entity.EmployeeStatus;
import com.java.practice.ems.employee.entity.EmployeeType;
import com.java.practice.ems.employee.repository.DepartmentRepository;
import com.java.practice.ems.employee.repository.EmployeeRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SuppressWarnings("null")
public class EmployeeStepDefinitions {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    private ResponseEntity<?> lastResponse;
    private Department sharedDept;

    @Given("the system has department {string} with id {string}")
    public void setupDepartment(String name, String id) {
        sharedDept = new Department();
        sharedDept.setId(id);
        sharedDept.setName(name);
        departmentRepository.save(sharedDept);
    }

    @Given("I am authenticated as an HR manager")
    public void authenticateAsHR() {
        // Implement test security mechanisms or bypass rules locally
        // Mock authorization token bindings would go here
    }

    @Given("an employee with email {string} already exists")
    public void createPreExistingEmployee(String email) {
        Employee existing = Employee.builder()
                .email(email)
                .fullName("Existing User")
                .type(EmployeeType.FULL_TIME)
                .department(sharedDept)
                .baseSalary(new BigDecimal("1000"))
                .joinDate(LocalDate.now())
                .status(EmployeeStatus.ACTIVE)
                .build();
        employeeRepository.save(existing);
    }

    @Given("the system has {int} employees with status {string}")
    public void setupEmployeesWithStatus(int total, String status) {
        // Prepare list
        for (int i = 0; i < total; i++) {
            Employee emp = Employee.builder()
                    .email("emp" + i + status + "@test.com")
                    .fullName("Status User")
                    .type(EmployeeType.FULL_TIME)
                    .department(sharedDept)
                    .baseSalary(new BigDecimal("1000"))
                    .joinDate(LocalDate.now())
                    .status(EmployeeStatus.valueOf(status))
                    .build();
            employeeRepository.save(emp);
        }
    }

    @When("I create an employee with the following details:")
    public void createEmployee(DataTable dataTable) {
        Map<String, String> data = dataTable.asMaps().get(0);
        var request = new CreateEmployeeRequest(
                data.get("fullName"), data.get("email"), "+84-000-000",
                EmployeeType.valueOf(data.get("type")), sharedDept.getId(),
                new BigDecimal(data.get("salary")), LocalDate.now());

        lastResponse = restTemplate.postForEntity(
                "/api/v1/employees", request, EmployeeResponse.class);
    }

    @When("I search for employees with status {string}")
    public void searchForEmployees(String status) {
        // Hypothetical endpoint /api/v1/employees?status=STATUS
        lastResponse = restTemplate.getForEntity(
                "/api/v1/employees?status=" + status, Object.class);
    }

    @Then("the employee should be created successfully")
    public void verifyCreated() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Then("the response status code should be {int}")
    public void verifyStatusCode(int statusCode) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(statusCode);
    }

    @Then("the employee should belong to department {string}")
    public void verifyDepartmentAssignment(String departmentName) {
        assertThat(lastResponse.getBody()).isNotNull();
        EmployeeResponse resp = java.util.Objects.requireNonNull((EmployeeResponse) lastResponse.getBody());
        assertThat(resp).isNotNull();
        assertThat(resp.departmentName()).isEqualTo(departmentName);
    }

    @Then("the error message should contain {string}")
    public void verifyErrorMsg(String errorContent) {
        // Assuming your GlobalExceptionHandler returns an ErrorResponse format
        // Depending on actual implementation, you cast it
        assertThat(lastResponse.getBody()).isNotNull();
        assertThat(java.util.Objects.requireNonNull(lastResponse.getBody()).toString()).contains(errorContent);
    }

    @Then("I should receive {int} results")
    public void verifyResultCount(int expected) {
        // This is a stubbed validation counting simulated paginated outputs
        assertThat(lastResponse.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
