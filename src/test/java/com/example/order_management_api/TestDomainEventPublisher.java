package com.example.order_management_api;

import com.example.order_management_api.event.model.DomainEvent;
import com.example.order_management_api.event.publisher.DomainEventPublisher;import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class TestDomainEventPublisher implements DomainEventPublisher {
    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        events.add(event);
    }

    public void clear() {
        events.clear();
    }
}