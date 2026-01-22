package com.example.order_management_api;

import com.example.order_management_api.api.CreateProductRequest;
import com.example.order_management_api.api.ProductResponse;
import com.example.order_management_api.api.UpdateProductRequest;
import com.example.order_management_api.api.UpdateStockRequest;
import com.example.order_management_api.event.model.ProductCreatedEvent;
import com.example.order_management_api.event.model.StockAdjustedEvent;
import com.example.order_management_api.event.publisher.DomainEventPublisher;
import com.example.order_management_api.exception.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIntegrationTest extends PostgresTestBase {

    @LocalServerPort
    int port;

    private RestClient restClient;

    @Autowired
    TestDomainEventPublisher recordingPublisher;

    private RestClient client() {
        if (restClient == null) {
            restClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return restClient;
    }

    private UUID createProductAndGetId(String name, BigDecimal price, int initialStock) {
        CreateProductRequest request = new CreateProductRequest(
                name,
                price,
                initialStock,
                true
        );

        ProductResponse created = client()
                .post()
                .uri("/products")
                .body(request)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        return created.id();
    }

    @BeforeEach
    void beforeEach() {
        recordingPublisher.clear();
    }


    @Test
    void shouldCreateProduct() {
        CreateProductRequest request = new CreateProductRequest(
                "Milk",
                BigDecimal.valueOf(3.99),
                10,
                true
        );

        ProductResponse body = client()
                .post()
                .uri("/products")
                .body(request)
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    return res.bodyTo(ProductResponse.class);
                });

        assertThat(body).isNotNull();
        assertThat(body.name()).isEqualTo("Milk");
        assertThat(body.price()).isEqualTo(BigDecimal.valueOf(3.99));
        assertThat(body.active()).isTrue();
        assertThat(body.available()).isEqualTo(10);
        assertThat(body.reserved()).isEqualTo(0);
        assertThat(body.createdAt()).isNotNull();
    }

    @Test
    void shouldGetProductById() {
        UUID id = createProductAndGetId("Bread", BigDecimal.valueOf(2.50), 5);

        ProductResponse found = client()
                .get()
                .uri("/products/" + id)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(found).isNotNull();
        assertThat(found.id()).isEqualTo(id);
        assertThat(found.name()).isEqualTo("Bread");
        assertThat(found.available()).isEqualTo(5);
    }

    @Test
    void shouldListProducts() {
        createProductAndGetId("Milk", BigDecimal.valueOf(3.99), 10);
        createProductAndGetId("Bread", BigDecimal.valueOf(2.50), 5);

        ProductResponse[] all = client()
                .get()
                .uri("/products")
                .retrieve()
                .body(ProductResponse[].class);

        assertThat(all).isNotNull();
        assertThat(all.length).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldFilterProductsByActive() {
        // active=true
        createProductAndGetId("ActiveOne", BigDecimal.valueOf(1.00), 1);

        // active=false
        CreateProductRequest inactiveReq = new CreateProductRequest(
                "InactiveOne",
                BigDecimal.valueOf(1.00),
                1,
                false
        );

        client().post().uri("/products").body(inactiveReq).retrieve().toBodilessEntity();

        ProductResponse[] activeOnly = client()
                .get()
                .uri("/products?active=true")
                .retrieve()
                .body(ProductResponse[].class);

        assertThat(activeOnly).isNotNull();
        assertThat(activeOnly.length).isGreaterThan(0);
        assertThat(activeOnly).allMatch(ProductResponse::active);
    }

    @Test
    void shouldPatchProduct() {
        UUID id = createProductAndGetId("Milk", BigDecimal.valueOf(3.99), 10);

        UpdateProductRequest patch = new UpdateProductRequest(
                "Milk 2.0",
                BigDecimal.valueOf(4.50),
                false
        );

        ProductResponse updated = client()
                .patch()
                .uri("/products/" + id)
                .body(patch)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(updated).isNotNull();
        assertThat(updated.id()).isEqualTo(id);
        assertThat(updated.name()).isEqualTo("Milk 2.0");
        assertThat(updated.price()).isEqualTo(BigDecimal.valueOf(4.50));
        assertThat(updated.active()).isFalse();
        assertThat(updated.available()).isEqualTo(10); // stock unchanged
    }

    @Test
    void shouldUpdateStockByDelta() {
        UUID id = createProductAndGetId("Milk", BigDecimal.valueOf(3.99), 10);

        UpdateStockRequest request = new UpdateStockRequest(-3, null);

        ProductResponse updated = client()
                .post()
                .uri("/products/" + id + "/stock")
                .body(request)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(updated).isNotNull();
        assertThat(updated.available()).isEqualTo(7);
    }

    @Test
    void shouldUpdateStockBySetTo() {
        UUID id = createProductAndGetId("Milk", BigDecimal.valueOf(3.99), 10);

        UpdateStockRequest request = new UpdateStockRequest(null, 25);

        ProductResponse updated = client()
                .post()
                .uri("/products/" + id + "/stock")
                .body(request)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(updated).isNotNull();
        assertThat(updated.available()).isEqualTo(25);
    }

    @Test
    void shouldReturn409WhenStockWouldGoBelowZero() {
        UUID id = createProductAndGetId("Milk", BigDecimal.valueOf(3.99), 2);

        UpdateStockRequest request = new UpdateStockRequest(-5, null);

        ErrorResponse error = client()
                .post()
                .uri("/products/" + id + "/stock")
                .body(request)
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    return res.bodyTo(ErrorResponse.class);
                });

        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(409);
        assertThat(error.message()).contains("Insufficient stock");
    }

    @Test
    void shouldReturn404WhenProductNotFound() {
        UUID randomId = UUID.randomUUID();

        ErrorResponse error = client()
                .get()
                .uri("/products/" + randomId)
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    return res.bodyTo(ErrorResponse.class);
                });

        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(404);
        assertThat(error.message()).contains("Product " + randomId + " not found");
    }

    @Test
    void shouldReturn400WhenUpdateStockRequestIsInvalid() {
        UUID id = createProductAndGetId("Milk", BigDecimal.valueOf(3.99), 10);

        // invalid: both delta and setTo provided
        UpdateStockRequest invalid = new UpdateStockRequest(-1, 5);

        ErrorResponse error = client()
                .post()
                .uri("/products/" + id + "/stock")
                .body(invalid)
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    return res.bodyTo(ErrorResponse.class);
                });

        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.message()).contains("Exactly one of delta or setTo must be provided");
    }

    @Test
    void shouldReturn400WhenCreateProductValidationFails() {
        // name blank + price null
        CreateProductRequest invalid = new CreateProductRequest(
                "",
                null,
                0,
                true
        );

        ErrorResponse error = client()
                .post()
                .uri("/products")
                .body(invalid)
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    return res.bodyTo(ErrorResponse.class);
                });

        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.message()).contains(":");
    }

    @Test
    void shouldPublishEventsWhenProductIsCreated() {
        CreateProductRequest request = new CreateProductRequest(
                "Milk",
                BigDecimal.valueOf(3.99),
                10,
                true
        );

        ProductResponse created = client()
                .post()
                .uri("/products")
                .body(request)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(created).isNotNull();

        assertThat(recordingPublisher.getEvents()).hasSize(2);

        assertThat(recordingPublisher.getEvents())
                .anySatisfy(e -> {
                    assertThat(e).isInstanceOf(ProductCreatedEvent.class);
                    ProductCreatedEvent ev = (ProductCreatedEvent) e;
                    assertThat(ev.productId()).isEqualTo(created.id());
                    assertThat(ev.name()).isEqualTo("Milk");
                    assertThat(ev.price()).isEqualTo(BigDecimal.valueOf(3.99));
                });

        assertThat(recordingPublisher.getEvents())
                .anySatisfy(e -> {
                    assertThat(e).isInstanceOf(StockAdjustedEvent.class);
                    StockAdjustedEvent ev = (StockAdjustedEvent) e;
                    assertThat(ev.productId()).isEqualTo(created.id());
                    assertThat(ev.previousAvailable()).isEqualTo(0);
                    assertThat(ev.newAvailable()).isEqualTo(10);
                    assertThat(ev.reserved()).isEqualTo(0);
                });
    }

    @Test
    void shouldPublishStockAdjustedEventWhenStockIsUpdated() {
        UUID id = createProductAndGetId("Milk", BigDecimal.valueOf(3.99), 10);
        recordingPublisher.clear();

        UpdateStockRequest request = new UpdateStockRequest(-3, null);

        ProductResponse updated = client()
                .post()
                .uri("/products/" + id + "/stock")
                .body(request)
                .retrieve()
                .body(ProductResponse.class);

        assertThat(updated).isNotNull();
        assertThat(updated.available()).isEqualTo(7);

        assertThat(recordingPublisher.getEvents()).hasSize(1);
        assertThat(recordingPublisher.getEvents().getFirst()).isInstanceOf(StockAdjustedEvent.class);

        StockAdjustedEvent ev = (StockAdjustedEvent) recordingPublisher.getEvents().getFirst();
        assertThat(ev.productId()).isEqualTo(id);
        assertThat(ev.previousAvailable()).isEqualTo(10);
        assertThat(ev.newAvailable()).isEqualTo(7);
        assertThat(ev.reserved()).isEqualTo(0);
    }

    @TestConfiguration
    static class DomainEventsTestConfig {

        @Bean
        TestDomainEventPublisher recordingDomainEventPublisher() {
            return new TestDomainEventPublisher();
        }

        @Bean
        @Primary
        DomainEventPublisher domainEventPublisher(TestDomainEventPublisher recording) {
            return recording;
        }


    }
}

