package com.example.order_management_api.event.model;


import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String customerEmail
) implements DomainEvent {

    public static OrderCreatedEvent now(UUID orderId, String customerEmail) {
        return new OrderCreatedEvent(UUID.randomUUID(), Instant.now(), orderId, customerEmail);
    }

    @Override public String type() { return "OrderCreated"; }
}
