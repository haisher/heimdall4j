package io.github.haisher.heimdall4j.retry.exception;

/**
 * Thrown when all retry attempts have been exhausted.
 */
public final class MaxRetriesExceededException extends RuntimeException {

    private final String name;
    private final int attempts;

    public MaxRetriesExceededException(String name, int attempts, Throwable lastException) {
        super("Retry '%s' exhausted after %d attempts".formatted(name, attempts), lastException);
        this.name = name;
        this.attempts = attempts;
    }

    public String retryName() {
        return name;
    }

    public int attempts() {
        return attempts;
    }
}
