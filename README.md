# Heimdall4j

Modern Java 25 circuit breaker library — slim, zero-dependency core with Spring Boot integration.

## Modules

| Module | Description |
|--------|-------------|
| `heimdall4j-core` | Standalone circuit breaker — zero dependencies, pure Java 25 |
| `heimdall4j-spring-boot` | Spring Boot auto-configuration, `@Heimdall` annotation |
| `heimdall4j-sidecar` | Actuator endpoint, Micrometer metrics, structured logging |

## Quick Start (Core)

```java
var config = CircuitBreakerConfig.builder()
    .failureRateThreshold(50)
    .ringBufferSize(100)
    .waitDurationInOpenState(Duration.ofSeconds(30))
    .permittedCallsInHalfOpen(10)
    .callTimeout(Duration.ofSeconds(2))
    .build();

var cb = CircuitBreaker.of("payments", config);

var result = cb.execute(
    () -> callExternalService(),
    () -> fallbackValue()
);
```

## Quick Start (Spring Boot)

```yaml
heimdall4j:
  instances:
    payments:
      failure-rate-threshold: 50
      ring-buffer-size: 100
      wait-duration: 30s
      call-timeout: 2s
```

```java
@Heimdall("payments")
public PaymentResult processPayment(Order order) {
    return gateway.charge(order);
}

public Supplier<PaymentResult> processPaymentFallback() {
    return () -> PaymentResult.declined("service unavailable");
}
```

## Requirements

- Java 25+
- Spring Boot 4.0+ (for spring-boot and sidecar modules)

## License

[MIT](LICENSE)
