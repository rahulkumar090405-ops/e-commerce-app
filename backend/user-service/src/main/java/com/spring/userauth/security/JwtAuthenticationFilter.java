package com.spring.userauth.security;

import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.spring.userauth.service.JwtService;
import com.spring.userauth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserDetailsService users;
    private final UserRepository accounts;

    public JwtAuthenticationFilter(JwtService jwt, UserDetailsService users, UserRepository accounts) {
        this.jwt = jwt;
        this.users = users;
        this.accounts = accounts;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return "/api/auth/register".equals(path)
                || "/api/auth/login".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ")) throw new IllegalArgumentException("Bearer token required");
                Claims claims = jwt.extractAllClaims(header.substring(7));
                var account = accounts.findById(claims.get("userId", Long.class))
                        .orElseThrow(() -> new UsernameNotFoundException("Account no longer exists"));
                if (!account.getEmail().equals(claims.getSubject())) {
                    throw new IllegalArgumentException("Application identity no longer matches");
                }
                UserDetails user = users.loadUserByUsername(claims.getSubject());
                if (!user.isEnabled()) throw new IllegalArgumentException("Account disabled");
                // Removed DB roles no longer authorize requests to user-service.
                List<?> roles = (List<?>) claims.get("roles");
                var authorities = user.getAuthorities().stream()
                        .filter(authority -> roles.contains(authority.getAuthority())).toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, authorities));
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Invalid or expired application token\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
    
}
