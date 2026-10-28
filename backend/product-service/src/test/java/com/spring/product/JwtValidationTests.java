package com.spring.product;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.spring.product.security.JwtService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

class JwtValidationTests {
    private static final String SECRET = "test-only-key-012345678901234567890123456789";
    private final JwtService jwt = new JwtService(SECRET, "http://localhost:8081");

    private JwtBuilder builder() {
        return Jwts.builder().issuer("http://localhost:8081").subject("admin@example.test")
                .audience().add("product-service").and()
                .claim("userId", 7L).claim("roles", List.of("ROLE_ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 60000));
    }
    private String sign(JwtBuilder builder) {
        return builder.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }
    @Test void validApplicationTokenPasses() { assertEquals(7L, jwt.extractAllClaims(sign(builder())).get("userId", Long.class)); }
    @Test void expiredTokenFails() { assertThrows(JwtException.class, () -> jwt.extractAllClaims(sign(builder().expiration(new Date(1))))); }
    @Test void wrongSignatureFails() {
        String token = builder().signWith(Keys.hmacShaKeyFor("another-test-key-0123456789012345678901234567".getBytes(StandardCharsets.UTF_8))).compact();
        assertThrows(JwtException.class, () -> jwt.extractAllClaims(token));
    }
    @Test void wrongIssuerFails() { assertThrows(JwtException.class, () -> jwt.extractAllClaims(sign(builder().issuer("https://accounts.google.com")))); }
    @Test void wrongAudienceFails() {
        assertThrows(JwtException.class, () -> jwt.extractAllClaims(sign(builder().audience().clear().add("another-service").and())));
    }
    @Test void missingExpiryFails() { assertThrows(JwtException.class, () -> jwt.extractAllClaims(sign(builder().expiration(null)))); }
    @Test void malformedRolesFail() { assertThrows(JwtException.class, () -> jwt.extractAllClaims(sign(builder().claim("roles", List.of(1))))); }
    @Test void unsignedTokenFails() { assertThrows(JwtException.class, () -> jwt.extractAllClaims(builder().compact())); }
}
