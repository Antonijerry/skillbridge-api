package com.skillbridge.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "skillbridge.jwt.secret=test-only-secret-that-is-at-least-32-bytes-long"
})
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    // One container for the whole test run (started once, stopped by Ryuk at JVM exit).
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}