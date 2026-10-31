package com.skillbridge.application.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.skillbridge.application.domain.Application;
import com.skillbridge.application.domain.ApplicationStatus;
import com.skillbridge.application.repository.ApplicationRepository;
import com.skillbridge.employee.domain.Role;
import com.skillbridge.opportunity.domain.Opportunity;
import com.skillbridge.opportunity.domain.OpportunityStatus;
import com.skillbridge.opportunity.repository.OpportunityRepository;
import com.skillbridge.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.skillbridge.support.ApiAssertions.assertFails;
import static com.skillbridge.support.ApiAssertions.assertForbidden;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationServiceTest {

    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final OpportunityRepository opportunities = mock(OpportunityRepository.class);
    private final ApplicationService service = new ApplicationService(applications, opportunities);

    private final UUID employeeId = UUID.randomUUID();
    private final UUID creatorId = UUID.randomUUID();
    private final UUID opportunityId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();

    private final CurrentUser employee = new CurrentUser(employeeId, Role.EMPLOYEE);
    private final CurrentUser creator = new CurrentUser(creatorId, Role.MANAGER);
    private final CurrentUser otherManager = new CurrentUser(UUID.randomUUID(), Role.MANAGER);
    private final CurrentUser admin = new CurrentUser(UUID.randomUUID(), Role.ADMIN);

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    // ---------- helpers ----------

    private void givenOpportunity(UUID createdBy, OpportunityStatus status, LocalDate closingDate) {
        Opportunity o = mock(Opportunity.class);
        when(o.getCreatedBy()).thenReturn(createdBy);
        when(o.getStatus()).thenReturn(status);
        when(o.getClosingDate()).thenReturn(closingDate);
        when(opportunities.findById(opportunityId)).thenReturn(Optional.of(o));
    }

    private void givenOpenOpportunity() {
        givenOpportunity(creatorId, OpportunityStatus.PUBLISHED, today().plusDays(7));
    }

    private Application givenApplication() {
        Application a = Application.submit(employeeId, opportunityId);
        when(applications.findById(applicationId)).thenReturn(Optional.of(a));
        return a;
    }

    private void givenSaveReturnsItsArgument() {
        when(applications.saveAndFlush(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- apply ----------

    @Test
    void applyCreatesASubmittedApplicationForTheCaller() {
        givenOpenOpportunity();
        givenSaveReturnsItsArgument();

        service.apply(employee, opportunityId);

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applications).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(saved.getValue().getEmployeeId()).isEqualTo(employeeId);
        assertThat(saved.getValue().getOpportunityId()).isEqualTo(opportunityId);
    }

    @Test
    void applyToUnknownOpportunityIsNotFound() {
        assertFails(() -> service.apply(employee, UUID.randomUUID()), "Opportunity not found");
    }

    @Test
    void cannotApplyToYourOwnOpportunity() {
        givenOpportunity(employeeId, OpportunityStatus.PUBLISHED, today().plusDays(7));

        assertFails(() -> service.apply(employee, opportunityId), "own opportunity");
        verify(applications, never()).saveAndFlush(any(Application.class));
    }

    @Test
    void cannotApplyToAnOpportunityThatIsNotPublished() {
        givenOpportunity(creatorId, OpportunityStatus.DRAFT, today().plusDays(7));

        assertFails(() -> service.apply(employee, opportunityId), "not open");
    }

    @Test
    void cannotApplyAfterTheClosingDate() {
        givenOpportunity(creatorId, OpportunityStatus.PUBLISHED, today().minusDays(1));

        assertFails(() -> service.apply(employee, opportunityId), "closing date");
    }

    @Test
    void canStillApplyOnTheClosingDateItself() {
        givenOpportunity(creatorId, OpportunityStatus.PUBLISHED, today());
        givenSaveReturnsItsArgument();

        service.apply(employee, opportunityId);

        verify(applications).saveAndFlush(any(Application.class));
    }

    @Test
    void cannotApplyWhenThereIsNoClosingDate() {
        givenOpportunity(creatorId, OpportunityStatus.PUBLISHED, null);

        assertFails(() -> service.apply(employee, opportunityId), "closing date");
    }

    @Test
    void cannotApplyTwice() {
        givenOpenOpportunity();
        when(applications.existsByEmployeeIdAndOpportunityId(employeeId, opportunityId)).thenReturn(true);

        assertFails(() -> service.apply(employee, opportunityId), "already applied");
        verify(applications, never()).saveAndFlush(any(Application.class));
    }

    // ---------- shortlist ----------

    @Test
    void managerWhoCreatedTheOpportunityCanShortlistASubmittedApplication() {
        givenOpenOpportunity();
        Application a = givenApplication();

        service.shortlist(creator, applicationId);

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
    }

    @Test
    void cannotShortlistTwice() {
        givenOpenOpportunity();
        Application a = givenApplication();
        a.shortlist();

        assertFails(() -> service.shortlist(creator, applicationId), "Only a submitted application");
    }

    @Test
    void employeeCannotShortlistEvenWhenTheApplicationDoesNotExist() {
        // role is checked first: 403, not 404
        assertForbidden(() -> service.shortlist(employee, UUID.randomUUID()));
    }

    @Test
    void managerWhoDidNotCreateTheOpportunityCannotShortlist() {
        givenOpenOpportunity();
        givenApplication();

        assertForbidden(() -> service.shortlist(otherManager, applicationId));
    }

    @Test
    void adminCanShortlistAnyApplication() {
        givenOpenOpportunity();
        Application a = givenApplication();

        service.shortlist(admin, applicationId);

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
    }

    @Test
    void shortlistingAnUnknownApplicationIsNotFound() {
        assertFails(() -> service.shortlist(creator, UUID.randomUUID()), "Application not found");
    }

    // ---------- reject ----------

    @Test
    void canRejectASubmittedApplication() {
        givenOpenOpportunity();
        Application a = givenApplication();

        service.reject(creator, applicationId);

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void canRejectAShortlistedApplication() {
        givenOpenOpportunity();
        Application a = givenApplication();
        a.shortlist();

        service.reject(creator, applicationId);

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void cannotRejectAnAlreadyRejectedApplication() {
        givenOpenOpportunity();
        Application a = givenApplication();
        a.reject();

        assertFails(() -> service.reject(creator, applicationId), "submitted or shortlisted");
    }

    @Test
    void cannotRejectAWithdrawnApplication() {
        givenOpenOpportunity();
        Application a = givenApplication();
        a.withdraw();

        assertFails(() -> service.reject(creator, applicationId), "submitted or shortlisted");
    }

    // ---------- withdraw ----------

    @Test
    void applicantCanWithdrawASubmittedApplication() {
        Application a = givenApplication();

        service.withdraw(employee, applicationId);

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void applicantCanWithdrawAShortlistedApplication() {
        Application a = givenApplication();
        a.shortlist();

        service.withdraw(employee, applicationId);

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void someoneElseCannotWithdrawYourApplicationEvenAManager() {
        givenApplication();

        assertForbidden(() -> service.withdraw(creator, applicationId));
    }

    @Test
    void cannotWithdrawARejectedApplication() {
        Application a = givenApplication();
        a.reject();

        assertFails(() -> service.withdraw(employee, applicationId), "submitted or shortlisted");
    }

    @Test
    void withdrawingAnUnknownApplicationIsNotFound() {
        assertFails(() -> service.withdraw(employee, UUID.randomUUID()), "Application not found");
    }
}