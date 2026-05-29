package io.github.haisher.heimdall4j.sidecar.endpoint;

import io.github.haisher.heimdall4j.core.CircuitBreaker;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Actuator endpoint exposing circuit breaker state at {@code /actuator/circuitbreakers}.
 */
@Endpoint(id = "circuitbreakers")
public class HeimdallEndpoint {

    private final HeimdallRegistry registry;

    public HeimdallEndpoint(HeimdallRegistry registry) {
        this.registry = registry;
    }

    @ReadOperation
    public Map<String, Object> circuitBreakers() {
        var result = new LinkedHashMap<String, Object>();
        var breakersMap = new LinkedHashMap<String, Object>();

        for (CircuitBreaker breaker : registry.getAll()) {
            var details = new LinkedHashMap<String, Object>();
            details.put("state", breaker.state().name());

            var config = breaker.config();
            var configMap = new LinkedHashMap<String, Object>();
            configMap.put("failureRateThreshold", config.failureRateThreshold());
            configMap.put("ringBufferSize", config.ringBufferSize());
            configMap.put("waitDurationInOpenState", config.waitDurationInOpenState().toString());
            configMap.put("permittedCallsInHalfOpen", config.permittedCallsInHalfOpen());
            configMap.put("callTimeout", config.callTimeout().toString());
            details.put("config", configMap);

            breakersMap.put(breaker.name(), details);
        }

        result.put("circuitBreakers", breakersMap);
        return result;
    }
}
