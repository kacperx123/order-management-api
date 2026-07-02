package com.example.order_management_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Demo-only delay used to widen the race window when reproducing
 * optimistic-locking conflicts on concurrent order placement.
 */
@Component
public class SimulatedProcessingDelay {

    private final long delayMs;

    public SimulatedProcessingDelay(@Value("${app.order.place.delay-ms:0}") long delayMs) {
        this.delayMs = delayMs;
    }

    public void apply() {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
