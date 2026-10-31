package com.skillbridge.skill.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.skillbridge.common.PageParams;
import com.skillbridge.security.CurrentUserResolver;
import com.skillbridge.skill.dto.CreateSkillRequest;
import com.skillbridge.skill.dto.SkillResponse;
import com.skillbridge.skill.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService service;
    private final CurrentUserResolver currentUsers;

    public SkillController(SkillService service, CurrentUserResolver currentUsers) {
        this.service = service;
        this.currentUsers = currentUsers;
    }

    @GetMapping
    public ResponseEntity<List<SkillResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        currentUsers.from(jwt);   // re-checks that the account still exists and is active
        return service.list(active, PageParams.of(page, size)).toResponse();
    }

    @GetMapping("/{id}")
    public SkillResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        currentUsers.from(jwt);
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<SkillResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CreateSkillRequest request) {
        SkillResponse created = service.create(currentUsers.from(jwt), request);
        return ResponseEntity.created(URI.create("/api/skills/" + created.id())).body(created);
    }
}