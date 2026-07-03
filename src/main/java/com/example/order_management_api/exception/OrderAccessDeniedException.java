package com.example.order_management_api.exception;

import java.util.UUID;

public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException(UUID orderId) {
        super("You do not have access to order " + orderId);
    }
}
