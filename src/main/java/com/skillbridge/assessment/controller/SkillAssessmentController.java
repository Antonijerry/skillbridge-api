package com.skillbridge.assessment.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;



import com.skillbridge.assessment.domain.AssessmentStatus;
import com.skillbridge.assessment.dto.AssessmentResponse;
import com.skillbridge.assessment.dto.CreateAssessmentRequest;
import com.skillbridge.assessment.dto.UpdateAssessmentRequest;
import com.skillbridge.assessment.service.SkillAssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller: parse, delegate, shape the HTTP response. Rules live in the service. */
@RestController
@RequestMapping("/api/skill-assessments")
public class SkillAssessmentController {

    private final SkillAssessmentService service;

    public SkillAssessmentController(SkillAssessmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AssessmentResponse> create(@Valid @RequestBody CreateAssessmentRequest request) {
        AssessmentResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/skill-assessments/" + created.id())).body(created);
    }

    @GetMapping
    public List<AssessmentResponse> list(
            @RequestParam(name = "employeeId", required = false) UUID employeeId,
            @RequestParam(name = "skillId", required = false) UUID skillId,
            @RequestParam(name = "status", required = false) AssessmentStatus status) {
        return service.list(employeeId, skillId, status);
    }

    @GetMapping("/{id}")
    public AssessmentResponse get(@PathVariable("id") UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public AssessmentResponse update(@PathVariable("id") UUID id,
                                     @Valid @RequestBody UpdateAssessmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
