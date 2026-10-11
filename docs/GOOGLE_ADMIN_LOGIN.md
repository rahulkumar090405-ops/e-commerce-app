# Google administrator login: implementation and learning guide

This guide follows the actual repository: Java 21, Spring Boot **4.1.1**, JJWT
**0.12.6**, and the existing React/Vite admin application. No framework upgrade,
API gateway, refresh-token system, or separate authentication service was added.

Google proves an identity. Our database decides whether that identity belongs
to an enabled administrator. Only user-service issues our application JWT.

## 1. Prerequisites and the inspected starting point

| Component or endpoint | Exact local URL |
| --- | --- |
| Admin frontend | `http://localhost:5173` |
| Customer frontend | `http://localhost:5174` |
| User service | `http://localhost:8081` |
| Product service | `http://localhost:8082` |
| Order service | `http://localhost:8083` |
| Start Google login | `http://localhost:8081/oauth2/authorization/google` |
| Google redirect URI registered in Google Cloud | `http://localhost:8081/login/oauth2/code/google` |
| Frontend completion route | `http://localhost:5173/auth/callback` |
| Obtain CSRF token after Google login | `GET http://localhost:8081/api/auth/csrf` |
| Exchange temporary session for application token | `POST http://localhost:8081/api/auth/google/token` |

Use `localhost` consistently. `127.0.0.1` is a different origin and cookie host.
Vite's preview port, 4173, is not an allowed authentication origin. Use the dev
server on 5173 for this walkthrough.

Install JDK 21, Node/npm, and the MySQL client. Have MySQL databases `user_db`
and `order_db`, plus PostgreSQL `product_db`, ready with your own credentials.
The services retain their existing database URLs; Docker profiles use
`host.docker.internal`. This change does not provision databases.

The inspection found:

1. `AuthController` and `AuthService` implement password login using
   `AuthenticationManager`, `DaoAuthenticationProvider`, and the existing
   delegating password encoder. `AuthResponse` is a record containing `token`.
2. `JwtService` already issued a signed JWT containing the local email in `sub`,
   a local `userId`, and `roles`. It lacked issuer/audience checks and ignored its
   configured expiration when generating a token.
3. `User.roles` uses a many-to-many `user_roles` join table, with enum values such
   as `ROLE_ADMIN`. There was no enabled flag or Google identity link.
4. `RoleConfig` seeds role definitions. It **does not seed an admin user**.
   Existing local database accounts were not inspected or changed during this work.
5. An OAuth client dependency, environment-based client ID/secret, `openid`
   scope, and the Google backend redirect URI were already present. The OAuth
   security chain redirected to the temporary `GoogleLoginDebugController`.
6. The actual admin `Login.jsx` did not yet contain the Google button described
   in the request. The frontend stored application tokens in session storage.
7. user-service and order-service had custom JWT filters; product-service had
   no Spring Security configuration. Several admin operations also allowed USER.
8. There was no Flyway/Liquibase migration runner. Hibernate used `ddl-auto=update`.
   The frontend product/order URL defaults were reversed relative to the backend.

Before the change, password login returned a token directly; Google login
created a separate browser session and displayed identity information without
issuing an application token. This guide explains the completed connection.

The local Google Cloud client must be a **Web application** with the backend
redirect URI above, configured scopes, consent screen, and any required test
users. The source configuration is verified; your Google Cloud registration and
secret values still need manual verification in your own account.

## 2. Architecture and sequence

```mermaid
sequenceDiagram
    actor Admin
    participant React as admin-web :5173
    participant User as user-service :8081
    participant Google
    participant DB as MySQL user_db
    participant API as product/order APIs
    Admin->>React: Click Sign in with Google
    React->>User: Browser navigation to /oauth2/authorization/google
    User-->>Google: Redirect with state, nonce and authorization request
    Google-->>User: Redirect /login/oauth2/code/google with code and state
    User->>Google: Server exchanges code using OAuth client credentials
    Google-->>User: ID token and Google access token
    User->>User: Spring validates OIDC authentication
    User->>DB: Find canonical issuer + subject; check enabled and ROLE_ADMIN
    alt Unknown, disabled or non-admin identity
        User->>User: Invalidate temporary session
        User-->>React: Fixed callback with access_denied error only
    else Linked enabled administrator
        User-->>React: Fixed /auth/callback; short-lived HttpOnly session cookie
        React->>User: GET /api/auth/csrf with credentials include
        User-->>React: Session-backed CSRF token and header name
        React->>User: POST /api/auth/google/token with cookie and CSRF header
        User->>DB: Recheck identity, enabled flag and ADMIN role
        User->>User: Generate application JWT and invalidate session
        User-->>React: JSON containing token; never a token in a redirect URL
        React->>User: Bearer JWT GET /api/users/me
        User-->>React: Current local profile and roles
        React->>React: Keep JWT in memory; navigate /dashboard
        React->>API: Application JWT in Authorization header
        API->>API: Validate signature, expiry, issuer, own audience and permissions
    end
```

The OAuth browser cookie is used only for the Google login/CSRF/exchange chain.
The API chains explicitly use a `NullSecurityContextRepository`, so a stored
session authentication cannot authenticate an API request. Google access tokens
and Google ID tokens are never product/order bearer tokens.

## 3. Implementation stage: explicit local identity links

`User.java` now has `enabled`, defaulting to true to preserve existing accounts.
`GoogleIdentity.java` maps `google_identities` to a local `User`.
`GoogleIdentityRepository.findByIssuerAndSubject` performs the lookup.

The identity key is the pair `(issuer, subject)`, with the unique constraint
`uk_google_issuer_subject`. MySQL uses `ascii_bin` for exact, case-sensitive
identifier comparison. The foreign key requires a real local user. One identity
cannot be attached to two users; several explicitly approved identities can
refer to one user.

`GoogleAdminService.requireAdmin` reads claims from the already validated OIDC
ID token, not browser parameters or an email match:

```java
String issuer = principal.getIdToken().getClaimAsString("iss");
String subject = principal.getIdToken().getSubject();
User user = identities.findByIssuerAndSubject(GOOGLE_ISSUER, subject)
        .orElseThrow(GoogleAdminService::denied).getUser();
```

The surrounding implementation first requires the Google registration and an
OIDC principal. It only accepts Google's documented issuer spellings
`https://accounts.google.com` and `accounts.google.com`, normalizing the latter
to the former for storage. It then rejects disabled users and users without
the **database** `ROLE_ADMIN` role. Email is not used to create or resolve links.
Google login never creates a user, changes roles, or creates a link.

### Apply the database migration

The versioned script is:

`backend/user-service/src/main/resources/db/manual-migrations/V1__google_admin_identities.sql`

It is intentionally a **manual migration**, matching the repository's absence
of a migration runner. It is not applied automatically by Spring. Before using
Google login against your existing database:

1. Stop user-service and back up `user_db` with your normal database tools.
2. Open MySQL from the repository root; `-p` prompts privately for the password:

   ```powershell
   mysql -h localhost -u root -p user_db
   ```

3. In the MySQL prompt, apply the script using the actual workspace path:

   ```sql
   SOURCE C:/e-commerce-app/backend/user-service/src/main/resources/db/manual-migrations/V1__google_admin_identities.sql;
   SHOW COLUMNS FROM users LIKE 'enabled';
   SHOW CREATE TABLE google_identities;
   SHOW INDEX FROM google_identities;
   ```

4. Confirm the unique issuer/subject key, `ascii_bin` columns, and user foreign
   key before inserting a link or restarting the service.

The script guards the enabled-column addition and table creation, so it also
works if Hibernate's local `update` already created them. It reapplies the
required identity collation without deleting data. If starting from an empty
database, start user-service once with the configured environment to let the
existing Hibernate setup create `users` and `roles`, stop it, and apply the
script. Do not attempt linking before the migration is applied.

Testing exposed an existing mapping defect: `UserRole` also mapped `user_roles`
as a `BaseEntity`, adding a required `version` column that ordinary many-to-many
inserts did not supply. `UserRole` is now an unused legacy DTO rather than a JPA
entity; `User.roles` owns this table. For an existing database, the migration
gives the legacy `version` column a default if that column exists. It does not
drop existing join-table columns or memberships.

### Deliberately link your own Google account to a trusted admin

This is an operator procedure for local learning. There is no browser endpoint
that accepts a target user ID or role, and the diagnostic `/api/auth/google/me`
endpoint was removed.

1. Identify your **already trusted** local admin using an operator-controlled
   SQL connection, and verify password login still works for that account:

   ```sql
   SELECT u.id, u.email, u.enabled, r.name
   FROM users u
   JOIN user_roles ur ON ur.user_id = u.id
   JOIN roles r ON r.id = ur.role_id
   WHERE r.name = 'ROLE_ADMIN';
   ```

   Do not select password hashes. If there is no admin, this repository does
   not create one for you. Create your own password account through normal
   registration, then make a deliberate, operator-approved local seed step to
   grant that known account `ROLE_ADMIN`. Public registration itself cannot
   grant ADMIN. For your own trusted account only, use a verified numeric ID:

   ```sql
   SET @trusted_admin_id = 123; -- REPLACE with the verified local account ID
   INSERT INTO user_roles (user_id, role_id)
   SELECT @trusted_admin_id, r.id FROM roles r
   WHERE r.name = 'ROLE_ADMIN'
     AND EXISTS (SELECT 1 FROM users WHERE id = @trusted_admin_id AND enabled = TRUE)
     AND NOT EXISTS (SELECT 1 FROM user_roles ur
                     WHERE ur.user_id = @trusted_admin_id AND ur.role_id = r.id);
   ```

2. Run user-service under the IDE debugger with your configured environment.
   Set a breakpoint on `admins.requireAdmin(google)` in
   `GoogleLoginHandlers.success`. Sign in with **your own** Google account.
   Reaching this handler means Spring's OIDC authentication has succeeded.
3. Inspect only these expressions in the debugger:

   ```java
   ((org.springframework.security.oauth2.core.oidc.user.OidcUser)
       google.getPrincipal()).getIdToken().getClaimAsString("iss")
   ((org.springframework.security.oauth2.core.oidc.user.OidcUser)
       google.getPrincipal()).getIdToken().getSubject()
   ```

   Record the issuer and subject privately. Do not copy the ID token, access
   token, client secret, or session cookie into logs or this repository. Do not
   take the identity from an unverified token or an email lookup. Resume the
   debugger promptly: the unlinked login should be denied normally.
4. In your SQL session, use the verified local admin ID and your validated
   subject. Store the canonical issuer spelling shown below:

   ```sql
   SET @trusted_admin_id = 123; -- REPLACE with the verified local account ID
   SET @google_subject = 'REPLACE_WITH_YOUR_VALIDATED_SUBJECT';
   START TRANSACTION;
   INSERT INTO google_identities (issuer, subject, user_id)
   SELECT 'https://accounts.google.com', @google_subject, u.id
   FROM users u
   WHERE u.id = @trusted_admin_id AND u.enabled = TRUE
     AND EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                 WHERE ur.user_id = u.id AND r.name = 'ROLE_ADMIN');
   SELECT ROW_COUNT(); -- Expect exactly 1; otherwise inspect and ROLLBACK.
   COMMIT; -- Only after checking the result and identity/account selection.
   ```

   A duplicate identity fails with the unique constraint. Never use an upsert
   that moves an existing identity to another user. If an existing link needs
   correction, review it explicitly through the trusted operator connection.
5. Start a **new** Google login. That identity should now resolve to your local
   admin. Removing the link makes subsequent Google logins fail. To unlink your
   own account deliberately, delete only the reviewed issuer/subject row in SQL.

## 4. Implementation stage: administrator authorization

`GoogleAdminService` checks the account at login success and again during token
exchange. Disabling an account or removing ADMIN between these requests denies
the exchange. The exchange accepts no user-ID/role request contract.

Password authentication still uses the existing password encoder and generator.
`CustomUserDetailsService` now exposes the enabled flag, and an ADMIN password
login requires `ROLE_ADMIN`. Customer/password logins without the ADMIN login
type still work normally. USER and EMPLOYEE can no longer enter the admin
workspace merely because the old code allowed them. The public USER registration
option remains a non-admin role and no longer authorizes admin APIs.

`AdminUserController` and the user count operation now require ADMIN at both
the security-chain and method levels. The user-service JWT filter checks the
signed local user ID against the current account/email, rejects disabled/deleted
accounts, and intersects token roles with current database roles.

## 5. Implementation stage: OAuth session to application JWT

`SecurityConfig` now declares two ordered chains:

| Chain | Routes | Authentication and CSRF |
| --- | --- | --- |
| Order 1: OAuth | Google authorization/callback, `/api/auth/csrf`, `/api/auth/google/token` | Temporary session; CSRF enabled for POST |
| Order 2: APIs | Remaining routes | Stateless application bearer JWT; no session authentication |

The custom JWT filter appears only in the API chain. A disabled
`FilterRegistrationBean` prevents Boot from also registering this component as
a container-wide filter that could interfere with OAuth requests. The OAuth
chain disables saved-request redirects and uses fixed success/failure handlers.

`GoogleLoginHandlers` redirects successful authorized logins only to
`http://localhost:5173/auth/callback`. Denials/cancellation use that same fixed
route with small error codes, never identity details or tokens. Sessions have a
five-minute inactivity limit. The completion timestamp also imposes a fixed
five-minute exchange deadline, even if requests keep the session active.

The session cookie is `ECOMMERCE_OAUTH_SESSION`, HttpOnly, SameSite=Lax, and uses
cookie-only tracking. Local HTTP requires `secure=false`; change this for HTTPS
production. Google authorized-client tokens are held in the temporary session
repository, so invalidating the session removes that session's Google tokens.

`GoogleAuthController.csrf` returns the actual Spring token and `X-CSRF-TOKEN`
header name. Reading the token materializes Spring's deferred session-backed
CSRF value; its normal masking behavior remains enabled. The GET itself requires
the authenticated OAuth session. A bearer JWT cannot substitute for that session.

The token POST checks the server-side session authentication and completion
deadline, then rechecks the database before calling the existing generator:

```java
return new AuthResponse(jwt.generateToken(admins.requireAdmin(google)));
```

A `finally` block invalidates the temporary session after a successful exchange
or account/deadline rejection. Synchronizing on the session prevents concurrent
requests from consuming it twice. Missing/invalid CSRF is rejected by Spring
before the controller runs; obtain a fresh token or restart login. A replay
without valid session/CSRF cannot issue another JWT.

CORS is exact: the CSRF and Google-exchange endpoints accept only
`http://localhost:5173`, credentials, and the CSRF header. Other APIs accept the
two local frontend origins. There is no wildcard origin. A rejected preflight
is 403. A missing authenticated session is 401, while a session POST with missing
CSRF may be 403 first because CSRF runs before authorization.

## 6. Implementation stage: the React callback

`Login.jsx` now navigates the whole browser to the backend Google authorization
URL. `App.jsx` exposes `AuthCallback.jsx` outside the protected workspace routes.
The callback displays cancellation/access-denied/login-failure messages or calls
`AuthContext.completeGoogleLogin`.

The shared `GoogleSignInButton.jsx` presents the Google option above the email
form with a bundled official icon. Its non-submit button starts Google login
without requiring email/password fields, shows a redirect state, and prevents
duplicate clicks. The callback uses a progress card and offers a fresh Google
login or a return to password login when authentication fails. Incomplete CSRF
or application-token responses produce a readable error rather than leaving
the callback loading indefinitely.

`authApi.js` requests a fresh CSRF value and performs the cookie-authenticated POST:

```javascript
const csrfResponse = await fetch(base + "/api/auth/csrf", {
  credentials: "include", cache: "no-store",
});
const csrf = await csrfResponse.json();
const response = await fetch(base + "/api/auth/google/token", {
  method: "POST", credentials: "include", cache: "no-store",
  headers: { [csrf.headerName]: csrf.token },
});
```

The implementation also checks response status and CSRF header/token shape. The
exchange helper shares one pending promise, preventing React StrictMode's two
development effect executions from issuing two exchanges.

`AuthContext` accepts `{ token }`, stores it in the API module's memory, fetches
`/api/users/me` with that application bearer token, and checks the returned local
ADMIN role before navigating to `/dashboard`. Decoding the JWT in React is only
used to schedule UI expiry; the backend remains the verifier and authorization
authority.

`api.js` no longer writes authentication to session/local storage and removes
old admin session-storage entries. Ordinary API requests use `credentials:
"omit"` and the application Authorization header. Only the two OAuth exchange
requests use cookies. An API 401 clears the auth context; a 403 reports denied
permissions without pretending the token is malformed.

**Reload:** JavaScript memory disappears, so the protected route returns to
login. The consumed Google session cannot restore the token. Sign in again;
Google may reuse its own SSO session. **Expiry:** JWT lifetime defaults to fifteen
minutes, configured by `JWT_EXPIRATION_MS`. The UI timer clears authentication,
and servers independently reject expired JWTs. **Logout:** the endpoint returns
204, and the frontend discards its token. A copied bearer token remains valid
until expiry; no revocation or refresh-token subsystem was added.

## 7. Implementation stage: secure the microservice APIs

user-service continues issuing `{ "token": "..." }` for both password and Google
login. Its JWT includes local `sub`, `userId`, `roles`, `iat`, `exp`, the configured
issuer, and audiences for `user-service`, `product-service`, and `order-service`.
Every service requires its own audience.

All parsers verify the signature with the environment-provided shared key,
require the issuer/audience, reject expired or missing-expiry tokens, restrict
the algorithm to HS256, and require well-formed local identity/role claims.
The generator now actually uses its expiration setting. Old tokens lacking the
new claims are rejected; sign in again after updating all services.

```java
var signed = Jwts.parser().verifyWith(key).requireIssuer(issuer)
        .requireAudience("product-service")
        .build().parseSignedClaims(token);
```

The following code in each parser also checks `HS256` and the required claims.
This is our token validation; Google ID-token validation is handled separately
by Spring's OIDC authentication provider.

| Operation | Required access |
| --- | --- |
| User profile read/update | Valid application JWT for the current enabled user |
| Admin user list/count | ADMIN |
| Product/category catalog GET | Public; supplying an invalid bearer token still produces 401 |
| Product/category mutations and demo product writes | ADMIN |
| List all orders; read order by ID; update/delete orders | ADMIN |
| Create order | ADMIN, USER or CUSTOMER; local user ID comes from the signed JWT |
| Read `/api/orders/user/{userId}` | ADMIN, or allowed customer/user reading their own signed ID |

The order filter places the verified local ID in authentication details.
`OrderService` uses it for order ownership and rejects reading another user's
orders. Order creation sets the owner from authentication and a quantity of one
for the existing single-product endpoint. `ProductClient` now uses the actual
public `/api/products/{id}` path; its previous `/products/{id}` path was wrong.
The temporary order diagnostic endpoint was removed.

Missing/invalid/expired bearer credentials return 401. An authenticated token
with insufficient permissions returns 403. Cookies alone cannot authorize
protected API operations in any service. Product reads deliberately remain
public for the customer catalog and the existing order-service product lookup.

## 8. Security vocabulary in this implementation

| Term | What it does here |
| --- | --- |
| Authorization code (`code`) | Short-lived Google callback value exchanged by the backend; it is not our API token. It appears on the backend callback as required by the code flow. |
| OAuth `state` | Connects the callback to the authorization request and browser session, helping prevent login request forgery. Spring manages it. |
| OIDC `nonce` | Binds the returned ID token to the authentication request and helps prevent token replay. Spring generates and validates it. |
| Google ID token | Signed Google identity statement. Spring validates it, including its signature, expected issuer/client audience, time validity and nonce handling. Its `iss`/`sub` identify the local link. |
| Google access token | Credential for Google APIs; held temporarily server-side. It is not accepted by our microservices. |
| Temporary session | Backend-held OIDC authentication and login state, identified by an HttpOnly cookie. It lasts only until exchange/denial/timeout. |
| CSRF token | Session-bound request token returned by our GET and echoed in a header for our POST. It protects cookie-authenticated POST requests. It is separate from state and nonce. |
| Application JWT | user-service's signed bearer credential for our APIs, returned in JSON and kept in admin JavaScript memory. |
| ADMIN authorization | A local database role check. Proving a Google identity or possessing a valid non-admin JWT does not grant this permission. |

## 9. Environment variables and startup

Never put secrets in `VITE_*`, source files, terminal transcripts, screenshots,
or version control. Configure backend secrets privately in the IDE/terminal.
The modified datasource configuration also uses environment variables rather
than the previous checked-in password literals.

| Variable | Used by | Meaning/default |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | user-service | Your Google Web application OAuth client ID; required |
| `GOOGLE_CLIENT_SECRET` | user-service | Its private client secret; required |
| `JWT_SECRET` | All three services | The same private random key; required, at least 32 bytes of UTF-8 key material |
| `JWT_ISSUER` | All three services | Same issuer string; defaults to `http://localhost:8081` |
| `JWT_EXPIRATION_MS` | user-service | Access-token lifetime, defaults to `900000` |
| `DB_PASSWORD` | Each backend terminal | Password for that service's database user; required |
| `DB_USERNAME` | Backend terminals | Optional; defaults to root for MySQL or postgres for PostgreSQL |
| `DB_URL` | user-service prod profile | Required JDBC URL in the existing prod profile |
| `SPRING_PROFILES_ACTIVE` | Backend | user-service defaults to dev; use the existing docker/prod profiles only with their corresponding configuration |
| `VITE_USER_SERVICE_URL` | Frontends | Public `http://localhost:8081` |
| `VITE_PRODUCT_SERVICE_URL` | Frontends | Public `http://localhost:8082` |
| `VITE_ORDER_SERVICE_URL` | Frontends | Public `http://localhost:8083` |

For local learning, generate one random JWT key privately and configure the
same value in all three service launch environments. This PowerShell example
sets it without printing it; run the service launches from an environment that
inherits this value, or transfer it privately to each IDE launch configuration:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtRandom.Dispose()
```

Do not generate a different key in each service terminal. Restarting with a new
key invalidates earlier tokens. Configure Google client variables and database
passwords privately; no real values are included in this guide.

After applying the migration and configuring each terminal's environment, run
these commands in separate terminals from the repository root:

```powershell
Set-Location C:\e-commerce-app\backend\user-service
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location C:\e-commerce-app\backend\product-service
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location C:\e-commerce-app\backend\order-service
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location C:\e-commerce-app\frontend\admin-web
# Optional: copy .env.example to .env if you need explicit URL overrides.
npm.cmd install
npm.cmd run dev
```

The default admin URLs are already correct. Existing `.env` files must use the
ports above. The customer frontend's examples/defaults were corrected as well.

### Browser testing walkthrough (still required manually)

1. Open `http://localhost:5173/login`. First verify your trusted ADMIN password
   login. Reload the dashboard and confirm it returns to login, as documented.
2. Complete the deliberate Google identity-linking procedure above.
3. Click **Sign in with Google**, select your own linked account, and consent.
4. Observe the backend Google callback followed by the fixed frontend callback,
   then `/dashboard`. The frontend callback URL must contain no application JWT.
5. In browser Network tools, verify CSRF GET then token POST, cookie inclusion,
   `X-CSRF-TOKEN`, and the JSON token response. Do not share those credential
   values. Later API requests use Authorization and omit OAuth cookies.
6. Cancel the Google consent step to check the cancellation message. If Google
   reuses consent without a prompt, use its account/consent controls to exercise
   cancellation explicitly. Try an unlinked account to check denied access.
7. Locally disable your linked account, or remove its ADMIN role, then start a
   fresh login: it should fail. Restore the deliberate local seed afterward.
8. Test missing/wrong CSRF headers with a fresh OAuth session: POST must be 403.
   Test cookies without bearer credentials against a protected API: 401.
9. Test a valid customer JWT on admin operations: 403. Test expired/invalid JWTs:
   401. For quick local expiry testing, lower `JWT_EXPIRATION_MS`, restart
   user-service, and sign in for a new token; restore the setting afterward.

Dashboard/product/order screens still include demo data from the existing app.
Logging in does not turn those demo lists into complete live CRUD screens. Use
the real API endpoints when testing backend permissions.

## 10. Automated verification and exact changed files

Run each backend's existing and focused tests from its directory:

```powershell
.\mvnw.cmd test
```

The `test` profile supplies synthetic keys/client credentials and isolated H2
databases; tests do not use your Google account or local MySQL/PostgreSQL data.
The previously undiscovered user context test was replaced with a discoverable
test in the application package. Product/order context tests now select their
actual application classes instead of searching incorrect packages.

From admin-web:

```powershell
npm.cmd test
npm.cmd run build
```

Run `npm.cmd run build` in customer-web too, because its service URL defaults changed.

Focused tests cover linked/unknown/disabled/non-admin identities, wrong issuer,
duplicate links, server-side identity resolution despite supplied browser data,
CSRF failures, fixed redirects and cancellation, exchange deadline/session
consumption, OAuth/JWT isolation, password login, exact credentialed CORS,
malformed/unsigned/wrong-signature/expired/wrong-issuer/wrong-audience JWTs, role
removal, product writes, order administration, and order ownership.

The OIDC principals in tests are synthetic **already authenticated** principals.
These tests verify our handoff and authorization; they do not exercise Google
network endpoints or replace Spring's Google token-validation implementation.
H2 verifies the mapped unique identity constraint. The MySQL-specific migration
and its binary collation still require a real local MySQL run.

Verified on October 11, 2026:

| Check | Result |
| --- | --- |
| user-service Maven tests | 30 passed; 0 failures/errors/skipped |
| product-service Maven tests | 13 passed; 0 failures/errors/skipped |
| order-service Maven tests | 13 passed; 0 failures/errors/skipped |
| admin-web native Node tests | 6 passed |
| admin-web production build | Passed |
| Login/callback component render checks | Passed: login, loading, cancellation, denied access, failure |
| customer-web production build | Passed |
| Tracked-file whitespace check (`git diff --check`) | Passed |
| Live Google login/consent/cookie round trip | Not performed; manual walkthrough required |
| Migration against real MySQL | Not performed; manual migration and schema verification required |

In this execution environment, the cached Maven 3.9.16 executable was used with
`-o -B -Dmaven.repo.local=C:\e-commerce-app\.build-cache\repository test` in each
backend directory. The dependency cache was copied into the ignored workspace
cache because the default Maven home was not writable. Normal local development
can use the wrapper commands above. No backend database or Google credentials
were printed, and no changes were committed.

| Files/classes changed | Why |
| --- | --- |
| `backend/user-service/.../entity/User.java` | Add enabled-account state |
| `backend/user-service/.../entity/GoogleIdentity.java` and `repository/GoogleIdentityRepository.java` | Persist explicit unique Google issuer/subject links |
| `backend/user-service/.../entity/UserRole.java` | Remove conflicting join-table entity mapping |
| `backend/user-service/.../service/GoogleAdminService.java` | Resolve validated identities and require enabled DB ADMIN |
| `backend/user-service/.../security/GoogleLoginHandlers.java` | Fixed redirects, denial/cancellation cleanup, exchange deadline |
| `backend/user-service/.../controller/GoogleAuthController.java` | Fresh CSRF GET and one-use session-authenticated JWT POST |
| `backend/user-service/.../config/SecurityConfig.java` | Separate OAuth/API chains, CSRF, CORS, filter registration |
| `backend/user-service/.../service/JwtService.java`, `security/JwtAuthenticationFilter.java` | Application JWT generation/validation and current-account checks |
| `backend/user-service/.../service/AuthService.java`, `CustomUserDetailsService.java` | Preserve password flow and require enabled/admin accounts |
| `backend/user-service/.../controller/AuthController.java` | Stateless logout contract returns 204 |
| `backend/user-service/.../controller/AdminUserController.java`, `UserController.java` | ADMIN-only user administration |
| Deleted `backend/user-service/.../controller/GoogleLoginDebugController.java` | Remove temporary identity disclosure endpoint |
| `backend/user-service/src/main/resources/db/manual-migrations/V1__google_admin_identities.sql` | Versioned local MySQL migration |
| `backend/product-service/src/main/java/com/spring/product/security/{SecurityConfig,JwtService,JwtAuthenticationFilter}.java` | Add stateless catalog API security and admin-only writes |
| `backend/order-service/src/main/java/com/spring/order/security/{SecurityConfig,JwtService,JwtAuthenticationFilter}.java` | Strict JWT validation and stateless authorization |
| `backend/order-service/.../controller/OrderController.java`, `service/OrderService.java` | ADMIN operations, signed owner checks, remove diagnostics |
| `backend/order-service/.../service/client/ProductClient.java` | Use the actual public product lookup route |
| All backend `application*.properties` | Environment-based database passwords/usernames; JWT key/issuer; OAuth session settings in user-service |
| All three backend `pom.xml` | H2 test dependency; security/JJWT dependencies where absent; preserve versions |
| `frontend/admin-web/src/pages/AuthCallback.jsx`, `App.jsx`, `pages/Login.jsx` | Callback route and Google button |
| `frontend/admin-web/src/components/GoogleSignInButton.jsx`, `src/styles.css`, `public/brand/google-g.png` | Google button, redirect state, official icon, and callback loading/error presentation |
| `frontend/admin-web/README.md` | Current frontend login, token storage, service ports, and testing instructions |
| `frontend/admin-web/src/context/AuthContext.jsx`, `services/api.js`, `services/authApi.js` | Memory-only tokens, CSRF/session exchange, expiry/error handling |
| `frontend/admin-web/src/pages/Register.jsx` | Explain that USER registration does not grant ADMIN |
| Both frontends' `src/config/services.js` and `.env.example` | Correct product/order ports |
| `frontend/admin-web/package.json`, `tests/google-auth.test.js` | Native Node tests for the real API helpers |
| Backend test classes and `src/test/resources/application-test.properties` | Security scenarios and corrected isolated context checks |
| `.gitignore` | Ignore local build cache, build outputs/dependencies and private environment files |
| `README.md`, `docs/GOOGLE_ADMIN_LOGIN.md` | Discoverable startup/login learning instructions |

For user-service, `...` above means
`src/main/java/com/spring/userauth`; for order-service it means
`src/main/java/com/spring/order`. Test classes are `GoogleAdminServiceTests`,
`GoogleLoginSecurityTests`, `JwtValidationTests`, `UserAuthServiceApplicationTests`,
`ProductApiSecurityTests`, `ProductServiceApplicationTests`, `OrderApiSecurityTests`,
and `OrderServiceApplicationTests` in their respective services.

## 11. Troubleshooting

| Symptom | Checks and fixes |
| --- | --- |
| `redirect_uri_mismatch` | Google Cloud must contain exactly `http://localhost:8081/login/oauth2/code/google` for the selected Web client. Check scheme, host, port, path and trailing slash, then verify the client ID belongs to that registration. The React callback is a different URL. |
| CORS failure | Use admin origin 5173. Check preflight allows POST and X-CSRF-TOKEN, exact origin and credentials. Do not run this flow from preview port 4173 or 127.0.0.1. Verify VITE user-service URL. |
| Lost Google session / 401 on CSRF GET | Both exchange fetches need `credentials: "include"`. Check the HttpOnly cookie, consistent localhost host, five-minute timeout, and that user-service did not restart. HTTP local development cannot use a Secure-only cookie. Begin a fresh login. |
| 403 on token POST | Missing/stale/wrong CSRF token, disallowed origin, disabled/unlinked/non-admin local account. Fetch CSRF after authentication; login rotates it. A rejected account consumes the session. Do not disable CSRF to fix this. |
| Google account is correct but login is denied | Query the exact canonical issuer/subject link, enabled flag and ROLE_ADMIN membership. Matching email is intentionally insufficient. |
| 401 on APIs | Supply our application bearer JWT. Check the same JWT_SECRET and JWT_ISSUER across services, correct service audience, exp, and current enabled user where applicable. Old tokens and Google tokens are rejected. |
| 403 on APIs | The token authenticated but lacks the required role, or an order belongs to another user. Inspect the local role assignment deliberately; do not change roles in the browser. |
| Role registration fails on `user_roles.version` | Apply the migration's guarded legacy version-default repair; the conflicting UserRole entity mapping is removed. |
| Google redirects work but dashboard does not | Check CSRF GET, token POST, then bearer `/api/users/me`. Inspect the callback's visible error and Network status without sharing token values. |
| Maven tests do not find an application | Use the corrected test classes/packages from this implementation and JDK 21. Run Maven in the corresponding backend service folder. |
| Services cannot start | Configure required JWT_SECRET/Google variables and each service's DB_PASSWORD. Verify DB availability and ports 8081/8082/8083. The automated test profile does not need real secrets. |

## 12. Local limitations and production changes

This local flow uses HTTP, an in-process short-lived OAuth session, and a shared
HMAC key to reuse the existing JWT architecture. All services holding that key
can sign tokens. Production should use HTTPS/Secure cookies, environment-specific
fixed callback/origin configuration, managed secrets/key rotation, and preferably
asymmetric JWT signing so product/order services only have verification keys.

Production also needs a managed migration runner and an audited, privileged
identity-linking workflow with appropriate reauthentication. Review/disable
Hibernate schema auto-update, SQL logging and broad debug logging; run migrations
against backups/staging before deployment. Configure external URLs and trusted
proxy headers correctly. Avoid leaving an IDE debugger or identity diagnostics
accessible on a deployed service.

user-service checks current account state on each JWT request. Product/order
enforce roles contained in the signed token and cannot see immediate database
role changes. Their existing bearer credentials can therefore retain access
until the short token expiry. Immediate cross-service revocation would require
additional design (for example centralized revocation or authorization checks).
No such subsystem, refresh token, or gateway was added silently.

Memory-only storage limits token persistence; it does not make scripts immune to
XSS. Production still needs normal browser hardening and dependency/security
maintenance. A future refresh system would need its own rotation, revocation,
cookie/CSRF rules and tests; it is a separate scope from this completed login flow.

### Primary references

- [Spring Security filter-chain architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- [Spring Security CSRF handling](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security OAuth login configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html)
- [Google OpenID Connect issuer and subject guidance](https://developers.google.com/identity/openid-connect/openid-connect)
- [Google sign-in button branding and official assets](https://developers.google.com/identity/branding-guidelines)
- [JJWT parser and claim validation documentation](https://github.com/jwtk/jjwt#reading-a-jwt)

These sources explain the framework/protocol behavior; the examples and file
names above come from this repository's implementation.
