package com.example.order_management_api.event.model;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId
) implements DomainEvent {

    public static OrderCancelledEvent now(UUID orderId) {
        return new OrderCancelledEvent(UUID.randomUUID(), Instant.now(), orderId);
    }

    @Override public String type() { return "OrderCancelled"; }
}
