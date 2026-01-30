package com.example.order_management_api.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_published_at", columnList = "publishedAt"),
                @Index(name = "idx_outbox_occurred_at", columnList = "occurredAt")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String aggregateType; // e.g. ORDER, PRODUCT

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String type; // e.g. OrderCreated, StockAdjusted

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Column(nullable = false)
    private Instant occurredAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Setter
    private Instant publishedAt;
}
