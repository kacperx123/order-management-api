package com.example.order_management_api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void createdCanTransitionToPaidAndCancelled() {
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.PAID)).isTrue();
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void paidIsTerminal() {
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CREATED)).isFalse();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void cancelledIsTerminal() {
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CREATED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAID)).isFalse();
    }
}
