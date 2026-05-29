package io.github.haisher.heimdall4j.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Test clock that can be manually advanced. Package-private test utility.
 */
final class MutableClock extends Clock {
    private Instant instant;

    MutableClock(Instant initial) {
        this.instant = initial;
    }

    void advance(Duration duration) {
        this.instant = this.instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
