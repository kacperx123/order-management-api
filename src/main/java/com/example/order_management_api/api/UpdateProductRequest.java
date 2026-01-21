package com.example.order_management_api.api;

import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateProductRequest(
        String name,
        @Min(1) BigDecimal price,
        Boolean active
) {}