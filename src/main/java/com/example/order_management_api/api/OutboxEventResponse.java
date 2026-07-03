package com.example.order_management_api.api;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventResponse(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String type,
        String payloadJson,
        Instant occurredAt,
        Instant createdAt,
        Instant publishedAt
) {}
