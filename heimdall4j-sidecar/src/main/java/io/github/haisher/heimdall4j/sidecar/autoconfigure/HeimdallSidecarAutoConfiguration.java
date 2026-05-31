package io.github.haisher.heimdall4j.sidecar.autoconfigure;

import io.github.haisher.heimdall4j.sidecar.endpoint.HeimdallEndpoint;
import io.github.haisher.heimdall4j.sidecar.health.HeimdallHealthIndicator;
import io.github.haisher.heimdall4j.sidecar.logging.HeimdallEventLogger;
import io.github.haisher.heimdall4j.sidecar.logging.HeimdallResilienceEventLogger;
import io.github.haisher.heimdall4j.sidecar.metrics.HeimdallMetricsBinder;
import io.github.haisher.heimdall4j.sidecar.metrics.HeimdallResilienceMetricsBinder;
import io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Heimdall4j sidecar — metrics, health, endpoint, and logging
 * for both circuit breaker and full resilience policies.
 */
@AutoConfiguration
@ConditionalOnBean(HeimdallRegistry.class)
public class HeimdallSidecarAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    public HeimdallMetricsBinder heimdallMetricsBinder(MeterRegistry meterRegistry, HeimdallRegistry registry) {
        return new HeimdallMetricsBinder(meterRegistry, registry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(HeimdallPolicyRegistry.class)
    public HeimdallResilienceMetricsBinder heimdallResilienceMetricsBinder(
            MeterRegistry meterRegistry, HeimdallPolicyRegistry policyRegistry) {
        return new HeimdallResilienceMetricsBinder(meterRegistry, policyRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(name = "heimdallHealthIndicator")
    @ConditionalOnClass(HealthIndicator.class)
    public HeimdallHealthIndicator heimdallHealthIndicator(HeimdallRegistry registry) {
        return new HeimdallHealthIndicator(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Endpoint.class)
    public HeimdallEndpoint heimdallEndpoint(HeimdallRegistry registry) {
        return new HeimdallEndpoint(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public HeimdallEventLogger heimdallEventLogger(HeimdallRegistry registry) {
        return new HeimdallEventLogger(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(HeimdallPolicyRegistry.class)
    public HeimdallResilienceEventLogger heimdallResilienceEventLogger(HeimdallPolicyRegistry policyRegistry) {
        return new HeimdallResilienceEventLogger(policyRegistry);
    }
}
