package io.github.haisher.heimdall4j.timeout.exception;

import java.time.Duration;

/**
 * Thrown when a call exceeds the configured timeout duration.
 */
public final class CallTimeoutException extends RuntimeException {

    /** Timeout executor name. */
    private final String name;
    /** Configured timeout duration. */
    private final Duration timeout;

    /**
     * Creates a new timeout exception.
     *
     * @param name timeout executor name
     * @param timeout the configured timeout that was exceeded
     */
    public CallTimeoutException(String name, Duration timeout) {
        super("Timeout '%s' exceeded after %s".formatted(name, timeout));
        this.name = name;
        this.timeout = timeout;
    }

    /** Returns the name of the timeout executor that timed out.
     * @return executor name
     */
    public String timeoutName() {
        return name;
    }

    /** Returns the configured timeout duration that was exceeded.
     * @return timeout duration
     */
    public Duration timeout() {
        return timeout;
    }
}
