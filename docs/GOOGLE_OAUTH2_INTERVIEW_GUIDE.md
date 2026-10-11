# Google OAuth 2.0 / OpenID Connect: project and interview guide

This guide explains Google sign-in as implemented by this e-commerce project,
then connects that implementation to the concepts interviewers commonly ask
about. The customer storefront has both Google and email/password login. The
admin login currently uses email and password; do not assume its UI has Google
sign-in just because the backend contains Google administrator authorization
code.

## 1. The short interview answer

> The browser starts a Google sign-in by navigating to our backend. Spring
> Security starts the OpenID Connect authorization-code flow, stores the
> request state in a short-lived server session, and redirects the browser to
> Google. Google authenticates the person and redirects an authorization code
> to our backend callback. The backend exchanges that code directly with
> Google, validates the OIDC response, and identifies the account by Google's
> issuer and stable subject. For a verified customer identity, we find or
> create a customer account, then exchange the temporary OAuth session for
> our own short-lived application JWT. The frontend uses that JWT for our APIs.
> Google tokens are not our API credentials.

The important distinction is: **OAuth 2.0 delegates authorization; OpenID
Connect (OIDC) adds a standard identity layer.** A "Sign in with Google" flow
uses OIDC, including an ID token. OAuth access tokens and OIDC ID tokens have
different purposes.

## 2. What the project implements

| Component | Responsibility |
| --- | --- |
| Customer React/Vite app, port `5174` | Shows Google and password login; handles the final callback route; exchanges the temporary backend session for the app JWT |
| Spring Boot `user-service`, port `8081` | OAuth 2.0 client; owns the Google client secret, stateful OIDC session, account checks, and app-JWT issuance |
| Google | Authenticates the user and returns an authorization code and OIDC identity |
| MySQL `user_db` | Stores local users, roles, and Google identity links |
| Product/order services | Trust this application's signed JWT, not Google credentials |

The admin UI at port `5173` uses normal email/password login. The user-service
also has an administrator identity-check path, but that does not mean the admin
frontend currently exposes a Google login button.

### Main code locations

- Customer login button and password form:
  [`frontend/customer-web/src/pages/Login.jsx`](../frontend/customer-web/src/pages/Login.jsx)
- Customer completion screen:
  [`frontend/customer-web/src/pages/AuthCallback.jsx`](../frontend/customer-web/src/pages/AuthCallback.jsx)
- Customer OAuth start, CSRF retrieval, and session exchange:
  [`frontend/customer-web/src/services/authApi.js`](../frontend/customer-web/src/services/authApi.js)
- Customer token/profile state:
  [`frontend/customer-web/src/context/AuthContext.jsx`](../frontend/customer-web/src/context/AuthContext.jsx)
- Vite proxy for user-service and OAuth paths:
  [`frontend/customer-web/vite.config.js`](../frontend/customer-web/vite.config.js)
- Ordered OAuth and stateless API security chains:
  [`backend/user-service/src/main/java/com/spring/userauth/config/SecurityConfig.java`](../backend/user-service/src/main/java/com/spring/userauth/config/SecurityConfig.java)
- Start, CSRF, and one-time token-exchange endpoints:
  [`backend/user-service/src/main/java/com/spring/userauth/controller/GoogleAuthController.java`](../backend/user-service/src/main/java/com/spring/userauth/controller/GoogleAuthController.java)
- Fixed callback destinations and short-lived session handling:
  [`backend/user-service/src/main/java/com/spring/userauth/security/GoogleLoginHandlers.java`](../backend/user-service/src/main/java/com/spring/userauth/security/GoogleLoginHandlers.java)
- Verified identity lookup and customer provisioning:
  [`backend/user-service/src/main/java/com/spring/userauth/service/GoogleCustomerService.java`](../backend/user-service/src/main/java/com/spring/userauth/service/GoogleCustomerService.java)
- Persisted issuer/subject-to-local-user link:
  [`backend/user-service/src/main/java/com/spring/userauth/entity/GoogleIdentity.java`](../backend/user-service/src/main/java/com/spring/userauth/entity/GoogleIdentity.java)
- Local JWT creation:
  [`backend/user-service/src/main/java/com/spring/userauth/service/JwtService.java`](../backend/user-service/src/main/java/com/spring/userauth/service/JwtService.java)
- End-to-end OAuth/security tests:
  [`backend/user-service/src/test/java/com/spring/userauth/GoogleLoginSecurityTests.java`](../backend/user-service/src/test/java/com/spring/userauth/GoogleLoginSecurityTests.java)

## 3. End-to-end flow

```mermaid
sequenceDiagram
    actor Customer
    participant Browser as customer-web :5174
    participant Backend as user-service :8081
    participant Google
    participant DB as MySQL user_db
    Customer->>Browser: Click Continue with Google
    Browser->>Backend: GET /api/auth/google/customer/start
    Backend->>Backend: Mark temporary session audience = customer
    Backend-->>Browser: Redirect to /oauth2/authorization/google
    Browser->>Google: Authorization request with state and OIDC nonce
    Google-->>Backend: Redirect /login/oauth2/code/google with code and state
    Backend->>Google: Exchange code using backend client credentials
    Google-->>Backend: ID token and authorized-client response
    Backend->>Backend: Spring validates OIDC response
    Backend->>DB: Resolve issuer + subject; check verified email/account/role
    Backend-->>Browser: Redirect to fixed customer /auth/callback
    Browser->>Backend: GET /api/auth/csrf with temporary session cookie
    Backend-->>Browser: Session-bound CSRF token and header name
    Browser->>Backend: POST /api/auth/google/customer/token with cookie + CSRF
    Backend->>DB: Recheck customer identity and enabled account
    Backend->>Backend: Issue local JWT; invalidate temporary OAuth session
    Backend-->>Browser: JSON { token: application JWT }
    Browser->>Backend: GET /api/users/me with Bearer application JWT
    Backend-->>Browser: Current local profile and customer role
    Browser->>Browser: Store app session in sessionStorage
```

### Step-by-step explanation

1. **Start:** `getGoogleCustomerLoginUrl()` points the browser at
   `/api/auth/google/customer/start`. In development, the Vite proxy forwards
   user-service and OAuth paths to port `8081`.
2. **Mark the purpose:** The start endpoint creates a server-side session and
   marks it as a customer login. This audience marker prevents a customer
   session from being exchanged at an administrator endpoint, or vice versa.
3. **Authorize:** Spring Security's
   `/oauth2/authorization/google` endpoint constructs the request. The browser
   is redirected to Google. Spring correlates the request and callback using
   `state`; OIDC also uses a `nonce`.
4. **Backend callback:** Google returns the browser to the registered backend
   callback `http://localhost:8081/login/oauth2/code/google`. Spring exchanges
   the one-time authorization code with Google using the confidential client
   credentials on the server. Spring validates the OIDC response and creates an
   authenticated principal.
5. **Resolve a local account:** The success handler runs
   `GoogleCustomerService.requireCustomer`. The application authorizes a local
   customer based on the validated Google issuer and subject—not on a browser
   supplied email or role.
6. **Return to the frontend:** The backend redirects to a fixed URL,
   `http://localhost:5174/auth/callback`. It does not put a Google token or
   application JWT into the redirect URL.
7. **CSRF-protected exchange:** The callback screen fetches a session-bound
   CSRF token and posts to `/api/auth/google/customer/token`, including the
   session cookie and returned CSRF header. The backend checks the session
   audience and completion deadline and rechecks the customer.
8. **Issue our credential:** The backend returns its own JWT in a JSON body and
   invalidates the temporary OAuth session. The customer app fetches
   `/api/users/me`, checks `ROLE_CUSTOMER`, and stores its app session in
   tab-scoped `sessionStorage`.
9. **Use the API:** Later requests send `Authorization: Bearer <app JWT>`.
   Product/order services validate the application's signature, issuer,
   audience, expiry, and claims. They do not accept Google's access token or ID
   token as an application credential.

## 4. OAuth and OIDC vocabulary

| Term | Meaning in general | How it appears here |
| --- | --- | --- |
| OAuth 2.0 | Framework for obtaining delegated authorization | Spring Security's Google client performs the authorization-code flow |
| OpenID Connect | Identity layer on OAuth 2.0 | The `openid` scope requests an ID token and authenticated OIDC principal |
| Authorization code | Short-lived, one-time value returned through the browser | Google returns it to the **backend** callback; backend exchanges it server-to-server |
| `state` | Correlates authorization request and response; mitigates login CSRF | Spring creates/checks it using the temporary login session |
| `nonce` | Binds an ID token to the OIDC request; mitigates token replay | Managed by Spring's OIDC flow |
| ID token | Signed statement about an authenticated identity, for the OAuth client | Validated by Spring; issuer and subject resolve the Google identity |
| Access token | Credential for calling the provider's APIs | It is not the application's API JWT |
| `iss` | Identity provider that issued the identity | Canonical Google issuer stored with the link |
| `sub` | Stable identifier for a person within an issuer | Preferred external identity key; do not use email as the stable key |
| Application JWT | Credential issued by this application for this application's APIs | Returned by user-service after account checks |
| CSRF token | Protects cookie-authenticated state-changing requests | Required for the POST exchanging the temporary session |

The Google OAuth scopes configured for the client are `openid`, `profile`, and
`email`. Email is useful as profile data, but an email match alone is not proof
that a Google identity has already been linked to a local account.

## 5. Account creation, linking, and authorization

The customer policy in this repository is:

- An existing Google identity is found by `(issuer, subject)`.
- On first Google login, the ID token must have an allowed Google issuer, a
  nonblank subject/email, and `email_verified=true`.
- If no local user exists for the normalized email, user-service creates a
  customer user and gives it only `ROLE_CUSTOMER`.
- If an existing local user has that email but the Google identity is not
  linked, login is denied. This avoids silently taking over or linking an
  existing password account based only on an email collision.
- A disabled linked account or an identity without the customer role is denied.
- The server rechecks the account when exchanging the temporary session for the
  application JWT.

In an application that supports account linking, make linking an explicit,
authenticated flow: require a current local login and recent reauthentication,
complete Google verification, then persist the issuer/subject link with a
unique constraint and audit trail. Never accept a client-supplied role, user
ID, Google subject, or unverified token as proof of authority.

## 6. Configuration for local learning

The Spring OAuth client registration is configured in:

[`backend/user-service/src/main/resources/application.properties`](../backend/user-service/src/main/resources/application.properties)

The app expects:

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8081/login/oauth2/code/google
```

The local `dev` profile contains placeholders so the backend can start without
real Google credentials. **Those placeholder values are not valid Google
credentials and cannot complete sign-in.** Set `GOOGLE_CLIENT_ID` and
`GOOGLE_CLIENT_SECRET` in the user-service IDE launch configuration or a private
local environment. Never add the client secret to `VITE_*`, frontend source,
Git, screenshots, or browser storage.

For Google Cloud setup:

1. Create/select a Google Cloud project and configure the OAuth consent screen.
   Add test users if the app is in testing mode.
2. Create an OAuth client of type **Web application**.
3. Add this exact authorized redirect URI:
   `http://localhost:8081/login/oauth2/code/google`
4. Put the client ID and secret in the user-service runtime environment.
5. Start the user-service and customer Vite app. Open
   `http://localhost:5174/login` and select **Continue with Google**.

Do not confuse the Google redirect URI (backend callback on `8081`) with the
application's post-login frontend callback (`5174/auth/callback`). They are
different hops with different jobs. Use the exact same host spelling
(`localhost`) throughout local testing; cookies are host-scoped.

Local profile properties are in
[`application-dev.properties`](../backend/user-service/src/main/resources/application-dev.properties).
Production should use HTTPS, valid provider credentials, a strong private JWT
signing key, secure cookie settings, and fixed environment-specific redirect
URIs—not development placeholders.

## 7. Security design and interview talking points

### Why use authorization code with a backend?

The client secret stays on the server. The browser handles redirects but does
not exchange the code with Google. The backend also controls identity
validation, account provisioning, and issuance of the application's credential.
For a browser-only public client with no backend, Authorization Code + PKCE is
the usual pattern; never ship a confidential client secret to JavaScript.

### Why not use the Google email as the account key?

Email can change and is profile data. A validated issuer/subject pair names the
external identity. Local authorization is then based on a database relationship
and local roles, not on a provider email string or a claim from the browser.

### Why issue a separate application JWT?

The app's APIs need their own issuer, audience, roles, expiration, and local
user ID. A Google ID token is intended for the OAuth client; a Google access
token is for Google APIs. Treating either as a general app bearer token couples
your API authorization to the provider and gives the token the wrong audience.

### Why use a temporary session if the APIs are stateless?

The OAuth flow needs short-lived server-side state across redirects: the
authorization request, OIDC authentication, and the single-use exchange. The
business APIs remain stateless and accept only the app's bearer JWT. The two
security filter chains separate these concerns.

### Why does the exchange POST need CSRF protection?

It relies on a browser cookie carrying the temporary session. Browsers attach
cookies automatically, so the backend requires a session-bound CSRF token in a
custom header. CORS is not a replacement for CSRF protection.

### What does logout mean for a JWT?

This app's logout drops the local browser session; a stateless JWT is not
automatically revoked. It remains usable until expiry unless the design adds
revocation, rotation, or a server-side deny list. Keep access-token lifetimes
short and choose a revocation strategy based on risk.

## 7. Practical implementation recipe

For a new Spring Boot application, implement the flow in this order:

1. **Add the OAuth client dependency.** This project uses Spring Security's
   `spring-boot-starter-oauth2-client`; avoid hand-writing authorization-code
   parsing or ID-token signature validation.
2. **Register the provider client.** Put the client ID and secret in the
   backend's runtime environment, request `openid,profile,email`, and configure
   an exact backend callback URI. Never expose the client secret to the
   frontend.
3. **Configure the OAuth security chain.** Keep OAuth redirect and callback
   requests session-capable. Let Spring generate and validate `state` and OIDC
   `nonce`, exchange the code, and authenticate the principal.

   Simplified shape from this project's
   [`SecurityConfig`](../backend/user-service/src/main/java/com/spring/userauth/config/SecurityConfig.java):

   ```java
   @Bean
   @Order(1)
   SecurityFilterChain oauthChain(HttpSecurity http) throws Exception {
       http.securityMatcher(
                   "/oauth2/authorization/google",
                   "/login/oauth2/code/google",
                   "/api/auth/csrf",
                   "/api/auth/google/**")
           .sessionManagement(session -> session
               .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
           .authorizeHttpRequests(auth -> auth
               .requestMatchers(
                   "/oauth2/authorization/google",
                   "/login/oauth2/code/google",
                   "/api/auth/google/customer/start").permitAll()
               .anyRequest().authenticated())
           .oauth2Login(oauth -> oauth.successHandler(successHandler));
       return http.build();
   }
   ```

   This is only the OAuth chain outline. The project also has a separate,
   ordered, stateless API chain with its JWT filter. Keep CSRF protection
   enabled for the session-backed OAuth exchange POST.
4. **Decide local-account policy.** After Spring authenticates the provider,
   validate the expected issuer and nonblank subject. Resolve by the unique
   `(issuer, subject)` pair, enforce enabled state and local roles, and make
   first-time provisioning/linking a deliberate policy decision. Here, new
   verified identities may create `ROLE_CUSTOMER`; an existing email without
   an identity link is denied.
5. **Use a fixed success/failure handler.** Route to a preconfigured frontend
   callback. Do not put access tokens, ID tokens, client secrets, or the local
   JWT in a redirect URL. Keep the temporary session short-lived and bind it to
   the intended audience.
6. **Exchange the temporary session once.** The frontend obtains a CSRF token
   after redirect, then makes an authenticated session POST. The server checks
   the session, CSRF, deadline, audience, and current account again; it issues
   an application JWT and invalidates the OAuth session.
7. **Verify local authorization in the client.** The callback accepts the
   local JWT response, requests the current local profile, checks the expected
   role, and stores only the app session. Every API sends the local JWT as a
   bearer credential.
8. **Test failure paths, not just the happy path.** Cover denied consent,
   invalid state/nonce, unverified email, unknown or disabled accounts, wrong
   local role, account-email collision, wrong exchange audience, missing CSRF,
   expired/replayed session, expired JWT, and non-admin/customer API access.

Useful implementation rule: let a maintained OIDC client library validate
provider signatures, issuer, audience, timestamps, and nonce. Do not decode a
JWT and treat decoded JSON as proof that its signature is valid.

## 8. Local test and troubleshooting

The backend tests build synthetic, already-authenticated OIDC principals. They
verify local identity resolution and the session-to-JWT handoff; they do not
sign in to Google or test real Google credentials.

```powershell
Set-Location .\backend\user-service
.\mvnw.cmd test

Set-Location ..\..\frontend\customer-web
npm.cmd run build
```

| Symptom | Likely check |
| --- | --- |
| `redirect_uri_mismatch` | Google Cloud redirect URI must exactly match `http://localhost:8081/login/oauth2/code/google`. |
| Google rejects the login before returning | Confirm client ID/secret, consent-screen settings, test-user access, and requested scopes. |
| Frontend callback says login failed | Check user-service logs and the fixed callback URL; verify the audience, identity, and customer role rules. |
| CSRF or exchange POST returns 403 | Start a fresh flow; fetch CSRF after authentication and send the returned header with the session cookie. Do not reuse the one-time exchange. |
| CSRF GET / exchange returns 401 | The temporary session cookie may be missing/expired; use the same `localhost` host and start Google sign-in again. |
| `An account with this email already exists` | Sign in to that existing local account first; account linking is deliberately not automatic. |
| API returns 401 after successful provider login | Confirm the frontend received the app JWT, sends it as a Bearer token, and has not let it expire. Google tokens are not accepted by these APIs. |
| Local app starts but Google cannot authenticate | Check whether the runtime still uses the dev placeholder client ID/secret; set real credentials privately. |

## 9. Interview questions with concise answers

**Q: Is Google login OAuth 2.0 or OpenID Connect?**  
A: It uses OAuth 2.0 Authorization Code flow plus OIDC. OAuth delegates access;
OIDC standardizes authentication and provides the ID token.

**Q: What is the difference between an ID token and an access token?**  
A: The ID token communicates authentication claims to the client. The access
token authorizes calls to a resource server. They have different audiences and
must not be substituted for each other.

**Q: Why does the callback go to the backend instead of React?**  
A: This is a confidential backend client. The backend keeps the client secret,
exchanges the one-time code securely, validates OIDC, and applies local account
policy. A SPA-only design should use PKCE and must not contain a client secret.

**Q: What do `state` and `nonce` protect against?**  
A: `state` correlates the authorization response with the browser's request and
mitigates login CSRF. `nonce` binds the ID token to the OIDC request and helps
mitigate token replay. The app's CSRF token protects a different POST during
session exchange.

**Q: Which user field should identify a Google account?**  
A: The validated `(issuer, subject)` pair (`iss`, `sub`). Email is profile data,
not a safe external identity key.

**Q: How do you prevent a Google login from becoming an admin login?**  
A: Authentication by Google is not authorization. Resolve the identity to a
local account, check that it is enabled, and check its database role. This
customer flow provisions only `ROLE_CUSTOMER`.

**Q: Why not automatically link accounts with the same email?**  
A: It can create account takeover risks or unintended privilege association.
Require the user to authenticate to the existing local account and perform an
explicit, verified linking operation.

**Q: What is the purpose of the application JWT after Google authentication?**  
A: It represents the user's local identity and permissions to this application's
services. It carries app-specific issuer, audience, role, expiry, and user ID
claims, and is signed by the app.

**Q: Why is there still a session when the REST API uses JWT?**  
A: The redirect-based login needs temporary server-side OAuth state. That
short-lived session is used only to finish login and exchange for a JWT; normal
API requests are stateless.

**Q: What happens if the exchange request is replayed?**  
A: The application invalidates the temporary session after exchange and
checks a five-minute completion deadline. The session audience is also checked
so customer and admin exchanges cannot be mixed.

**Q: Is CORS enough to stop CSRF?**  
A: No. CORS controls which browser origins may read responses; CSRF exploits
ambient credentials such as cookies. The exchange POST additionally requires a
session-bound CSRF token.

**Q: Can the app revoke a JWT on logout?**  
A: Not with a purely stateless JWT by itself. This implementation clears the
browser token and relies on expiry; immediate revocation needs server-side
state, short-lived access tokens with refresh-token rotation, or another
revocation design.

**Q: What should change before production?**  
A: Use HTTPS and secure cookies, strict production redirect URIs, managed
secrets, a production JWT signing/rotation strategy, rate limiting and audit
logging, account-linking safeguards, monitoring, a revocation strategy, and
provider credentials scoped to the intended app. Do not use local defaults.

## 10. A 60-second whiteboard summary

```text
React button
  -> backend /start (create session; mark CUSTOMER)
  -> Google authorization (Spring-generated state + OIDC nonce)
  -> backend callback (code exchange; Spring validates OIDC)
  -> local account lookup/provision by validated issuer + subject
  -> fixed frontend callback (no tokens in URL)
  -> CSRF-protected one-time session exchange
  -> user-service application JWT
  -> verify local profile/ROLE_CUSTOMER
  -> Bearer JWT to this app's APIs
```

Keep three questions separate in an interview: **Who authenticated the
identity?** Google, through OIDC. **Which local account is it?** The app, through
the verified issuer/subject link. **What can the account do?** The app, through
its own roles and authorization rules.
