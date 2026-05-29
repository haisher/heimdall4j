package com.heimdall4j.core.exception;

import com.heimdall4j.core.StateName;

/**
 * Thrown when a call is attempted while the circuit breaker is OPEN.
 */
public final class CircuitOpenException extends RuntimeException {

    private final String circuitBreakerName;

    public CircuitOpenException(String circuitBreakerName) {
        super("CircuitBreaker '%s' is OPEN and not accepting calls".formatted(circuitBreakerName));
        this.circuitBreakerName = circuitBreakerName;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }
}
