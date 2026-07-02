package com.example.order_management_api.mapper;

import com.example.order_management_api.api.OrderItemResponse;
import com.example.order_management_api.api.OrderResponse;
import com.example.order_management_api.model.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProductId(),
                        i.getProductNameSnapshot(),
                        i.getQuantity(),
                        i.getUnitPriceAtPurchase()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getStatus(),
                items,
                order.getCreatedAt()
        );
    }
}
