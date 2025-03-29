package org.horikita.controller;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
@Slf4j
class DemoController {

    @Autowired
    private CounterProvider counterProvider;
    public static final int FAILURE_SIMULATOR_COUNT = 3;

    @Autowired
    private CircuitBreaker cb;

    /*
    .transform(TimeLimiterOperator.of(timeLimiter))
    .transform(CircuitBreakerOperator.of(circuitBreaker))
    .transform(RetryOperator.of(retry))
     */
    @GetMapping("/unstable")
    public Mono<ResponseEntity<String>> unstableEndpoint(@RequestParam(value = "simulateFail", defaultValue = "false") boolean simulateFail) {
        log.info("Circuit Breaker State when call is made: {}", cb.getState());
        log.info("Failed count {}", cb.getMetrics().getNumberOfFailedCalls());

        return callExternalAPI(simulateFail)
                .transform(CircuitBreakerOperator.of(cb))
                .onErrorResume(e -> {
                    log.error("Error occurred: {}", e.getMessage());
                    if (e instanceof CallNotPermittedException) {
                       return closedFallback(Mono.just(e)); // Handle circuit breaker open state
                    } else {
                        return retryFallback(Mono.just(e)); // Handle other errors
                    }
                });
    }

    private Mono<ResponseEntity<String>> retryFallback(Mono<Throwable> error) {
        return error.flatMap(e -> {
            if (e instanceof CallNotPermittedException) {
                log.error("Circuit Breaker is open: {}, cbState {}", e.getMessage(), cb.getState());
                return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Final Fallback: Circuit Breaker is open"));
            }
            log.warn("Retry Fallback triggered: {}, cbState {}", e.getMessage(), cb.getState());
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Retry Fallback: " + e.getMessage()));
        });
    }


    private Mono<ResponseEntity<String>> closedFallback(Mono<Throwable> error) {
        return error.flatMap(e -> {
            log.warn("Final Fallback triggered: {}", e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Fallback: " + e.getMessage()));
        });
    }

    private Mono<ResponseEntity<String>> callExternalAPI(boolean simulateFail) {
        return Mono.fromSupplier(() -> counterProvider.getCounter().incrementAndGet())
                .flatMap(current -> {
                    if (simulateFail) {
                        log.warn("Simulating failure for counter {}", current);
                        return Mono.error(new RuntimeException("Simulated failure on attempt " + current));
                    }
                    return Mono.just(ResponseEntity.ok("Success " + current));
                });
    }


}