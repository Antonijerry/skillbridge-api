package com.skillbridge.opportunity.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.skillbridge.opportunity.domain.OpportunityStatus;
import com.skillbridge.opportunity.dto.AddRequirementRequest;
import com.skillbridge.opportunity.dto.OpportunityRequest;
import com.skillbridge.opportunity.dto.OpportunityResponse;
import com.skillbridge.opportunity.dto.RequirementResponse;
import com.skillbridge.opportunity.service.OpportunityService;
import com.skillbridge.security.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final OpportunityService service;
    private final CurrentUserResolver currentUsers;

    public OpportunityController(OpportunityService service, CurrentUserResolver currentUsers) {
        this.service = service;
        this.currentUsers = currentUsers;
    }

    @PostMapping
    public ResponseEntity<OpportunityResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody OpportunityRequest request) {
        OpportunityResponse created = service.create(currentUsers.from(jwt), request);
        return ResponseEntity.created(URI.create("/api/opportunities/" + created.id())).body(created);
    }

    @GetMapping
    public List<OpportunityResponse> list(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(name = "status", required = false) OpportunityStatus status) {
        return service.list(currentUsers.from(jwt), status);
    }

    @GetMapping("/{id}")
    public OpportunityResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return service.get(currentUsers.from(jwt), id);
    }

    @PutMapping("/{id}")
    public OpportunityResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                      @Valid @RequestBody OpportunityRequest request) {
        return service.update(currentUsers.from(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        service.delete(currentUsers.from(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementResponse addRequirement(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                              @Valid @RequestBody AddRequirementRequest request) {
        return service.addRequirement(currentUsers.from(jwt), id, request);
    }

    @DeleteMapping("/{id}/requirements/{requirementId}")
    public ResponseEntity<Void> removeRequirement(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                                  @PathVariable("requirementId") UUID requirementId) {
        service.removeRequirement(currentUsers.from(jwt), id, requirementId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    public OpportunityResponse publish(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return service.publish(currentUsers.from(jwt), id);
    }

    @PatchMapping("/{id}/close")
    public OpportunityResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return service.close(currentUsers.from(jwt), id);
    }
}