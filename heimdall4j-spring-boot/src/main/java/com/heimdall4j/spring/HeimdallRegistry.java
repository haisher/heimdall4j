package com.heimdall4j.spring;

import com.heimdall4j.core.CircuitBreaker;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry of named {@link CircuitBreaker} instances.
 * Registered as a singleton Spring bean by auto-configuration.
 */
public final class HeimdallRegistry {

    private final ConcurrentMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    /**
     * Registers a circuit breaker. Throws if a breaker with the same name already exists.
     */
    public void register(CircuitBreaker breaker) {
        var previous = breakers.putIfAbsent(breaker.name(), breaker);
        if (previous != null) {
            throw new IllegalArgumentException("Circuit breaker already registered: " + breaker.name());
        }
    }

    /**
     * Returns the circuit breaker with the given name, or empty if not registered.
     */
    public Optional<CircuitBreaker> get(String name) {
        return Optional.ofNullable(breakers.get(name));
    }

    /**
     * Returns an unmodifiable view of all registered circuit breakers.
     */
    public Collection<CircuitBreaker> getAll() {
        return Collections.unmodifiableCollection(breakers.values());
    }

    /**
     * Returns the number of registered circuit breakers.
     */
    public int size() {
        return breakers.size();
    }
}
