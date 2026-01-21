package com.example.order_management_api.api;

import jakarta.validation.constraints.AssertTrue;

public record UpdateStockRequest(
        Integer delta,
        Integer setTo
) {
    @AssertTrue(message = "Exactly one of delta or setTo must be provided")
    public boolean isValid() {
        return (delta == null) ^ (setTo == null);
    }
}