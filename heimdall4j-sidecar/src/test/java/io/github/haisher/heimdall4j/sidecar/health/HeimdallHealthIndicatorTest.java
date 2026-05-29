package io.github.haisher.heimdall4j.sidecar.health;

import io.github.haisher.heimdall4j.core.CircuitBreaker;
import io.github.haisher.heimdall4j.core.CircuitBreakerConfig;
import io.github.haisher.heimdall4j.core.StateName;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HeimdallHealthIndicatorTest {

    @Test
    @DisplayName("reports UP when all breakers are CLOSED")
    void allClosed() {
        var registry = new HeimdallRegistry();
        registry.register(CircuitBreaker.of("a", CircuitBreakerConfig.ofDefaults()));
        registry.register(CircuitBreaker.of("b", CircuitBreakerConfig.ofDefaults()));

        var indicator = new HeimdallHealthIndicator(registry);
        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("a", "CLOSED");
        assertThat(health.getDetails()).containsEntry("b", "CLOSED");
    }

    @Test
    @DisplayName("reports DOWN when any breaker is OPEN")
    void oneOpen() {
        var registry = new HeimdallRegistry();

        var smallConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(2)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedCallsInHalfOpen(1)
                .callTimeout(Duration.ofSeconds(5))
                .build();

        var breaker = CircuitBreaker.of("failing", smallConfig);
        registry.register(breaker);

        // Trip the breaker
        breaker.execute(() -> "ok"); // fill 1
        try { breaker.execute(() -> { throw new RuntimeException(); }); } catch (RuntimeException _) {}

        assertThat(breaker.state()).isEqualTo(StateName.OPEN);

        var indicator = new HeimdallHealthIndicator(registry);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("reports UP when registry is empty")
    void emptyRegistry() {
        var registry = new HeimdallRegistry();
        var indicator = new HeimdallHealthIndicator(registry);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("includes all breaker states in details")
    void detailsIncludeAllBreakers() {
        var registry = new HeimdallRegistry();
        registry.register(CircuitBreaker.of("svc1", CircuitBreakerConfig.ofDefaults()));
        registry.register(CircuitBreaker.of("svc2", CircuitBreakerConfig.ofDefaults()));

        var indicator = new HeimdallHealthIndicator(registry);
        var health = indicator.health();

        assertThat(health.getDetails()).hasSize(2);
    }
}
