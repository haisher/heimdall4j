package io.github.haisher.heimdall4j.spring;

import io.github.haisher.heimdall4j.resilience.HeimdallPolicy;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry of named {@link HeimdallPolicy} instances.
 * Registered as a singleton Spring bean by auto-configuration.
 */
public final class HeimdallPolicyRegistry {

    private final ConcurrentMap<String, HeimdallPolicy> policies = new ConcurrentHashMap<>();

    /**
     * Registers a policy. Throws if a policy with the same name already exists.
     */
    public void register(HeimdallPolicy policy) {
        var previous = policies.putIfAbsent(policy.name(), policy);
        if (previous != null) {
            throw new IllegalArgumentException("Policy already registered: " + policy.name());
        }
    }

    /**
     * Returns the policy with the given name, or empty if not registered.
     */
    public Optional<HeimdallPolicy> get(String name) {
        return Optional.ofNullable(policies.get(name));
    }

    /**
     * Returns an unmodifiable view of all registered policies.
     */
    public Collection<HeimdallPolicy> getAll() {
        return Collections.unmodifiableCollection(policies.values());
    }

    /**
     * Returns the number of registered policies.
     */
    public int size() {
        return policies.size();
    }
}
