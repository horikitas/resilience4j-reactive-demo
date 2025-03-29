package org.horikita.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.horikita.controller.DemoController.FAILURE_SIMULATOR_COUNT;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DemoControllerTest {

    private static final String UNSTABLE_ENDPOINT = "/api/unstable";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CircuitBreaker cb;

    @Test
    public void contextLoads() {
        assertNotNull(webTestClient);
    }

    @Order(1)
    @Test
    void testWithOneFailure() throws InterruptedException {
        //1, 2, 3
        Flux<ResultPair> threeSuccess = Flux.range(1, 3).concatMap(counter -> this.hitApi(counter, false));
        StepVerifier.create(threeSuccess)
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .verifyComplete();
        assertEquals("CLOSED", cb.getState().toString());
        assertEquals(0, cb.getMetrics().getNumberOfFailedCalls());

        //4, 5, 6, 7, 8, 9 ==> count 6
        Flux<ResultPair> failSeries = Flux.range(4, 6).concatMap(counter -> this.hitApi(counter, true));

        StepVerifier.create(failSeries)
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Retry Fallback:"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Retry Fallback:"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Retry Fallback:"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Retry Fallback:"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Retry Fallback:"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Retry Fallback:"))
                .verifyComplete();
        assertEquals("CLOSED", cb.getState().toString());
        assertEquals(6, cb.getMetrics().getNumberOfFailedCalls());

        //The call that will OPEN circuit breaker
        Flux<ResultPair> finalCall = Flux.range(11, 1).concatMap(counter -> this.hitApi(counter, true));

        StepVerifier.create(finalCall)
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Retry Fallback:"))
                .verifyComplete();
        assertEquals("OPEN", cb.getState().toString());
        assertEquals(7, cb.getMetrics().getNumberOfFailedCalls());

        //callInOpenState
        Flux<ResultPair> callInOpenState = Flux.range(12, 1).concatMap(counter -> this.hitApi(counter, true));

        StepVerifier.create(callInOpenState)
                .expectNextMatches(resultPair -> resultPair.message.contains("Fallback: CircuitBreaker 'unstableEndpoint' is OPEN"))
                .verifyComplete();
        assertEquals("OPEN", cb.getState().toString());
        assertEquals(7, cb.getMetrics().getNumberOfFailedCalls());

        Thread.sleep(600);
        //9 calls
        StepVerifier.create(threeSuccess.mergeWith(threeSuccess).mergeWith(threeSuccess))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .verifyComplete();

        assertEquals("HALF_OPEN", cb.getState().toString());
        assertEquals(0, cb.getMetrics().getNumberOfFailedCalls());

        StepVerifier.create(threeSuccess)
                .assertNext(resultPair -> {
                    assertTrue(resultPair.message.startsWith("Success"));
                    assertEquals("CLOSED", cb.getState().toString());
                })
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .verifyComplete();

    }


    private Flux<ResultPair> hitApi(int i, boolean simulateFail) {
        log.info("Hitting api {} time with simulateFail {}", i, simulateFail);
        return webTestClient.get()
                .uri(UNSTABLE_ENDPOINT+"?simulateFail="+simulateFail)
                .exchange()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(body -> responseLogger(i, body))
                .map(body -> new ResultPair(i, body));
    }

    private static void responseLogger(long i, String body) {
        log.info("Response {} is : {}", i, body);
    }

    private record ResultPair(int counter, String message) {}


}
