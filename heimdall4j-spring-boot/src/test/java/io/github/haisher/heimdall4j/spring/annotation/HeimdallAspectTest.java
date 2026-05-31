package io.github.haisher.heimdall4j.spring.annotation;

import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreaker;
import io.github.haisher.heimdall4j.circuitbreaker.CircuitBreakerConfig;
import io.github.haisher.heimdall4j.circuitbreaker.StateName;
import io.github.haisher.heimdall4j.circuitbreaker.exception.CircuitOpenException;
import io.github.haisher.heimdall4j.spring.HeimdallRegistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        HeimdallAspectTest.TestConfig.class,
        AopAutoConfiguration.class
})
class HeimdallAspectTest {

    @Autowired
    private TestService testService;

    @Autowired
    private HeimdallRegistry registry;

    @Test
    @DisplayName("successful call passes through circuit breaker")
    void successfulCall() {
        var result = testService.protectedCall();
        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("circuit opens after failures and invokes fallback")
    void fallbackOnOpenCircuit() {
        // Trip the breaker: ring buffer size is 2, threshold 50%
        // Need buffer full with failure as last call (threshold check is in recordFailure)
        testService.protectedCallWithFallback(false); // success, buffer count=1
        assertThatThrownBy(() -> testService.protectedCallWithFallback(true))
                .isInstanceOf(RuntimeException.class); // failure, buffer full, 50% ≥ 50% → OPEN

        var breaker = registry.get("withFallback").orElseThrow();
        assertThat(breaker.state()).isEqualTo(StateName.OPEN);

        var result = testService.protectedCallWithFallback(false);
        assertThat(result).isEqualTo("fallback");
    }

    @Test
    @DisplayName("throws CircuitOpenException when no fallback and circuit open")
    void noFallbackThrows() {
        // Trip the breaker: buffer size 2, threshold 50%
        testService.noFallbackCall(false); // success, buffer count=1
        assertThatThrownBy(() -> testService.noFallbackCall(true))
                .isInstanceOf(RuntimeException.class); // failure, buffer full → OPEN

        var breaker = registry.get("noFallback").orElseThrow();
        assertThat(breaker.state()).isEqualTo(StateName.OPEN);

        assertThatThrownBy(() -> testService.noFallbackCall(false))
                .isInstanceOf(CircuitOpenException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException for unregistered breaker name")
    void unregisteredBreaker() {
        assertThatThrownBy(() -> testService.unregisteredCall())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    @DisplayName("wraps checked exception thrown by annotated method")
    void checkedExceptionWrapping() {
        assertThatThrownBy(() -> testService.checkedExceptionCall())
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(Exception.class)
                .hasRootCauseMessage("checked");
    }

    @Test
    @DisplayName("resolves private fallback method via getDeclaredMethod")
    void privateFallbackResolution() {
        // Trip the breaker
        testService.privateFallbackCall(false);
        assertThatThrownBy(() -> testService.privateFallbackCall(true))
                .isInstanceOf(RuntimeException.class);

        var breaker = registry.get("privateFallback").orElseThrow();
        assertThat(breaker.state()).isEqualTo(StateName.OPEN);

        var result = testService.privateFallbackCall(false);
        assertThat(result).isEqualTo("private-fallback");
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        HeimdallRegistry heimdallRegistry() {
            var registry = new HeimdallRegistry();

            var smallConfig = CircuitBreakerConfig.builder()
                    .failureRateThreshold(50)
                    .ringBufferSize(2)
                    .waitDurationInOpenState(Duration.ofSeconds(60))
                    .permittedCallsInHalfOpen(1)
                    .callTimeout(Duration.ofSeconds(5))
                    .build();

            registry.register(CircuitBreaker.of("test", smallConfig));
            registry.register(CircuitBreaker.of("withFallback", smallConfig));
            registry.register(CircuitBreaker.of("noFallback", smallConfig));
            registry.register(CircuitBreaker.of("checked", smallConfig));
            registry.register(CircuitBreaker.of("privateFallback", smallConfig));
            return registry;
        }

        @Bean
        HeimdallAspect heimdallAspect(HeimdallRegistry registry) {
            return new HeimdallAspect(registry);
        }

        @Bean
        TestService testService() {
            return new TestService();
        }
    }

    static class TestService {

        @Heimdall("test")
        public String protectedCall() {
            return "ok";
        }

        @Heimdall("withFallback")
        public String protectedCallWithFallback(boolean shouldFail) {
            if (shouldFail) throw new RuntimeException("boom");
            return "ok";
        }

        public Supplier<String> protectedCallWithFallbackFallback() {
            return () -> "fallback";
        }

        @Heimdall("noFallback")
        public String noFallbackCall(boolean shouldFail) {
            if (shouldFail) throw new RuntimeException("boom");
            return "ok";
        }

        @Heimdall("nonexistent")
        public String unregisteredCall() {
            return "ok";
        }

        @Heimdall("checked")
        public String checkedExceptionCall() throws Exception {
            throw new Exception("checked");
        }

        @Heimdall("privateFallback")
        public String privateFallbackCall(boolean shouldFail) {
            if (shouldFail) throw new RuntimeException("boom");
            return "ok";
        }

        @SuppressWarnings("unused")
        private Supplier<String> privateFallbackCallFallback() {
            return () -> "private-fallback";
        }
    }
}
