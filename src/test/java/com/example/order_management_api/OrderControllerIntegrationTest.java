package com.example.order_management_api;

import com.example.order_management_api.api.*;
import com.example.order_management_api.event.publisher.DomainEventPublisher;
import com.example.order_management_api.exception.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(OrderControllerIntegrationTest.DomainEventsTestConfig.class)
class OrderControllerIntegrationTest extends PostgresTestBase {

    @LocalServerPort
    int port;

    private RestClient plainClient;
    private RestClient adminClient;
    private RestClient userClient;
    private String userEmail;

    @Autowired
    @Qualifier("recordingDomainEventPublisher")
    TestDomainEventPublisher recordingPublisher;

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
            userEmail = "user-" + UUID.randomUUID() + "@test.com";
            String token = AuthTestSupport.registerAndLogin(client(), userEmail, "password123");
            userClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
        }
        return userClient;
    }

    private UUID createProductAndGetId(String name, double price, int initialStock) {
        CreateProductRequest req = new CreateProductRequest(
                name,
                BigDecimal.valueOf(price),
                initialStock,
                true
        );

        ProductResponse created = admin()
                .post()
                .uri("/products")
                .body(req)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(created).isNotNull();
        return created.id();
    }

    private UUID createOrderAndGetId() {
        UUID productId = createProductAndGetId("Milk", 3.99, 100);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, 2))
        );

        OrderResponse created = user()
                .post()
                .uri("/orders")
                .body(request)
                .retrieve()
                .body(OrderResponse.class);

        assertThat(created).isNotNull();
        return created.id();
    }


    @BeforeEach
    void beforeEach() {
        recordingPublisher.clear();
    }

    @Test
    void shouldCreateOrder() {
        // given
        UUID productId = createProductAndGetId("Milk", 3.99, 10);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, 2))
        );

        // when
        ResponseEntity<OrderResponse> response = user()
                .post()
                .uri("/orders")
                .body(request)
                .retrieve()
                .toEntity(OrderResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        OrderResponse order = response.getBody();
        assertThat(order.customerEmail()).isEqualTo(userEmail);
        assertThat(order.items()).hasSize(1);

        OrderItemResponse item = order.items().get(0);
        assertThat(item.productId()).isEqualTo(productId);
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.unitPriceAtPurchase()).isEqualTo(BigDecimal.valueOf(3.99));

        // and: stock was decreased
        ProductResponse productAfter = client()
                .get()
                .uri("/products/" + productId)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(productAfter).isNotNull();
        assertThat(productAfter.available()).isEqualTo(8);
    }

    @Test
    void shouldReturn401WhenCreatingOrderWithoutToken() {
        UUID productId = createProductAndGetId("Milk", 3.99, 10);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, 1))
        );

        HttpStatus status = (HttpStatus) client()
                .post()
                .uri("/orders")
                .body(request)
                .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn404WhenOrderNotFound() {
        UUID randomId = UUID.randomUUID();

        HttpStatus status = (HttpStatus) user()
                .get()
                .uri("/orders/" + randomId)
                .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldFilterOrdersByStatus() {
        // given
        UUID productId = createProductAndGetId("Bread", 2.50, 10);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, 1))
        );

        user().post().uri("/orders").body(request).retrieve().toBodilessEntity();

        // when: listing all orders is an admin operation
        OrderResponse[] response = admin()
                .get()
                .uri("/orders?status=CREATED")
                .retrieve()
                .body(OrderResponse[].class);

        // then
        assertThat(response).isNotNull();
        assertThat(response.length).isGreaterThan(0);
        assertThat(response[0].status().name()).isEqualTo("CREATED");
    }

    @Test
    void shouldListOwnOrders() {
        UUID orderId = createOrderAndGetId();

        OrderResponse[] myOrders = user()
                .get()
                .uri("/orders/my")
                .retrieve()
                .body(OrderResponse[].class);

        assertThat(myOrders).isNotNull();
        assertThat(myOrders).anyMatch(o -> o.id().equals(orderId));
        assertThat(myOrders).allMatch(o -> o.customerEmail().equals(userEmail));
    }

    @Test
    void shouldSnapshotPriceAtPurchase() {
        UUID productId = createProductAndGetId("Milk", 3.99, 10);

        // change product price
        UpdateProductRequest patch = new UpdateProductRequest(null, BigDecimal.valueOf(9.99), null);
        admin().patch().uri("/products/" + productId).body(patch).retrieve().toBodilessEntity();

        // place order after price change
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, 1))
        );

        OrderResponse order = user()
                .post()
                .uri("/orders")
                .body(request)
                .retrieve()
                .body(OrderResponse.class);

        assertThat(order).isNotNull();
        assertThat(order.items()).hasSize(1);

        // snapshot should be 9.99 (because order placed after patch)
        assertThat(order.items().getFirst().unitPriceAtPurchase()).isEqualTo(BigDecimal.valueOf(9.99));
    }

    @Test
    void shouldPayCreatedOrder() {
        UUID orderId = createOrderAndGetId();

        OrderResponse paid = user()
                .post()
                .uri("/orders/" + orderId + "/pay")
                .retrieve()
                .body(OrderResponse.class);

        assertThat(paid).isNotNull();
        assertThat(paid.status().name()).isEqualTo("PAID");
    }

    @Test
    void shouldCancelCreatedOrder() {
        UUID orderId = createOrderAndGetId();

        OrderResponse cancelled = user()
                .post()
                .uri("/orders/" + orderId + "/cancel")
                .retrieve()
                .body(OrderResponse.class);

        assertThat(cancelled).isNotNull();
        assertThat(cancelled.status().name()).isEqualTo("CANCELLED");
    }

    @Test
    void shouldRestoreStockWhenOrderIsCancelled() {
        // given: product with 10 in stock and an order for 3
        UUID productId = createProductAndGetId("Milk", 3.99, 10);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, 3))
        );

        OrderResponse order = user()
                .post()
                .uri("/orders")
                .body(request)
                .retrieve()
                .body(OrderResponse.class);

        assertThat(order).isNotNull();

        ProductResponse afterOrder = client()
                .get()
                .uri("/products/" + productId)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(afterOrder).isNotNull();
        assertThat(afterOrder.available()).isEqualTo(7);

        // when: the order is cancelled
        user().post().uri("/orders/" + order.id() + "/cancel").retrieve().toBodilessEntity();

        // then: the reserved stock is returned
        ProductResponse afterCancel = client()
                .get()
                .uri("/products/" + productId)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(afterCancel).isNotNull();
        assertThat(afterCancel.available()).isEqualTo(10);
    }

    @Test
    void shouldReturn400WhenTransitionIsInvalid() {
        UUID orderId = createOrderAndGetId();

        // pay first
        user().post().uri("/orders/" + orderId + "/pay").retrieve().toBodilessEntity();

        // we only care whether the INVALID cancel publishes anything
        recordingPublisher.clear();

        ErrorResponse error = user()
                .post()
                .uri("/orders/" + orderId + "/cancel")
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    return res.bodyTo(ErrorResponse.class);
                });

        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.message()).contains("Invalid status transition");

        assertThat(recordingPublisher.getEvents()).isEmpty();
    }

    @Test
    void shouldReturn409WhenProductIsOutOfStock() {
        // given
        UUID productId = createProductAndGetId("Milk", 3.99, 1);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, 2))
        );

        // when
        ErrorResponse error = user()
                .post()
                .uri("/orders")
                .body(request)
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    return res.bodyTo(ErrorResponse.class);
                });

        // then
        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(409);
        assertThat(error.message()).contains("Out of stock");
    }

    @Test
    void shouldReturn409WhenProductIsInactive() {
        // given
        CreateProductRequest req = new CreateProductRequest(
                "Milk",
                BigDecimal.valueOf(3.99),
                10,
                false // inactive
        );

        ProductResponse product = admin()
                .post()
                .uri("/products")
                .body(req)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(product).isNotNull();

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(product.id(), 1))
        );

        // when
        ErrorResponse error = user()
                .post()
                .uri("/orders")
                .body(orderRequest)
                .exchange((req2, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    return res.bodyTo(ErrorResponse.class);
                });

        // then
        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(409);
        assertThat(error.message()).contains("inactive");
    }

    @Test
    void shouldNotDecreaseStockWhenOrderFails() {
        UUID productId = createProductAndGetId("Milk", 3.99, 1);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, 5))
        );

        user()
                .post()
                .uri("/orders")
                .body(request)
                .exchange((req, res) -> res.bodyTo(ErrorResponse.class));

        ProductResponse productAfter = client()
                .get()
                .uri("/products/" + productId)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(productAfter).isNotNull();
        assertThat(productAfter.available()).isEqualTo(1);
    }

    @TestConfiguration
    static class DomainEventsTestConfig {

        @Bean
        TestDomainEventPublisher recordingDomainEventPublisher() {
            return new TestDomainEventPublisher();
        }

        @Bean
        DomainEventPublisher domainEventPublisher(TestDomainEventPublisher recording) {
            return recording;
        }
    }
}
