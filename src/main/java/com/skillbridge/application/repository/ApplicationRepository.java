package com.skillbridge.application.repository;

import java.util.List;
import java.util.UUID;

import com.skillbridge.application.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByEmployeeIdAndOpportunityId(UUID employeeId, UUID opportunityId);

    List<Application> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    List<Application> findAllByOrderByCreatedAtDesc();

    /** My own applications, plus applications to opportunities I created. */
    @Query("select a from Application a where a.employeeId = :me "
            + "or a.opportunityId in (select o.id from Opportunity o where o.createdBy = :me) "
            + "order by a.createdAt desc")
    List<Application> findVisibleTo(@Param("me") UUID me);
}