package com.example.order_management_api.api;

import com.example.order_management_api.model.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Role role,
        Instant createdAt
) {}
