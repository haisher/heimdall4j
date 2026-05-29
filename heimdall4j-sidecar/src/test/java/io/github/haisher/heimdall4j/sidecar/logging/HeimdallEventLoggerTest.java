package io.github.haisher.heimdall4j.sidecar.logging;

import io.github.haisher.heimdall4j.core.CircuitBreaker;
import io.github.haisher.heimdall4j.core.CircuitBreakerConfig;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatNoException;

class HeimdallEventLoggerTest {

    @Test
    @DisplayName("subscribes to breaker events without errors")
    void subscribesSuccessfully() {
        var registry = new HeimdallRegistry();
        registry.register(CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults()));

        assertThatNoException().isThrownBy(() -> new HeimdallEventLogger(registry));
    }

    @Test
    @DisplayName("logs events without throwing on success")
    void logsSuccess() throws Exception {
        var registry = new HeimdallRegistry();
        var breaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
        registry.register(breaker);
        new HeimdallEventLogger(registry);

        // Execute a call — logger should handle the event without error
        assertThatNoException().isThrownBy(() -> breaker.execute(() -> "ok"));
        Thread.sleep(50); // allow async event delivery
    }

    @Test
    @DisplayName("logs events without throwing on failure")
    void logsFailure() throws Exception {
        var registry = new HeimdallRegistry();
        var breaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
        registry.register(breaker);
        new HeimdallEventLogger(registry);

        try {
            breaker.execute(() -> { throw new RuntimeException("boom"); });
        } catch (RuntimeException _) {}
        Thread.sleep(50); // allow async event delivery
    }

    @Test
    @DisplayName("logs state transition events")
    void logsStateTransition() throws Exception {
        var registry = new HeimdallRegistry();
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .ringBufferSize(2)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedCallsInHalfOpen(1)
                .callTimeout(Duration.ofSeconds(5))
                .build();
        var breaker = CircuitBreaker.of("test", config);
        registry.register(breaker);
        new HeimdallEventLogger(registry);

        // Trip the breaker
        breaker.execute(() -> "ok");
        try { breaker.execute(() -> { throw new RuntimeException(); }); } catch (RuntimeException _) {}
        Thread.sleep(50); // allow async event delivery
        // If we reach here, logging didn't throw
    }
}
