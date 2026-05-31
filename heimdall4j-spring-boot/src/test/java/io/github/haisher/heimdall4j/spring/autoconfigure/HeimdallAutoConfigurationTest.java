package io.github.haisher.heimdall4j.spring.autoconfigure;

import io.github.haisher.heimdall4j.circuitbreaker.StateName;
import io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;
import io.github.haisher.heimdall4j.spring.annotation.HeimdallAspect;
import io.github.haisher.heimdall4j.spring.annotation.ResilientAspect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HeimdallAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HeimdallAutoConfiguration.class));

    @Test
    @DisplayName("creates registry bean with no instances configured")
    void emptyRegistry() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HeimdallRegistry.class);
            assertThat(context.getBean(HeimdallRegistry.class).size()).isZero();
            assertThat(context).hasSingleBean(HeimdallPolicyRegistry.class);
            assertThat(context.getBean(HeimdallPolicyRegistry.class).size()).isZero();
        });
    }

    @Test
    @DisplayName("registers circuit breakers from legacy flat properties")
    void registersFromLegacyProperties() {
        contextRunner
                .withPropertyValues(
                        "heimdall4j.instances.payments.failure-rate-threshold=60",
                        "heimdall4j.instances.payments.ring-buffer-size=50",
                        "heimdall4j.instances.payments.wait-duration=10s",
                        "heimdall4j.instances.payments.permitted-calls-in-half-open=5",
                        "heimdall4j.instances.payments.call-timeout=3s",
                        "heimdall4j.instances.inventory.failure-rate-threshold=70"
                )
                .run(context -> {
                    var registry = context.getBean(HeimdallRegistry.class);
                    assertThat(registry.size()).isEqualTo(2);

                    var payments = registry.get("payments");
                    assertThat(payments).isPresent();
                    assertThat(payments.get().state()).isEqualTo(StateName.CLOSED);

                    var inventory = registry.get("inventory");
                    assertThat(inventory).isPresent();
                });
    }

    @Test
    @DisplayName("registers policies from nested config properties")
    void registersFromNestedProperties() {
        contextRunner
                .withPropertyValues(
                        "heimdall4j.instances.payments.circuit-breaker.failure-rate-threshold=60",
                        "heimdall4j.instances.payments.retry.max-attempts=3",
                        "heimdall4j.instances.payments.retry.delay=200ms",
                        "heimdall4j.instances.payments.rate-limiter.limit-for-period=100",
                        "heimdall4j.instances.payments.rate-limiter.refresh-period=1s",
                        "heimdall4j.instances.payments.timeout.duration=2s"
                )
                .run(context -> {
                    var policyRegistry = context.getBean(HeimdallPolicyRegistry.class);
                    assertThat(policyRegistry.size()).isEqualTo(1);

                    var policy = policyRegistry.get("payments");
                    assertThat(policy).isPresent();
                    assertThat(policy.get().circuitBreaker()).isNotNull();
                    assertThat(policy.get().retryExecutor()).isNotNull();
                    assertThat(policy.get().rateLimiterExecutor()).isNotNull();
                    assertThat(policy.get().timeoutExecutor()).isNotNull();
                });
    }

    @Test
    @DisplayName("applies default values for unspecified properties")
    void defaultPropertyValues() {
        contextRunner
                .withPropertyValues("heimdall4j.instances.myservice.failure-rate-threshold=40")
                .run(context -> {
                    var registry = context.getBean(HeimdallRegistry.class);
                    var breaker = registry.get("myservice");
                    assertThat(breaker).isPresent();
                    assertThat(breaker.get().state()).isEqualTo(StateName.CLOSED);
                });
    }

    @Test
    @DisplayName("does not override user-provided registry bean")
    void respectsConditionalOnMissingBean() {
        contextRunner
                .withBean(HeimdallRegistry.class, HeimdallRegistry::new)
                .withPropertyValues("heimdall4j.instances.payments.failure-rate-threshold=50")
                .run(context -> {
                    assertThat(context).hasSingleBean(HeimdallRegistry.class);
                    assertThat(context.getBean(HeimdallRegistry.class).size()).isZero();
                });
    }

    @Test
    @DisplayName("registers HeimdallAspect and ResilientAspect beans")
    void registersAspectBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HeimdallAspect.class);
            assertThat(context).hasSingleBean(ResilientAspect.class);
        });
    }
}
