package com.spring.userauth.security;

import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;
import com.spring.userauth.service.GoogleAdminService;
import com.spring.userauth.service.GoogleCustomerService;
import jakarta.servlet.http.*;

@Component
public class GoogleLoginHandlers {
    public static final String ADMIN_CALLBACK = "http://localhost:5173/auth/callback";
    public static final String CUSTOMER_CALLBACK = "http://localhost:5174/auth/callback";
    public static final String LOGIN_AUDIENCE = "googleLoginAudience";
    public static final String CUSTOMER_AUDIENCE = "customer";
    public static final String LOGIN_COMPLETED_AT = "googleLoginCompletedAt";
    public static final int SESSION_SECONDS = 300;
    private final GoogleAdminService admins;
    private final GoogleCustomerService customers;

    public GoogleLoginHandlers(GoogleAdminService admins, GoogleCustomerService customers) {
        this.admins = admins;
        this.customers = customers;
    }

    public void success(HttpServletRequest request, HttpServletResponse response,
                        Authentication authentication) throws IOException {
        HttpSession session = request.getSession(false);
        boolean customerLogin = isCustomerLogin(session);
        String callback = callback(customerLogin);
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken google)) {
                throw new AccessDeniedException("Google login required");
            }
            if (customerLogin) customers.requireCustomer(google);
            else admins.requireAdmin(google);
            session = request.getSession();
            session.setMaxInactiveInterval(SESSION_SECONDS);
            session.setAttribute(LOGIN_COMPLETED_AT, System.currentTimeMillis());
            response.sendRedirect(callback);
        } catch (AccessDeniedException ex) {
            invalidate(request);
            response.sendRedirect(callback + "?error=" + (customerLogin ? "customer_access_denied" : "access_denied"));
        }
    }

    public void failure(HttpServletRequest request, HttpServletResponse response,
                        org.springframework.security.core.AuthenticationException exception) throws IOException {
        boolean customerLogin = isCustomerLogin(request.getSession(false));
        boolean cancelled = exception instanceof OAuth2AuthenticationException oauth
                && "access_denied".equals(oauth.getError().getErrorCode());
        invalidate(request);
        response.sendRedirect(callback(customerLogin) + "?error=" + (cancelled ? "cancelled" : "login_failed"));
    }

    private static boolean isCustomerLogin(HttpSession session) {
        return session != null && CUSTOMER_AUDIENCE.equals(session.getAttribute(LOGIN_AUDIENCE));
    }

    private static String callback(boolean customerLogin) {
        return customerLogin ? CUSTOMER_CALLBACK : ADMIN_CALLBACK;
    }

    public static void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
    }
}
