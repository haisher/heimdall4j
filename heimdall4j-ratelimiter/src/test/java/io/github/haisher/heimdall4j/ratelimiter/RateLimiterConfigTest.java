package io.github.haisher.heimdall4j.ratelimiter;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterConfigTest {

    @Test
    void builderCreatesConfigWithDefaults() {
        var config = RateLimiterConfig.builder().build();
        assertEquals(50, config.limitForPeriod());
        assertEquals(Duration.ofSeconds(1), config.refreshPeriod());
    }

    @Test
    void factoryMethod() {
        var config = RateLimiterConfig.of(100, Duration.ofSeconds(2));
        assertEquals(100, config.limitForPeriod());
        assertEquals(Duration.ofSeconds(2), config.refreshPeriod());
    }

    @Test
    void builderCustomValues() {
        var config = RateLimiterConfig.builder()
                .limitForPeriod(200)
                .refreshPeriod(Duration.ofMinutes(1))
                .build();
        assertEquals(200, config.limitForPeriod());
        assertEquals(Duration.ofMinutes(1), config.refreshPeriod());
    }

    @Test
    void rejectsZeroLimit() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.of(0, Duration.ofSeconds(1)));
    }

    @Test
    void rejectsNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.of(-1, Duration.ofSeconds(1)));
    }

    @Test
    void rejectsZeroRefreshPeriod() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.of(10, Duration.ZERO));
    }

    @Test
    void rejectsNegativeRefreshPeriod() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.of(10, Duration.ofMillis(-1)));
    }

    @Test
    void rejectsNullRefreshPeriod() {
        assertThrows(NullPointerException.class, () -> RateLimiterConfig.of(10, null));
    }
}
