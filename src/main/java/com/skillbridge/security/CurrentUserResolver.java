package com.skillbridge.security;

import java.util.UUID;

import com.skillbridge.common.ApiException;
import com.skillbridge.employee.domain.Employee;
import com.skillbridge.employee.repository.EmployeeRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    private final EmployeeRepository employees;

    public CurrentUserResolver(EmployeeRepository employees) {
        this.employees = employees;
    }

    /** The token proves who you are. The database decides whether you still exist, are active, and what role you hold. */
    public CurrentUser from(Jwt jwt) {
        UUID id;
        try {
            id = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ApiException.unauthorized();
        }
        Employee employee = employees.findById(id)
                .filter(Employee::isActive)
                .orElseThrow(ApiException::unauthorized);
        return new CurrentUser(employee.getId(), employee.getRole());
    }
}