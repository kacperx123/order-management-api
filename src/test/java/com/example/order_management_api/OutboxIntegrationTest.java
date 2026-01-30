package com.example.order_management_api;

import com.example.order_management_api.api.CreateOrderItemRequest;
import com.example.order_management_api.api.CreateOrderRequest;
import com.example.order_management_api.api.CreateProductRequest;
import com.example.order_management_api.api.OrderResponse;
import com.example.order_management_api.api.ProductResponse;
import com.example.order_management_api.PostgresTestBase;
import com.example.order_management_api.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxIntegrationTest extends PostgresTestBase {

    @LocalServerPort
    int port;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    private RestClient restClient;

    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return restClient;
    }

    @Test
    void shouldPersistOutboxEventWhenOrderIsCreated() {
        // given: product
        CreateProductRequest productRequest = new CreateProductRequest(
                "Milk",
                BigDecimal.valueOf(3.99),
                10,
                true
        );

        ProductResponse product = client()
                .post()
                .uri("/products")
                .body(productRequest)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(product).isNotNull();

        // when: order is created
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "test@example.com",
                List.of(new CreateOrderItemRequest(product.id(), 1))
        );

        OrderResponse order = client()
                .post()
                .uri("/orders")
                .body(orderRequest)
                .retrieve()
                .body(OrderResponse.class);

        // then: order created
        assertThat(order).isNotNull();
        assertThat(order.status().name()).isEqualTo("CREATED");

        // and: outbox contains event
        var events = outboxEventRepository.findAll();

        assertThat(events).isNotEmpty();
        assertThat(events)
                .anyMatch(e ->
                        e.getAggregateType().equals("ORDER")
                                && e.getAggregateId().equals(order.id())
                                && e.getType().equals("OrderCreated")
                                && e.getPublishedAt() == null
                );
    }
}
