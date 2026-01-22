package com.example.order_management_api.exception;

import java.util.UUID;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(UUID productId, int requested, int available) {
        super("Out of stock for product " + productId + ": requested=" + requested + ", available=" + available);
    }
}
