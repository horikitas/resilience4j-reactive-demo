package org.horikita.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.horikita.controller.DemoController.FAILURE_SIMULATOR_COUNT;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@WebFluxTest(DemoController.class)
public class DemoControllerTest {

    private static final String UNSTABLE_ENDPOINT = "/api/unstable";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void contextLoads() {
        assertNotNull(webTestClient);
    }

    @Test
    void singleFirstTest() {
        StepVerifier.create(this.expectSuccess(1))
                .expectNextMatches(resultPair -> resultPair.message.startsWith("Success"))
                .verifyComplete();
    }


    @Test
    void testUnstableWithoutSimulatingFail() {
        Flux.range(1,2)
                .concatMap(this::expectSuccess)
                .collectList() // Mono<List<String>>
                .doOnNext(responses -> {
                    long fallbackCount = responses.stream()
                            .filter(resultPair -> resultPair.message.startsWith("Fallback"))
                            .count();
                    log.info("Fallback triggered for {} of 10 requests", fallbackCount);
                    assertEquals(0, fallbackCount);
                })
                .block();
    }

    @Test
    void testUnstableEndpointWithFallbackTriggered() {
        Flux<ResultPair> pairFlux = Flux.range(1, 2)
                .concatMap(counter -> isExpectedToFail(counter.intValue())? expectFail(counter.intValue()) : expectSuccess(counter.intValue()));

        StepVerifier.create(pairFlux)
                .assertNext(this::logAndAssert)
                .assertNext(this::logAndAssert)
                .verifyComplete();
    }

    @Test
    void testWithInterval() {
        Flux<ResultPair> delayedFlux = Flux.range(1, 3)
                .concatMap(counter -> isExpectedToFail(counter)? expectFail(counter) : expectSuccess(counter));

        StepVerifier.withVirtualTime(() -> delayedFlux)
                .thenAwait(Duration.ofMillis(300))
                .expectNextCount(3)
                .thenAwait(Duration.ofMillis(300))
                .verifyComplete();
    }

    private void logAndAssert(ResultPair t) {
        responseLogger(t.counter, t.message);
        if (isExpectedToFail(t.counter)) {
            assertFallback(t.message);
        } else {
            assertSuccess(t.message);
        }
    }

    private boolean isExpectedToFail(int counter) {
        return counter % FAILURE_SIMULATOR_COUNT == 0;
    }


    private Flux<ResultPair> expectSuccess(int i) {
        return webTestClient.get()
                .uri(UNSTABLE_ENDPOINT)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(body -> responseLogger(i, body))
                .map(body -> new ResultPair(i, body));
    }

    private Flux<ResultPair> expectFail(int i) {
        return webTestClient.get()
                .uri(UNSTABLE_ENDPOINT)
                .exchange()
                .expectStatus().is5xxServerError()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(body -> responseLogger(i, body))
                .map(body -> new ResultPair(i, body));
    }

    private void assertSuccess(String body) {
        assertTrue(body.startsWith("Success"));
    }

    private void assertFallback(String body) {
        assertTrue(body.startsWith("Simulating failure for attempt"));
    }


    private static void responseLogger(long i, String body) {
        log.info("Response {} is : {}", i, body);
    }

    private record ResultPair(int counter, String message) {}


   /* @Test
    void testWithDelayBetweenCallsWithRetries() {
        Flux.interval(Duration.ofMillis(500))
                .take(10)
                .concatMap(i ->
                        webTestClient.get()
                                .uri("/api/unstable")
                                .exchange()
                                .onErrorResume(ex -> {
                                    log.info("Request " + i + " failed: " + ex.getMessage());
                                    return Mono.empty();
                                })
                                .expectStatus().isOk()
                                .returnResult(String.class)
                                .getResponseBody()
                                .doOnNext(body -> log.info("Response " + i + ": " + body))
                )
                .blockLast();  // wait for all requests to finish
    }*/

}
