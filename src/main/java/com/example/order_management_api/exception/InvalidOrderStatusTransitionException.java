package com.example.order_management_api.exception;

import com.example.order_management_api.model.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Invalid status transition: " + from + " -> " + to);
    }
}
