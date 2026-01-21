package com.example.order_management_api.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(int attempted, int available) {
        super("Insufficient stock: attempted=" + attempted + ", available=" + available);
    }
}