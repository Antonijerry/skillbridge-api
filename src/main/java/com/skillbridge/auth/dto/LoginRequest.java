package com.skillbridge.auth.dto;

import java.util.Locale;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "is required") String email,
        @NotBlank(message = "is required") String password) {

    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=<hidden>]";
    }
}