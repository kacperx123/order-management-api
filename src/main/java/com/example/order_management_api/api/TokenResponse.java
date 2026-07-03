package com.example.order_management_api.api;

import java.time.Instant;

public record TokenResponse(
        String token,
        Instant expiresAt
) {}
