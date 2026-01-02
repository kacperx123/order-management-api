package com.example.order_management_api.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.order_management_api.model.Order;
import com.example.order_management_api.model.OrderStatus;

@Component
public class OrderStore {

    private final ConcurrentHashMap<UUID, Order> orders = new ConcurrentHashMap<>();

    public Order save(Order order) {
        orders.put(order.id(), order);
        return order;
    }

    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    public List<Order> findAll() {
        return List.copyOf(orders.values());
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orders.values().stream()
                .filter(o -> o.status() == status)
                .toList();
    }
}
