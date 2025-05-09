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
            @RequestParam(value = "isRetriable", defaultValue = "true") boolean isRetriable,
            @RequestBody Mono<OrderRequestDTO> orderRequestMono) {

        return orderRequestMono.flatMap(orderRequestDTO -> orderService.placeOrder(orderRequestDTO, simulateFail, isRetriable))
                .doOnSuccess(result -> log.info("Result: {}", result))
                .map(ResponseEntity::ok);
    }



}

/*
TODO - other transformations to try later
.transform(TimeLimiterOperator.of(timeLimiter))
.transform(CircuitBreakerOperator.of(circuitBreaker))
.transform(RetryOperator.of(retry))
 */