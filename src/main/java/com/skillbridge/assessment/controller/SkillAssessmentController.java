package com.skillbridge.assessment.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.skillbridge.assessment.domain.AssessmentStatus;
import com.skillbridge.assessment.dto.*;
import com.skillbridge.assessment.service.SkillAssessmentService;
import com.skillbridge.security.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Thin controller: resolve who is calling, parse, delegate, shape the HTTP response. Rules live in the service. */
@RestController
@RequestMapping("/api/skill-assessments")
public class SkillAssessmentController {

    private final SkillAssessmentService service;
    private final CurrentUserResolver currentUsers;

    public SkillAssessmentController(SkillAssessmentService service, CurrentUserResolver currentUsers) {
        this.service = service;
        this.currentUsers = currentUsers;
    }

    @PostMapping
    public ResponseEntity<AssessmentResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody CreateAssessmentRequest request) {
        AssessmentResponse created = service.create(currentUsers.from(jwt), request);
        return ResponseEntity.created(URI.create("/api/skill-assessments/" + created.id())).body(created);
    }

    @GetMapping
    public List<AssessmentResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "employeeId", required = false) UUID employeeId,
            @RequestParam(name = "skillId", required = false) UUID skillId,
            @RequestParam(name = "status", required = false) AssessmentStatus status) {
        return service.list(currentUsers.from(jwt), employeeId, skillId, status);
    }

    @GetMapping("/{id}")
    public AssessmentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return service.get(currentUsers.from(jwt), id);
    }

    @PutMapping("/{id}")
    public AssessmentResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                     @Valid @RequestBody UpdateAssessmentRequest request) {
        return service.update(currentUsers.from(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        service.delete(currentUsers.from(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/verify")
    public AssessmentResponse verify(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                     @Valid @RequestBody(required = false) VerifyRequest request) {
        String comment = request == null ? null : request.reviewComment();
        return service.verify(currentUsers.from(jwt), id, comment);
    }

    @PatchMapping("/{id}/reject")
    public AssessmentResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                     @Valid @RequestBody RejectRequest request) {
        return service.reject(currentUsers.from(jwt), id, request.reviewComment());
    }
}