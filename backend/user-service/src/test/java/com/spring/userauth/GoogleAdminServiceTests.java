package com.spring.userauth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import com.spring.userauth.entity.*;
import com.spring.userauth.enumer.RoleName;
import com.spring.userauth.repository.GoogleIdentityRepository;
import com.spring.userauth.service.GoogleAdminService;

class GoogleAdminServiceTests {
    private final GoogleIdentityRepository identities = mock(GoogleIdentityRepository.class);
    private final GoogleAdminService admins = new GoogleAdminService(identities);

    static OAuth2AuthenticationToken google(String issuer, String subject) {
        var id = new OidcIdToken("synthetic-test-id-token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("iss", issuer, "sub", subject, "email", "matching-local-email@example.test"));
        var principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), id);
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    static User local(RoleName role, boolean enabled) {
        User user = new User(); user.setId(7L); user.setEmail("admin@example.test");
        user.setPassword("unused"); user.setEnabled(enabled);
        Role entity = new Role(); entity.setName(role); user.setRoles(Set.of(entity));
        return user;
    }

    @Test void linkedEnabledAdminSucceeds() {
        User user = local(RoleName.ROLE_ADMIN, true);
        when(identities.findByIssuerAndSubject(GoogleAdminService.GOOGLE_ISSUER, "subject"))
                .thenReturn(Optional.of(new GoogleIdentity(GoogleAdminService.GOOGLE_ISSUER, "subject", user)));
        assertSame(user, admins.requireAdmin(google(GoogleAdminService.GOOGLE_ISSUER, "subject")));
    }

    @Test void unknownIdentityFailsEvenWhenEmailMatches() {
        assertThrows(AccessDeniedException.class,
                () -> admins.requireAdmin(google(GoogleAdminService.GOOGLE_ISSUER, "unknown")));
        verify(identities).findByIssuerAndSubject(GoogleAdminService.GOOGLE_ISSUER, "unknown");
        verifyNoMoreInteractions(identities);
    }

    @Test void disabledAdminFails() { reject(RoleName.ROLE_ADMIN, false); }
    @Test void nonAdminFails() { reject(RoleName.ROLE_USER, true); }

    private void reject(RoleName role, boolean enabled) {
        when(identities.findByIssuerAndSubject(GoogleAdminService.GOOGLE_ISSUER, "subject"))
                .thenReturn(Optional.of(new GoogleIdentity(GoogleAdminService.GOOGLE_ISSUER, "subject", local(role, enabled))));
        assertThrows(AccessDeniedException.class,
                () -> admins.requireAdmin(google(GoogleAdminService.GOOGLE_ISSUER, "subject")));
    }

    @Test void wrongIssuerFailsBeforeDatabaseLookup() {
        assertThrows(AccessDeniedException.class, () -> admins.requireAdmin(google("https://other.example", "subject")));
        verifyNoInteractions(identities);
    }
}
