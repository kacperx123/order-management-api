package com.example.order_management_api;

import com.example.order_management_api.api.CreateOrderItemRequest;
import com.example.order_management_api.api.CreateOrderRequest;
import com.example.order_management_api.api.CreateProductRequest;
import com.example.order_management_api.api.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "app.order.place.delay-ms=200")
class OrderOptimisticLockingIntegrationTest extends PostgresTestBase {

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

    private UUID createProductAndGetId(String name, double price, int initialStock) {
        CreateProductRequest req = new CreateProductRequest(
                name,
                BigDecimal.valueOf(price),
                initialStock,
                true
        );

        ProductResponse created = client()
                .post()
                .uri("/products")
                .body(req)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(created).isNotNull();
        return created.id();
    }

    @Test
    void shouldAllowOnlyOneBuyerToPurchaseLastItem() throws Exception {
        // given: only 1 item in stock
        UUID productId = createProductAndGetId("Milk", 3.99, 1);

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "test@example.com",
                List.of(new CreateOrderItemRequest(productId, 1))
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<HttpStatus> call = () -> {
            ready.countDown();

            boolean started = start.await(2, TimeUnit.SECONDS);
            assertThat(started).isTrue();

            return (HttpStatus) client()
                    .post()
                    .uri("/orders")
                    .body(orderRequest)
                    .exchange((_, res) -> res.getStatusCode());
        };

        HttpStatus s1;
        HttpStatus s2;

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<HttpStatus> f1 = pool.submit(call);
            Future<HttpStatus> f2 = pool.submit(call);

            boolean bothReady = ready.await(2, TimeUnit.SECONDS);
            assertThat(bothReady).isTrue();

            start.countDown();

            s1 = f1.get(10, TimeUnit.SECONDS);
            s2 = f2.get(10, TimeUnit.SECONDS);
        }

        // then: exactly one succeeds, one fails with conflict
        assertThat(List.of(s1, s2))
                .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
    }
}
