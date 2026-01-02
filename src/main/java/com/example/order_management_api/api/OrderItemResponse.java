package com.example.order_management_api.api;

public record OrderItemResponse(
        String productName,
        int quantity
) {}
