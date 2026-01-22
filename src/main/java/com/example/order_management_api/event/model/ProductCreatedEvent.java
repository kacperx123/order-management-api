package com.example.order_management_api.event.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID productId,
        String name,
        BigDecimal price
) implements DomainEvent {

    public static ProductCreatedEvent now(UUID productId, String name, BigDecimal price) {
        return new ProductCreatedEvent(UUID.randomUUID(), Instant.now(), productId, name, price);
    }

    @Override
    public String type() {
        return "ProductCreated";
    }
}
