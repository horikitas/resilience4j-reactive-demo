package org.horikita.controller;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.horikita.dto.OrderRequestDTO;
import org.horikita.dto.OrderResponseDTO;
import org.horikita.dto.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.horikita.service.OrderService;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
class OrderController {

    @Autowired
    private final @NonNull OrderService orderService;


    @PostMapping("/place-order")
    public Mono<ResponseEntity<OrderResponseDTO>> unstableEndpoint(
            @RequestParam(value = "simulateFail", defaultValue = "false") boolean simulateFail,
            @RequestBody Mono<OrderRequestDTO> orderRequestMono) {

        return orderRequestMono.flatMap(orderRequestDTO -> orderService.placeOrder(orderRequestDTO, simulateFail))
                .doOnSuccess(result -> log.info("Result: {}", result))
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Unexpected error occurred: {}", e.getMessage()); //Any unexpected errors are caught here, this isn't the place for circuit breaker
                    return Mono.just(ResponseEntity.internalServerError().body(FALLBACK_RESPONSE));
                });
    }

    private static final OrderResponseDTO FALLBACK_RESPONSE =
            OrderResponseDTO.builder().orderId(-1).status(OrderStatusEnum.ERROR).build();

}

/*
TODO - other transformations to try later
.transform(TimeLimiterOperator.of(timeLimiter))
.transform(CircuitBreakerOperator.of(circuitBreaker))
.transform(RetryOperator.of(retry))
 */