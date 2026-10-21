package com.skillbridge.auth.service;

import java.nio.charset.StandardCharsets;

import com.skillbridge.auth.dto.LoginRequest;
import com.skillbridge.auth.dto.RegisterRequest;
import com.skillbridge.auth.dto.RegisterResponse;
import com.skillbridge.auth.dto.TokenResponse;
import com.skillbridge.common.ApiException;
import com.skillbridge.employee.domain.Employee;
import com.skillbridge.employee.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    /** Checked when the email is unknown, so "no such account" costs about as long as "wrong password". */
    private final String timingDummyHash;

    public AuthService(EmployeeRepository employees, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.timingDummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must be at most 72 bytes");
        }
        if (employees.existsByEmail(request.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        Employee saved = employees.saveAndFlush(
                Employee.register(request.fullName(), request.email(), passwordEncoder.encode(request.password())));
        return RegisterResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Employee employee = employees.findByEmail(request.email()).orElse(null);
        String hash = employee != null ? employee.getPasswordHash() : timingDummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        // Unknown email, wrong password and inactive account all look identical to the caller.
        if (employee == null || !passwordMatches || !employee.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        return tokens.issue(employee);
    }
}