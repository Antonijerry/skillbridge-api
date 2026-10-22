package com.skillbridge.employee.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveChangeRequest(@NotNull(message = "is required") Boolean active) { }