package com.spring.userauth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.spring.userauth.dto.AuthResponse;
import com.spring.userauth.security.GoogleLoginHandlers;
import com.spring.userauth.service.GoogleAdminService;
import com.spring.userauth.service.GoogleCustomerService;
import com.spring.userauth.service.JwtService;
import jakarta.servlet.http.*;

@RestController
public class GoogleAuthController {
    private final GoogleAdminService admins;
    private final GoogleCustomerService customers;
    private final JwtService jwt;

    public GoogleAuthController(GoogleAdminService admins, GoogleCustomerService customers, JwtService jwt) {
        this.admins = admins;
        this.customers = customers;
        this.jwt = jwt;
    }

    @GetMapping("/api/auth/google/customer/start")
    public void startCustomerLogin(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        request.getSession(true).setAttribute(GoogleLoginHandlers.LOGIN_AUDIENCE, GoogleLoginHandlers.CUSTOMER_AUDIENCE);
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/api/auth/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        // Materialize the fresh session-backed token after OAuth authentication.
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @PostMapping("/api/auth/google/token")
    public AuthResponse token(Authentication authentication, HttpServletRequest request) {
        return exchangeToken(authentication, request, false);
    }

    @PostMapping("/api/auth/google/customer/token")
    public AuthResponse customerToken(Authentication authentication, HttpServletRequest request) {
        return exchangeToken(authentication, request, true);
    }

    private AuthResponse exchangeToken(Authentication authentication, HttpServletRequest request, boolean customerLogin) {
        HttpSession session = request.getSession(false);
        if (session == null || !(authentication instanceof OAuth2AuthenticationToken google)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google login session required");
        }
        boolean sessionIsCustomerLogin = GoogleLoginHandlers.CUSTOMER_AUDIENCE.equals(
                session.getAttribute(GoogleLoginHandlers.LOGIN_AUDIENCE));
        if (sessionIsCustomerLogin != customerLogin) {
            GoogleLoginHandlers.invalidate(request);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Google login session is for a different application");
        }
        // Consume once, including concurrent POSTs for the same session.
        synchronized (session) {
            try {
                Object completed = session.getAttribute(GoogleLoginHandlers.LOGIN_COMPLETED_AT);
                if (!(completed instanceof Long at) || System.currentTimeMillis() - at
                        >= GoogleLoginHandlers.SESSION_SECONDS * 1000L) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google login session expired");
                }
                var user = customerLogin ? customers.requireCustomer(google) : admins.requireAdmin(google);
                return new AuthResponse(jwt.generateToken(user));
            } catch (AccessDeniedException ex) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
            } catch (IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google login session already consumed");
            } finally {
                try { GoogleLoginHandlers.invalidate(request); } catch (IllegalStateException ignored) {
                    // Another request already invalidated this session.
                }
            }
        }
    }

    public record CsrfResponse(String headerName, String token) {}
}
