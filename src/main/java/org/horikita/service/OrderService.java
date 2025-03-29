package org.horikita.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.horikita.dto.OrderRequestDTO;
import org.horikita.dto.OrderResponseDTO;
import org.horikita.dto.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/***
 * A service that interacts with an external API, simulating success and failure scenarios.
 * It uses a Circuit Breaker pattern to manage the state of the API calls.
 */
@Slf4j
@Component
public class OrderService {

    //Since Qualifier doesn't work well with Lombok, using explicit constructor binding
    private final @NonNull CircuitBreaker cb;

    public OrderService(final @NonNull @Qualifier("orderVendorAPICB") @Autowired CircuitBreaker cb) {
        this.cb = cb;
    }

    public Mono<OrderResponseDTO> placeOrder(final OrderRequestDTO request, boolean simulateFail) {
        return callExternalAPI(request, simulateFail)
                .onErrorResume(e -> {
                    log.error("Error occurred while processing order : {}", e.getMessage());
                    return fallbackOnErrors(e, request);
                });
    }

    /***
     * Simulates an external API call that can either succeed or fail.
     */
    private Mono<OrderResponseDTO> callExternalAPI(OrderRequestDTO request, boolean simulateFail) {
        return Mono.fromSupplier(() -> request)
                .flatMap(current -> {
                    if (simulateFail) {
                        log.warn("Simulating failure for counter {}", current);
                        return Mono.error(new RuntimeException("Simulated failure on attempt " + current));
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
            log.error("Circuit Breaker is open: {}, cbState {}", error.getMessage(), cb.getState());
            return Mono.just(pendingOrderResponse);
        }
        log.warn("Retry Fallback triggered: {}, cbState {}", error.getMessage(), cb.getState());
        return Mono.just(pendingOrderResponse);
    }


}
