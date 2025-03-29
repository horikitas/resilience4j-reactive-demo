package org.horikita.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
@Slf4j
class DemoController {

    private final AtomicInteger counter = new AtomicInteger(0);

    public static final int FAILURE_SIMULATOR_COUNT = 3;

    @GetMapping("/unstable")
    @CircuitBreaker(name = "unstableService", fallbackMethod = "fallback")
    @Retry(name = "unstableService", fallbackMethod = "retryFallback")
    public Mono<ResponseEntity<String>> unstableEndpoint() {

        return Mono.fromSupplier(counter::incrementAndGet)
                .doOnNext(current -> log.info("Attempt: {}", current))
                .flatMap(current -> {
                    if (current % FAILURE_SIMULATOR_COUNT == 0) {
                        log.warn("Simulating failure for attempt {}", current);
                        return Mono.error(new RuntimeException("Simulated failure on attempt " + current));
                    }
                    return Mono.just(ResponseEntity.ok("Success " + current));
                });
    }

    public Mono<ResponseEntity<String>> retryFallback(Mono<Throwable> error) {
        return error.flatMap(e -> {
            log.warn("Retry Fallback triggered: {}", e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Fallback: " + e.getMessage()));
        });
    }


    public Mono<ResponseEntity<String>> fallback(Mono<Throwable> error) {
        return error.flatMap(e -> {
            log.warn("Fallback triggered: {}", e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Fallback: " + e.getMessage()));
        });
    }
}