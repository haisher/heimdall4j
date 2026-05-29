package io.github.haisher.heimdall4j.spring.autoconfigure;

import io.github.haisher.heimdall4j.core.StateName;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;
import io.github.haisher.heimdall4j.spring.annotation.HeimdallAspect;

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
        });
    }

    @Test
    @DisplayName("registers circuit breakers from properties")
    void registersFromProperties() {
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
    @DisplayName("applies default values for unspecified properties")
    void defaultPropertyValues() {
        contextRunner
                .withPropertyValues("heimdall4j.instances.myservice.failure-rate-threshold=40")
                .run(context -> {
                    var registry = context.getBean(HeimdallRegistry.class);
                    var breaker = registry.get("myservice");
                    assertThat(breaker).isPresent();
                    // Breaker starts in CLOSED state
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
                    // User-provided bean is empty — auto-config didn't populate it
                    assertThat(context.getBean(HeimdallRegistry.class).size()).isZero();
                });
    }

    @Test
    @DisplayName("registers HeimdallAspect bean when AOP is available")
    void registersAspectBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HeimdallAspect.class);
        });
    }
}
