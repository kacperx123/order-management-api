package com.example.order_management_api.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        @NotNull @Min(1) BigDecimal price,
        @Min(0) int initialStock,
        Boolean active
) {}
