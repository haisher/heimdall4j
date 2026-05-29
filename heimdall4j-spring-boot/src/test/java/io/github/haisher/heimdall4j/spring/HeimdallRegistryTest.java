package io.github.haisher.heimdall4j.spring;

import io.github.haisher.heimdall4j.core.CircuitBreaker;
import io.github.haisher.heimdall4j.core.CircuitBreakerConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeimdallRegistryTest {

    @Test
    @DisplayName("register and retrieve a circuit breaker")
    void registerAndGet() {
        var registry = new HeimdallRegistry();
        var breaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());

        registry.register(breaker);

        assertThat(registry.get("test")).isPresent();
        assertThat(registry.get("test").get()).isSameAs(breaker);
    }

    @Test
    @DisplayName("get returns empty for unknown name")
    void getReturnsEmpty() {
        var registry = new HeimdallRegistry();
        assertThat(registry.get("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("register throws on duplicate name")
    void rejectsDuplicate() {
        var registry = new HeimdallRegistry();
        var breaker1 = CircuitBreaker.of("dup", CircuitBreakerConfig.ofDefaults());
        var breaker2 = CircuitBreaker.of("dup", CircuitBreakerConfig.ofDefaults());

        registry.register(breaker1);

        assertThatThrownBy(() -> registry.register(breaker2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dup");
    }

    @Test
    @DisplayName("getAll returns all registered breakers")
    void getAllReturnsAll() {
        var registry = new HeimdallRegistry();
        registry.register(CircuitBreaker.of("a", CircuitBreakerConfig.ofDefaults()));
        registry.register(CircuitBreaker.of("b", CircuitBreakerConfig.ofDefaults()));

        assertThat(registry.getAll()).hasSize(2);
    }

    @Test
    @DisplayName("size reflects registered count")
    void sizeTracksRegistrations() {
        var registry = new HeimdallRegistry();
        assertThat(registry.size()).isZero();

        registry.register(CircuitBreaker.of("x", CircuitBreakerConfig.ofDefaults()));
        assertThat(registry.size()).isEqualTo(1);
    }
}
