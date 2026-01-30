package com.example.order_management_api.outbox;

import com.example.order_management_api.event.model.*;
import com.example.order_management_api.event.publisher.DomainEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Primary
@RequiredArgsConstructor
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        OutboxEvent outboxEvent = mapToOutboxEvent(event);
        outboxEventRepository.save(outboxEvent);
    }

    private OutboxEvent mapToOutboxEvent(DomainEvent event) {
        String aggregateType = resolveAggregateType(event);
        UUID aggregateId = resolveAggregateId(event);

        return OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .type(event.type())
                .payloadJson(toJson(event))
                .occurredAt(event.occurredAt())
                .build();
    }

    private String resolveAggregateType(DomainEvent event) {
        if (event instanceof OrderCreatedEvent
                || event instanceof OrderPaidEvent
                || event instanceof OrderCancelledEvent) {
            return "ORDER";
        }
        if (event instanceof ProductCreatedEvent
                || event instanceof StockAdjustedEvent) {
            return "PRODUCT";
        }
        // Keep it explicit to avoid silently producing invalid outbox records.
        throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getName());
    }

    private UUID resolveAggregateId(DomainEvent event) {
        if (event instanceof OrderCreatedEvent e) return e.orderId();
        if (event instanceof OrderPaidEvent e) return e.orderId();
        if (event instanceof OrderCancelledEvent e) return e.orderId();

        if (event instanceof ProductCreatedEvent e) return e.productId();
        if (event instanceof StockAdjustedEvent e) return e.productId();

        throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getName());
    }

    private String toJson(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            // Fail fast: an event that cannot be serialized should not be silently dropped.
            throw new IllegalStateException("Failed to serialize domain event: " + event.getClass().getName(), ex);
        }
    }
}
