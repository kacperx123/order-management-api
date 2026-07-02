package com.example.order_management_api.model;

import com.example.order_management_api.exception.InvalidOrderStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void newOrderStartsAsCreated() {
        Order order = Order.newOrder("test@example.com");

        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getCustomerEmail()).isEqualTo("test@example.com");
    }

    @Test
    void payTransitionsCreatedOrderToPaid() {
        Order order = Order.newOrder("test@example.com");

        order.pay();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void cancelTransitionsCreatedOrderToCancelled() {
        Order order = Order.newOrder("test@example.com");

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cannotCancelPaidOrder() {
        Order order = Order.newOrder("test@example.com");
        order.pay();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("PAID")
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void cannotPayCancelledOrder() {
        Order order = Order.newOrder("test@example.com");
        order.cancel();

        assertThatThrownBy(order::pay)
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void cannotPayOrderTwice() {
        Order order = Order.newOrder("test@example.com");
        order.pay();

        assertThatThrownBy(order::pay)
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }
}
