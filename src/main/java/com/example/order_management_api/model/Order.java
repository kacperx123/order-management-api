package com.example.order_management_api.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        String customerEmail,
        OrderStatus status,
        List<OrderItem> items,
        Instant createdAt
) {}
