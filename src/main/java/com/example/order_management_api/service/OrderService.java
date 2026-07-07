package com.example.order_management_api.service;

import com.example.order_management_api.api.CreateOrderItemRequest;
import com.example.order_management_api.api.CreateOrderRequest;
import com.example.order_management_api.api.OrderResponse;
import com.example.order_management_api.event.model.OrderCancelledEvent;
import com.example.order_management_api.event.model.OrderCreatedEvent;
import com.example.order_management_api.event.model.OrderPaidEvent;
import com.example.order_management_api.event.publisher.DomainEventPublisher;
import com.example.order_management_api.exception.OrderAccessDeniedException;
import com.example.order_management_api.exception.OrderNotFoundException;
import com.example.order_management_api.mapper.OrderMapper;
import com.example.order_management_api.model.Order;
import com.example.order_management_api.model.OrderItem;
import com.example.order_management_api.model.OrderStatus;
import com.example.order_management_api.model.Product;
import com.example.order_management_api.model.User;
import com.example.order_management_api.repository.OrderRepository;
import com.example.order_management_api.repository.UserRepository;
import com.example.order_management_api.security.CurrentUser;
import com.example.order_management_api.service.validation.OrderValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final DomainEventPublisher domainEventPublisher;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderValidator orderValidator;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, CurrentUser currentUser) {
        User user = userRepository.findById(currentUser.id()).orElseThrow();
        Order order = Order.newOrder(user);

        for (CreateOrderItemRequest item : request.items()) {
            Product product = orderValidator.validateOrderable(item.productId());
            inventoryService.reserve(item.productId(), product.getName(), item.quantity());

            order.addItem(new OrderItem(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    item.quantity()
            ));
        }

        // Persist order + items and stock changes atomically.
        Order saved = orderRepository.saveAndFlush(order);

        domainEventPublisher.publish(OrderCreatedEvent.now(saved.getId(), saved.getCustomerEmail()));

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id, CurrentUser currentUser) {
        return orderMapper.toResponse(findOrderAuthorized(id, currentUser));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findAll()
                : orderRepository.findByStatus(status);

        return orders.stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMyOrders(CurrentUser currentUser) {
        return orderRepository.findByUser_Id(currentUser.id()).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse payOrder(UUID id, CurrentUser currentUser) {
        Order order = findOrderAuthorized(id, currentUser);

        order.pay();

        domainEventPublisher.publish(OrderPaidEvent.now(order.getId()));

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id, CurrentUser currentUser) {
        Order order = findOrderAuthorized(id, currentUser);

        order.cancel();

        for (OrderItem item : order.getItems()) {
            inventoryService.release(item.getProductId(), item.getQuantity());
        }

        domainEventPublisher.publish(OrderCancelledEvent.now(order.getId()));

        return orderMapper.toResponse(order);
    }

    private Order findOrderAuthorized(UUID id, CurrentUser currentUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (!currentUser.isAdmin() && !order.isOwnedBy(currentUser.id())) {
            throw new OrderAccessDeniedException(id);
        }

        return order;
    }
}
