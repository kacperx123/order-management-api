package com.example.order_management_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.example.order_management_api.api.CreateOrderRequest;
import com.example.order_management_api.api.CreateOrderItemRequest;
import com.example.order_management_api.api.OrderResponse;
import com.example.order_management_api.exception.ErrorResponse;

import java.util.List;

class OrderControllerIntegrationTest extends PostgresTestBase {

    @LocalServerPort
    int port;

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
    void shouldCreateOrder() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
                "test@example.com",
                List.of(new CreateOrderItemRequest("Milk", 2))
        );

        // when
        ResponseEntity<OrderResponse> response = client()
                .post()
                .uri("/orders")
                .body(request)
                .retrieve()
                .toEntity(OrderResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().customerEmail()).isEqualTo("test@example.com");
        assertThat(response.getBody().items()).hasSize(1);
    }

    @Test
    void shouldReturn404WhenOrderNotFound() {
        UUID randomId = UUID.randomUUID();

        try {
            client()
                    .get()
                    .uri("/orders/" + randomId)
                    .retrieve()
                    .body(OrderResponse.class);
        } catch (Exception ex) {
            assertThat(ex.getMessage()).contains("404");
        }
    }

    @Test
    void shouldFilterOrdersByStatus() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
                "filter@test.com",
                List.of(new CreateOrderItemRequest("Bread", 1))
        );

        client().post().uri("/orders").body(request).retrieve().toBodilessEntity();

        // when
        OrderResponse[] response = client()
                .get()
                .uri("/orders?status=CREATED")
                .retrieve()
                .body(OrderResponse[].class);

        // then
        assertThat(response).isNotNull();
        assertThat(response.length).isGreaterThan(0);
        assertThat(response[0].status().name()).isEqualTo("CREATED");
    }

    private UUID createOrderAndGetId() {
        CreateOrderRequest request = new CreateOrderRequest(
                "test@example.com",
                List.of(new CreateOrderItemRequest("Milk", 2))
        );

        OrderResponse created = client()
                .post()
                .uri("/orders")
                .body(request)
                .retrieve()
                .body(OrderResponse.class);

        assertThat(created).isNotNull();
        return created.id();
    }

    @Test
    void shouldPayCreatedOrder() {
        UUID id = createOrderAndGetId();

        OrderResponse paid = client()
                .post()
                .uri("/orders/" + id + "/pay")
                .retrieve()
                .body(OrderResponse.class);

        assertThat(paid).isNotNull();
        assertThat(paid.status().name()).isEqualTo("PAID");
    }

    @Test
    void shouldCancelCreatedOrder() {
        UUID id = createOrderAndGetId();

        OrderResponse cancelled = client()
                .post()
                .uri("/orders/" + id + "/cancel")
                .retrieve()
                .body(OrderResponse.class);

        assertThat(cancelled).isNotNull();
        assertThat(cancelled.status().name()).isEqualTo("CANCELLED");
    }

    @Test
    void shouldReturn400WhenTransitionIsInvalid() {
        UUID id = createOrderAndGetId();

        // pay first
        client().post().uri("/orders/" + id + "/pay").retrieve().toBodilessEntity();

        // then cancel should be invalid (PAID -> CANCELLED)
        ErrorResponse error = client()
                .post()
                .uri("/orders/" + id + "/cancel")
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    return res.bodyTo(ErrorResponse.class);
                });

        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.message()).contains("Invalid status transition");
    }
}
