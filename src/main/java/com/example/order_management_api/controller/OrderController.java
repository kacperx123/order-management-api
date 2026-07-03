package com.example.order_management_api.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.example.order_management_api.api.CreateOrderRequest;
import com.example.order_management_api.api.OrderResponse;
import com.example.order_management_api.model.OrderStatus;
import com.example.order_management_api.security.CurrentUser;
import com.example.order_management_api.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        OrderResponse created = orderService.createOrder(request, CurrentUser.from(jwt));
        return ResponseEntity
                .created(URI.create("/orders/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.getOrder(id, CurrentUser.from(jwt));
    }

    @GetMapping
    public List<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status) {
        return orderService.listOrders(status);
    }

    @GetMapping("/my")
    public List<OrderResponse> listMyOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.listMyOrders(CurrentUser.from(jwt));
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.payOrder(id, CurrentUser.from(jwt));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.cancelOrder(id, CurrentUser.from(jwt));
    }
}
