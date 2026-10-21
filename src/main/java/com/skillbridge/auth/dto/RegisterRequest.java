package com.skillbridge.auth.dto;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "is required")
        @Size(min = 2, max = 120, message = "must be between 2 and 120 characters") String fullName,
        @NotBlank(message = "is required")
        @Email(message = "must be a valid email address")
        @Size(max = 160, message = "must be at most 160 characters") String email,
        @NotBlank(message = "is required")
        @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password) {

    public RegisterRequest {
        fullName = fullName == null ? null : fullName.trim();
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        // The password is never trimmed or altered.
    }

    /** A record's default toString prints every field. This one must never print the password. */
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=<hidden>]";
    }
}