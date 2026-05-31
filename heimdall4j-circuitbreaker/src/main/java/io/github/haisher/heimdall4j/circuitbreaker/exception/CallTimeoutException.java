package io.github.haisher.heimdall4j.circuitbreaker.exception;

import java.time.Duration;

/**
 * Thrown when a call exceeds the configured timeout duration.
 */
public final class CallTimeoutException extends RuntimeException {

    private final String circuitBreakerName;
    private final Duration configuredTimeout;

    public CallTimeoutException(String circuitBreakerName, Duration configuredTimeout) {
        super("CircuitBreaker '%s' call timed out after %s".formatted(circuitBreakerName, configuredTimeout));

        this.circuitBreakerName = circuitBreakerName;
        this.configuredTimeout = configuredTimeout;
    }

    /** Returns the name of the circuit breaker whose call timed out. */
    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    /** Returns the configured timeout duration that was exceeded. */
    public Duration getConfiguredTimeout() {
        return configuredTimeout;
    }
}
