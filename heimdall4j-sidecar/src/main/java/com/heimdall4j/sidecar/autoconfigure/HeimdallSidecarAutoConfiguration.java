package com.heimdall4j.sidecar.autoconfigure;

import com.heimdall4j.sidecar.health.HeimdallHealthIndicator;
import com.heimdall4j.sidecar.metrics.HeimdallMetricsBinder;
import com.heimdall4j.spring.HeimdallRegistry;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Heimdall4j sidecar — metrics and health.
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
}
