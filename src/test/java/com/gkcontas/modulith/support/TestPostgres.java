package com.gkcontas.modulith.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One container for the whole suite, started once per JVM.
 *
 * <p>Deliberately not driven by the {@code @Testcontainers} extension: that ties a static
 * container's lifetime to the test class, stopping it when the class ends, while Spring
 * caches the application context for the next one — which then holds a pool pointing at a
 * database that no longer exists.
 *
 * <p>It is a plain holder rather than a base class because the module tests cannot extend
 * one: {@code @ApplicationModuleTest} derives the module under test from the package of
 * the annotated class, so each test class has to live in its own module's package.
 */
public final class TestPostgres {

    public static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("shop")
                    .withUsername("shop")
                    .withPassword("shop");

    static {
        INSTANCE.start();
    }

    private TestPostgres() {
    }

    public static void registerOn(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", INSTANCE::getUsername);
        registry.add("spring.datasource.password", INSTANCE::getPassword);
    }
}
