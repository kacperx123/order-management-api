package com.example.order_management_api;

import com.example.order_management_api.api.*;
import com.example.order_management_api.exception.ErrorResponse;
import com.example.order_management_api.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends PostgresTestBase {

    @LocalServerPort
    int port;

    private RestClient plainClient;

    private RestClient client() {
        if (plainClient == null) {
            plainClient = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return plainClient;
    }

    private RestClient withToken(String token) {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    @Test
    void shouldRegisterUserWithUserRole() {
        String email = uniqueEmail();

        ResponseEntity<UserResponse> response = client()
                .post()
                .uri("/auth/register")
                .body(new RegisterRequest(email, "password123"))
                .retrieve()
                .toEntity(UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UserResponse user = response.getBody();
        assertThat(user).isNotNull();
        assertThat(user.email()).isEqualTo(email);
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.id()).isNotNull();
    }

    @Test
    void shouldReturn409WhenEmailAlreadyRegistered() {
        String email = uniqueEmail();
        client().post().uri("/auth/register")
                .body(new RegisterRequest(email, "password123"))
                .retrieve().toBodilessEntity();

        ErrorResponse error = client()
                .post()
                .uri("/auth/register")
                .body(new RegisterRequest(email, "password123"))
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    return res.bodyTo(ErrorResponse.class);
                });

        assertThat(error).isNotNull();
        assertThat(error.message()).contains("already in use");
    }

    @Test
    void shouldLoginAndReceiveToken() {
        String email = uniqueEmail();
        client().post().uri("/auth/register")
                .body(new RegisterRequest(email, "password123"))
                .retrieve().toBodilessEntity();

        TokenResponse token = client()
                .post()
                .uri("/auth/login")
                .body(new LoginRequest(email, "password123"))
                .retrieve()
                .body(TokenResponse.class);

        assertThat(token).isNotNull();
        assertThat(token.token()).isNotBlank();
        assertThat(token.expiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    void shouldReturn401OnWrongPassword() {
        String email = uniqueEmail();
        client().post().uri("/auth/register")
                .body(new RegisterRequest(email, "password123"))
                .retrieve().toBodilessEntity();

        ErrorResponse error = client()
                .post()
                .uri("/auth/login")
                .body(new LoginRequest(email, "wrong-password"))
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    return res.bodyTo(ErrorResponse.class);
                });

        assertThat(error).isNotNull();
        assertThat(error.message()).contains("Invalid email or password");
    }

    @Test
    void shouldReturnCurrentUserProfile() {
        String email = uniqueEmail();
        String token = AuthTestSupport.registerAndLogin(client(), email, "password123");

        UserResponse me = withToken(token)
                .get()
                .uri("/users/me")
                .retrieve()
                .body(UserResponse.class);

        assertThat(me).isNotNull();
        assertThat(me.email()).isEqualTo(email);
        assertThat(me.role()).isEqualTo(Role.USER);
    }

    @Test
    void shouldReturn401ForAnonymousOrderAccess() {
        HttpStatus status = (HttpStatus) client()
                .get()
                .uri("/orders/my")
                .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldForbidUserFromCreatingProducts() {
        String token = AuthTestSupport.registerAndLogin(client(), uniqueEmail(), "password123");

        CreateProductRequest request = new CreateProductRequest(
                "Milk", BigDecimal.valueOf(3.99), 10, true);

        HttpStatus status = (HttpStatus) withToken(token)
                .post()
                .uri("/products")
                .body(request)
                .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldForbidUserFromAdminPanel() {
        String token = AuthTestSupport.registerAndLogin(client(), uniqueEmail(), "password123");

        HttpStatus status = (HttpStatus) withToken(token)
                .get()
                .uri("/admin/users")
                .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldAllowAdminToListUsers() {
        // make sure at least one regular user exists
        String email = uniqueEmail();
        AuthTestSupport.registerAndLogin(client(), email, "password123");

        String adminToken = AuthTestSupport.loginAdmin(client());

        UserResponse[] users = withToken(adminToken)
                .get()
                .uri("/admin/users")
                .retrieve()
                .body(UserResponse[].class);

        assertThat(users).isNotNull();
        assertThat(users).anyMatch(u -> u.email().equals(AuthTestSupport.ADMIN_EMAIL) && u.role() == Role.ADMIN);
        assertThat(users).anyMatch(u -> u.email().equals(email) && u.role() == Role.USER);
    }

    @Test
    void shouldForbidAccessToSomeoneElsesOrder() {
        // admin creates a product
        String adminToken = AuthTestSupport.loginAdmin(client());
        ProductResponse product = withToken(adminToken)
                .post()
                .uri("/products")
                .body(new CreateProductRequest("Milk", BigDecimal.valueOf(3.99), 10, true))
                .retrieve()
                .body(ProductResponse.class);
        assertThat(product).isNotNull();

        // user A places an order
        String tokenA = AuthTestSupport.registerAndLogin(client(), uniqueEmail(), "password123");
        OrderResponse order = withToken(tokenA)
                .post()
                .uri("/orders")
                .body(new CreateOrderRequest(List.of(new CreateOrderItemRequest(product.id(), 1))))
                .retrieve()
                .body(OrderResponse.class);
        assertThat(order).isNotNull();

        // user B cannot see or cancel it
        String tokenB = AuthTestSupport.registerAndLogin(client(), uniqueEmail(), "password123");

        HttpStatus getStatus = (HttpStatus) withToken(tokenB)
                .get()
                .uri("/orders/" + order.id())
                .exchange((req, res) -> res.getStatusCode());
        assertThat(getStatus).isEqualTo(HttpStatus.FORBIDDEN);

        HttpStatus cancelStatus = (HttpStatus) withToken(tokenB)
                .post()
                .uri("/orders/" + order.id() + "/cancel")
                .exchange((req, res) -> res.getStatusCode());
        assertThat(cancelStatus).isEqualTo(HttpStatus.FORBIDDEN);

        // but the admin can see it
        OrderResponse adminView = withToken(adminToken)
                .get()
                .uri("/orders/" + order.id())
                .retrieve()
                .body(OrderResponse.class);
        assertThat(adminView).isNotNull();
        assertThat(adminView.id()).isEqualTo(order.id());
    }
}
