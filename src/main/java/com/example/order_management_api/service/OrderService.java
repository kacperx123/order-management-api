package com.example.order_management_api.service;

import com.example.order_management_api.event.model.OrderCancelledEvent;
import com.example.order_management_api.event.model.OrderCreatedEvent;
import com.example.order_management_api.event.model.OrderPaidEvent;
import com.example.order_management_api.event.publisher.DomainEventPublisher;
import com.example.order_management_api.exception.InactiveProductException;
import com.example.order_management_api.exception.InvalidOrderStatusTransitionException;
import com.example.order_management_api.exception.OrderNotFoundException;
import com.example.order_management_api.exception.OutOfStockException;
import com.example.order_management_api.exception.ProductNotFoundException;
import com.example.order_management_api.model.Inventory;
import com.example.order_management_api.model.Order;
import com.example.order_management_api.model.OrderItem;
import com.example.order_management_api.model.OrderStatus;
import com.example.order_management_api.model.Product;
import com.example.order_management_api.repository.InventoryRepository;
import com.example.order_management_api.repository.OrderRepository;
import com.example.order_management_api.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.order_management_api.api.CreateOrderItemRequest;
import com.example.order_management_api.api.CreateOrderRequest;

@Service
public class OrderService {

    private final DomainEventPublisher domainEventPublisher;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public OrderService(
            DomainEventPublisher domainEventPublisher,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository
    ) {
        this.domainEventPublisher = domainEventPublisher;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        UUID id = UUID.randomUUID();

        Order order = new Order(
                id,
                request.customerEmail(),
                OrderStatus.CREATED,
                Instant.now()
        );

        for (CreateOrderItemRequest item : request.items()) {
            UUID productId = item.productId();
            int quantity = item.quantity();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            if (!product.isActive()) {
                throw new InactiveProductException(productId);
            }

            Inventory inventory = inventoryRepository.findByProduct_Id(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            int available = inventory.getAvailable();
            if (available < quantity) {
                throw new OutOfStockException(productId, quantity, available);
            }

            // Decrease stock in the same transaction as order creation.
            inventory.setAvailable(available - quantity);

            OrderItem orderItem = new OrderItem(
                    productId,
                    product.getName(),
                    product.getPrice(),
                    quantity
            );

            order.addItem(orderItem);
        }

        // Persist order + items and stock changes atomically.
        Order saved = orderRepository.saveAndFlush(order);

        domainEventPublisher.publish(OrderCreatedEvent.now(saved.getId(), saved.getCustomerEmail()));

        return saved;
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

        domainEventPublisher.publish(OrderPaidEvent.now(order.getId()));

        return orderRepository.save(order);
    }

    public Order cancelOrder(UUID id) {
        Order order = getOrder(id);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOrderStatusTransitionException(order.getStatus(), OrderStatus.CANCELLED);
        }

        order.setStatus(OrderStatus.CANCELLED);

        domainEventPublisher.publish(OrderCancelledEvent.now(order.getId()));

        return orderRepository.save(order);
    }
}
