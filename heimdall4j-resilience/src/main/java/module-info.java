/** Resilience module — composable policy combining circuit breaker, retry, rate limiter, and timeout. */
module io.github.haisher.heimdall4j.resilience {
    requires transitive io.github.haisher.heimdall4j.circuitbreaker;
    requires transitive io.github.haisher.heimdall4j.retry;
    requires transitive io.github.haisher.heimdall4j.ratelimiter;
    requires transitive io.github.haisher.heimdall4j.timeout;

    exports io.github.haisher.heimdall4j.resilience;
}
