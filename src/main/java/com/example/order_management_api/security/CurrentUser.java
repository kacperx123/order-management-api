package com.example.order_management_api.security;

import com.example.order_management_api.model.Role;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Identity of the authenticated caller, resolved from the JWT in controllers
 * and passed down to services for ownership checks.
 */
public record CurrentUser(UUID id, String email, Role role) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                Role.valueOf(jwt.getClaimAsString("role"))
        );
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
