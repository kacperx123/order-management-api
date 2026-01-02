package com.example.order_management_api.api;

import com.example.order_management_api.model.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerEmail,
        OrderStatus status,
        List<OrderItemResponse> items,
        Instant createdAt
) {}
