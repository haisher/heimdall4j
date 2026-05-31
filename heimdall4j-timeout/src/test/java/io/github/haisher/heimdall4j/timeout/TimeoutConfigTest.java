package io.github.haisher.heimdall4j.timeout;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TimeoutConfigTest {

    @Test
    void builderCreatesConfigWithDefaults() {
        var config = TimeoutConfig.builder().build();
        assertEquals(Duration.ofSeconds(5), config.duration());
    }

    @Test
    void factoryMethod() {
        var config = TimeoutConfig.ofDuration(Duration.ofSeconds(2));
        assertEquals(Duration.ofSeconds(2), config.duration());
    }

    @Test
    void builderCustomDuration() {
        var config = TimeoutConfig.builder()
                .duration(Duration.ofMillis(500))
                .build();
        assertEquals(Duration.ofMillis(500), config.duration());
    }

    @Test
    void rejectsZeroDuration() {
        assertThrows(IllegalArgumentException.class, () -> TimeoutConfig.ofDuration(Duration.ZERO));
    }

    @Test
    void rejectsNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> TimeoutConfig.ofDuration(Duration.ofMillis(-1)));
    }

    @Test
    void rejectsNullDuration() {
        assertThrows(NullPointerException.class, () -> TimeoutConfig.ofDuration(null));
    }
}
