package com.example.order_management_api.event.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DomainEventLoggingListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventLoggingListener.class);

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        log.info("DOMAIN_EVENT type={} id={} occurredAt={}",
                event.type(), event.eventId(), event.occurredAt());
    }
}
