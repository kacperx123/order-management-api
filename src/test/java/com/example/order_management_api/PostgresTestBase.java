package com.example.order_management_api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PostgresTestBase {

    // Singleton container shared by all test classes and cached Spring contexts.
    // A per-class @Container lifecycle breaks when Spring reuses a context across
    // classes while JUnit restarts the container underneath it.
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("orders")
                    .withUsername("orders")
                    .withPassword("orders");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "false");

        registry.add("app.outbox.publisher.enabled", () -> "false");
        registry.add("app.kafka.consumer.enabled", () -> "false");
        registry.add("app.kafka.producer.enabled", () -> "false");
    }

}
