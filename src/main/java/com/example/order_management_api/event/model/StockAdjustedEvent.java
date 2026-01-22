package com.example.order_management_api.event.model;

import java.time.Instant;
import java.util.UUID;

public record StockAdjustedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID productId,
        int previousAvailable,
        int newAvailable,
        int reserved
) implements DomainEvent {

    public static StockAdjustedEvent now(UUID productId, int previousAvailable, int newAvailable, int reserved) {
        return new StockAdjustedEvent(UUID.randomUUID(), Instant.now(), productId, previousAvailable, newAvailable, reserved);
    }

    @Override
    public String type() {
        return "StockAdjusted";
    }
}