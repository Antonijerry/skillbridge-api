package com.skillbridge.employee.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Week 1 needs only existence checks on employees. Full mapping arrives with authentication in Week 2. */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    private UUID id;

    protected Employee() { }

    public UUID getId() { return id; }
}
