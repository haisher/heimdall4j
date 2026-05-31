package io.github.haisher.heimdall4j.spring.autoconfigure;

import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreaker;
import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreakerConfig;
import io.github.haisher.heimdall4j.ratelimiter.RateLimiterConfig;
import io.github.haisher.heimdall4j.resilience.HeimdallPolicy;
import io.github.haisher.heimdall4j.retry.RetryConfig;
import io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;
import io.github.haisher.heimdall4j.spring.annotation.HeimdallAspect;
import io.github.haisher.heimdall4j.spring.annotation.ResilientAspect;
import io.github.haisher.heimdall4j.timeout.TimeoutConfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that reads {@code heimdall4j.instances.*} properties
 * and registers configured resilience policies and circuit breakers.
 */
@AutoConfiguration
@EnableConfigurationProperties(HeimdallProperties.class)
public class HeimdallAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HeimdallRegistry heimdallRegistry(HeimdallProperties properties) {
        var registry = new HeimdallRegistry();

        properties.getInstances().forEach((name, props) -> {
            CircuitBreakerConfig config;

            if (props.isLegacyConfig()) {
                config = buildFromLegacyProps(props);
            } else if (props.getCircuitBreaker() != null) {
                config = buildFromCbProps(props.getCircuitBreaker());
            } else {
                return;
            }

            registry.register(CircuitBreaker.of(name, config));
        });

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public HeimdallPolicyRegistry heimdallPolicyRegistry(HeimdallProperties properties) {
        var registry = new HeimdallPolicyRegistry();

        properties.getInstances().forEach((name, props) -> {
            if (props.isLegacyConfig()) {
                return;
            }

            var builder = HeimdallPolicy.of(name);
            boolean hasStrategy = false;

            if (props.getCircuitBreaker() != null) {
                builder.withCircuitBreaker(buildFromCbProps(props.getCircuitBreaker()));
                hasStrategy = true;
            }

            if (props.getRetry() != null) {
                var retryProps = props.getRetry();
                var retryConfig = RetryConfig.builder()
                        .maxAttempts(retryProps.getMaxAttempts())
                        .delay(retryProps.getDelay())
                        .multiplier(retryProps.getMultiplier())
                        .build();
                builder.withRetry(retryConfig);
                hasStrategy = true;
            }

            if (props.getRateLimiter() != null) {
                var rlProps = props.getRateLimiter();
                var rlConfig = RateLimiterConfig.builder()
                        .limitForPeriod(rlProps.getLimitForPeriod())
                        .refreshPeriod(rlProps.getRefreshPeriod())
                        .build();
                builder.withRateLimiter(rlConfig);
                hasStrategy = true;
            }

            if (props.getTimeout() != null) {
                var timeoutConfig = TimeoutConfig.builder()
                        .duration(props.getTimeout().getDuration())
                        .build();
                builder.withTimeout(timeoutConfig);
                hasStrategy = true;
            }

            if (hasStrategy) {
                registry.register(builder.build());
            }
        });

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public HeimdallAspect heimdallAspect(HeimdallRegistry registry) {
        return new HeimdallAspect(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public ResilientAspect resilientAspect(HeimdallPolicyRegistry policyRegistry) {
        return new ResilientAspect(policyRegistry);
    }

    private CircuitBreakerConfig buildFromLegacyProps(HeimdallProperties.InstanceProperties props) {
        var builder = CircuitBreakerConfig.builder();
        if (props.getFailureRateThreshold() != null) builder.failureRateThreshold(props.getFailureRateThreshold());
        if (props.getRingBufferSize() != null) builder.ringBufferSize(props.getRingBufferSize());
        if (props.getWaitDuration() != null) builder.waitDurationInOpenState(props.getWaitDuration());
        if (props.getPermittedCallsInHalfOpen() != null) builder.permittedCallsInHalfOpen(props.getPermittedCallsInHalfOpen());
        if (props.getCallTimeout() != null) builder.callTimeout(props.getCallTimeout());
        return builder.build();
    }

    private CircuitBreakerConfig buildFromCbProps(HeimdallProperties.CircuitBreakerProperties cbProps) {
        return CircuitBreakerConfig.builder()
                .failureRateThreshold(cbProps.getFailureRateThreshold())
                .ringBufferSize(cbProps.getRingBufferSize())
                .waitDurationInOpenState(cbProps.getWaitDuration())
                .permittedCallsInHalfOpen(cbProps.getPermittedCallsInHalfOpen())
                .callTimeout(cbProps.getCallTimeout())
                .build();
    }
}
