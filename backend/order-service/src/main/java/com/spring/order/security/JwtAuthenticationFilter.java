package com.spring.order.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("========== CUSTOM JWT FILTER ==========");
        System.out.println("URL: " + request.getRequestURI());

        String authHeader = request.getHeader("Authorization");

        System.out.println("AUTH HEADER EXISTS: " + (authHeader != null));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            System.out.println("NO BEARER TOKEN");

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            System.out.println("TOKEN RECEIVED");

            Claims claims = jwtService.extractAllClaims(token);

            String email = claims.getSubject();

            System.out.println("EMAIL: " + email);

            List<String> roles =
                    claims.get("roles", List.class);
            
            Long userId = claims.get("userId", Long.class);

            System.out.println("ROLES: " + roles);

            if (roles == null || roles.isEmpty()) {

                System.out.println("NO ROLES FOUND IN JWT");

            } else {

                List<SimpleGrantedAuthority> authorities =
                        roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList();

                System.out.println(
                        "AUTHORITIES: " + authorities);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                authorities
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

                System.out.println(
                        "SECURITY CONTEXT: "
                        + SecurityContextHolder
                                .getContext()
                                .getAuthentication());
            }

        } catch (Exception e) {

            System.out.println("========== JWT ERROR ==========");
            e.printStackTrace();
            System.out.println("==============================");
        }

        filterChain.doFilter(request, response);
    }
}