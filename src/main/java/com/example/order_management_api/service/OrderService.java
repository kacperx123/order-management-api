package com.example.order_management_api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.order_management_api.exception.InvalidOrderStatusTransitionException;
import org.springframework.stereotype.Service;

import com.example.order_management_api.api.CreateOrderItemRequest;
import com.example.order_management_api.api.CreateOrderRequest;
import com.example.order_management_api.exception.OrderNotFoundException;
import com.example.order_management_api.model.Order;
import com.example.order_management_api.model.OrderItem;
import com.example.order_management_api.model.OrderStatus;
import com.example.order_management_api.store.OrderStore;

@Service
public class OrderService {

    private final OrderStore orderStore;

    public OrderService(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    public Order createOrder(CreateOrderRequest request) {
        UUID id = UUID.randomUUID();

        List<OrderItem> items = request.items().stream()
                .map(this::toOrderItem)
                .toList();

        Order order = new Order(
                id,
                request.customerEmail(),
                OrderStatus.CREATED,
                items,
                Instant.now()
        );

        return orderStore.save(order);
    }

    public Order payOrder(UUID id) {
        Order order = getOrder(id);

        if (order.status() != OrderStatus.CREATED) {
            throw new InvalidOrderStatusTransitionException(order.status(), OrderStatus.PAID);
        }

        Order updated = new Order(
                order.id(),
                order.customerEmail(),
                OrderStatus.PAID,
                order.items(),
                order.createdAt()
        );

        return orderStore.save(updated);
    }

    public Order cancelOrder(UUID id) {
        Order order = getOrder(id);

        if (order.status() != OrderStatus.CREATED) {
            throw new InvalidOrderStatusTransitionException(order.status(), OrderStatus.CANCELLED);
        }

        Order updated = new Order(
                order.id(),
                order.customerEmail(),
                OrderStatus.CANCELLED,
                order.items(),
                order.createdAt()
        );

        return orderStore.save(updated);
    }

    public Order getOrder(UUID id) {
        return orderStore.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> listOrders(OrderStatus status) {
        if (status == null) {
            return orderStore.findAll();
        }
        return orderStore.findByStatus(status);
    }

    private OrderItem toOrderItem(CreateOrderItemRequest item) {
        return new OrderItem(item.productName(), item.quantity());
    }
}
