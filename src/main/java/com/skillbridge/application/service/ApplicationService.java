package com.skillbridge.application.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.skillbridge.application.domain.Application;
import com.skillbridge.application.domain.ApplicationStatus;
import com.skillbridge.application.dto.ApplicationResponse;
import com.skillbridge.application.repository.ApplicationRepository;
import com.skillbridge.common.ApiException;
import com.skillbridge.common.PageParams;
import com.skillbridge.common.PagedResult;
import com.skillbridge.opportunity.domain.Opportunity;
import com.skillbridge.opportunity.domain.OpportunityStatus;
import com.skillbridge.opportunity.repository.OpportunityRepository;
import com.skillbridge.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private final ApplicationRepository applications;
    private final OpportunityRepository opportunities;

    public ApplicationService(ApplicationRepository applications, OpportunityRepository opportunities) {
        this.applications = applications;
        this.opportunities = opportunities;
    }

    @Transactional
    public ApplicationResponse apply(CurrentUser actor, UUID opportunityId) {
        Opportunity opportunity = opportunities.findById(opportunityId)
                .orElseThrow(() -> ApiException.notFound("Opportunity not found"));
        if (opportunity.getCreatedBy().equals(actor.id())) {
            throw ApiException.conflict("INVALID_STATE", "You cannot apply to your own opportunity");
        }
        if (opportunity.getStatus() != OpportunityStatus.PUBLISHED) {
            throw ApiException.conflict("INVALID_STATE", "This opportunity is not open for applications");
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (opportunity.getClosingDate() == null || opportunity.getClosingDate().isBefore(today)) {
            throw ApiException.conflict("INVALID_STATE", "The closing date for this opportunity has passed");
        }
        if (applications.existsByEmployeeIdAndOpportunityId(actor.id(), opportunityId)) {
            throw ApiException.conflict("ALREADY_APPLIED", "You have already applied to this opportunity");
        }
        return ApplicationResponse.from(applications.saveAndFlush(Application.submit(actor.id(), opportunityId)));
    }

    @Transactional(readOnly = true)
    public PagedResult<ApplicationResponse> list(CurrentUser actor, ApplicationStatus status, UUID opportunityId,
                                                 PageParams paging) {
        Specification<Application> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actor.isEmployee()) {
                predicates.add(cb.equal(root.get("employeeId"), actor.id()));
            } else if (!actor.isAdmin()) {
                // A MANAGER sees their own applications plus applications to opportunities they created.
                Subquery<UUID> mine = query.subquery(UUID.class);
                Root<Opportunity> o = mine.from(Opportunity.class);
                mine.select(o.<UUID>get("id")).where(cb.equal(o.get("createdBy"), actor.id()));
                predicates.add(cb.or(cb.equal(root.get("employeeId"), actor.id()),
                        root.get("opportunityId").in(mine)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (opportunityId != null) {
                predicates.add(cb.equal(root.get("opportunityId"), opportunityId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Application> page = applications.findAll(filter, paging.pageable(PageParams.NEWEST_FIRST));
        return new PagedResult<>(page.getContent().stream().map(ApplicationResponse::from).toList(),
                page.getTotalElements());
    }

    @Transactional
    public ApplicationResponse shortlist(CurrentUser actor, UUID id) {
        Application application = findForReview(actor, id);
        if (application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw ApiException.conflict("INVALID_STATE", "Only a submitted application can be shortlisted");
        }
        application.shortlist();
        return saved(application);
    }

    @Transactional
    public ApplicationResponse reject(CurrentUser actor, UUID id) {
        Application application = findForReview(actor, id);
        if (application.getStatus() != ApplicationStatus.SUBMITTED
                && application.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw ApiException.conflict("INVALID_STATE", "Only a submitted or shortlisted application can be rejected");
        }
        application.reject();
        return saved(application);
    }

    /** Only the applicant. 404 first, then ownership (403). */
    @Transactional
    public ApplicationResponse withdraw(CurrentUser actor, UUID id) {
        Application application = applications.findById(id)
                .orElseThrow(() -> ApiException.notFound("Application not found"));
        if (!actor.owns(application.getEmployeeId())) {
            throw ApiException.forbidden();
        }
        if (application.getStatus() != ApplicationStatus.SUBMITTED
                && application.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw ApiException.conflict("INVALID_STATE", "Only a submitted or shortlisted application can be withdrawn");
        }
        application.withdraw();
        return saved(application);
    }

    /** Role first (403), then existence (404), then "creator of the opportunity or ADMIN" (403). */
    private Application findForReview(CurrentUser actor, UUID id) {
        if (!actor.isStaff()) {
            throw ApiException.forbidden();
        }
        Application application = applications.findById(id)
                .orElseThrow(() -> ApiException.notFound("Application not found"));
        Opportunity opportunity = opportunities.findById(application.getOpportunityId())
                .orElseThrow(() -> ApiException.notFound("Opportunity not found"));
        if (!actor.isAdmin() && !opportunity.getCreatedBy().equals(actor.id())) {
            throw ApiException.forbidden();
        }
        return application;
    }

    /** The entity is already managed: flush (not saveAndFlush) so updated_at is set before we build the response. */
    private ApplicationResponse saved(Application application) {
        applications.flush();
        return ApplicationResponse.from(application);
    }
}