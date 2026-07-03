package com.example.order_management_api;

import com.example.order_management_api.api.LoginRequest;
import com.example.order_management_api.api.RegisterRequest;
import com.example.order_management_api.api.TokenResponse;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP-level auth helpers for integration tests. The admin account is seeded
 * by AdminUserInitializer from application.properties defaults.
 */
public final class AuthTestSupport {

    public static final String ADMIN_EMAIL = "admin@example.com";
    public static final String ADMIN_PASSWORD = "admin123";

    private AuthTestSupport() {
    }

    public static String registerAndLogin(RestClient client, String email, String password) {
        client.post()
                .uri("/auth/register")
                .body(new RegisterRequest(email, password))
                .retrieve()
                .toBodilessEntity();
        return login(client, email, password);
    }

    public static String login(RestClient client, String email, String password) {
        TokenResponse token = client.post()
                .uri("/auth/login")
                .body(new LoginRequest(email, password))
                .retrieve()
                .body(TokenResponse.class);
        assertThat(token).isNotNull();
        return token.token();
    }

    public static String loginAdmin(RestClient client) {
        return login(client, ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
