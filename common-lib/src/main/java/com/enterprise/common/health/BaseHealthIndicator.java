package com.enterprise.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Template base class for custom health indicators. Subclasses implement
 * {@link #doCheck()} with the actual probe; the base handles exception
 * translation, so an unexpected failure reports {@code DOWN} rather than
 * crashing the {@code /actuator/health} endpoint.
 */
public abstract class BaseHealthIndicator implements HealthIndicator {

    private final String componentName;

    protected BaseHealthIndicator(String componentName) {
        this.componentName = componentName;
    }

    @Override
    public final Health health() {
        try {
            return doCheck();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("component", componentName)
                    .withDetail("error", ex.getClass().getSimpleName())
                    .withDetail("message", ex.getMessage())
                    .build();
        }
    }

    /**
     * Perform the actual probe. Return {@link Health#up()} or
     * {@link Health#down()} with any details required. Uncaught exceptions
     * are translated to {@code DOWN} automatically.
     */
    protected abstract Health doCheck() throws Exception;

    /** @return the human-readable component label used in error details. */
    protected String getComponentName() { return componentName; }
}
