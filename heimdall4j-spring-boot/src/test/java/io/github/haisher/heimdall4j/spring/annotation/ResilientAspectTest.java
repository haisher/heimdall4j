package io.github.haisher.heimdall4j.spring.annotation;

import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreakerConfig;
import io.github.haisher.heimdall4j.resilience.HeimdallPolicy;
import io.github.haisher.heimdall4j.retry.RetryConfig;
import io.github.haisher.heimdall4j.retry.exception.MaxRetriesExceededException;
import io.github.haisher.heimdall4j.spring.HeimdallPolicyRegistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        ResilientAspectTest.TestConfig.class,
        AopAutoConfiguration.class
})
class ResilientAspectTest {

    @Autowired
    private TestService testService;

    @Test
    @DisplayName("successful call passes through policy")
    void successfulCall() {
        var result = testService.resilientCall();
        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("retries on transient failure")
    void retriesOnFailure() {
        testService.resetCounter();
        var result = testService.retriableCall();
        assertThat(result).isEqualTo("recovered");
        assertThat(testService.getCounter()).isEqualTo(3);
    }

    @Test
    @DisplayName("invokes fallback when retries exhausted")
    void fallbackWhenExhausted() {
        var result = testService.alwaysFailsWithFallback();
        assertThat(result).isEqualTo("fallback-value");
    }

    @Test
    @DisplayName("throws when no fallback and retries exhausted")
    void throwsWhenNoFallback() {
        assertThatThrownBy(() -> testService.alwaysFailsNoFallback())
                .isInstanceOf(MaxRetriesExceededException.class);
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        HeimdallPolicyRegistry policyRegistry() {
            var registry = new HeimdallPolicyRegistry();

            registry.register(HeimdallPolicy.of("simple")
                    .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                    .build());

            registry.register(HeimdallPolicy.of("retriable")
                    .withRetry(RetryConfig.fixedDelay(3, Duration.ZERO))
                    .build());

            registry.register(HeimdallPolicy.of("failing")
                    .withRetry(RetryConfig.fixedDelay(2, Duration.ZERO))
                    .build());

            return registry;
        }

        @Bean
        ResilientAspect resilientAspect(HeimdallPolicyRegistry registry) {
            return new ResilientAspect(registry);
        }

        @Bean
        TestService testService() {
            return new TestService();
        }
    }

    static class TestService {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Resilient("simple")
        public String resilientCall() {
            return "ok";
        }

        @Resilient("retriable")
        public String retriableCall() {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("transient");
            }
            return "recovered";
        }

        @Resilient("failing")
        public String alwaysFailsWithFallback() {
            throw new RuntimeException("always fails");
        }

        public Supplier<String> alwaysFailsWithFallbackFallback() {
            return () -> "fallback-value";
        }

        @Resilient("failing")
        public String alwaysFailsNoFallback() {
            throw new RuntimeException("always fails");
        }

        public void resetCounter() {
            counter.set(0);
        }

        public int getCounter() {
            return counter.get();
        }
    }
}
