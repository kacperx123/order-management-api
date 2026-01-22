package com.example.order_management_api.exception;

import java.util.UUID;

public class InactiveProductException extends RuntimeException {
    public InactiveProductException(UUID productId) {
        super("Product " + productId + " is inactive");
    }
}

