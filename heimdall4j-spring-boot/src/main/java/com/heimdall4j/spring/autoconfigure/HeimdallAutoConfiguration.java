package com.heimdall4j.spring.autoconfigure;

import com.heimdall4j.core.CircuitBreaker;
import com.heimdall4j.core.CircuitBreakerConfig;
import com.heimdall4j.spring.HeimdallRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that reads {@code heimdall4j.instances.*} properties
 * and registers configured {@link CircuitBreaker} instances in a {@link HeimdallRegistry}.
 */
@AutoConfiguration
@EnableConfigurationProperties(HeimdallProperties.class)
public class HeimdallAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HeimdallRegistry heimdallRegistry(HeimdallProperties properties) {
        var registry = new HeimdallRegistry();

        properties.getInstances().forEach((name, props) -> {
            var config = CircuitBreakerConfig.builder()
                    .failureRateThreshold(props.getFailureRateThreshold())
                    .ringBufferSize(props.getRingBufferSize())
                    .waitDurationInOpenState(props.getWaitDuration())
                    .permittedCallsInHalfOpen(props.getPermittedCallsInHalfOpen())
                    .callTimeout(props.getCallTimeout())
                    .build();

            registry.register(CircuitBreaker.of(name, config));
        });

        return registry;
    }
}
