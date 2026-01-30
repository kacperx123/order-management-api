package com.example.order_management_api.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(
        name = "app.kafka.consumer.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Slf4j
@Component
public class DomainEventsConsumer {

    @KafkaListener(topics = "${app.kafka.topics.order}", groupId = "order-management-api")
    public void onOrderEvent(String payload) {
        // This represents a downstream system (e.g., billing/shipping) consuming domain events.
        log.info("Received ORDER event: {}", payload);
    }

    @KafkaListener(topics = "${app.kafka.topics.product}", groupId = "order-management-api")
    public void onProductEvent(String payload) {
        // This represents a downstream system (e.g., catalog/search) consuming domain events.
        log.info("Received PRODUCT event: {}", payload);
    }
}
