package io.github.haisher.heimdall4j.circuitbreaker.exception;

import java.time.Duration;

/**
 * Thrown when a call exceeds the configured timeout duration.
 */
public final class CallTimeoutException extends RuntimeException {

    /** Circuit breaker name. */
    private final String circuitBreakerName;
    /** Configured timeout duration. */
    private final Duration configuredTimeout;

    /**
     * Creates a new timeout exception.
     *
     * @param circuitBreakerName circuit breaker name
     * @param configuredTimeout the configured timeout that was exceeded
     */
    public CallTimeoutException(String circuitBreakerName, Duration configuredTimeout) {
        super("CircuitBreaker '%s' call timed out after %s".formatted(circuitBreakerName, configuredTimeout));

        this.circuitBreakerName = circuitBreakerName;
        this.configuredTimeout = configuredTimeout;
    }

    /** Returns the name of the circuit breaker whose call timed out.
     * @return circuit breaker name
     */
    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    /** Returns the configured timeout duration that was exceeded.
     * @return timeout duration
     */
    public Duration getConfiguredTimeout() {
        return configuredTimeout;
    }
}
