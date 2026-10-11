package com.spring.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductApiSecurityTests {
    @Autowired MockMvc mvc;

    private String token(String role, Date expiry) {
        return Jwts.builder().issuer("http://localhost:8081").subject("user@example.test")
                .audience().add("product-service").and().claim("userId", 7L).claim("roles", List.of(role))
                .expiration(expiry).signWith(Keys.hmacShaKeyFor(
                        "test-only-key-012345678901234567890123456789".getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }

    @Test void publicCatalogIsReadable() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test void productAndCategoryWritesRequireAdmin() throws Exception {
        for (String path : List.of("/api/products", "/api/category", "/api/demo/products")) {
            mvc.perform(post(path).contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post(path).header("Authorization", "Bearer " + token("ROLE_CUSTOMER", new Date(System.currentTimeMillis()+60000)))
                    .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + token("ROLE_ADMIN", new Date(System.currentTimeMillis()+60000)))
                .contentType("application/json").content("{\"productName\":\"Test product\",\"price\":10,\"stock\":5}"))
                .andExpect(status().isCreated());
    }

    @Test void invalidAndExpiredJwtReturn401EvenOnPublicCatalog() throws Exception {
        mvc.perform(get("/api/products").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/products").header("Authorization", "Bearer " + token("ROLE_ADMIN", new Date(1))))
                .andExpect(status().isUnauthorized());
    }

    @Test void browserSessionDoesNotAuthorizeProductWrites() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        var admin = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new org.springframework.security.core.context.SecurityContextImpl(admin));
        mvc.perform(post("/api/products").session(session).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
