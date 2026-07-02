package com.example.order_management_api.model;

public enum OrderStatus {
    CREATED,
    PAID,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return this == CREATED && (target == PAID || target == CANCELLED);
    }
}
