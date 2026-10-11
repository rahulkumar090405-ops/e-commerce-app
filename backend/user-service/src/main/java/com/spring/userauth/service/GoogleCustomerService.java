package com.spring.userauth.service;

import java.util.Date;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.userauth.entity.GoogleIdentity;
import com.spring.userauth.entity.Role;
import com.spring.userauth.entity.User;
import com.spring.userauth.enumer.RoleName;
import com.spring.userauth.repository.GoogleIdentityRepository;
import com.spring.userauth.repository.RoleRepository;
import com.spring.userauth.repository.UserRepository;

@Service
public class GoogleCustomerService {
    private final GoogleIdentityRepository identities;
    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public GoogleCustomerService(
            GoogleIdentityRepository identities,
            RoleRepository roles,
            UserRepository users,
            PasswordEncoder passwordEncoder) {
        this.identities = identities;
        this.roles = roles;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User requireCustomer(OAuth2AuthenticationToken authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !"google".equals(authentication.getAuthorizedClientRegistrationId())
                || !(authentication.getPrincipal() instanceof OidcUser principal)) {
            throw denied();
        }

        String issuer = principal.getIdToken().getClaimAsString("iss");
        String subject = principal.getIdToken().getSubject();
        String email = principal.getIdToken().getClaimAsString("email");
        Boolean emailVerified = principal.getAttribute("email_verified");
        if (!(GoogleAdminService.GOOGLE_ISSUER.equals(issuer) || "accounts.google.com".equals(issuer))
                || subject == null || subject.isBlank()
                || email == null || email.isBlank()
                || !Boolean.TRUE.equals(emailVerified)) {
            throw denied();
        }

        var existingIdentity = identities.findByIssuerAndSubject(GoogleAdminService.GOOGLE_ISSUER, subject);
        if (existingIdentity.isPresent()) {
            User user = existingIdentity.get().getUser();
            if (!user.isEnabled() || user.getRoles().stream()
                    .noneMatch(role -> role.getName() == RoleName.ROLE_CUSTOMER)) {
                throw denied();
            }
            return user;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw new AccessDeniedException(
                    "An account with this email already exists. Sign in with that account before linking Google.");
        }

        Role customerRole = roles.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Customer role is not configured"));
        User user = new User();
        String firstName = principal.getClaimAsString("given_name");
        String lastName = principal.getClaimAsString("family_name");
        String displayName = principal.getClaimAsString("name");
        if (firstName == null || firstName.isBlank()) {
            firstName = displayName == null || displayName.isBlank() ? normalizedEmail : displayName.trim();
        }
        if (lastName == null) lastName = "";

        Date now = new Date();
        user.setCreatedDate(now);
        user.setUpdatedDate(now);
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setName(displayName == null || displayName.isBlank()
                ? (firstName + " " + lastName).trim()
                : displayName.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setRoles(Set.of(customerRole));
        user = users.save(user);
        identities.save(new GoogleIdentity(GoogleAdminService.GOOGLE_ISSUER, subject, user));
        return user;
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("A verified Google customer account is required");
    }
}
