package com.example.order_management_api.outbox;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(
        name = "app.outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Component
@RequiredArgsConstructor
public class OutboxKafkaPublisherJob {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxKafkaProperties topics;

    private boolean enabled;

    @Value("${app.outbox.publisher.batch-size:50}")
    private int batchSize;

    // @Transactional must sit on the method invoked through the Spring proxy;
    // a self-invoked internal method would silently run without a transaction.
    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:1000}")
    @Transactional
    public void publishUnsentEvents() {
        var page = outboxEventRepository
                .findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, batchSize));

        if (page.isEmpty()) return;

        for (OutboxEvent e : page.getContent()) {
            publishSingle(e);
            e.setPublishedAt(Instant.now());
        }
    }

    private void publishSingle(OutboxEvent event) {
        String topic = resolveTopic(event.getAggregateType());
        String key = event.getAggregateId().toString();

        try {
            kafkaTemplate
                    .send(topic, key, event.getPayloadJson())
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to publish outbox event " + event.getId(), ex
            );
        }
    }

    private String resolveTopic(String aggregateType) {
        return switch (aggregateType) {
            case "ORDER" -> topics.getOrder();
            case "PRODUCT" -> topics.getProduct();
            default -> throw new IllegalArgumentException(
                    "Unknown aggregateType: " + aggregateType
            );
        };
    }
}

