package io.github.haisher.heimdall4j.spring.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HeimdallPropertiesTest {

    @Test
    @DisplayName("instance properties start with null legacy values")
    void defaultValues() {
        var props = new HeimdallProperties.InstanceProperties();

        assertThat(props.getFailureRateThreshold()).isNull();
        assertThat(props.getRingBufferSize()).isNull();
        assertThat(props.getWaitDuration()).isNull();
        assertThat(props.getPermittedCallsInHalfOpen()).isNull();
        assertThat(props.getCallTimeout()).isNull();
        assertThat(props.getCircuitBreaker()).isNull();
        assertThat(props.getRetry()).isNull();
        assertThat(props.getRateLimiter()).isNull();
        assertThat(props.getTimeout()).isNull();
    }

    @Test
    @DisplayName("legacy setters update property values")
    void legacySettersWork() {
        var props = new HeimdallProperties.InstanceProperties();
        props.setFailureRateThreshold(75);
        props.setRingBufferSize(200);
        props.setWaitDuration(Duration.ofSeconds(60));
        props.setPermittedCallsInHalfOpen(20);
        props.setCallTimeout(Duration.ofSeconds(10));

        assertThat(props.getFailureRateThreshold()).isEqualTo(75);
        assertThat(props.getRingBufferSize()).isEqualTo(200);
        assertThat(props.getWaitDuration()).isEqualTo(Duration.ofSeconds(60));
        assertThat(props.getPermittedCallsInHalfOpen()).isEqualTo(20);
        assertThat(props.getCallTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(props.isLegacyConfig()).isTrue();
    }

    @Test
    @DisplayName("nested config properties have defaults")
    void nestedConfigDefaults() {
        var cbProps = new HeimdallProperties.CircuitBreakerProperties();
        assertThat(cbProps.getFailureRateThreshold()).isEqualTo(50);
        assertThat(cbProps.getRingBufferSize()).isEqualTo(100);
        assertThat(cbProps.getWaitDuration()).isEqualTo(Duration.ofSeconds(30));

        var retryProps = new HeimdallProperties.RetryProperties();
        assertThat(retryProps.getMaxAttempts()).isEqualTo(3);
        assertThat(retryProps.getDelay()).isEqualTo(Duration.ofMillis(500));
        assertThat(retryProps.getMultiplier()).isEqualTo(1.0);

        var rlProps = new HeimdallProperties.RateLimiterProperties();
        assertThat(rlProps.getLimitForPeriod()).isEqualTo(50);
        assertThat(rlProps.getRefreshPeriod()).isEqualTo(Duration.ofSeconds(1));

        var timeoutProps = new HeimdallProperties.TimeoutProperties();
        assertThat(timeoutProps.getDuration()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("instances map starts empty")
    void emptyInstancesMap() {
        var properties = new HeimdallProperties();
        assertThat(properties.getInstances()).isEmpty();
    }

    @Test
    @DisplayName("isLegacyConfig detects nested config")
    void isLegacyConfigFalseWithNestedConfig() {
        var props = new HeimdallProperties.InstanceProperties();
        props.setCircuitBreaker(new HeimdallProperties.CircuitBreakerProperties());
        assertThat(props.isLegacyConfig()).isFalse();
    }
}
