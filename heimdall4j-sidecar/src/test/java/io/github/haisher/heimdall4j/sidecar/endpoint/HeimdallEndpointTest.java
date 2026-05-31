package io.github.haisher.heimdall4j.sidecar.endpoint;

import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreaker;
import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreakerConfig;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeimdallEndpointTest {

    @Test
    @DisplayName("returns empty circuit breakers map when registry is empty")
    void emptyRegistry() {
        var registry = new HeimdallRegistry();
        var endpoint = new HeimdallEndpoint(registry);

        var result = endpoint.circuitBreakers();

        assertThat(result).containsKey("circuitBreakers");
        @SuppressWarnings("unchecked")
        var breakers = (Map<String, Object>) result.get("circuitBreakers");
        assertThat(breakers).isEmpty();
    }

    @Test
    @DisplayName("exposes breaker state and config")
    @SuppressWarnings("unchecked")
    void exposesStateAndConfig() {
        var registry = new HeimdallRegistry();
        var config = CircuitBreakerConfig.builder()
                .failureRateThreshold(60)
                .ringBufferSize(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedCallsInHalfOpen(5)
                .callTimeout(Duration.ofSeconds(3))
                .build();
        registry.register(CircuitBreaker.of("payments", config));

        var endpoint = new HeimdallEndpoint(registry);
        var result = endpoint.circuitBreakers();

        var breakers = (Map<String, Object>) result.get("circuitBreakers");
        assertThat(breakers).containsKey("payments");

        var details = (Map<String, Object>) breakers.get("payments");
        assertThat(details.get("state")).isEqualTo("CLOSED");

        var configMap = (Map<String, Object>) details.get("config");
        assertThat(configMap.get("failureRateThreshold")).isEqualTo(60);
        assertThat(configMap.get("ringBufferSize")).isEqualTo(50);
        assertThat(configMap.get("permittedCallsInHalfOpen")).isEqualTo(5);
        assertThat(configMap.get("callTimeout")).isEqualTo("PT3S");
    }

    @Test
    @DisplayName("exposes multiple breakers")
    @SuppressWarnings("unchecked")
    void multipleBreakers() {
        var registry = new HeimdallRegistry();
        registry.register(CircuitBreaker.of("svc1", CircuitBreakerConfig.ofDefaults()));
        registry.register(CircuitBreaker.of("svc2", CircuitBreakerConfig.ofDefaults()));

        var endpoint = new HeimdallEndpoint(registry);
        var breakers = (Map<String, Object>) endpoint.circuitBreakers().get("circuitBreakers");

        assertThat(breakers).hasSize(2);
        assertThat(breakers).containsKeys("svc1", "svc2");
    }
}
