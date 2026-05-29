package com.heimdall4j.spring.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HeimdallPropertiesTest {

    @Test
    @DisplayName("instance properties have sensible defaults")
    void defaultValues() {
        var props = new HeimdallProperties.InstanceProperties();

        assertThat(props.getFailureRateThreshold()).isEqualTo(50);
        assertThat(props.getRingBufferSize()).isEqualTo(100);
        assertThat(props.getWaitDuration()).isEqualTo(Duration.ofSeconds(30));
        assertThat(props.getPermittedCallsInHalfOpen()).isEqualTo(10);
        assertThat(props.getCallTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("setters update property values")
    void settersWork() {
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
    }

    @Test
    @DisplayName("instances map starts empty")
    void emptyInstancesMap() {
        var properties = new HeimdallProperties();
        assertThat(properties.getInstances()).isEmpty();
    }
}
