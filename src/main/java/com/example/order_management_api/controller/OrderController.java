package com.example.order_management_api.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.order_management_api.api.CreateOrderRequest;
import com.example.order_management_api.api.OrderResponse;
import com.example.order_management_api.model.OrderStatus;
import com.example.order_management_api.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity
                .created(URI.create("/orders/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }

    @GetMapping
    public List<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status) {
        return orderService.listOrders(status);
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable UUID id) {
        return orderService.payOrder(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        return orderService.cancelOrder(id);
    }
}
