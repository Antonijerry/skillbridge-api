package com.skillbridge.security;

import java.util.UUID;

import com.skillbridge.employee.domain.Role;

/** Who is calling: identity from the token, role and active status read fresh from the database. */
public record CurrentUser(UUID id, Role role) {

    public boolean isEmployee() {
        return role == Role.EMPLOYEE;
    }

    public boolean owns(UUID employeeId) {
        return id.equals(employeeId);
    }
}
