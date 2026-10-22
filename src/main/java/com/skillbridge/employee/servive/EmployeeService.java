package com.skillbridge.employee.service;

import com.skillbridge.common.ApiException;
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
}