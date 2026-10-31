package com.skillbridge.application.repository;

import java.util.List;
import java.util.UUID;

import com.skillbridge.application.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    boolean existsByEmployeeIdAndOpportunityId(UUID employeeId, UUID opportunityId);

}