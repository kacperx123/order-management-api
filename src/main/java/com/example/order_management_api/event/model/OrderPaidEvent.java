package com.example.order_management_api.event.model;

import java.time.Instant;
import java.util.UUID;

public record OrderPaidEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId
) implements DomainEvent {

    public static OrderPaidEvent now(UUID orderId) {
        return new OrderPaidEvent(UUID.randomUUID(), Instant.now(), orderId);
    }

    @Override public String type() { return "OrderPaid"; }
}