package io.github.haisher.heimdall4j.circuitbreaker;

/**
 * Public representation of circuit breaker states.
 */
public enum StateName {
    CLOSED,
    OPEN,
    HALF_OPEN
}
