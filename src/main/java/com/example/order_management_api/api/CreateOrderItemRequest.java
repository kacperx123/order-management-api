package com.example.order_management_api.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderItemRequest(
        @NotBlank String productName,
        @Min(1) int quantity
) {}
