package com.skillbridge.opportunity.controller;

import java.util.UUID;

import com.skillbridge.opportunity.dto.MatchResponse;
import com.skillbridge.opportunity.service.MatchService;
import com.skillbridge.security.CurrentUserResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/opportunities")
public class MatchController {

    private final MatchService matchService;
    private final CurrentUserResolver currentUsers;

    public MatchController(MatchService matchService, CurrentUserResolver currentUsers) {
        this.matchService = matchService;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/{id}/match")
    public MatchResponse match(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                               @RequestParam(name = "employeeId", required = false) UUID employeeId) {
        return matchService.match(currentUsers.from(jwt), id, employeeId);
    }
}