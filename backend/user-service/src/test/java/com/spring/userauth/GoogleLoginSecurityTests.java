package com.spring.userauth;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.spring.userauth.entity.*;
import com.spring.userauth.enumer.RoleName;
import com.spring.userauth.repository.*;
import com.spring.userauth.security.GoogleLoginHandlers;
import com.spring.userauth.service.GoogleAdminService;
import com.spring.userauth.service.JwtService;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleLoginSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired GoogleIdentityRepository identities;
    @Autowired GoogleLoginHandlers handlers;
    @Autowired JwtService jwt;
    private final ObjectMapper json = new ObjectMapper();
    private User admin;

    @BeforeEach void seed() {
        identities.deleteAll(); users.deleteAll();
        admin = new User(); admin.setEmail("admin@example.test");
        admin.setPassword("{noop}test-password");
        admin.setRoles(Set.of(roles.findByName(RoleName.ROLE_ADMIN).orElseThrow()));
        admin = users.saveAndFlush(admin);
        identities.saveAndFlush(new GoogleIdentity("https://accounts.google.com", "linked", admin));
    }

    private OAuth2AuthenticationToken google(String subject) {
        return GoogleAdminServiceTests.google("https://accounts.google.com", subject);
    }

    private OAuth2AuthenticationToken googleCustomer(String subject, String email) {
        return googleCustomer(subject, email, true);
    }

    private OAuth2AuthenticationToken googleCustomer(String subject, String email, boolean emailVerified) {
        var idToken = new OidcIdToken("customer-test-token", java.time.Instant.now(),
                java.time.Instant.now().plusSeconds(300),
                Map.of(
                        "iss", "https://accounts.google.com",
                        "sub", subject,
                        "email", email,
                        "email_verified", emailVerified,
                        "given_name", "New",
                        "family_name", "Customer",
                        "name", "New Customer"));
        var principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    private MockHttpSession loginSession(String subject) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(google(subject)));
        session.setAttribute(GoogleLoginHandlers.LOGIN_COMPLETED_AT, System.currentTimeMillis());
        return session;
    }

    private String csrf(MockHttpSession session) throws Exception {
        var result = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }

    @Test void linkedAdminGetsApplicationJwtAndSessionIsConsumed() throws Exception {
        var session = loginSession("linked");
        var result = mvc.perform(post("/api/auth/google/token").session(session)
                .header("X-CSRF-TOKEN", csrf(session))
                .contentType("application/json").content("{\"userId\":999,\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = json.readTree(result.getResponse().getContentAsString()).get("token").asString();
        assertEquals(admin.getId(), jwt.extractAllClaims(token).get("userId", Long.class));
        assertTrue(session.isInvalid());
        mvc.perform(post("/api/auth/google/token").header("X-CSRF-TOKEN", "old-token"))
                .andExpect(status().isForbidden());
    }

    @Test void unknownIdentityFailsAndInvalidatesSession() throws Exception {
        var session = loginSession("unknown");
        mvc.perform(post("/api/auth/google/token").session(session).header("X-CSRF-TOKEN", csrf(session)))
                .andExpect(status().isForbidden());
        assertTrue(session.isInvalid());
    }

    @Test void disabledAccountIsRecheckedAtExchange() throws Exception {
        var session = loginSession("linked");
        admin.setEnabled(false); admin = users.saveAndFlush(admin);
        mvc.perform(post("/api/auth/google/token").session(session).header("X-CSRF-TOKEN", csrf(session)))
                .andExpect(status().isForbidden());
        assertTrue(session.isInvalid());
    }

    @Test void removedAdminRoleIsRecheckedAtExchange() throws Exception {
        var session = loginSession("linked");
        admin.setRoles(Set.of(roles.findByName(RoleName.ROLE_CUSTOMER).orElseThrow())); users.saveAndFlush(admin);
        mvc.perform(post("/api/auth/google/token").session(session).header("X-CSRF-TOKEN", csrf(session)))
                .andExpect(status().isForbidden());
    }

    @Test void missingAndInvalidCsrfAreRejected() throws Exception {
        var session = loginSession("linked"); csrf(session);
        mvc.perform(post("/api/auth/google/token").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/google/token").session(session).header("X-CSRF-TOKEN", "wrong"))
                .andExpect(status().isForbidden());
        assertFalse(session.isInvalid());
    }

    @Test void expiredLoginSessionFailsEvenWithValidCsrf() throws Exception {
        var session = loginSession("linked");
        session.setAttribute(GoogleLoginHandlers.LOGIN_COMPLETED_AT, System.currentTimeMillis() - 301000L);
        mvc.perform(post("/api/auth/google/token").session(session).header("X-CSRF-TOKEN", csrf(session)))
                .andExpect(status().isUnauthorized());
        assertTrue(session.isInvalid());
    }

    @Test void oauthSessionCannotAuthenticateStatelessApi() throws Exception {
        mvc.perform(get("/api/admin/users").session(loginSession("linked"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/csrf").header("Authorization", "Bearer " + jwt.generateToken(admin)))
                .andExpect(status().isUnauthorized());
    }

    @Test void userRoleAndInvalidJwtCannotReadAdminUsers() throws Exception {
        admin.setRoles(Set.of(roles.findByName(RoleName.ROLE_USER).orElseThrow())); users.saveAndFlush(admin);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + jwt.generateToken(admin)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());
    }

    @Test void passwordAdminLoginStillWorksAndUserIsDenied() throws Exception {
        String body = "{\"email\":\"admin@example.test\",\"password\":\"test-password\",\"loginType\":\"ADMIN\"}";
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        admin.setRoles(Set.of(roles.findByName(RoleName.ROLE_USER).orElseThrow())); users.saveAndFlush(admin);
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }

    @Test void googleSuccessRedirectIsFixedAndContainsNoToken() throws Exception {
        var request = new MockHttpServletRequest(); request.setSession(loginSession("linked"));
        var response = new MockHttpServletResponse();
        handlers.success(request, response, google("linked"));
        assertEquals(GoogleLoginHandlers.ADMIN_CALLBACK, response.getRedirectedUrl());
        assertEquals(300, request.getSession().getMaxInactiveInterval());
        request = new MockHttpServletRequest(); request.setSession(loginSession("unknown"));
        response = new MockHttpServletResponse();
        handlers.success(request, response, google("unknown"));
        assertEquals(GoogleLoginHandlers.ADMIN_CALLBACK + "?error=access_denied", response.getRedirectedUrl());
        assertNull(request.getSession(false));
    }

    @Test void customerStartMarksTheSessionAndRedirectsToGoogleOAuth() throws Exception {
        var result = mvc.perform(get("/api/auth/google/customer/start"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/oauth2/authorization/google"))
                .andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(GoogleLoginHandlers.CUSTOMER_AUDIENCE,
                session.getAttribute(GoogleLoginHandlers.LOGIN_AUDIENCE));
    }

    @Test void newVerifiedGoogleCustomerIsProvisionedWithCustomerRoleAndCustomerCallback() throws Exception {
        var request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(
                GoogleLoginHandlers.LOGIN_AUDIENCE, GoogleLoginHandlers.CUSTOMER_AUDIENCE);
        var response = new MockHttpServletResponse();

        handlers.success(request, response, googleCustomer("new-customer", "NEW.CUSTOMER@example.test"));

        assertEquals(GoogleLoginHandlers.CUSTOMER_CALLBACK, response.getRedirectedUrl());
        var customer = identities.findByIssuerAndSubject(
                GoogleAdminService.GOOGLE_ISSUER, "new-customer").orElseThrow().getUser();
        assertEquals("new.customer@example.test", customer.getEmail());
        assertEquals(Set.of(RoleName.ROLE_CUSTOMER),
                customer.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
        assertFalse(customer.getPassword().contains("customer-test-token"));
    }

    @Test void customerGoogleLoginDoesNotLinkAnExistingEmailOrAcceptUnverifiedEmail() throws Exception {
        var collisionRequest = new MockHttpServletRequest();
        collisionRequest.getSession(true).setAttribute(
                GoogleLoginHandlers.LOGIN_AUDIENCE, GoogleLoginHandlers.CUSTOMER_AUDIENCE);
        var collisionResponse = new MockHttpServletResponse();
        handlers.success(collisionRequest, collisionResponse,
                googleCustomer("email-collision", admin.getEmail()));
        assertEquals(GoogleLoginHandlers.CUSTOMER_CALLBACK + "?error=customer_access_denied",
                collisionResponse.getRedirectedUrl());
        assertTrue(identities.findByIssuerAndSubject(
                GoogleAdminService.GOOGLE_ISSUER, "email-collision").isEmpty());

        var unverifiedRequest = new MockHttpServletRequest();
        unverifiedRequest.getSession(true).setAttribute(
                GoogleLoginHandlers.LOGIN_AUDIENCE, GoogleLoginHandlers.CUSTOMER_AUDIENCE);
        var unverifiedResponse = new MockHttpServletResponse();
        handlers.success(unverifiedRequest, unverifiedResponse,
                googleCustomer("unverified-email", "unverified@example.test", false));
        assertEquals(GoogleLoginHandlers.CUSTOMER_CALLBACK + "?error=customer_access_denied",
                unverifiedResponse.getRedirectedUrl());
        assertTrue(identities.findByIssuerAndSubject(
                GoogleAdminService.GOOGLE_ISSUER, "unverified-email").isEmpty());
    }

    @Test void customerTokenExchangeRejectsAnAdminAudienceSession() throws Exception {
        var session = loginSession("linked");
        session.setAttribute(GoogleLoginHandlers.LOGIN_AUDIENCE, GoogleLoginHandlers.CUSTOMER_AUDIENCE);
        mvc.perform(post("/api/auth/google/token").session(session))
                .andExpect(status().isForbidden());
    }

    @Test void customerOAuthSessionExchangesForCustomerJwt() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(googleCustomer("token-customer", "token-customer@example.test")));
        session.setAttribute(GoogleLoginHandlers.LOGIN_AUDIENCE, GoogleLoginHandlers.CUSTOMER_AUDIENCE);
        session.setAttribute(GoogleLoginHandlers.LOGIN_COMPLETED_AT, System.currentTimeMillis());
        String csrfToken = csrf(session);

        var result = mvc.perform(post("/api/auth/google/customer/token")
                .session(session)
                .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        String token = json.readTree(result.getResponse().getContentAsString()).get("token").asString();
        assertEquals(List.of("ROLE_CUSTOMER"), jwt.extractAllClaims(token).get("roles", List.class));
        assertTrue(session.isInvalid());
    }

    @Test void credentialedCorsAndCsrfHeaderAreAllowedForBothFrontends() throws Exception {
        mvc.perform(options("/api/auth/google/token").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "X-CSRF-TOKEN"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/auth/google/token").header("Origin", "http://localhost:5174")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "X-CSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/auth/google/customer/token").header("Origin", "http://localhost:5174")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "X-CSRF-TOKEN"))
                .andExpect(status().isOk());
        mvc.perform(options("/api/auth/google/customer/token").header("Origin", "http://localhost:5175")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }

    @Test void oauthRedirectIsNotBlockedByJwtFilter() throws Exception {
        mvc.perform(get("/oauth2/authorization/google").header("Authorization", "Bearer invalid"))
                .andExpect(status().is3xxRedirection());
    }

    @Test void disabledUserJwtAndRemovedAdminRoleStopWorking() throws Exception {
        String token = jwt.generateToken(admin);
        admin.setEnabled(false); admin = users.saveAndFlush(admin);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        admin.setEnabled(true);
        admin.setRoles(Set.of(roles.findByName(RoleName.ROLE_USER).orElseThrow())); users.saveAndFlush(admin);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test void expiredApplicationJwtReturns401() throws Exception {
        var expired = new JwtService("test-only-key-012345678901234567890123456789", -1000L, "http://localhost:8081");
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + expired.generateToken(admin)))
                .andExpect(status().isUnauthorized());
    }

    @Test void cancelledGoogleLoginClearsSessionAndRedirectsToFrontend() throws Exception {
        var request = new MockHttpServletRequest(); request.setSession(loginSession("linked"));
        var response = new MockHttpServletResponse();
        handlers.failure(request, response, new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                new org.springframework.security.oauth2.core.OAuth2Error("access_denied")));
        assertEquals(GoogleLoginHandlers.ADMIN_CALLBACK + "?error=cancelled", response.getRedirectedUrl());
        assertNull(request.getSession(false));
    }

    @Test void duplicateIdentityIsRejectedByDatabase() {
        assertThrows(DataIntegrityViolationException.class,
                () -> identities.saveAndFlush(new GoogleIdentity("https://accounts.google.com", "linked", admin)));
    }
}
