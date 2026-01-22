package com.example.order_management_api.api;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPriceAtPurchase
) {
}