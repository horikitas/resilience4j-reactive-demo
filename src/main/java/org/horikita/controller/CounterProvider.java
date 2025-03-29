package org.horikita.controller;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class CounterProvider {

    @Getter
    private final AtomicInteger counter = new AtomicInteger(0);
}
