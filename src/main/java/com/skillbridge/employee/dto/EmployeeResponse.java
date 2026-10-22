package com.skillbridge.employee.dto;

import java.time.Instant;
import java.util.UUID;

import com.skillbridge.employee.domain.Employee;
import com.skillbridge.employee.domain.Role;

/** The password hash is deliberately not a field here, so it can never be serialised. */
public record EmployeeResponse(UUID id, String fullName, String email, Role role, boolean active, Instant createdAt) {

    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(e.getId(), e.getFullName(), e.getEmail(), e.getRole(), e.isActive(), e.getCreatedAt());
    }
}