package com.heimdall4j.sidecar.autoconfigure;

import com.heimdall4j.sidecar.endpoint.HeimdallEndpoint;
import com.heimdall4j.sidecar.health.HeimdallHealthIndicator;
import com.heimdall4j.sidecar.logging.HeimdallEventLogger;
import com.heimdall4j.sidecar.metrics.HeimdallMetricsBinder;
import com.heimdall4j.spring.HeimdallRegistry;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Heimdall4j sidecar — metrics, health, endpoint, and logging.
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
}
