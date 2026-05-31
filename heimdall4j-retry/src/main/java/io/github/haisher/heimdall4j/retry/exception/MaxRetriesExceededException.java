package io.github.haisher.heimdall4j.retry.exception;

/**
 * Thrown when all retry attempts have been exhausted.
 */
public final class MaxRetriesExceededException extends RuntimeException {

    /** Retry executor name. */
    private final String name;
    /** Total attempts made. */
    private final int attempts;

    /**
     * Creates a new exception indicating retry exhaustion.
     *
     * @param name retry executor name
     * @param attempts total attempts made
     * @param lastException the final exception that caused exhaustion
     */
    public MaxRetriesExceededException(String name, int attempts, Throwable lastException) {
        super("Retry '%s' exhausted after %d attempts".formatted(name, attempts), lastException);
        this.name = name;
        this.attempts = attempts;
    }

    /** Returns the name of the retry executor that exhausted its attempts.
     * @return executor name
     */
    public String retryName() {
        return name;
    }

    /** Returns the total number of attempts made.
     * @return attempt count
     */
    public int attempts() {
        return attempts;
    }
}
