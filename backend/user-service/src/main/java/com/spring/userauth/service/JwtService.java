package com.spring.userauth.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.spring.userauth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiration;
    private final String issuer;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration,
                      @Value("${jwt.issuer}") String issuer) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.issuer = issuer;
    }

    public String generateToken(User user) {
        if (!user.isEnabled()) throw new IllegalArgumentException("Account is disabled");
        return Jwts.builder().issuer(issuer).subject(user.getEmail())
                .audience().add(List.of("user-service", "product-service", "order-service")).and()
                .claim("userId", user.getId())
                .claim("roles", user.getRoles().stream().map(role -> role.getName().name()).toList())
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public Claims extractAllClaims(String token) {
        var signed = Jwts.parser().verifyWith(key).requireIssuer(issuer)
                .requireAudience("user-service")
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

    public String extractEmail(String token) { return extractAllClaims(token).getSubject(); }

    public boolean isTokenValid(String token, UserDetails user) {
        return user.isEnabled() && user.getUsername().equals(extractEmail(token));
    }
}
