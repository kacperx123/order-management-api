package com.example.order_management_api.model;

import com.example.order_management_api.exception.InvalidOrderStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order newOrder() {
        User user = new User("test@example.com", "hash", Role.USER);
        return Order.newOrder(user);
    }

    @Test
    void newOrderStartsAsCreated() {
        Order order = newOrder();

        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getCustomerEmail()).isEqualTo("test@example.com");
        assertThat(order.getUser()).isNotNull();
    }

    @Test
    void payTransitionsCreatedOrderToPaid() {
        Order order = newOrder();

        order.pay();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void cancelTransitionsCreatedOrderToCancelled() {
        Order order = newOrder();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cannotCancelPaidOrder() {
        Order order = newOrder();
        order.pay();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("PAID")
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void cannotPayCancelledOrder() {
        Order order = newOrder();
        order.cancel();

        assertThatThrownBy(order::pay)
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void cannotPayOrderTwice() {
        Order order = newOrder();
        order.pay();

        assertThatThrownBy(order::pay)
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }
}
