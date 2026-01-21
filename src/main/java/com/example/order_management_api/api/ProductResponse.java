package com.example.order_management_api.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        BigDecimal price,
        boolean active,
        int available,
        int reserved,
        Instant createdAt
) {}