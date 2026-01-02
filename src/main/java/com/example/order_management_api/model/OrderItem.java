package com.example.order_management_api.model;

public record OrderItem(
        String productName,
        int quantity
) {}
