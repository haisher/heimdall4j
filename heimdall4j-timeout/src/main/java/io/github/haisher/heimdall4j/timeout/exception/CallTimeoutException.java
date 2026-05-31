package io.github.haisher.heimdall4j.timeout.exception;

import java.time.Duration;

/**
 * Thrown when a call exceeds the configured timeout duration.
 */
public final class CallTimeoutException extends RuntimeException {

    private final String name;
    private final Duration timeout;

    public CallTimeoutException(String name, Duration timeout) {
        super("Timeout '%s' exceeded after %s".formatted(name, timeout));
        this.name = name;
        this.timeout = timeout;
    }

    /** Returns the name of the timeout executor that timed out. */
    public String timeoutName() {
        return name;
    }

    /** Returns the configured timeout duration that was exceeded. */
    public Duration timeout() {
        return timeout;
    }
}
