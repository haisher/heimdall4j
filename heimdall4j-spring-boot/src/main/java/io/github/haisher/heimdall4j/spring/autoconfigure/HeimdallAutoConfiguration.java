package io.github.haisher.heimdall4j.spring.autoconfigure;

import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreaker;
import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreakerConfig;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;
import io.github.haisher.heimdall4j.spring.annotation.HeimdallAspect;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public HeimdallAspect heimdallAspect(HeimdallRegistry registry) {
        return new HeimdallAspect(registry);
    }
}
