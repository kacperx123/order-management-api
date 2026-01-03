package com.example.order_management_api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.order_management_api.exception.InvalidOrderStatusTransitionException;
import com.example.order_management_api.repository.OrderRepository;
import org.springframework.stereotype.Service;

import com.example.order_management_api.api.CreateOrderItemRequest;
import com.example.order_management_api.api.CreateOrderRequest;
import com.example.order_management_api.exception.OrderNotFoundException;
import com.example.order_management_api.model.Order;
import com.example.order_management_api.model.OrderItem;
import com.example.order_management_api.model.OrderStatus;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(CreateOrderRequest request) {
        UUID id = UUID.randomUUID();

        Order order = new Order(
                id,
                request.customerEmail(),
                OrderStatus.CREATED,
                Instant.now()
        );

        request.items().forEach(i -> order.addItem(new OrderItem(i.productName(), i.quantity())));

        return orderRepository.save(order);
    }

    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> listOrders(OrderStatus status) {
        if (status == null) {
            return orderRepository.findAll();
        }
        return orderRepository.findByStatus(status);
    }

    public Order payOrder(UUID id) {
        Order order = getOrder(id);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStatusTransitionException(order.getStatus(), OrderStatus.PAID);
        }

        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }

    public Order cancelOrder(UUID id) {
        Order order = getOrder(id);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStatusTransitionException(order.getStatus(), OrderStatus.CANCELLED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
