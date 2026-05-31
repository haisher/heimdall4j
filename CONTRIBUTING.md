# Contributing to Heimdall4j

Thanks for your interest in contributing! Here's how to get started.

## Development Setup

```bash
git clone https://github.com/haisher/heimdall4j.git
cd heimdall4j
./gradlew clean build
```

**Requirements:**
- Java 25+
- Gradle 9.5+ (wrapper included)

## Running Tests

```bash
# All modules
./gradlew test

# Single module
./gradlew :heimdall4j-circuitbreaker:test
```

## Project Structure

| Directory | Description |
|-----------|-------------|
| `heimdall4j-circuitbreaker/` | Core circuit breaker (zero deps) |
| `heimdall4j-retry/` | Retry executor (zero deps) |
| `heimdall4j-ratelimiter/` | Rate limiter (zero deps) |
| `heimdall4j-timeout/` | Timeout executor (zero deps) |
| `heimdall4j-resilience/` | Composable policy facade |
| `heimdall4j-spring-boot/` | Spring Boot auto-configuration |
| `heimdall4j-sidecar/` | Metrics, health, logging |

## Guidelines

### Code Style

- Follow existing patterns (records, sealed interfaces, `of()` factories, CAS loops)
- Keep standalone modules zero-dependency
- Use `Clock` injection for testability
- Emit events via `Flow.Publisher`

### Pull Requests

1. Fork the repo and create a branch from `main`
2. Add tests for any new functionality
3. Ensure `./gradlew clean build` passes
4. Add javadoc to public API methods
5. Keep PRs focused — one feature or fix per PR

### Commit Messages

Use conventional-style messages:

```
feat: add bulkhead executor module
fix: race condition in rate limiter window reset
docs: update retry configuration reference
```

## Reporting Bugs

Use the [Bug Report](https://github.com/haisher/heimdall4j/issues/new?template=bug_report.yml) template.

## Suggesting Features

Use the [Feature Request](https://github.com/haisher/heimdall4j/issues/new?template=feature_request.yml) template or start a [Discussion](https://github.com/haisher/heimdall4j/discussions).

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
