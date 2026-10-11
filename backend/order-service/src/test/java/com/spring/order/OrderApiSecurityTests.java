package com.spring.order;

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
class OrderApiSecurityTests {
    @Autowired MockMvc mvc;

    private String token(String role, Date expiry) {
        return Jwts.builder().issuer("http://localhost:8081").subject("user@example.test")
                .audience().add("order-service").and().claim("userId", 7L).claim("roles", List.of(role))
                .expiration(expiry).signWith(Keys.hmacShaKeyFor(
                        "test-only-key-012345678901234567890123456789".getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }

    @Test void orderAdministrationRequiresAdmin() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        String customer = token("ROLE_CUSTOMER", new Date(System.currentTimeMillis()+60000));
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + customer)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/orders/42").header("Authorization", "Bearer " + customer)).andExpect(status().isForbidden());
        mvc.perform(put("/api/orders/42").header("Authorization", "Bearer " + customer)
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token("ROLE_ADMIN", new Date(System.currentTimeMillis()+60000))))
                .andExpect(status().isOk());
    }

    @Test void customersCanOnlyReadTheirOwnOrders() throws Exception {
        String customer = token("ROLE_CUSTOMER", new Date(System.currentTimeMillis()+60000));
        mvc.perform(get("/api/orders/user/7").header("Authorization", "Bearer " + customer)).andExpect(status().isOk());
        mvc.perform(get("/api/orders/user/8").header("Authorization", "Bearer " + customer)).andExpect(status().isForbidden());
    }

    @Test void invalidAndExpiredJwtReturn401() throws Exception {
        mvc.perform(get("/api/orders").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token("ROLE_ADMIN", new Date(1))))
                .andExpect(status().isUnauthorized());
    }

    @Test void browserSessionDoesNotAuthorizeOrderApi() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        var admin = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new org.springframework.security.core.context.SecurityContextImpl(admin));
        mvc.perform(get("/api/orders").session(session)).andExpect(status().isUnauthorized());
    }
}
