package com.heimdall4j.sidecar.health;

import com.heimdall4j.core.CircuitBreaker;
import com.heimdall4j.core.StateName;
import com.heimdall4j.spring.HeimdallRegistry;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Spring Boot Actuator health indicator for circuit breakers.
 *
 * <p>Reports UP when all breakers are CLOSED, DOWN when any is OPEN,
 * and UNKNOWN when any is HALF_OPEN (but none OPEN).</p>
 */
public class HeimdallHealthIndicator implements HealthIndicator {

    private final HeimdallRegistry registry;

    public HeimdallHealthIndicator(HeimdallRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        var builder = Health.up();
        boolean anyOpen = false;
        boolean anyHalfOpen = false;

        for (CircuitBreaker breaker : registry.getAll()) {
            var state = breaker.state();
            builder.withDetail(breaker.name(), state.name());

            if (state == StateName.OPEN) {
                anyOpen = true;
            } else if (state == StateName.HALF_OPEN) {
                anyHalfOpen = true;
            }
        }

        if (anyOpen) {
            return builder.down().build();
        }
        if (anyHalfOpen) {
            return builder.unknown().build();
        }
        return builder.build();
    }
}
