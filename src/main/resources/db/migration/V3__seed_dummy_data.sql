-- V3: dummy data for Week 1 demo. No real people, no real passwords.
-- The password hash is a clearly fake placeholder: it matches no password,
-- so seed users cannot log in. Week 2 registers real test accounts instead.

INSERT INTO employees (id, full_name, email, password_hash, role) VALUES
                                                                      ('11111111-1111-1111-1111-111111111111', 'Ada Okafor',  'ada@skillbridge.local',   '$2a$10$dummy.placeholder.not.a.real.hash.seed.data.only.xxxxxxxxxxxx', 'EMPLOYEE'),
                                                                      ('22222222-2222-2222-2222-222222222222', 'Tunde Bello', 'tunde@skillbridge.local', '$2a$10$dummy.placeholder.not.a.real.hash.seed.data.only.xxxxxxxxxxxx', 'MANAGER'),
                                                                      ('33333333-3333-3333-3333-333333333333', 'Chidi Eze',   'chidi@skillbridge.local', '$2a$10$dummy.placeholder.not.a.real.hash.seed.data.only.xxxxxxxxxxxx', 'ADMIN');

INSERT INTO skills (id, name, description, category) VALUES
                                                         ('aaaaaaaa-0000-0000-0000-000000000001', 'Java',              'Core Java language and JVM',            'Programming'),
                                                         ('aaaaaaaa-0000-0000-0000-000000000002', 'SQL',               'Relational queries and schema design',  'Data'),
                                                         ('aaaaaaaa-0000-0000-0000-000000000003', 'REST API Design',   'Resource modelling and HTTP semantics', 'Architecture'),
                                                         ('aaaaaaaa-0000-0000-0000-000000000004', 'Spring Boot',       'Building services with Spring Boot',    'Programming'),
                                                         ('aaaaaaaa-0000-0000-0000-000000000005', 'Docker',            'Containerising and running services',   'DevOps'),
                                                         ('aaaaaaaa-0000-0000-0000-000000000006', 'Technical Writing', 'Clear documentation for developers',    'Communication');

-- Two sample assessments so GET and the 409 rules can be demonstrated.
-- Ada has VERIFIED Java at BEGINNER (reviewed by Tunde) and a PENDING SQL claim.
INSERT INTO skill_assessments
(employee_id, skill_id, claimed_level, evidence_summary, status, reviewer_id, reviewed_at, review_comment)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-0000-0000-0000-000000000001', 'BEGINNER',
     'Completed an internal Java fundamentals course and a small CLI project.',
     'VERIFIED', '22222222-2222-2222-2222-222222222222', now(), 'Project reviewed and confirmed.');

INSERT INTO skill_assessments
(employee_id, skill_id, claimed_level, evidence_summary)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-0000-0000-0000-000000000002', 'INTERMEDIATE',
     'Wrote reporting queries with joins and indexes for the payroll team.');