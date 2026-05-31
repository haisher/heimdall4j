package io.github.haisher.heimdall4j.circuitbreaker.exception;

/**
 * Thrown when a call is attempted while the circuit breaker is OPEN.
 */
public final class CircuitOpenException extends RuntimeException {

    /** Circuit breaker name. */
    private final String circuitBreakerName;

    /**
     * Creates a new exception indicating the circuit is open.
     *
     * @param circuitBreakerName circuit breaker name
     */
    public CircuitOpenException(String circuitBreakerName) {
        super("CircuitBreaker '%s' is OPEN and not accepting calls".formatted(circuitBreakerName));

        this.circuitBreakerName = circuitBreakerName;
    }

    /** Returns the name of the circuit breaker that rejected the call.
     * @return circuit breaker name
     */
    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }
}
