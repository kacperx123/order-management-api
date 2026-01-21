package com.example.order_management_api.event.publisher;

import com.example.order_management_api.event.model.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}