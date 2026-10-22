package com.skillbridge.employee.dto;

import com.skillbridge.employee.domain.Role;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(@NotNull(message = "is required") Role role) { }