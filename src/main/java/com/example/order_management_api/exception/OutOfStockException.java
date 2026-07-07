package com.example.order_management_api.exception;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String productName, int requested, int available) {
        super("Product " + productName + " is out of stock for that request: requested = "
                + requested + ", available = " + available + ".");
    }
}
