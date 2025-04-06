package org.horikita.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.horikita.dto.OrderRequestDTO;
import org.horikita.dto.OrderResponseDTO;
import org.horikita.dto.OrderStatusEnum;
import org.horikita.exceptions.NonRetriableException;
import org.horikita.exceptions.RetriableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/***
 * A service that interacts with an external API, simulating success and failure scenarios.
 * It uses a Circuit Breaker pattern to manage the state of the API calls.
 */
@Slf4j
@Component
public class OrderService {

    //Since Qualifier doesn't work well with Lombok, using explicit constructor binding
    private final @NonNull CircuitBreaker circuitBreaker;

    private final @NonNull Retry retry;

    //Since Qualifier doesn't work well with Lombok, using explicit constructor binding
    public OrderService(final @NonNull @Qualifier("orderVendorAPICB") @Autowired CircuitBreaker cb,
                        final @NonNull @Qualifier("orderVendorAPIRetry") @Autowired Retry retry) {
        this.circuitBreaker = cb;
        this.retry = retry;
    }

    public Mono<OrderResponseDTO> placeOrder(final OrderRequestDTO request,
                                             boolean simulateFail,
                                             boolean isRetriable) {
        return callExternalAPI(request, simulateFail, isRetriable)
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(e -> {
                    log.error("Error occurred while processing order : {}", e.getMessage());
                    return fallbackOnErrors(e, request);
                });
    }

    /***
     * Simulates an external API call that can either succeed or fail.
     */
    private Mono<OrderResponseDTO> callExternalAPI(OrderRequestDTO request,
                                                   boolean simulateFail,
                                                   boolean isRetriable) {
        return Mono.fromSupplier(() -> request)
                .flatMap(current -> {
                    if (simulateFail && isRetriable) {
                        log.warn("Simulating retriable failure for counter {}", current);
                        return Mono.error(new RetriableException("Simulated retriable failure on attempt " + current));
                    }
                    if(simulateFail) {
                        log.warn("Simulating non-retriable failure for counter {}", current);
                        return Mono.error(new NonRetriableException("Simulated non-retriable failure on attempt " + current));
                    }
                    return Mono.just(OrderResponseDTO.builder() //Happy scenario
                            .orderId(request.getOrderId())
                            .status(OrderStatusEnum.PLACED)
                            .build());
                });
    }

    /**
     * Usually, fallback on 5xx errors from downstream.
     * Use a different strategy for 4xx errors with DLQ or similar.
     */
    private Mono<OrderResponseDTO> fallbackOnErrors(Throwable error, OrderRequestDTO request) {

        //Build a fallback response, typically store unprocessed order somewhere
        final OrderResponseDTO pendingOrderResponse =
                OrderResponseDTO.builder().orderId(request.getOrderId()).status(OrderStatusEnum.PENDING).build();
        if (error instanceof CallNotPermittedException) {
            log.error("Final fallback - Circuit Breaker is open: {}, cbState {}", error.getMessage(), circuitBreaker.getState());
            return Mono.just(pendingOrderResponse);
        }
        log.warn("Final fallback triggered: {}, cbState {}", error.getMessage(), circuitBreaker.getState());
        return Mono.just(pendingOrderResponse);
    }


}
