package com.spring.userauth.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.spring.userauth.entity.User;
import com.spring.userauth.enumer.RoleName;
import com.spring.userauth.repository.GoogleIdentityRepository;

@Service
public class GoogleAdminService {
    public static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private final GoogleIdentityRepository identities;

    public GoogleAdminService(GoogleIdentityRepository identities) {
        this.identities = identities;
    }

    @Transactional(readOnly = true)
    public User requireAdmin(OAuth2AuthenticationToken authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !"google".equals(authentication.getAuthorizedClientRegistrationId())
                || !(authentication.getPrincipal() instanceof OidcUser principal)) {
            throw denied();
        }
        // Read validated ID-token claims, never UserInfo email or browser input.
        String issuer = principal.getIdToken().getClaimAsString("iss");
        String subject = principal.getIdToken().getSubject();
        if (!(GOOGLE_ISSUER.equals(issuer) || "accounts.google.com".equals(issuer))
                || subject == null || subject.isBlank()) {
            throw denied();
        }
        // Google documents these two equivalent issuer spellings; store one form.
        User user = identities.findByIssuerAndSubject(GOOGLE_ISSUER, subject)
                .orElseThrow(GoogleAdminService::denied).getUser();
        if (!user.isEnabled() || user.getRoles().stream()
                .noneMatch(role -> role.getName() == RoleName.ROLE_ADMIN)) {
            throw denied();
        }
        return user;
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("A linked, enabled administrator account is required");
    }
}
