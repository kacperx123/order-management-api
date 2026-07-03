package com.example.order_management_api.security;

import com.example.order_management_api.api.TokenResponse;
import com.example.order_management_api.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.expiration-minutes:60}") long expirationMinutes
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public TokenResponse issueToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        return new TokenResponse(token, expiresAt);
    }
}
