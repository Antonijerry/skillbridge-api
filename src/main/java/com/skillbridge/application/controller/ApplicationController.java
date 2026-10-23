package com.skillbridge.application.controller;

import java.util.List;
import java.util.UUID;

import com.skillbridge.application.domain.ApplicationStatus;
import com.skillbridge.application.dto.ApplicationResponse;
import com.skillbridge.application.dto.ApplyRequest;
import com.skillbridge.application.service.ApplicationService;
import com.skillbridge.security.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;
    private final CurrentUserResolver currentUsers;

    public ApplicationController(ApplicationService service, CurrentUserResolver currentUsers) {
        this.service = service;
        this.currentUsers = currentUsers;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> apply(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody ApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.apply(currentUsers.from(jwt), request.opportunityId()));
    }

    @GetMapping
    public List<ApplicationResponse> list(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(name = "status", required = false) ApplicationStatus status,
                                          @RequestParam(name = "opportunityId", required = false) UUID opportunityId) {
        return service.list(currentUsers.from(jwt), status, opportunityId);
    }

    @PatchMapping("/{id}/shortlist")
    public ApplicationResponse shortlist(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return service.shortlist(currentUsers.from(jwt), id);
    }

    @PatchMapping("/{id}/reject")
    public ApplicationResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return service.reject(currentUsers.from(jwt), id);
    }

    @PatchMapping("/{id}/withdraw")
    public ApplicationResponse withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return service.withdraw(currentUsers.from(jwt), id);
    }
}