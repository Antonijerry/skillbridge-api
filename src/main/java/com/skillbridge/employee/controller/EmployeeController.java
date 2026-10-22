package com.skillbridge.employee.controller;

import java.util.List;
import java.util.UUID;

import com.skillbridge.assessment.domain.AssessmentStatus;
import com.skillbridge.assessment.dto.AssessmentResponse;
import com.skillbridge.assessment.service.SkillAssessmentService;
import com.skillbridge.employee.dto.EmployeeResponse;
import com.skillbridge.employee.service.EmployeeService;
import com.skillbridge.security.CurrentUserResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final SkillAssessmentService assessmentService;
    private final CurrentUserResolver currentUsers;

    public EmployeeController(EmployeeService employeeService, SkillAssessmentService assessmentService,
                              CurrentUserResolver currentUsers) {
        this.employeeService = employeeService;
        this.assessmentService = assessmentService;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/me")
    public EmployeeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return employeeService.me(currentUsers.from(jwt));
    }

    @GetMapping("/{id}/skill-assessments")
    public List<AssessmentResponse> assessments(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                                                @RequestParam(name = "status", required = false) AssessmentStatus status) {
        return assessmentService.listForEmployee(currentUsers.from(jwt), id, status);
    }
}