package com.skillbridge.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.skillbridge.employee.domain.Employee;
import com.skillbridge.employee.domain.Role;

public record RegisterResponse(UUID id, String fullName, String email, Role role, Instant createdAt) {

    public static RegisterResponse from(Employee e) {
        return new RegisterResponse(e.getId(), e.getFullName(), e.getEmail(), e.getRole(), e.getCreatedAt());
    }
}