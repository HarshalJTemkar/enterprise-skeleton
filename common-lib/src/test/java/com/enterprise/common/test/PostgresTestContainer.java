package com.enterprise.common.test;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable PostgreSQL Testcontainer wrapper.
 *
 * <p>Use as a static field:</p>
 * <pre>{@code
 * @Testcontainers
 * class MyIT {
 *     @Container
 *     static PostgresTestContainer postgres = PostgresTestContainer.getInstance();
 *
 *     @DynamicPropertySource
 *     static void props(DynamicPropertyRegistry r) {
 *         r.add("spring.datasource.url", postgres::getJdbcUrl);
 *         r.add("spring.datasource.username", postgres::getUsername);
 *         r.add("spring.datasource.password", postgres::getPassword);
 *     }
 * }
 * }</pre>
 *
 * <p>The {@code stop()} override is a no-op so JUnit doesn't kill the
 * container between tests when reused as a singleton.</p>
 */
public class PostgresTestContainer extends PostgreSQLContainer<PostgresTestContainer> {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:16-alpine");
    private static PostgresTestContainer INSTANCE;

    private PostgresTestContainer() {
        super(IMAGE);
        withDatabaseName("test");
        withUsername("test");
        withPassword("test");
        withReuse(true);
    }

    /** Singleton accessor — shares one container across all tests in a JVM. */
    public static synchronized PostgresTestContainer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PostgresTestContainer();
            INSTANCE.start();
        }
        return INSTANCE;
    }

    /** Disable per-test stop; JVM shutdown hook handles cleanup. */
    @Override
    public void stop() {
        // intentional no-op for the singleton pattern
    }
}
