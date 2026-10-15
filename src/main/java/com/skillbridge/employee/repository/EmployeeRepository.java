package com.skillbridge.employee.repository;

import java.util.UUID;

import com.skillbridge.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
}
