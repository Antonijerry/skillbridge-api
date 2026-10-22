package com.skillbridge.opportunity.repository;

import java.util.List;
import java.util.UUID;

import com.skillbridge.opportunity.domain.Opportunity;
import com.skillbridge.opportunity.domain.OpportunityStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    @EntityGraph(attributePaths = "requirements")
    List<Opportunity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "requirements")
    List<Opportunity> findByStatusOrderByCreatedAtDesc(OpportunityStatus status);
}