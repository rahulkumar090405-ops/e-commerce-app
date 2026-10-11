package com.spring.order.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.issuer}") String issuer) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    public Claims extractAllClaims(String token) {
        var signed = Jwts.parser().verifyWith(key).requireIssuer(issuer)
                .requireAudience("order-service")
                .build().parseSignedClaims(token);
        if (!"HS256".equals(signed.getHeader().getAlgorithm())) {
            throw new UnsupportedJwtException("Only HS256 application tokens are accepted");
        }
        Claims claims = signed.getPayload();
        if (claims.getExpiration() == null || claims.getSubject() == null
                || claims.getSubject().isBlank() || claims.get("userId", Long.class) == null
                || !(claims.get("roles") instanceof List<?> roles)
                || roles.stream().anyMatch(role -> !(role instanceof String name) || !name.startsWith("ROLE_"))) {
            throw new MalformedJwtException("Required application claims are missing or invalid");
        }
        return claims;
    }
}
