package com.example.order_management_api;

import com.example.order_management_api.api.CreateOrderItemRequest;
import com.example.order_management_api.api.CreateOrderRequest;
import com.example.order_management_api.api.CreateProductRequest;
import com.example.order_management_api.api.OrderResponse;
import com.example.order_management_api.api.ProductResponse;
import com.example.order_management_api.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxIntegrationTest extends PostgresTestBase {

    @LocalServerPort
    int port;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    private RestClient plainClient;
    private RestClient adminClient;
    private RestClient userClient;

    private RestClient client() {
        if (plainClient == null) {
            plainClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return plainClient;
    }

    private RestClient admin() {
        if (adminClient == null) {
            String token = AuthTestSupport.loginAdmin(client());
            adminClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
        }
        return adminClient;
    }

    private RestClient user() {
        if (userClient == null) {
            String email = "user-" + UUID.randomUUID() + "@test.com";
            String token = AuthTestSupport.registerAndLogin(client(), email, "password123");
            userClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
        }
        return userClient;
    }

    private ProductResponse createProduct(String name, double price, int initialStock) {
        CreateProductRequest productRequest = new CreateProductRequest(
                name,
                BigDecimal.valueOf(price),
                initialStock,
                true
        );

        ProductResponse product = admin()
                .post()
                .uri("/products")
                .body(productRequest)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(product).isNotNull();
        return product;
    }

    @Test
    void shouldPersistOutboxEventWhenOrderIsCreated() {
        // given: product
        ProductResponse product = createProduct("Milk", 3.99, 10);

        // when: order is created
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(product.id(), 1))
        );

        OrderResponse order = user()
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

    @Test
    void shouldPersistOutboxEventWhenOrderIsPaid() {
        // given: product + order
        ProductResponse product = createProduct("Bread", 2.50, 10);

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(product.id(), 1))
        );

        OrderResponse order = user()
                .post()
                .uri("/orders")
                .body(orderRequest)
                .retrieve()
                .body(OrderResponse.class);

        assertThat(order).isNotNull();

        // when: order is paid
        user().post().uri("/orders/" + order.id() + "/pay").retrieve().toBodilessEntity();

        // then: OrderPaid event was persisted in the same transaction as the status change
        var events = outboxEventRepository.findAll();

        assertThat(events)
                .anyMatch(e ->
                        e.getAggregateType().equals("ORDER")
                                && e.getAggregateId().equals(order.id())
                                && e.getType().equals("OrderPaid")
                );
    }
}
