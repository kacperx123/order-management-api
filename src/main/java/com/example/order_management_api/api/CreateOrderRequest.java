package com.example.order_management_api.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
        @Email @NotEmpty String customerEmail,
        @NotEmpty @Valid List<CreateOrderItemRequest> items
) {}
