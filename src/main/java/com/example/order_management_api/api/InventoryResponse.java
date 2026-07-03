package com.example.order_management_api.api;

import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        String productName,
        int available,
        int reserved,
        Long version
) {}
