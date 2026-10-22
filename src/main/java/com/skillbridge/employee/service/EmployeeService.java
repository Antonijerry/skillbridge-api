package com.skillbridge.employee.service;

import java.util.UUID;

import com.skillbridge.common.ApiException;
import com.skillbridge.employee.domain.Employee;
import com.skillbridge.employee.domain.Role;
import com.skillbridge.employee.dto.EmployeeResponse;
import com.skillbridge.employee.repository.EmployeeRepository;
import com.skillbridge.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employees;

    public EmployeeService(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public EmployeeResponse me(CurrentUser actor) {
        return employees.findById(actor.id()).map(EmployeeResponse::from)
                .orElseThrow(ApiException::unauthorized);
    }

    @Transactional
    public EmployeeResponse changeRole(CurrentUser actor, UUID id, Role role) {
        Employee target = findForAdmin(actor, id);
        if (actor.owns(id) && role != Role.ADMIN) {
            throw ApiException.conflict("INVALID_STATE", "An admin cannot demote themselves");
        }
        target.changeRole(role);
        return EmployeeResponse.from(target);
    }

    @Transactional
    public EmployeeResponse changeActive(CurrentUser actor, UUID id, boolean active) {
        Employee target = findForAdmin(actor, id);
        if (actor.owns(id) && !active) {
            throw ApiException.conflict("INVALID_STATE", "An admin cannot deactivate themselves");
        }
        target.changeActive(active);
        return EmployeeResponse.from(target);
    }

    /** Role first (403), then existence (404). */
    private Employee findForAdmin(CurrentUser actor, UUID id) {
        if (!actor.isAdmin()) {
            throw ApiException.forbidden();
        }
        return employees.findById(id).orElseThrow(() -> ApiException.notFound("Employee not found"));
    }
}