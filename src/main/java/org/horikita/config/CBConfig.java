package org.horikita.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CBConfig {

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    RetryRegistry retryRegistry;

    @Bean("orderVendorAPICB")
    public CircuitBreaker orderVendorCB() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("orderVendorAPICB");
        logCircuitBreaker(cb);
        return cb;
    }

    @Bean("orderVendorAPIRetry")
    public Retry orderVendorRetry() {
        final Retry retry =  retryRegistry.retry("orderVendorAPIRetry");
        retry.getEventPublisher()
                .onRetry(event ->log.warn("Retrying... attempt #{}", event.getNumberOfRetryAttempts()));
        logRetryConfig(retry);
        return retry;
    }

    private void logCircuitBreaker(CircuitBreaker cb) {
        log.info("Request received for unstable endpoint");
        log.info("Circuit Breaker getFailureRateThreshold: {}", cb.getCircuitBreakerConfig().getFailureRateThreshold());
        log.info("Circuit Breaker getMinimumNumberOfCalls: {}", cb.getCircuitBreakerConfig().getMinimumNumberOfCalls());
        log.info("Circuit Breaker Name: {}", cb.getName());
    }

    private void logRetryConfig(Retry retry) {
        log.info("Retry config getMaxAttempts: {}", retry.getRetryConfig().getMaxAttempts());
        log.info("Retry config isFailAfterMaxAttempts: {}", retry.getRetryConfig().isFailAfterMaxAttempts());
        log.info("Retry getName: {}", retry.getName());
    }
}
