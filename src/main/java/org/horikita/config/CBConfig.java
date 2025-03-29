package org.horikita.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class CBConfig {

    @Bean("orderVendorAPICB")
    public CircuitBreaker circuitBreaker() {
        //TODO - To investigate later why it's not loading properties from spring configuration
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(30) // percentage
                .minimumNumberOfCalls(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .waitDurationInOpenState(Duration.ofMillis(500))
                .slidingWindowSize(10)
                .build();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(config);
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("orderVendorAPICB");
        logCircuitBreaker(cb);
        return cb;
    }

    private void logCircuitBreaker(CircuitBreaker cb) {
        log.info("Request received for unstable endpoint");
        log.info("Circuit Breaker getFailureRateThreshold: {}", cb.getCircuitBreakerConfig().getFailureRateThreshold());
        log.info("Circuit Breaker getMinimumNumberOfCalls: {}", cb.getCircuitBreakerConfig().getMinimumNumberOfCalls());
        log.info("Circuit Breaker Name: {}", cb.getName());
    }
}
