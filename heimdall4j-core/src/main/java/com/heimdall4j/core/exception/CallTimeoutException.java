package com.heimdall4j.core.exception;

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

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public Duration getConfiguredTimeout() {
        return configuredTimeout;
    }
}
