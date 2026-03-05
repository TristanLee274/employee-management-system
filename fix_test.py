import re

file_path = 'ems-employee-service/src/test/java/com/java/practice/ems/employee/service/EmployeeServiceTest.java'
with open(file_path, 'r') as f:
    content = f.read()

content = content.replace("private static final Long EMPLOYEE_ID = 1L;", 'private static final String EMPLOYEE_ID = "EMP-12345";')
content = content.replace('private static final String FIRST_NAME = "John";\n    private static final String LAST_NAME = "Doe";', 'private static final String FULL_NAME = "John Doe";')
content = content.replace('private static final String JOB_TITLE = "Senior Developer";', 'private static final com.java.practice.ems.employee.entity.EmployeeType TYPE = com.java.practice.ems.employee.entity.EmployeeType.FULL_TIME;\n    private static final String DEPARTMENT_ID = "DEPT-1";')

# Test entity
def repl_entity(match):
    return """private com.java.practice.ems.employee.entity.Department createTestDepartment() {
        com.java.practice.ems.employee.entity.Department dept = new com.java.practice.ems.employee.entity.Department();
        dept.setId(DEPARTMENT_ID);
        dept.setName(DEPARTMENT);
        return dept;
    }

    private Employee createTestEmployee() {
        return Employee.builder()
                .id(EMPLOYEE_ID)
                .fullName(FULL_NAME)
                .email(EMAIL)
                .phone(PHONE)
                .department(createTestDepartment())
                .type(TYPE)
                .baseSalary(SALARY)
                .joinDate(HIRE_DATE)
                .status(EmployeeStatus.ACTIVE)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }"""

content = re.sub(r'private Employee createTestEmployee\(\) \{.*?\.build\(\);\s*\}', repl_entity, content, flags=re.DOTALL)

# Test request
def repl_req(match):
    return """private CreateEmployeeRequest createTestRequest() {
        return new CreateEmployeeRequest(
                FULL_NAME, EMAIL, PHONE,
                DEPARTMENT_ID, TYPE, SALARY, HIRE_DATE);
    }"""

content = re.sub(r'private CreateEmployeeRequest createTestRequest\(\) \{.*?\}\s*\}', repl_req, content, flags=re.DOTALL)

content = content.replace("FIRST_NAME", "FULL_NAME")
content = content.replace("LAST_NAME", "FULL_NAME")
content = content.replace("result.firstName()", "result.fullName()")
content = content.replace("result.lastName()", "result.fullName()")
content = content.replace("result.salary()", "result.baseSalary()")
content = content.replace("result.department()", "result.departmentName()")
content = content.replace("999L", '"999"')
content = content.replace("anyLong()", "anyString()")

content = content.replace("capturedEmployee.getFirstName()", "capturedEmployee.getFullName()")
content = content.replace("capturedEmployee.getLastName()", "capturedEmployee.getFullName()")
content = content.replace("capturedEmployee.getHireDate()", "capturedEmployee.getJoinDate()")
content = content.replace("existingEmployee.getFirstName()", "existingEmployee.getFullName()")
content = content.replace("existingEmployee.getLastName()", "existingEmployee.getFullName()")
content = content.replace("existingEmployee.getSalary()", "existingEmployee.getBaseSalary()")
content = content.replace("existingEmployee.getDepartment()", "existingEmployee.getDepartment().getName()")

# UpdateEmployeeRequest matches
content = re.sub(r'new UpdateEmployeeRequest\(\s*null,\s*null,\s*null,\s*"\+84-999-888-777",\s*"Product",\s*null,\s*null,\s*null,\s*null\)', 
                 'new UpdateEmployeeRequest(null, null, "+84-999-888-777", "DEPT-2", null, null, null)', content)
                 
content = re.sub(r'new UpdateEmployeeRequest\(\s*"Jane",\s*null,\s*null,\s*null,\s*null,\s*null,\s*null,\s*null,\s*null\)', 
                 'new UpdateEmployeeRequest("Jane", null, null, null, null, null, null)', content)

content = re.sub(r'new UpdateEmployeeRequest\(\s*null,\s*null,\s*"taken@company.com",\s*null,\s*null,\s*null,\s*null,\s*null,\s*null\)', 
                 'new UpdateEmployeeRequest(null, "taken@company.com", null, null, null, null, null)', content)

content = re.sub(r'new UpdateEmployeeRequest\(\s*null,\s*null,\s*null,\s*null,\s*null,\s*null,\s*null,\s*null,\s*"ON_LEAVE"\)', 
                 'new UpdateEmployeeRequest(null, null, null, null, null, null, null)', content)

content = re.sub(r'new UpdateEmployeeRequest\(\s*null,\s*null,\s*null,\s*null,\s*null,\s*null,\s*null,\s*null,\s*"INVALID_STATUS"\)', 
                 'new UpdateEmployeeRequest(null, null, null, null, null, null, null)', content)
                 
content = content.replace('requestWithoutDate = new CreateEmployeeRequest(\n                    FULL_NAME, FULL_NAME, EMAIL, PHONE,\n                    DEPARTMENT, JOB_TITLE, SALARY,\n                    null // hire date intentionally null\n            );',
                          'requestWithoutDate = new CreateEmployeeRequest(\n                    FULL_NAME, EMAIL, PHONE,\n                    DEPARTMENT_ID, TYPE, SALARY, null);')
                          
with open(file_path, 'w') as f:
    f.write(content)

