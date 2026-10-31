package com.skillbridge.opportunity.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.skillbridge.opportunity.domain.Opportunity;
import com.skillbridge.opportunity.domain.OpportunityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {


    Page<Opportunity> findByStatus(OpportunityStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "requirements")
    List<Opportunity> findByIdIn(Collection<UUID> ids);
}